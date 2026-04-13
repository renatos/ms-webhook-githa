package com.githa.core.gateway;

import com.githa.core.domain.CalendarWebhookChannel;

import java.util.List;
import java.util.Optional;

/**
 * Contract for persisting and retrieving Google Calendar webhook channel metadata.
 */
public interface WebhookChannelGateway {
    CalendarWebhookChannel save(CalendarWebhookChannel channel);
    Optional<CalendarWebhookChannel> findByChannelId(String channelId);
    Optional<CalendarWebhookChannel> findActiveByUserEmail(String userEmail);
    List<CalendarWebhookChannel> findExpiringBefore(long epochMs);
    void deleteByChannelId(String channelId);
}
