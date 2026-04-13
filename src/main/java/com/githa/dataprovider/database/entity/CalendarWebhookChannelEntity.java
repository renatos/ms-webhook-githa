package com.githa.dataprovider.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_webhook_channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarWebhookChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false, unique = true)
    private String channelId;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "verification_token", nullable = false)
    private String verificationToken;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "expiration_epoch_ms", nullable = false)
    private Long expirationEpochMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
