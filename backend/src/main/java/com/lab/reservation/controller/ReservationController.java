package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.entity.Reservation;
import com.lab.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    /** 我的预约,可按状态与关键字筛选 */
    @GetMapping("/my")
    public ApiResponse<List<Reservation>> my(@AuthenticationPrincipal Long userId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(reservationService.myReservations(userId, status, keyword));
    }

    /** 修改待审批预约 */
    @PutMapping("/{id}")
    public ApiResponse<Reservation> update(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ReservationRequest request) {
        return ApiResponse.ok(reservationService.update(userId, id, request));
    }

    /** 设备在指定日期的占用时段(时间轴) */
    @GetMapping("/device/{deviceId}/busy")
    public ApiResponse<List<Map<String, Object>>> busy(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long excludeId) {
        return ApiResponse.ok(reservationService.busySlots(deviceId, date, excludeId));
    }

    /** 管理员查看全部预约记录 */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Reservation>> all(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(reservationService.listAll(status, keyword));
    }

    /** 导出预约记录(CSV,Excel 可直接打开) */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        byte[] data = reservationService.exportCsv(status, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reservations.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(data);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        reservationService.cancel(userId, id);
        return ApiResponse.ok();
    }
}
