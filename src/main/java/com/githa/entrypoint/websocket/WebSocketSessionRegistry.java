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
    
    // Maps User Login (email) to a set of active sessions
    private final Map<String, Set<Session>> userSessions = new ConcurrentHashMap<>();

    // Maps Role (e.g., ADMIN, PROFESSIONAL) to a set of active sessions
    private final Map<String, Set<Session>> roleSessions = new ConcurrentHashMap<>();
    
    // Maps Session ID to its identity for monitoring
    private final Map<String, SessionIdentity> sessionIdentities = new ConcurrentHashMap<>();

    public void register(Long accountGroupId, Session session, String login, Set<String> roles) {
        accountSessions.computeIfAbsent(accountGroupId, k -> new CopyOnWriteArraySet<>()).add(session);
        userSessions.computeIfAbsent(login, k -> new CopyOnWriteArraySet<>()).add(session);
        
        if (roles != null) {
            roles.forEach(role -> roleSessions.computeIfAbsent(role, k -> new CopyOnWriteArraySet<>()).add(session));
        }
        
        SessionIdentity identity = SessionIdentity.builder()
                .sessionId(session.getId())
                .login(login)
                .accountGroupId(accountGroupId)
                .roles(roles != null ? new ArrayList<>(roles) : Collections.emptyList())
                .connectedAt(java.time.LocalDateTime.now())
                .build();
        
        sessionIdentities.put(session.getId(), identity);
        
        log.info("[Registry] Registered session {} for user {} with roles {}. Groups: {}, Roles: {}", 
                session.getId(), login, roles, accountSessions.size(), roleSessions.size());
    }

    public void unregister(Long accountGroupId, Session session) {
        log.info("Unregistering WebSocket session {} for account group {}", session.getId(), accountGroupId);
        
        // Remove from account sessions
        Set<Session> groupSessions = accountSessions.get(accountGroupId);
        if (groupSessions != null) {
            groupSessions.remove(session);
            if (groupSessions.isEmpty()) accountSessions.remove(accountGroupId);
        }
        
        SessionIdentity identity = sessionIdentities.get(session.getId());
        if (identity != null) {
            // Remove from user sessions
            Set<Session> uSessions = userSessions.get(identity.getLogin());
            if (uSessions != null) {
                uSessions.remove(session);
                if (uSessions.isEmpty()) userSessions.remove(identity.getLogin());
            }
            
            // Remove from role sessions
            if (identity.getRoles() != null) {
                identity.getRoles().forEach(role -> {
                    Set<Session> rSessions = roleSessions.get(role);
                    if (rSessions != null) {
                        rSessions.remove(session);
                        if (rSessions.isEmpty()) roleSessions.remove(role);
                    }
                });
            }
        }
        
        sessionIdentities.remove(session.getId());
    }

    public Set<Session> getSessions(Long accountGroupId) {
        return accountSessions.getOrDefault(accountGroupId, Collections.emptySet());
    }

    public Set<Session> getSessionsByUser(String login) {
        return userSessions.getOrDefault(login, Collections.emptySet());
    }

    public Set<Session> getSessionsByRole(String role) {
        return roleSessions.getOrDefault(role, Collections.emptySet());
    }

    public Set<Session> getAllSessions() {
        Set<Session> allSessions = new HashSet<>();
        accountSessions.values().forEach(allSessions::addAll);
        return allSessions;
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
