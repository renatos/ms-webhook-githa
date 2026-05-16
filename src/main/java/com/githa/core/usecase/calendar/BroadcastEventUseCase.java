package com.githa.core.usecase.calendar;

import com.githa.entrypoint.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    public void execute(Long accountGroupId, Object notification) {
        String tempPayload;
        String tempType = "UNKNOWN";
        String eventId = null;
        String action = null;
        
        try {
            tempPayload = (notification instanceof String) ? (String) notification : objectMapper.writeValueAsString(notification);
            
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(tempPayload);
            if (node.has("type")) {
                String fullType = node.get("type").asText();
                tempType = fullType;
                
                // Try to extract eventId and action for deduplication
                // Handling both root-level and nested 'data' level
                com.fasterxml.jackson.databind.JsonNode dataNode = node.has("data") ? node.get("data") : node;
                
                if (dataNode.has("eventId")) eventId = dataNode.get("eventId").asText();
                if (dataNode.has("action")) action = dataNode.get("action").asText();
                if (dataNode.has("appointmentId") && eventId == null) eventId = dataNode.get("appointmentId").asText();
            }
            
            // Deduplication Logic
            if (eventId != null && action != null) {
                String dedupKey = String.format("%s:%s:%s:%s", accountGroupId, tempType, eventId, action);
                long now = System.currentTimeMillis();
                
                if (deduplicationCache.containsKey(dedupKey) && deduplicationCache.get(dedupKey) > now) {
                    log.info("[Notification-DEDUP] Skipping duplicate {} notification for event {} in group {}", 
                            tempType, eventId, accountGroupId);
                    return;
                }
                
                deduplicationCache.put(dedupKey, now + DEDUPLICATION_WINDOW_MS);
                
                // Periodic cleanup of the cache (simple approach)
                if (deduplicationCache.size() > 1000) {
                    deduplicationCache.entrySet().removeIf(entry -> entry.getValue() < now);
                }
            }

        } catch (Exception e) {
            log.error("[Notification-WS] Failed to process message for logging or dedup", e);
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
