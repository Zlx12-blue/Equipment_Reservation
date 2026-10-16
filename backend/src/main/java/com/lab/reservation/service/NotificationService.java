package com.lab.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.entity.Notification;
import com.lab.reservation.entity.User;
import com.lab.reservation.repository.NotificationRepository;
import com.lab.reservation.repository.UserRepository;
import com.lab.reservation.websocket.NotifyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotifyWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 落库 + 实时推送给单个用户 */
    public void notifyUser(Long userId, String type, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setContent(content);
        notificationRepository.save(n);
        try {
            webSocketHandler.pushToUser(userId, objectMapper.writeValueAsString(n));
        } catch (Exception e) {
            log.warn("WebSocket 推送失败 userId={}", userId, e);
        }
    }

    /** 推送给全体审批人(教师 + 管理员):学生提交预约时触发 */
    public void notifyAllApprovers(String content) {
        List<User> approvers = userRepository.findByRoleIn(
                List.of(User.Role.TEACHER, User.Role.ADMIN));
        approvers.forEach(u -> notifyUser(u.getId(), "NEW_APPROVAL", content));
    }

    public List<Notification> myNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setReadFlag(true);
            notificationRepository.save(n);
        });
    }
}
