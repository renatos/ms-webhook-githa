package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.githa.core.gateway.WebhookChannelGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Use case to stop a Google Calendar webhook channel for a user.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class StopWebhookChannelUseCase {

    private final WebhookChannelGateway webhookChannelGateway;
    private final CalendarWebhookGateway calendarWebhookGateway;

    public void execute(String userEmail, String accessToken) {
        log.info("Stopping webhook channel for user {}", userEmail);
        
        Optional<CalendarWebhookChannel> channelOpt = webhookChannelGateway.findActiveByUserEmail(userEmail);
        if (channelOpt.isEmpty()) {
            log.info("No active webhook channel found for user {}", userEmail);
            return;
        }

        CalendarWebhookChannel channel = channelOpt.get();
        log.info("Stopping channel {} (ResourceId: {})", channel.getChannelId(), channel.getResourceId());
        
        // 1. Call Google API to stop receiving notifications
        calendarWebhookGateway.stopChannel(channel.getChannelId(), channel.getResourceId(), accessToken);
        
        // 2. Delete the record from our database
        webhookChannelGateway.deleteByChannelId(channel.getChannelId());
        
        log.info("Webhook channel stopped successfully for user {}", userEmail);
    }
}
