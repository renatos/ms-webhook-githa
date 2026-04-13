package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopWebhookChannelUseCaseTest {

    @Mock
    WebhookChannelGateway webhookChannelGateway;

    @Mock
    CalendarWebhookGateway calendarWebhookGateway;

    @InjectMocks
    StopWebhookChannelUseCase stopWebhookChannelUseCase;

    private static final String ACCESS_TOKEN = "access-token";
    private static final String USER_EMAIL = "user@example.com";

    @Test
    void shouldStopAndChannelIfActive() {
        CalendarWebhookChannel channel = CalendarWebhookChannel.builder()
                .channelId("channel-id")
                .resourceId("resource-id")
                .userEmail(USER_EMAIL)
                .build();

        when(webhookChannelGateway.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(channel));

        stopWebhookChannelUseCase.execute(USER_EMAIL, ACCESS_TOKEN);

        verify(calendarWebhookGateway).stopChannel("channel-id", "resource-id", ACCESS_TOKEN);
        verify(webhookChannelGateway).deleteByChannelId("channel-id");
    }

    @Test
    void shouldDoNothingIfNoActiveChannel() {
        when(webhookChannelGateway.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        stopWebhookChannelUseCase.execute(USER_EMAIL, ACCESS_TOKEN);

        verifyNoInteractions(calendarWebhookGateway);
        verify(webhookChannelGateway, never()).deleteByChannelId(anyString());
    }
}
