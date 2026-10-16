package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.dto.ApprovalRequest;
import com.lab.reservation.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class ApprovalController {

    private final ApprovalService approvalService;

    /** 教师/管理员待办任务列表(Flowable 候选组任务) */
    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> pending() {
        return ApiResponse.ok(approvalService.listPendingTasks());
    }

    /** 审批通过 / 驳回 */
    @PostMapping("/complete")
    public ApiResponse<Void> complete(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody ApprovalRequest request) {
        approvalService.completeTask(request.getTaskId(), request.getApproved(),
                request.getComment(), userId);
        return ApiResponse.ok();
    }
}
