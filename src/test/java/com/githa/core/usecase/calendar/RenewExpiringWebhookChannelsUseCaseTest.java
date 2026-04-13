package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.AuthTokenGateway;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenewExpiringWebhookChannelsUseCaseTest {

    @Mock
    WebhookChannelGateway webhookChannelGateway;

    @Mock
    CalendarWebhookGateway calendarWebhookGateway;

    @Mock
    AuthTokenGateway authTokenGateway;

    @InjectMocks
    RenewExpiringWebhookChannelsUseCase renewUseCase;

    private static final String WEBHOOK_BASE_URL = "https://githa.com";

    @BeforeEach
    void setUp() {
        renewUseCase.webhookBaseUrl = WEBHOOK_BASE_URL;
    }

    @Test
    void shouldRenewExpiringChannelsFaultTolerantly() {
        CalendarWebhookChannel expiring1 = CalendarWebhookChannel.builder()
                .channelId("old-1")
                .resourceId("res-1")
                .userEmail("user1@example.com")
                .build();
        CalendarWebhookChannel expiring2 = CalendarWebhookChannel.builder()
                .channelId("old-2")
                .resourceId("res-2")
                .userEmail("user2@example.com")
                .build();

        when(webhookChannelGateway.findExpiringBefore(anyLong())).thenReturn(List.of(expiring1, expiring2));
        
        // Mock token fetching from REST
        when(authTokenGateway.fetchFreshAccessToken("user1@example.com")).thenReturn("token1");
        when(authTokenGateway.fetchFreshAccessToken("user2@example.com")).thenReturn("token2");

        // Mock registration successes
        when(calendarWebhookGateway.registerChannel(eq("token1"), eq("user1@example.com"), eq(WEBHOOK_BASE_URL)))
                .thenReturn(CalendarWebhookChannel.builder().channelId("new-1").build());
        when(calendarWebhookGateway.registerChannel(eq("token2"), eq("user2@example.com"), eq(WEBHOOK_BASE_URL)))
                .thenReturn(CalendarWebhookChannel.builder().channelId("new-2").build());

        renewUseCase.execute();

        verify(calendarWebhookGateway).registerChannel("token1", "user1@example.com", WEBHOOK_BASE_URL);
        verify(calendarWebhookGateway).registerChannel("token2", "user2@example.com", WEBHOOK_BASE_URL);
        verify(webhookChannelGateway).deleteByChannelId("old-1");
        verify(webhookChannelGateway).deleteByChannelId("old-2");
    }

    @Test
    void shouldContinueIfOneChannelFails() {
        CalendarWebhookChannel expiring1 = CalendarWebhookChannel.builder()
                .channelId("old-1")
                .userEmail("user1@example.com")
                .build();
        CalendarWebhookChannel expiring2 = CalendarWebhookChannel.builder()
                .channelId("old-2")
                .userEmail("user2@example.com")
                .build();

        when(webhookChannelGateway.findExpiringBefore(anyLong())).thenReturn(List.of(expiring1, expiring2));
        
        // First one fails
        when(authTokenGateway.fetchFreshAccessToken("user1@example.com")).thenThrow(new RuntimeException("API Error"));

        // Second one succeeds
        when(authTokenGateway.fetchFreshAccessToken("user2@example.com")).thenReturn("token2");
        when(calendarWebhookGateway.registerChannel(eq("token2"), eq("user2@example.com"), eq(WEBHOOK_BASE_URL)))
                .thenReturn(CalendarWebhookChannel.builder().channelId("new-2").build());

        renewUseCase.execute();

        // Failed one shouldn't stop the loop
        verify(calendarWebhookGateway).registerChannel("token2", "user2@example.com", WEBHOOK_BASE_URL);
    }
}
