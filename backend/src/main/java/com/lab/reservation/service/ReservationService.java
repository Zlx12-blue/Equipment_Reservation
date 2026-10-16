package com.lab.reservation.service;

import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.entity.Device;
import com.lab.reservation.entity.Reservation;
import com.lab.reservation.entity.User;
import com.lab.reservation.repository.DeviceRepository;
import com.lab.reservation.repository.ReservationRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final RuntimeService runtimeService;
    private final NotificationService notificationService;

    /**
     * 学生提交预约:落库(PENDING)并启动 Flowable 审批流程,
     * 同时通过 WebSocket 通知全体审批人(多端互动入口)。
     */
    @Transactional
    public Reservation submit(Long applicantId, ReservationRequest request) {
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (device.getStatus() != Device.Status.AVAILABLE) {
            throw new IllegalStateException("设备当前不可预约");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }

        // 时间冲突检测:同一设备已有待审批/已通过的预约与所选时段重叠时拒绝提交
        var conflicts = reservationRepository.findOverlapping(
                device.getId(),
                List.of(Reservation.Status.PENDING, Reservation.Status.APPROVED),
                request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            Reservation c = conflicts.get(0);
            throw new IllegalStateException("该设备在所选时间段已被预约("
                    + c.getStartTime() + " ~ " + c.getEndTime() + "),请选择其他时段");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Reservation reservation = new Reservation();
        reservation.setApplicant(applicant);
        reservation.setDevice(device);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPurpose(request.getPurpose());
        reservationRepository.save(reservation);

        // 启动 Flowable 审批流程,businessKey = 预约 ID
        Map<String, Object> variables = new HashMap<>();
        variables.put("reservationId", reservation.getId());
        variables.put("applicantId", applicant.getId());
        variables.put("applicantName", applicant.getRealName());
        variables.put("deviceName", device.getName());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "deviceReservation", String.valueOf(reservation.getId()), variables);
        reservation.setProcessInstanceId(instance.getId());

        // 通知所有教师/管理员:有新的待审批(买家下单 → 卖家收到订单)
        notificationService.notifyAllApprovers(
                "新的设备预约申请:" + applicant.getRealName() + " 预约「" + device.getName() + "」");

        return reservation;
    }

    public List<Reservation> myReservations(Long applicantId) {
        return reservationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
    }

    /** 管理员查看全部预约记录 */
    public List<Reservation> listAll() {
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 学生取消待审批预约:同步终止流程实例 */
    @Transactional
    public void cancel(Long applicantId, Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!r.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("只能取消自己的预约");
        }
        if (r.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalStateException("仅待审批的预约可以取消");
        }
        r.setStatus(Reservation.Status.CANCELLED);
        if (r.getProcessInstanceId() != null) {
            runtimeService.deleteProcessInstance(r.getProcessInstanceId(), "申请人取消");
        }
    }
}
