package com.githa.dataprovider.database.mapper;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.dataprovider.database.entity.CalendarWebhookChannelEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface CalendarWebhookChannelEntityMapper {
    CalendarWebhookChannel toDomain(CalendarWebhookChannelEntity entity);
    CalendarWebhookChannelEntity toEntity(CalendarWebhookChannel domain);
}
