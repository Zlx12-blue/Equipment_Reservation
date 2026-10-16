package com.lab.reservation.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用户维度维护 WebSocket 会话,服务端可向指定用户推送通知。
 * 多端互动的核心:学生提交预约 → 推送给全体教师;审批完成 → 推送给该学生。
 */
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId != null) {
            sessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().forEach(list -> list.remove(session));
    }

    public void pushToUser(Long userId, String jsonMessage) {
        List<WebSocketSession> list = sessions.get(userId);
        if (list == null) return;
        for (WebSocketSession s : list) {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException ignored) {
            }
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String userId = UriComponentsBuilder.newInstance().query(query).build()
                    .getQueryParams().getFirst("userId");
            return userId == null ? null : Long.valueOf(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
