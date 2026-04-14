package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.WebhookChannelGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ValidateWebhookUseCase {

    private final WebhookChannelGateway webhookChannelGateway;

    public CalendarWebhookChannel execute(String channelId, String channelToken) {
        if (channelId == null || channelId.isBlank()) {
            log.warn("Webhook validation failed: Missing channel ID");
            throw new IllegalArgumentException("Missing channel ID");
        }

        Optional<CalendarWebhookChannel> channelOpt = webhookChannelGateway.findByChannelId(channelId);
        if (channelOpt.isEmpty()) {
            log.warn("Webhook validation failed: Received notification for unknown channel ID: {}", channelId);
            throw new SecurityException("Received notification for unknown channel ID");
        }

        CalendarWebhookChannel channel = channelOpt.get();
        if (channelToken == null || !channelToken.equals(channel.getVerificationToken())) {
            log.warn("Webhook validation failed: Token mismatch for channel: {}", channelId);
            throw new SecurityException("Token mismatch for channel");
        }

        log.info("Webhook validation successful for channel: {} (User: {})", channelId, channel.getUserEmail());
        return channel;
    }
}
