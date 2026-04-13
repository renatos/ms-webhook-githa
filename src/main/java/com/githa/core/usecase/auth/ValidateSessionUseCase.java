package com.githa.core.usecase.auth;

import com.githa.core.gateway.AuthTokenGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case to validate a session token.
 * For now, it delegates to AuthTokenGateway or validates locally if secret is shared.
 * In this architecture, it will return the tenant identifier (accountGroupId).
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ValidateSessionUseCase {

    // For now we might need a way to validate the token. 
    // If we have the secret, we can do it locally.
    
    public Long execute(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Missing token");
        }
        
        // TODO: Implement actual JWT validation logic.
        // For the POC/Initial migration, we might default to 0L if valid.
        // In a real scenario, we'd extract the accountGroupId from the token claims.
        
        log.info("Validating session token");
        
        // Dummy implementation for now - will be refined when we implement the Gateway/Provider
        return 0L; 
    }
}
