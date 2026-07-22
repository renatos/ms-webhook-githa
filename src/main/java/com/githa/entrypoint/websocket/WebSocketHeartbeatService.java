package com.githa.entrypoint.websocket;

import com.githa.core.domain.SessionIdentity;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

/**
 * Service executing periodic WebSocket heartbeats (PING frames) and token expiration checks.
 */
@Slf4j
@ApplicationScoped
public class WebSocketHeartbeatService {

    @Inject
    WebSocketSessionRegistry sessionRegistry;

    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void executeHeartbeat() {
        List<SessionIdentity> identities = sessionRegistry.getActiveIdentities();
        if (identities.isEmpty()) {
            return;
        }

        log.debug("[Heartbeat] Checking {} active WebSocket sessions", identities.size());
        Instant now = Instant.now();
        String pingPayload = String.format("{\"type\":\"PING\",\"timestamp\":%d}", now.toEpochMilli());

        for (SessionIdentity identity : identities) {
            Session session = findSessionById(identity.getSessionId());
            if (session == null || !session.isOpen()) {
                sessionRegistry.unregister(identity.getAccountGroupId(), session);
                continue;
            }

            // Check if token has expired
            if (identity.getExpiresAt() != null && identity.getExpiresAt().isBefore(now)) {
                log.warn("[Heartbeat] Session {} for user {} expired at {}. Closing connection.",
                        session.getId(), identity.getLogin(), identity.getExpiresAt());
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Token expired"));
                } catch (Exception e) {
                    log.error("Error closing expired session {}: {}", session.getId(), e.getMessage());
                } finally {
                    sessionRegistry.unregister(identity.getAccountGroupId(), session);
                }
                continue;
            }

            // Send PING frame
            session.getAsyncRemote().sendText(pingPayload, result -> {
                if (!result.isOK()) {
                    log.warn("[Heartbeat] Failed to send PING to session {}: {}", 
                            session.getId(), result.getException() != null ? result.getException().getMessage() : "unknown error");
                    sessionRegistry.unregister(identity.getAccountGroupId(), session);
                }
            });
        }
    }

    private Session findSessionById(String sessionId) {
        if (sessionId == null) return null;
        return sessionRegistry.getAllSessions().stream()
                .filter(s -> sessionId.equals(s.getId()))
                .findFirst()
                .orElse(null);
    }
}
