package com.githa.entrypoint.websocket;

import com.githa.core.domain.SessionIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry to keep track of active WebSocket sessions grouped by account group ID.
 */
@Slf4j
@ApplicationScoped
public class WebSocketSessionRegistry {

    // Maps AccountGroupId to a set of active sessions
    private final Map<Long, Set<Session>> accountSessions = new ConcurrentHashMap<>();
    
    // Maps Session ID to its identity for monitoring
    private final Map<String, SessionIdentity> sessionIdentities = new ConcurrentHashMap<>();

    public void register(Long accountGroupId, Session session, String login) {
        log.info("Registering WebSocket session {} for user {} in account group {}", 
                session.getId(), login, accountGroupId);
        
        accountSessions.computeIfAbsent(accountGroupId, k -> new CopyOnWriteArraySet<>()).add(session);
        
        SessionIdentity identity = SessionIdentity.builder()
                .sessionId(session.getId())
                .login(login)
                .accountGroupId(accountGroupId)
                .connectedAt(java.time.LocalDateTime.now())
                .build();
        
        sessionIdentities.put(session.getId(), identity);
    }

    public void unregister(Long accountGroupId, Session session) {
        log.info("Unregistering WebSocket session {} for account group {}", session.getId(), accountGroupId);
        
        Set<Session> sessions = accountSessions.get(accountGroupId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                accountSessions.remove(accountGroupId);
            }
        }
        
        sessionIdentities.remove(session.getId());
    }

    public Set<Session> getSessions(Long accountGroupId) {
        return accountSessions.getOrDefault(accountGroupId, Collections.emptySet());
    }

    public List<SessionIdentity> getActiveIdentities() {
        return new ArrayList<>(sessionIdentities.values());
    }

    public void clearAll() {
        log.info("Clearing all active WebSocket sessions");
        sessionIdentities.clear();
        accountSessions.values().forEach(sessions -> {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.error("Error closing session {}: {}", session.getId(), e.getMessage());
                    }
                }
            });
            sessions.clear();
        });
        accountSessions.clear();
    }
}
