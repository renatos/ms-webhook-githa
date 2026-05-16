package com.githa.core.usecase.calendar;

import com.githa.entrypoint.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class BroadcastEventUseCase {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    
    // Deduplication cache: key -> expirationTimestamp (ms)
    private final Map<String, Long> deduplicationCache = new ConcurrentHashMap<>();
    private static final long DEDUPLICATION_WINDOW_MS = 10000;

    public void execute(Long accountGroupId, String targetLogin, String targetRole, Object notification) {
        String tempPayload;
        String tempType = "UNKNOWN";
        String eventId = null;
        String action = null;
        
        try {
            tempPayload = (notification instanceof String) ? (String) notification : objectMapper.writeValueAsString(notification);
            
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(tempPayload);
            if (node.has("type")) {
                tempType = node.get("type").asText();
                com.fasterxml.jackson.databind.JsonNode dataNode = node.has("data") ? node.get("data") : node;
                if (dataNode.has("eventId")) eventId = dataNode.get("eventId").asText();
                if (dataNode.has("action")) action = dataNode.get("action").asText();
                if (dataNode.has("appointmentId") && eventId == null) eventId = dataNode.get("appointmentId").asText();
            }
            
            if (eventId != null && action != null) {
                String dedupKey = String.format("%s:%s:%s:%s:%s:%s", accountGroupId, targetLogin, targetRole, tempType, eventId, action);
                long now = System.currentTimeMillis();
                if (deduplicationCache.containsKey(dedupKey) && deduplicationCache.get(dedupKey) > now) {
                    return;
                }
                deduplicationCache.put(dedupKey, now + DEDUPLICATION_WINDOW_MS);
            }

        } catch (Exception e) {
            log.error("[Notification-WS] Process error", e);
            try {
                tempPayload = (notification instanceof String) ? (String) notification : objectMapper.writeValueAsString(notification);
            } catch (Exception ex) {
                return;
            }
        }

        final String payload = tempPayload;
        final String type = tempType;

        Set<Session> sessionsToNotify = new HashSet<>();
        
        // 1. Target specific user if login provided
        if (targetLogin != null && !targetLogin.isBlank()) {
            sessionsToNotify.addAll(sessionRegistry.getSessionsByUser(targetLogin));
        } 
        
        // 2. Target specific profile (role) if provided
        if (targetRole != null && !targetRole.isBlank()) {
            sessionsToNotify.addAll(sessionRegistry.getSessionsByRole(targetRole));
        }

        // 3. Target account group if provided (future use)
        if (sessionsToNotify.isEmpty() && accountGroupId != null && accountGroupId > 0) {
            sessionsToNotify.addAll(sessionRegistry.getSessions(accountGroupId));
        }

        // 4. Fallback to Global if no target specified
        if (sessionsToNotify.isEmpty() && (targetLogin == null || targetLogin.isBlank()) && (targetRole == null || targetRole.isBlank())) {
            sessionsToNotify.addAll(sessionRegistry.getAllSessions());
            log.info("[Notification-WS-{}] Global broadcast to {} sessions", type, sessionsToNotify.size());
        }

        // 5. Always include ADMIN group (1L) for monitoring (if it exists)
        sessionsToNotify.addAll(sessionRegistry.getSessions(1L));
        // Also include anyone with "ADMIN" role specifically
        sessionsToNotify.addAll(sessionRegistry.getSessionsByRole("ADMIN"));

        if (sessionsToNotify.isEmpty()) {
            return;
        }

        log.info("[Notification-WS-{}] Sending to {} sessions (TargetUser: {}, TargetRole: {})", 
                type, sessionsToNotify.size(), targetLogin, targetRole);

        for (Session session : sessionsToNotify) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(payload, result -> {
                    if (!result.isOK()) {
                        log.warn("[Notification-WS-{}] Send failed to session {}: {}", 
                                type, session.getId(), result.getException().getMessage());
                    }
                });
            }
        }
    }
}
