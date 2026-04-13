package com.githa.core.usecase.calendar;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.WebhookChannelGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class ValidateWebhookUseCase {

    private final WebhookChannelGateway webhookChannelGateway;

    public CalendarWebhookChannel execute(String channelId, String channelToken) {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("Missing channel ID");
        }

        Optional<CalendarWebhookChannel> channelOpt = webhookChannelGateway.findByChannelId(channelId);
        if (channelOpt.isEmpty()) {
            throw new SecurityException("Received notification for unknown channel ID");
        }

        CalendarWebhookChannel channel = channelOpt.get();
        if (channelToken == null || !channelToken.equals(channel.getVerificationToken())) {
            throw new SecurityException("Token mismatch for channel");
        }

        return channel;
    }
}
