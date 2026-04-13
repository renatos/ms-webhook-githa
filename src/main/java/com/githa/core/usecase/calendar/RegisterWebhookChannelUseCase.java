package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Use case to register a Google Calendar webhook channel for a user.
 * Idempotent: returns existing active channel if present.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RegisterWebhookChannelUseCase {

    private final WebhookChannelGateway webhookChannelGateway;
    private final CalendarWebhookGateway calendarWebhookGateway;

    @ConfigProperty(name = "app.webhook.base-url")
    String webhookBaseUrl;

    public CalendarWebhookChannel execute(String accessToken, String userEmail) {
        log.info("Checking for existing active webhook channel for user {}", userEmail);
        
        Optional<CalendarWebhookChannel> activeChannel = webhookChannelGateway.findActiveByUserEmail(userEmail);
        if (activeChannel.isPresent()) {
            log.info("Active channel already exists for user {}: {}", userEmail, activeChannel.get().getChannelId());
            return activeChannel.get();
        }

        log.info("No active channel found. Registering new channel for user {}", userEmail);
        CalendarWebhookChannel newChannel = calendarWebhookGateway.registerChannel(accessToken, userEmail, webhookBaseUrl);
        
        return webhookChannelGateway.save(newChannel);
    }
}
