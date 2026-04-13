package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterWebhookChannelUseCaseTest {

    @Mock
    WebhookChannelGateway webhookChannelGateway;

    @Mock
    CalendarWebhookGateway calendarWebhookGateway;

    @InjectMocks
    RegisterWebhookChannelUseCase registerWebhookChannelUseCase;

    private static final String ACCESS_TOKEN = "access-token";
    private static final String USER_EMAIL = "user@example.com";
    private static final String WEBHOOK_BASE_URL = "https://githa.com";

    @BeforeEach
    void setUp() {
        registerWebhookChannelUseCase.webhookBaseUrl = WEBHOOK_BASE_URL;
    }

    @Test
    void shouldReturnExistingChannelIfActive() {
        CalendarWebhookChannel existingChannel = CalendarWebhookChannel.builder()
                .channelId("existing-id")
                .userEmail(USER_EMAIL)
                .build();

        when(webhookChannelGateway.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(existingChannel));

        CalendarWebhookChannel result = registerWebhookChannelUseCase.execute(ACCESS_TOKEN, USER_EMAIL);

        assertEquals("existing-id", result.getChannelId());
        verifyNoInteractions(calendarWebhookGateway);
        verify(webhookChannelGateway, never()).save(any());
    }

    @Test
    void shouldRegisterNewChannelIfNoneActive() {
        CalendarWebhookChannel newChannel = CalendarWebhookChannel.builder()
                .channelId("new-id")
                .userEmail(USER_EMAIL)
                .build();

        when(webhookChannelGateway.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(calendarWebhookGateway.registerChannel(eq(ACCESS_TOKEN), eq(USER_EMAIL), eq(WEBHOOK_BASE_URL)))
                .thenReturn(newChannel);
        when(webhookChannelGateway.save(newChannel)).thenReturn(newChannel);

        CalendarWebhookChannel result = registerWebhookChannelUseCase.execute(ACCESS_TOKEN, USER_EMAIL);

        assertEquals("new-id", result.getChannelId());
        verify(calendarWebhookGateway).registerChannel(ACCESS_TOKEN, USER_EMAIL, WEBHOOK_BASE_URL);
        verify(webhookChannelGateway).save(newChannel);
    }
}
