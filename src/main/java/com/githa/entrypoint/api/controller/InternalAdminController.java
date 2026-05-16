package com.githa.entrypoint.api.controller;

import com.githa.core.usecase.calendar.ClearConnectionsUseCase;
import com.githa.entrypoint.api.dto.ConnectionReportDTO;
import com.githa.entrypoint.websocket.WebSocketSessionRegistry;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Path("/internal/admin")
@ApplicationScoped
public class InternalAdminController {

    @Inject
    ClearConnectionsUseCase clearConnectionsUseCase;

    @Inject
    WebSocketSessionRegistry sessionRegistry;

    @GET
    @Path("/connections")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response getConnections() {
        log.info("Received request to list active connections");
        var identities = sessionRegistry.getActiveIdentities();
        
        var report = identities.stream()
                .collect(Collectors.groupingBy(id -> id.getAccountGroupId()))
                .entrySet().stream()
                .map(entry -> ConnectionReportDTO.builder()
                        .accountGroupId(entry.getKey())
                        .connections(entry.getValue())
                        .build())
                .collect(Collectors.toList());
        
        return Response.ok(report).build();
    }

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
