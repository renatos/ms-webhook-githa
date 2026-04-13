package com.githa.entrypoint.api.controller;

import com.githa.core.usecase.calendar.ClearConnectionsUseCase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/internal/admin")
@ApplicationScoped
public class InternalAdminController {

    @Inject
    ClearConnectionsUseCase clearConnectionsUseCase;

    @DELETE
    @Path("/connections")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response clearConnections() {
        log.info("Received request to clear all connections");
        try {
            clearConnectionsUseCase.execute();
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Failed to clear connections", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
