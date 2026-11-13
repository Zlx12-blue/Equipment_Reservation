package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.service.AdminUserService;
import com.lab.reservation.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;
    private final StatsService statsService;

    /** 管理端统计概览 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(statsService.overview());
    }

    /** 用户列表 */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.ok(adminUserService.listUsers());
    }

    /** 启用 / 禁用账号 */
    @PutMapping("/users/{id}/enabled")
    public ApiResponse<Void> setEnabled(@AuthenticationPrincipal Long operatorId,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Boolean> body) {
        adminUserService.setEnabled(operatorId, id, Boolean.TRUE.equals(body.get("enabled")));
        return ApiResponse.ok();
    }

    /** 重置密码 */
    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        adminUserService.resetPassword(id, body.get("password"));
        return ApiResponse.ok();
    }

    /** 删除账号 */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long operatorId, @PathVariable Long id) {
        adminUserService.deleteUser(operatorId, id);
        return ApiResponse.ok();
    }
}
