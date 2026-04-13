package com.githa.dataprovider.database.gateway;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.WebhookChannelGateway;
import com.githa.dataprovider.database.entity.CalendarWebhookChannelEntity;
import com.githa.dataprovider.database.mapper.CalendarWebhookChannelEntityMapper;
import com.githa.dataprovider.database.repository.CalendarWebhookChannelRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class WebhookChannelGatewayImpl implements WebhookChannelGateway {

    private final CalendarWebhookChannelRepository repository;
    private final CalendarWebhookChannelEntityMapper mapper;

    @Override
    public CalendarWebhookChannel save(CalendarWebhookChannel channel) {
        CalendarWebhookChannelEntity entity = mapper.toEntity(channel);
        if (entity.getId() == null) {
            repository.persist(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<CalendarWebhookChannel> findByChannelId(String channelId) {
        return repository.findByChannelId(channelId).map(mapper::toDomain);
    }

    @Override
    public Optional<CalendarWebhookChannel> findActiveByUserEmail(String userEmail) {
        return repository.findActiveByUserEmail(userEmail, Instant.now().toEpochMilli())
                .map(mapper::toDomain);
    }

    @Override
    public List<CalendarWebhookChannel> findExpiringBefore(long epochMs) {
        return repository.findExpiringBefore(epochMs).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByChannelId(String channelId) {
        repository.deleteByChannelId(channelId);
    }
}
