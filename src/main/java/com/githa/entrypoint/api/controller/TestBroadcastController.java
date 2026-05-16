package com.githa.entrypoint.api.controller;

import com.githa.core.usecase.calendar.BroadcastEventUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/internal/test")
@ApplicationScoped
public class TestBroadcastController {

    @Inject
    BroadcastEventUseCase broadcastEventUseCase;

    @POST
    @Path("/broadcast")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testBroadcast(TestRequest request) {
        log.info("[TEST-BROADCAST] Triggering test notification. User: {}, Role: {}", 
                request.getTargetLogin(), request.getTargetRole());
        
        try {
            // GroupId is optional and might not exist yet
            Long groupId = request.getAccountGroupId();
            
            broadcastEventUseCase.execute(groupId, request.getTargetLogin(), request.getTargetRole(), request.getPayload());
            
            return Response.ok()
                    .entity("{\"status\": \"Message sent\"}")
                    .build();
        } catch (Exception e) {
            log.error("Test broadcast failed", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Data
    public static class TestRequest {
        private Long accountGroupId;
        private String targetLogin;
        private String targetRole;
        private Object payload;
    }
}
