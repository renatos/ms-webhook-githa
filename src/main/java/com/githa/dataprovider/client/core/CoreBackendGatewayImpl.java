package com.githa.dataprovider.client.core;

import com.githa.core.gateway.CoreBackendGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@RequiredArgsConstructor
public class CoreBackendGatewayImpl implements CoreBackendGateway {

    @RestClient
    GithaCoreRestClient restClient;

    @Override
    public void forwardCalendarWebhook(String channelId) {
        restClient.forwardCalendarWebhook(channelId);
    }
}
