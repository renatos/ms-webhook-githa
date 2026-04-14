package com.githa.dataprovider.client.core;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.githa.dataprovider.client.core.GithaCoreRestClient")
@Path("/internal")
public interface GithaCoreRestClient {

    @GET
    @Path("/auth/google/access-token")
    @Produces(MediaType.TEXT_PLAIN)
    String fetchFreshAccessToken(@QueryParam("email") String email);

    @POST
    @Path("/webhooks/sync")
    @Consumes(MediaType.APPLICATION_JSON)
    void forwardCalendarWebhook(WebhookSyncRequest request);

    record WebhookSyncRequest(String channelId) {}
}
