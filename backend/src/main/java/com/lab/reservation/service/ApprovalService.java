package com.lab.reservation.service;

import com.lab.reservation.entity.Reservation;
import com.lab.reservation.repository.ReservationRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    /** 教师/管理员共享任务池:查询 teachers 组的待办 */
    public List<Map<String, Object>> listPendingTasks() {
        return taskService.createTaskQuery()
                .taskCandidateGroup("teachers")
                .orderByTaskCreateTime().desc()
                .list()
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /** 当前审批人已处理的历史任务 */
    public List<Map<String, Object>> listHistory(Long operatorId) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(operatorId))
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
        return tasks.stream()
                .map(this::toHistoryView)
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /** 完成审批任务,approved 作为流程变量驱动网关走向通过/驳回分支 */
    public void completeTask(String taskId, boolean approved, String comment, Long operatorId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或已被处理");
        }
        taskService.claim(taskId, String.valueOf(operatorId));
        String operatorName = userRepository.findById(operatorId)
                .map(u -> u.getRealName()).orElse("未知");
        taskService.complete(taskId, Map.of(
                "approved", approved,
                "comment", comment == null ? "" : comment,
                "operatorId", operatorId,
                "operatorName", operatorName));
    }

    private Map<String, Object> toView(Task task) {
        Map<String, Object> vars = taskService.getVariables(task.getId());
        Object reservationId = vars.get("reservationId");
        Reservation reservation = reservationId == null ? null : reservationRepository
                .findById(Long.valueOf(reservationId.toString())).orElse(null);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("taskId", task.getId());
        view.put("taskName", task.getName());
        view.put("createTime", task.getCreateTime().toString());
        view.put("reservationId", reservationId == null ? "" : reservationId);
        view.put("applicantName", vars.getOrDefault("applicantName", ""));
        view.put("deviceName", vars.getOrDefault("deviceName", ""));
        view.put("purpose", reservation == null || reservation.getPurpose() == null ? "" : reservation.getPurpose());
        view.put("deviceId", reservation == null ? null : reservation.getDevice().getId());
        view.put("startTime", reservation == null ? null : reservation.getStartTime().toString());
        view.put("endTime", reservation == null ? null : reservation.getEndTime().toString());
        return view;
    }

    private Map<String, Object> toHistoryView(HistoricTaskInstance task) {
        Reservation r = reservationRepository.findByProcessInstanceId(task.getProcessInstanceId()).orElse(null);
        if (r == null) {
            return null;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("reservationId", r.getId());
        view.put("applicantName", r.getApplicant().getRealName());
        view.put("deviceName", r.getDevice().getName());
        view.put("startTime", r.getStartTime().toString());
        view.put("endTime", r.getEndTime().toString());
        view.put("purpose", r.getPurpose());
        view.put("status", r.getStatus().name());
        view.put("approvalComment", r.getApprovalComment());
        view.put("approverName", r.getApproverName());
        view.put("finishedAt", task.getEndTime() == null ? "" : LocalDateTime.ofInstant(
                task.getEndTime().toInstant(), ZoneId.systemDefault()).toString());
        return view;
    }
}
