package com.githa.core.gateway;

import com.githa.core.domain.CalendarWebhookChannel;

/**
 * Interface for interacting with Google Calendar API to manage webhook channels.
 */
public interface CalendarWebhookGateway {
    /**
     * Registers a new push notification channel for the user's primary calendar.
     * @param accessToken Valid Google OAuth access token.
     * @param userEmail The user's email for generating a verification token.
     * @param webhookBaseUrl Base URL of this system (must be public HTTPS).
     * @return The registered channel details.
     */
    CalendarWebhookChannel registerChannel(String accessToken, String userEmail, String webhookBaseUrl);

    /**
     * Stops an existing push notification channel.
     * @param channelId Unique identifier for the channel.
     * @param resourceId Unique identifier for the resource being watched.
     * @param accessToken Valid Google OAuth access token.
     */
    void stopChannel(String channelId, String resourceId, String accessToken);
}
