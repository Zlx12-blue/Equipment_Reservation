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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final List<Reservation.Status> ACTIVE_STATUSES =
            List.of(Reservation.Status.PENDING, Reservation.Status.APPROVED);

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
        Device device = loadAvailableDevice(request.getDeviceId());
        validateTime(request.getStartTime(), request.getEndTime());
        checkConflict(device.getId(), request.getStartTime(), request.getEndTime(), null);

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

    /** 学生修改待审批预约:校验冲突并同步流程变量 */
    @Transactional
    public Reservation update(Long applicantId, Long reservationId, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!reservation.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("只能修改自己的预约");
        }
        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalStateException("仅待审批的预约可以修改");
        }
        Device device = loadAvailableDevice(request.getDeviceId());
        validateTime(request.getStartTime(), request.getEndTime());
        checkConflict(device.getId(), request.getStartTime(), request.getEndTime(), reservation.getId());

        reservation.setDevice(device);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPurpose(request.getPurpose());

        // 同步流程变量,审批端的设备名称同步更新
        if (reservation.getProcessInstanceId() != null) {
            runtimeService.setVariable(reservation.getProcessInstanceId(), "deviceName", device.getName());
        }
        notificationService.notifyAllApprovers("预约申请已修改:"
                + reservation.getApplicant().getRealName() + " 改为预约「" + device.getName()
                + "」" + request.getStartTime() + " ~ " + request.getEndTime());
        return reservation;
    }

    /** 我的预约,支持按状态 / 关键字(设备名、用途)筛选 */
    public List<Reservation> myReservations(Long applicantId, String status, String keyword) {
        return reservationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId).stream()
                .filter(r -> matchesStatus(r, status))
                .filter(r -> matchesKeyword(r, keyword))
                .toList();
    }

    /** 管理员查看全部预约记录,支持筛选 */
    public List<Reservation> listAll(String status, String keyword) {
        return reservationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> matchesStatus(r, status))
                .filter(r -> matchesKeyword(r, keyword))
                .toList();
    }

    /** 某设备指定日期的占用时段(用于时间轴展示) */
    public List<Map<String, Object>> busySlots(Long deviceId, LocalDate date, Long excludeId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Reservation> list = excludeId == null
                ? reservationRepository.findOverlapping(deviceId, ACTIVE_STATUSES, start, end)
                : reservationRepository.findOverlappingExcluding(
                        deviceId, ACTIVE_STATUSES, start, end, excludeId);
        return list.stream().map(r -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", r.getId());
            view.put("startTime", r.getStartTime());
            view.put("endTime", r.getEndTime());
            view.put("status", r.getStatus().name());
            view.put("applicantName", r.getApplicant().getRealName());
            return view;
        }).toList();
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

    /** 导出全部预约记录为 Excel 可直接打开的 CSV(UTF-8 BOM) */
    public byte[] exportCsv(String status, String keyword) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("申请人,设备,开始时间,结束时间,用途,状态,审批老师,审批意见,提交时间\n");
        listAll(status, keyword).forEach(r -> sb.append(String.join(",",
                csv(r.getApplicant().getRealName()),
                csv(r.getDevice().getName()),
                csv(r.getStartTime().toString()),
                csv(r.getEndTime().toString()),
                csv(r.getPurpose()),
                csv(statusText(r.getStatus())),
                csv(r.getApproverName()),
                csv(r.getApprovalComment()),
                csv(r.getCreatedAt().toString()))).append('\n'));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Device loadAvailableDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (device.getStatus() != Device.Status.AVAILABLE) {
            throw new IllegalStateException("设备当前不可预约");
        }
        return device;
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }

    private void checkConflict(Long deviceId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Reservation> conflicts = excludeId == null
                ? reservationRepository.findOverlapping(deviceId, ACTIVE_STATUSES, start, end)
                : reservationRepository.findOverlappingExcluding(deviceId, ACTIVE_STATUSES, start, end, excludeId);
        if (!conflicts.isEmpty()) {
            Reservation c = conflicts.get(0);
            throw new IllegalStateException("该设备在所选时间段已被预约("
                    + c.getStartTime() + " ~ " + c.getEndTime() + "),请选择其他时段");
        }
    }

    private boolean matchesStatus(Reservation r, String status) {
        return status == null || status.isBlank() || r.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean matchesKeyword(Reservation r, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String k = keyword.trim().toLowerCase(Locale.ROOT);
        return r.getDevice().getName().toLowerCase(Locale.ROOT).contains(k)
                || (r.getPurpose() != null && r.getPurpose().toLowerCase(Locale.ROOT).contains(k));
    }

    private String statusText(Reservation.Status status) {
        return switch (status) {
            case PENDING -> "待审批";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
            case CANCELLED -> "已取消";
        };
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
