package com.githa.entrypoint.api.controller;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.gateway.WebhookChannelGateway;
import com.githa.core.usecase.calendar.RegisterWebhookChannelUseCase;
import com.githa.core.usecase.calendar.StopWebhookChannelUseCase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal controller to manage webhooks for the main monolith.
 */
@Slf4j
@Path("/internal/webhooks")
@ApplicationScoped
public class InternalWebhookController {

    @Inject
    RegisterWebhookChannelUseCase registerUseCase;

    @Inject
    StopWebhookChannelUseCase stopUseCase;

    @Inject
    WebhookChannelGateway webhookChannelGateway;

    @POST
    @Path("/google/calendar/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response register(WebhookRegistrationRequest request) {
        log.info("Received internal request to register webhook for user {}", request.getUserEmail());
        try {
            CalendarWebhookChannel channel = registerUseCase.execute(request.getAccessToken(), request.getUserEmail());
            log.info("Webhook channel registered successfully for user {}: channelId={}", 
                    request.getUserEmail(), channel.getChannelId());
            return Response.ok(channel).build();
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            log.error("Failed to register webhook for user {}: [{}] {}", 
                    request.getUserEmail(), root.getClass().getSimpleName(), root.getMessage(), e);
            return Response.serverError()
                    .entity(java.util.Map.of(
                            "error", root.getClass().getSimpleName(),
                            "message", root.getMessage() != null ? root.getMessage() : "Unknown error"))
                    .build();
        }
    }

    @POST
    @Path("/google/calendar/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response stop(WebhookStopRequest request) {
        log.info("Received internal request to stop webhook for user {}", request.getUserEmail());
        try {
            stopUseCase.execute(request.getUserEmail(), request.getAccessToken());
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Failed to stop webhook", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{channelId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelById(@PathParam("channelId") String channelId) {
        log.info("Internal request to fetch channel by ID: {}", channelId);
        return webhookChannelGateway.findByChannelId(channelId)
                .map(channel -> Response.ok(channel).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Data
    public static class WebhookRegistrationRequest {
        private String accessToken;
        private String userEmail;
    }

    @Data
    public static class WebhookStopRequest {
        private String accessToken;
        private String userEmail;
    }
}
