package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.entity.Reservation;
import com.lab.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 学生提交预约 → 启动审批流程 → 通知审批人 */
    @PostMapping
    public ApiResponse<Reservation> submit(@AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody ReservationRequest request) {
        return ApiResponse.ok(reservationService.submit(userId, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<Reservation>> my(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(reservationService.myReservations(userId));
    }

    /** 管理员查看全部预约记录 */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Reservation>> all() {
        return ApiResponse.ok(reservationService.listAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        reservationService.cancel(userId, id);
        return ApiResponse.ok();
    }
}
