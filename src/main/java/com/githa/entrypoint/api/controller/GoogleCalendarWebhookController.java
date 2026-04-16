package com.githa.entrypoint.api.controller;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.usecase.calendar.ForwardWebhookPayloadUseCase;
import com.githa.core.usecase.calendar.ValidateWebhookUseCase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Public controller to receive Google Calendar webhook notifications.
 */
@Slf4j
@Path("/api/webhooks/google/calendar")
@ApplicationScoped
public class GoogleCalendarWebhookController {

    @Inject
    ValidateWebhookUseCase validateWebhookUseCase;

    @Inject
    ForwardWebhookPayloadUseCase forwardWebhookPayloadUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response handleWebhook(
            @HeaderParam("X-Goog-Channel-ID") String channelId,
            @HeaderParam("X-Goog-Channel-Token") String channelToken,
            @HeaderParam("X-Goog-Resource-ID") String resourceId,
            @HeaderParam("X-Goog-Resource-State") String resourceState,
            @HeaderParam("X-Goog-Message-Number") String messageNumber,
            @HeaderParam("X-Goog-Resource-URI") String resourceUri) {

        log.info("Received Google Calendar webhook. ChannelId: {}, ResourceState: {}, ResourceId: {}, MessageNumber: {}, ResourceUri: {}", 
                channelId, resourceState, resourceId, messageNumber, resourceUri);

        try {
            // 1. Validate channel and token
            CalendarWebhookChannel channel = validateWebhookUseCase.execute(channelId, channelToken);

            // 2. Handle resource state
            if ("sync".equalsIgnoreCase(resourceState)) {
                log.info("Channel registration confirmation for {}", channelId);
                return Response.ok().build();
            }

            // 3. Process actual changes by forwarding to core
            forwardWebhookPayloadUseCase.execute(channelId);

            return Response.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook attempt: {}", e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Failed to process inbound sync for channel {}: {}", channelId, e.getMessage());
            // We still return 200 so Google doesn't keep retrying the notification 
            // if it's a processing error (it's already validated)
            return Response.ok().build();
        }
    }
}
