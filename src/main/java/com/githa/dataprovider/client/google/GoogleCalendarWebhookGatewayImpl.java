package com.githa.dataprovider.client.google;

import com.githa.core.common.Constants;
import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.CalendarWebhookGateway;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class GoogleCalendarWebhookGatewayImpl implements CalendarWebhookGateway {

    private static final String CHANNEL_TYPE = "web_hook";

    @ConfigProperty(name = "app.webhook.base-url")
    String webhookBaseUrl;

    @Override
    public CalendarWebhookChannel registerChannel(String accessToken, String userEmail, String webhookBaseUrlOverride) {
        try {
            Calendar service = createService(accessToken);
            
            String channelId = UUID.randomUUID().toString();
            // Use email prefix (sanitized) + UUID for verification token
            String emailPrefix = userEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
            String verificationToken = "email-" + emailPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
            String targetUrl = (webhookBaseUrlOverride != null ? webhookBaseUrlOverride : webhookBaseUrl) + "/api/webhooks/google/calendar";

            Channel content = new Channel()
                    .setId(channelId)
                    .setType(CHANNEL_TYPE)
                    .setAddress(targetUrl)
                    .setToken(verificationToken);

            log.info("Registering Google Calendar webhook channel for {}: id={}, url={}", userEmail, channelId, targetUrl);
            
            Channel registered = service.events().watch(userEmail, content).execute();
            
            log.info("Channel registered successfully. ResourceId: {}, Expiration: {}", 
                    registered.getResourceId(), registered.getExpiration());

            return CalendarWebhookChannel.builder()
                    .channelId(registered.getId())
                    .resourceId(registered.getResourceId())
                    .verificationToken(verificationToken)
                    .userEmail(userEmail)
                    .expirationEpochMs(registered.getExpiration())
                    .build();

        } catch (Exception e) {
            log.error("Error registering Google Calendar webhook channel", e);
            throw new RuntimeException("Failed to register webhook channel", e);
        }
    }

    @Override
    public void stopChannel(String channelId, String resourceId, String accessToken) {
        try {
            Calendar service = createService(accessToken);
            Channel channel = new Channel()
                    .setId(channelId)
                    .setResourceId(resourceId);

            log.info("Stopping Google Calendar webhook channel: id={}", channelId);
            service.channels().stop(channel).execute();
            log.info("Channel stopped successfully.");

        } catch (Exception e) {
            log.warn("Error stopping Google Calendar webhook channel (might already be expired): {}", e.getMessage());
            // We don't throw here to allow cleanup to continue even if Google returns 404/410
        }
    }

    private Calendar createService(String accessToken) throws GeneralSecurityException, IOException {
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(Constants.APPLICATION_NAME)
                .build();
    }
}
