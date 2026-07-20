package com.githa.entrypoint.websocket;

import com.githa.core.domain.SessionIdentity;
import com.githa.core.usecase.auth.ValidateSessionUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket endpoint for real-time notifications.
 * Clients connect with their JWT token to receive updates for their account group.
 */
@Slf4j
@ServerEndpoint("/ws/notifications/{token}")
@ApplicationScoped
public class NotificationWebSocketEndpoint {

    @Inject
    WebSocketSessionRegistry sessionRegistry;

    @Inject
    ValidateSessionUseCase validateSessionUseCase;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        log.info("New WebSocket connection attempt: session={}", session.getId());

        try {
            SessionIdentity identity = validateSessionUseCase.execute(token);
            if (identity != null) {
                MDC.put("login", identity.getLogin());
            }
            
            // Store identity in session user properties for easy access on close
            session.getUserProperties().put("identity", identity);
            
            Set<String> roles = identity.getRoles() != null ? new HashSet<>(identity.getRoles()) : new HashSet<>();
            sessionRegistry.register(identity.getAccountGroupId(), session, identity.getLogin(), roles);

            log.info("WebSocket session {} authorized for user {} with roles {}", 
                    session.getId(), identity.getLogin(), roles);
        } catch (SecurityException e) {
            log.warn("Unauthorized WebSocket connection attempt: session={}. Error: {}", session.getId(), e.getMessage());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, e.getMessage()));
            } catch (Exception closeEx) {
                log.error("Error closing session: {}", closeEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error in WebSocket onOpen: session={}", session.getId(), e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal error"));
            } catch (Exception closeEx) {
                // ignore
            }
        } finally {
            MDC.remove("login");
        }
    }

    @OnClose
    public void onClose(Session session) {
        SessionIdentity identity = session != null ? (SessionIdentity) session.getUserProperties().get("identity") : null;
        if (identity != null) {
            MDC.put("login", identity.getLogin());
        }
        try {
            if (identity != null) {
                sessionRegistry.unregister(identity.getAccountGroupId(), session);
                log.info("WebSocket session {} closed and unregistered for user {}", session.getId(), identity.getLogin());
            }
        } finally {
            MDC.remove("login");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        SessionIdentity identity = session != null ? (SessionIdentity) session.getUserProperties().get("identity") : null;
        if (identity != null) {
            MDC.put("login", identity.getLogin());
        }
        try {
            log.error("WebSocket error for session {}: {}", session != null ? session.getId() : "null", throwable != null ? throwable.getMessage() : "null");
            onClose(session);
        } finally {
            MDC.remove("login");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        SessionIdentity identity = session != null ? (SessionIdentity) session.getUserProperties().get("identity") : null;
        if (identity != null) {
            MDC.put("login", identity.getLogin());
        }
        try {
            // We handle only inbound notifications typically, but clients might send heartbeats
            log.debug("Received WebSocket message from session {}: {}", session != null ? session.getId() : "null", message);
        } finally {
            MDC.remove("login");
        }
    }
}
