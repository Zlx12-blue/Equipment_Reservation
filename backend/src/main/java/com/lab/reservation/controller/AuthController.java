package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.dto.LoginRequest;
import com.lab.reservation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
