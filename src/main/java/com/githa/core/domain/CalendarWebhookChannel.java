package com.githa.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain POJO representing a Google Calendar push notification (webhook) channel.
 * Clean Architecture: Domain entity (Core layer). No framework annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarWebhookChannel {
    private Long id;
    private String channelId;
    private String resourceId;
    private String verificationToken;
    private String userEmail;
    private Long expirationEpochMs;
    private LocalDateTime createdAt;
}
