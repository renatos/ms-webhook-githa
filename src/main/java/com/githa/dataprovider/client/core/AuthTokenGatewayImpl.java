package com.githa.dataprovider.client.core;

import com.githa.core.gateway.AuthTokenGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@RequiredArgsConstructor
public class AuthTokenGatewayImpl implements AuthTokenGateway {

    @RestClient
    GithaCoreRestClient restClient;

    @Override
    public String fetchFreshAccessToken(String userEmail) {
        return restClient.fetchFreshAccessToken(userEmail);
    }
}
