package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarUpdateNotification;
import com.githa.entrypoint.websocket.AppointmentSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BroadcastEventUseCaseTest {

    @Mock
    AppointmentSessionRegistry sessionRegistry;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    Session successfulSession;

    @Mock
    Session failingSession;

    @Mock
    RemoteEndpoint.Async successfulRemote;

    @Mock
    RemoteEndpoint.Async failingRemote;

    @InjectMocks
    BroadcastEventUseCase broadcastUseCase;

    @Test
    void shouldBroadcastToAllSessionsInGroupAndRemoveDeadOnes() throws IOException {
        Long accountGroupId = 1L;
        CalendarUpdateNotification notification = CalendarUpdateNotification.builder()
                .appointmentId(100L)
                .action("UPDATED")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        Set<Session> sessions = new CopyOnWriteArraySet<>();
        sessions.add(successfulSession);
        sessions.add(failingSession);

        when(sessionRegistry.getSessions(accountGroupId)).thenReturn(sessions);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(successfulSession.isOpen()).thenReturn(true);
        when(failingSession.isOpen()).thenReturn(true);
        when(successfulSession.getAsyncRemote()).thenReturn(successfulRemote);
        when(failingSession.getAsyncRemote()).thenReturn(failingRemote);

        // Success for one
        doAnswer(invocation -> {
            jakarta.websocket.SendHandler handler = invocation.getArgument(1);
            handler.onResult(new jakarta.websocket.SendResult());
            return null;
        }).when(successfulRemote).sendText(anyString(), any());

        // Failure for the other
        doAnswer(invocation -> {
            jakarta.websocket.SendHandler handler = invocation.getArgument(1);
            handler.onResult(new jakarta.websocket.SendResult(new RuntimeException("Failure")));
            return null;
        }).when(failingRemote).sendText(anyString(), any());

        broadcastUseCase.execute(accountGroupId, notification);

        verify(successfulRemote).sendText(anyString(), any());
        verify(failingRemote).sendText(anyString(), any());
        verify(sessionRegistry).unregister(accountGroupId, failingSession);
        verify(sessionRegistry, never()).unregister(accountGroupId, successfulSession);
    }
}
