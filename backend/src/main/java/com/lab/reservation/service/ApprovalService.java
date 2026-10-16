package com.lab.reservation.service;

import com.lab.reservation.entity.Reservation;
import com.lab.reservation.repository.ReservationRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final TaskService taskService;
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
        String purpose = reservationId == null ? null : reservationRepository
                .findById(Long.valueOf(reservationId.toString()))
                .map(Reservation::getPurpose).orElse(null);
        return Map.of(
                "taskId", task.getId(),
                "taskName", task.getName(),
                "createTime", task.getCreateTime().toString(),
                "reservationId", reservationId == null ? "" : reservationId,
                "applicantName", vars.getOrDefault("applicantName", ""),
                "deviceName", vars.getOrDefault("deviceName", ""),
                "purpose", purpose == null ? "" : purpose);
    }
}
