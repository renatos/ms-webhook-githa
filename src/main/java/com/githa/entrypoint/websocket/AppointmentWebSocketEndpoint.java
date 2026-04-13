package com.githa.entrypoint.websocket;

import com.githa.core.usecase.auth.ValidateSessionUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket endpoint for real-time appointment updates.
 * Clients connect with their JWT token to receive updates for their account group.
 */
@Slf4j
@ServerEndpoint("/ws/appointments/{token}")
@ApplicationScoped
public class AppointmentWebSocketEndpoint {

    @Inject
    AppointmentSessionRegistry sessionRegistry;

    @Inject
    ValidateSessionUseCase validateSessionUseCase;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        log.info("New WebSocket connection attempt: session={}", session.getId());

        try {
            Long accountGroupId = validateSessionUseCase.execute(token);
            
            // Store accountGroupId in session user properties for easy access on close
            session.getUserProperties().put("accountGroupId", accountGroupId);
            
            sessionRegistry.register(accountGroupId, session);
            log.info("WebSocket session {} authorized for account group {}", session.getId(), accountGroupId);
        } catch (SecurityException e) {
            log.warn("Invalid token for WebSocket connection: session={}. Error: {}", session.getId(), e.getMessage());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, e.getMessage()));
            } catch (Exception closeEx) {
                log.error("Error closing session: {}", closeEx.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long accountGroupId = (Long) session.getUserProperties().get("accountGroupId");
        if (accountGroupId != null) {
            sessionRegistry.unregister(accountGroupId, session);
            log.info("WebSocket session {} closed and unregistered", session.getId());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error for session {}: {}", session.getId(), throwable.getMessage());
        onClose(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // We handle only inbound notifications typically, but clients might send heartbeats
        log.debug("Received WebSocket message from session {}: {}", session.getId(), message);
    }
}
