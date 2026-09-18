package com.example.security;

import com.example.pojo.Result;
import com.example.utils.JwtUtil;
import com.example.utils.ThreadLocalUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, TokenSessionService tokenSessionService,
                                   StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/user/login".equals(path)
                || "/user/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (authorization.startsWith("Bearer ")) {
                authenticateNewToken(request, authorization.substring(7));
            } else if (!request.getRequestURI().startsWith("/api/auth/")) {
                authenticateLegacyToken(authorization);
            } else {
                throw new IllegalArgumentException("Bearer prefix required");
            }
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ThreadLocalUtil.remove();
        }
    }

    private void authenticateNewToken(HttpServletRequest request, String token) {
        JwtService.TokenClaims claims = jwtService.parse(token);
        if (!tokenSessionService.isValid(claims.userId(), claims.tokenId())) {
            throw new IllegalArgumentException("Token revoked");
        }

        List<String> roles = claims.roles() == null ? List.of() : claims.roles();
        AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.username(), roles);
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, token, authorities));
        request.setAttribute("tokenId", claims.tokenId());

        Map<String, Object> legacyClaims = new HashMap<>();
        legacyClaims.put("id", claims.userId().intValue());
        legacyClaims.put("username", claims.username());
        ThreadLocalUtil.set(legacyClaims);
    }

    private void authenticateLegacyToken(String token) {
        String storedToken = redisTemplate.opsForValue().get(token);
        if (!token.equals(storedToken)) {
            throw new IllegalArgumentException("Legacy token revoked");
        }
        Map<String, Object> claims = JwtUtil.parseToken(token);
        ThreadLocalUtil.set(claims);
        AuthenticatedUser principal = new AuthenticatedUser(
                ((Number) claims.get("id")).longValue(), (String) claims.get("username"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, token, List.of()));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(401, "未登录或登录已过期"));
    }
}
