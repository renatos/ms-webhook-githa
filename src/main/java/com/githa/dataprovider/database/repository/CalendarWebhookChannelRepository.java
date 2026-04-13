package com.githa.dataprovider.database.repository;

import com.githa.dataprovider.database.entity.CalendarWebhookChannelEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CalendarWebhookChannelRepository extends BaseRepository<CalendarWebhookChannelEntity> {

    public Optional<CalendarWebhookChannelEntity> findByChannelId(String channelId) {
        return find("channelId", channelId).firstResultOptional();
    }

    public Optional<CalendarWebhookChannelEntity> findActiveByUserEmail(String userEmail, long currentEpochMs) {
        return find("userEmail = ?1 AND expirationEpochMs > ?2", userEmail, currentEpochMs)
                .firstResultOptional();
    }

    public List<CalendarWebhookChannelEntity> findExpiringBefore(long epochMs) {
        return find("expirationEpochMs < ?1", epochMs).list();
    }

    public void deleteByChannelId(String channelId) {
        delete("channelId", channelId);
    }
}
