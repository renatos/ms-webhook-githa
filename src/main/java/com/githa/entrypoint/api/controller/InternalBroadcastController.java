package com.githa.entrypoint.api.controller;

import com.githa.core.usecase.calendar.BroadcastEventUseCase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/internal/events")
@ApplicationScoped
public class InternalBroadcastController {

    @Inject
    BroadcastEventUseCase broadcastEventUseCase;

    @POST
    @Path("/broadcast")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response broadcast(BroadcastRequest request) {
        log.info("Received internal broadcast request for account group {}", request.getAccountGroupId());
        
        try {
            broadcastEventUseCase.execute(request.getAccountGroupId(), request.getPayload());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Failed to execute internal broadcast", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Data
    public static class BroadcastRequest {
        private Long accountGroupId;
        private String payload;
    }
}
