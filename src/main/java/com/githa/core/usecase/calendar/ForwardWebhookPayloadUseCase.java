package com.githa.core.usecase.calendar;

import com.githa.core.gateway.CoreBackendGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ForwardWebhookPayloadUseCase {

    private final CoreBackendGateway coreBackendGateway;

    public void execute(String channelId) {
        log.info("Forwarding validated webhook payload for channelId: {}", channelId);
        try {
            coreBackendGateway.forwardCalendarWebhook(channelId);
        } catch (Exception e) {
            log.error("Failed to forward webhook to core backend. Channel: {}", channelId, e);
            throw new RuntimeException("Core backend integration failed", e);
        }
    }
}
