package com.lab.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequest {

    @NotBlank
    private String taskId;    // Flowable 任务 ID

    @NotNull
    private Boolean approved; // true 通过 / false 驳回

    private String comment;   // 审批意见
}
