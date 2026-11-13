package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.dto.LoginRequest;
import com.lab.reservation.dto.RegisterRequest;
import com.lab.reservation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<Object> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(authService.currentUser(userId));
    }

    /** 未登录页面注册学生 / 教师账号 */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /** 管理员登录后添加新的管理员账号 */
    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.registerAdmin(request));
    }
}
