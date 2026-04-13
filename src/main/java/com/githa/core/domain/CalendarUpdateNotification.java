package com.githa.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WebSocket notifications when a calendar update occurs via inbound sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarUpdateNotification {
    private String type; // e.g., "CALENDAR_SYNC"
    private String action; // e.g., "UPDATED", "DELETED"
    private Long appointmentId;
    private String eventId;
    private String message;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private long timestamp;
}
