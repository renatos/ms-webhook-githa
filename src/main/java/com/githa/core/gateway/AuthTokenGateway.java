package com.githa.core.gateway;

public interface AuthTokenGateway {
    String fetchFreshAccessToken(String userEmail);
}
