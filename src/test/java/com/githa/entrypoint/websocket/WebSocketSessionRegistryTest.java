package com.githa.entrypoint.websocket;

import com.githa.core.domain.SessionIdentity;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionRegistryTest {

    WebSocketSessionRegistry registry;

    @Mock
    Session session;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry();
    }

    @Test
    void shouldRegisterSessionWithIdentity() {
        // Given
        Long groupId = 1L;
        String login = "user@test.com";
        String sessionId = "sess-1";
        when(session.getId()).thenReturn(sessionId);

        // When
        registry.register(groupId, session, login);

        // Then
        assertTrue(registry.getSessions(groupId).contains(session));
        List<SessionIdentity> identities = registry.getActiveIdentities();
        assertEquals(1, identities.size());
        assertEquals(login, identities.get(0).getLogin());
        assertEquals(groupId, identities.get(0).getAccountGroupId());
    }

    @Test
    void shouldUnregisterSessionAndIdentity() {
        // Given
        Long groupId = 1L;
        String login = "user@test.com";
        String sessionId = "sess-1";
        when(session.getId()).thenReturn(sessionId);
        registry.register(groupId, session, login);

        // When
        registry.unregister(groupId, session);

        // Then
        assertFalse(registry.getSessions(groupId).contains(session));
        assertTrue(registry.getActiveIdentities().isEmpty());
    }

    @Test
    void shouldClearAllSessionsAndIdentities() {
        // Given
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);
        registry.register(1L, session, "user1");
        
        // When
        registry.clearAll();

        // Then
        assertTrue(registry.getSessions(1L).isEmpty());
        assertTrue(registry.getActiveIdentities().isEmpty());
        try {
            verify(session).close();
        } catch (Exception e) {
            fail("Should not throw exception on close");
        }
    }
}
