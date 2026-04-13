package com.githa.entrypoint.cron;

import com.githa.core.usecase.calendar.RenewExpiringWebhookChannelsUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.smallrye.common.annotation.RunOnVirtualThread;

/**
 * Scheduled job to renew Google Calendar webhook channels before they expire.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class WebhookRenewalScheduledJob {

    private final RenewExpiringWebhookChannelsUseCase renewUseCase;

    @RunOnVirtualThread
    @Scheduled(cron = "0 0 */6 * * ?") // Every 6 hours
    public void renewWebhookChannels() {
        log.info("Starting scheduled webhook channel renewal job in ms-webhook-githa");
        try {
            renewUseCase.execute();
            log.info("Scheduled webhook channel renewal job completed successfully");
        } catch (Exception e) {
            log.error("Failed to execute scheduled webhook renewal job", e);
        }
    }
}
