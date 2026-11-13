package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.entity.Notification;
import com.lab.reservation.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ApiResponse<List<Notification>> my(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(notificationService.myNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.ok();
    }
}
