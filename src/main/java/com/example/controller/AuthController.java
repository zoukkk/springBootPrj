package com.example.controller;

import com.example.dto.LoginRequest;
import com.example.dto.LoginResponse;
import com.example.dto.MenuResponse;
import com.example.dto.UserInfoResponse;
import com.example.pojo.Result;
import com.example.security.AuthenticatedUser;
import com.example.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser user,
                               @RequestAttribute("tokenId") String tokenId) {
        authService.logout(user, tokenId);
        return Result.success();
    }

    @GetMapping("/userinfo")
    public Result<UserInfoResponse> userInfo(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(authService.getUserInfo(user));
    }

    @GetMapping("/menus")
    public Result<List<MenuResponse>> menus(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(authService.getMenus(user));
    }
}
