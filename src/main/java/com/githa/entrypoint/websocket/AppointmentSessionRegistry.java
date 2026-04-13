package com.githa.entrypoint.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry to keep track of active WebSocket sessions grouped by account group ID.
 */
@Slf4j
@ApplicationScoped
public class AppointmentSessionRegistry {

    // Maps AccountGroupId to a set of active sessions
    private final Map<Long, Set<Session>> accountSessions = new ConcurrentHashMap<>();

    public void register(Long accountGroupId, Session session) {
        log.info("Registering WebSocket session {} for account group {}", session.getId(), accountGroupId);
        accountSessions.computeIfAbsent(accountGroupId, k -> new CopyOnWriteArraySet<>()).add(session);
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
    }

    public Set<Session> getSessions(Long accountGroupId) {
        return accountSessions.getOrDefault(accountGroupId, Collections.emptySet());
    }

    public void clearAll() {
        log.info("Clearing all active WebSocket sessions");
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
