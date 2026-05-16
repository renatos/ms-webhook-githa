package com.githa.core.usecase.calendar;

import com.githa.entrypoint.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BroadcastEventUseCaseDedupTest {

    @Mock
    WebSocketSessionRegistry sessionRegistry;

    @Mock
    Session session;

    @Mock
    RemoteEndpoint.Async asyncRemote;

    BroadcastEventUseCase useCase;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        useCase = new BroadcastEventUseCase(sessionRegistry, objectMapper);
        when(session.isOpen()).thenReturn(true);
        when(session.getAsyncRemote()).thenReturn(asyncRemote);
    }

    @Test
    void shouldBroadcastOnlyOnceForDuplicateMessages() {
        // Given
        Long accountGroupId = 1L;
        String notification = "{\"type\":\"CALENDAR_UPDATE\",\"data\":{\"eventId\":\"evt-1\",\"action\":\"CANCELLED\"}}";
        when(sessionRegistry.getSessions(accountGroupId)).thenReturn(Set.of(session));

        // When - Call twice
        useCase.execute(accountGroupId, notification);
        useCase.execute(accountGroupId, notification);

        // Then
        verify(asyncRemote, times(1)).sendText(anyString(), any());
    }

    @Test
    void shouldBroadcastMultipleTimesForDifferentEvents() {
        // Given
        Long accountGroupId = 1L;
        String notif1 = "{\"type\":\"CALENDAR_UPDATE\",\"data\":{\"eventId\":\"evt-1\",\"action\":\"CANCELLED\"}}";
        String notif2 = "{\"type\":\"CALENDAR_UPDATE\",\"data\":{\"eventId\":\"evt-2\",\"action\":\"CANCELLED\"}}";
        when(sessionRegistry.getSessions(accountGroupId)).thenReturn(Set.of(session));

        // When
        useCase.execute(accountGroupId, notif1);
        useCase.execute(accountGroupId, notif2);

        // Then
        verify(asyncRemote, times(2)).sendText(anyString(), any());
    }
}
