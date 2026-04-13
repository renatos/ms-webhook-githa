package com.githa.core.usecase.calendar;

import com.githa.entrypoint.websocket.AppointmentSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class BroadcastEventUseCase {

    private final AppointmentSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void execute(Long accountGroupId, Object notification) {
        log.info("Broadcasting update to account group {}", accountGroupId);
        
        Set<Session> sessions = sessionRegistry.getSessions(accountGroupId);
        if (sessions.isEmpty()) {
            log.info("No active sessions for account group {}", accountGroupId);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(notification);
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(payload, result -> {
                        if (!result.isOK()) {
                            log.warn("Failed to send message to session {}: {}. Unregistering.", 
                                    session.getId(), result.getException().getMessage());
                            sessionRegistry.unregister(accountGroupId, session);
                        }
                    });
                } else {
                    sessionRegistry.unregister(accountGroupId, session);
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize or broadcast message", e);
        }
    }
}
