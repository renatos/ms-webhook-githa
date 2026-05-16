package com.githa.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Domain model representing the identity and metadata of an active WebSocket session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionIdentity {
    private String sessionId;
    private String login;
    private Long accountGroupId;
    private List<String> roles;
    private LocalDateTime connectedAt;
}
