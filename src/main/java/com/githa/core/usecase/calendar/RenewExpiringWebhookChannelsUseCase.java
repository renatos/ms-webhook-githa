package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.AuthTokenGateway;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RenewExpiringWebhookChannelsUseCase {

    private final WebhookChannelGateway webhookChannelGateway;
    private final CalendarWebhookGateway calendarWebhookGateway;
    private final AuthTokenGateway authTokenGateway;

    @ConfigProperty(name = "app.webhook.base-url")
    String webhookBaseUrl;

    public void execute() {
        // Find channels expiring in the next 24 hours
        long threshold = Instant.now().plus(24, ChronoUnit.HOURS).toEpochMilli();
        List<CalendarWebhookChannel> expiring = webhookChannelGateway.findExpiringBefore(threshold);

        log.info("Found {} webhook channels expiring before threshold", expiring.size());

        for (CalendarWebhookChannel channel : expiring) {
            renewChannel(channel);
        }
    }

    private void renewChannel(CalendarWebhookChannel oldChannel) {
        log.info("Renewing webhook channel {} for user {}", oldChannel.getChannelId(), oldChannel.getUserEmail());

        try {
            // Fetch fresh token from Core Backend via REST
            String accessToken = authTokenGateway.fetchFreshAccessToken(oldChannel.getUserEmail());

            // 1. Register new channel
            CalendarWebhookChannel newChannel = calendarWebhookGateway.registerChannel(accessToken, oldChannel.getUserEmail(), webhookBaseUrl);
            webhookChannelGateway.save(newChannel);

            // 2. Stop old channel
            calendarWebhookGateway.stopChannel(oldChannel.getChannelId(), oldChannel.getResourceId(), accessToken);
            webhookChannelGateway.deleteByChannelId(oldChannel.getChannelId());

            log.info("Successfully renewed channel for user {}. New channel ID: {}", oldChannel.getUserEmail(), newChannel.getChannelId());
        } catch (Exception e) {
            log.error("Failed to renew webhook channel for user {}: {}", oldChannel.getUserEmail(), e.getMessage());
            // Optionally delete the channel if the backend responds that the user is no longer connected (e.g., HTTP 404).
            // That specific logic will be handled by the REST client throwing a Specific exception.
        }
    }
}
