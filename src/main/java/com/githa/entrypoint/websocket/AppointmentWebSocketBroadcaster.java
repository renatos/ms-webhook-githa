package com.githa.entrypoint.websocket;

import com.githa.core.domain.CalendarUpdateNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Service to broadcast calendar update notifications to active WebSocket sessions.
 */
@Slf4j
@ApplicationScoped
public class AppointmentWebSocketBroadcaster {

    @Inject
    AppointmentSessionRegistry sessionRegistry;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Broadcasts a notification to all active sessions in the specified account group.
     */
    public void broadcast(Long accountGroupId, CalendarUpdateNotification notification) {
        Set<Session> sessions = sessionRegistry.getSessions(accountGroupId);
        if (sessions.isEmpty()) {
            log.info("No active WebSocket sessions for account group {}. Skipping broadcast.", accountGroupId);
            return;
        }

        log.info("Broadcasting {} notification to {} sessions in account group {}", 
                notification.getAction(), sessions.size(), accountGroupId);

        try {
            String json = objectMapper.writeValueAsString(notification);
            
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(json, result -> {
                        if (!result.isOK()) {
                            log.warn("Failed to send message to session {}: {}. Unregistering.", 
                                    session.getId(), result.getException().getMessage());
                            sessionRegistry.unregister(accountGroupId, session);
                        }
                    });
                } else {
                    log.info("Session {} is closed. Unregistering from account group {}.", session.getId(), accountGroupId);
                    sessionRegistry.unregister(accountGroupId, session);
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize or broadcast WebSocket notification", e);
        }
    }
}
