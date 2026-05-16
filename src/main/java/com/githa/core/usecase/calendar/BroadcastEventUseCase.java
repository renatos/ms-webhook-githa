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
        String tempPayload;
        String tempType = "UNKNOWN";
        
        try {
            tempPayload = (notification instanceof String) ? (String) notification : objectMapper.writeValueAsString(notification);
            
            // Extract type for better logging using Jackson
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(tempPayload);
            if (node.has("type")) {
                String fullType = node.get("type").asText();
                if ("WHATSAPP_NOTIFICATION".equals(fullType)) {
                    tempType = "WHATSAPP";
                } else if ("CALENDAR_UPDATE".equals(fullType)) {
                    tempType = "CALENDAR";
                } else {
                    tempType = fullType;
                }
            }
        } catch (Exception e) {
            log.error("[Notification-WS] Failed to process message for logging", e);
            try {
                tempPayload = (notification instanceof String) ? (String) notification : objectMapper.writeValueAsString(notification);
            } catch (Exception ex) {
                return;
            }
        }

        final String payload = tempPayload;
        final String type = tempType;

        log.info("[Notification-WS-{}] Broadcasting update to account group {}", type, accountGroupId);
        
        Set<Session> sessions = sessionRegistry.getSessions(accountGroupId);
        if (sessions.isEmpty()) {
            log.info("[Notification-WS-{}] No active sessions for account group {}", type, accountGroupId);
            return;
        }

        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(payload, result -> {
                    if (!result.isOK()) {
                        log.warn("[Notification-WS-{}] Failed to send message to session {}: {}. Unregistering.", 
                                type, session.getId(), result.getException().getMessage());
                        sessionRegistry.unregister(accountGroupId, session);
                    }
                });
            } else {
                sessionRegistry.unregister(accountGroupId, session);
            }
        }
    }
}
