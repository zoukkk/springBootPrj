package com.example.service;

import com.example.dto.LoginRequest;
import com.example.dto.LoginResponse;
import com.example.dto.MenuResponse;
import com.example.dto.UserInfoResponse;
import com.example.exception.BusinessException;
import com.example.mapper.AuthMapper;
import com.example.pojo.SysMenu;
import com.example.pojo.SysUser;
import com.example.security.AuthenticatedUser;
import com.example.security.JwtService;
import com.example.security.TokenSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TokenSessionService tokenSessionService) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = authMapper.findUserByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(1, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(1, HttpStatus.FORBIDDEN, "账号已停用");
        }

        List<String> roles = authMapper.findRoleCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(1, HttpStatus.FORBIDDEN, "账号未分配角色");
        }

        Instant issuedAt = Instant.now();
        String tokenId = jwtService.newTokenId();
        String token = jwtService.createToken(
                new AuthenticatedUser(user.getId(), user.getUsername(), roles), tokenId, issuedAt);
        tokenSessionService.register(user.getId(), tokenId, issuedAt,
                Duration.ofSeconds(jwtService.getExpiresInSeconds()));
        return new LoginResponse(token, "Bearer", jwtService.getExpiresInSeconds());
    }

    public UserInfoResponse getUserInfo(AuthenticatedUser principal) {
        SysUser user = requireUser(principal.userId());
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), authMapper.findRoleCodesByUserId(user.getId()));
    }

    public List<MenuResponse> getMenus(AuthenticatedUser principal) {
        List<String> roles = authMapper.findRoleCodesByUserId(principal.userId());
        List<SysMenu> allMenus = authMapper.findAllActiveMenus();
        if (roles.contains("admin")) {
            return buildTree(allMenus);
        }

        List<SysMenu> assignedMenus = authMapper.findActiveMenusByUserId(principal.userId());
        Map<Long, SysMenu> allById = new HashMap<>();
        allMenus.forEach(menu -> allById.put(menu.getId(), menu));
        Set<Long> visibleIds = new HashSet<>();
        for (SysMenu menu : assignedMenus) {
            SysMenu current = menu;
            while (current != null && visibleIds.add(current.getId()) && current.getParentId() != 0) {
                current = allById.get(current.getParentId());
            }
        }
        return buildTree(allMenus.stream().filter(menu -> visibleIds.contains(menu.getId())).toList());
    }

    public void logout(AuthenticatedUser principal, String tokenId) {
        tokenSessionService.revoke(principal.userId(), tokenId);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = authMapper.findUserById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(401, HttpStatus.UNAUTHORIZED, "用户不存在或已停用");
        }
        return user;
    }

    private List<MenuResponse> buildTree(List<SysMenu> menus) {
        Map<Long, MutableMenu> nodes = new LinkedHashMap<>();
        menus.forEach(menu -> nodes.put(menu.getId(), new MutableMenu(menu)));
        List<MutableMenu> roots = new ArrayList<>();
        for (MutableMenu node : nodes.values()) {
            MutableMenu parent = nodes.get(node.menu.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        return roots.stream().map(MutableMenu::toResponse).toList();
    }

    private static final class MutableMenu {
        private final SysMenu menu;
        private final List<MutableMenu> children = new ArrayList<>();

        private MutableMenu(SysMenu menu) {
            this.menu = menu;
        }

        private MenuResponse toResponse() {
            return new MenuResponse(menu.getId(), menu.getParentId(), menu.getName(), menu.getPath(),
                    menu.getComponent(), menu.getIcon(), menu.getSort(),
                    children.stream().map(MutableMenu::toResponse).toList());
        }
    }
}
