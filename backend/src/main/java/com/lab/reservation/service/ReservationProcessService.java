package com.lab.reservation.service;

import com.lab.reservation.entity.Reservation;
import com.lab.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BPMN 中 ServiceTask 的回调 Bean(bean 名须与流程表达式一致)。
 * 审批通过/驳回后:更新预约状态 → 通知申请人(多端互动出口)。
 */
@Service("reservationProcessService")
@RequiredArgsConstructor
public class ReservationProcessService {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    @Transactional
    public void onApproved(DelegateExecution execution) {
        handle(execution, Reservation.Status.APPROVED, "已通过");
    }

    @Transactional
    public void onRejected(DelegateExecution execution) {
        handle(execution, Reservation.Status.REJECTED, "被驳回");
    }

    private void handle(DelegateExecution execution, Reservation.Status status, String verb) {
        Long reservationId = Long.valueOf(String.valueOf(execution.getVariable("reservationId")));
        Long applicantId = Long.valueOf(String.valueOf(execution.getVariable("applicantId")));
        String deviceName = String.valueOf(execution.getVariable("deviceName"));
        Object comment = execution.getVariable("comment");

        Object operatorName = execution.getVariable("operatorName");

        reservationRepository.findById(reservationId).ifPresent(r -> {
            r.setStatus(status);
            r.setApprovalComment(comment == null ? null : comment.toString());
            r.setApproverName(operatorName == null ? null : operatorName.toString());
            reservationRepository.save(r);
        });

        notificationService.notifyUser(applicantId, "APPROVAL_RESULT",
                "您对「" + deviceName + "」的预约" + verb
                        + (comment == null || comment.toString().isBlank()
                           ? "" : ",审批意见:" + comment));
    }
}
