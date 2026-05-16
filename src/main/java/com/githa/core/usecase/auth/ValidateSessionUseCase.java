package com.githa.core.usecase.auth;

import com.githa.core.domain.SessionIdentity;
import com.githa.core.gateway.TokenValidationGateway;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Use case to validate a session token.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ValidateSessionUseCase {

    private final TokenValidationGateway tokenValidationGateway;
    
    public SessionIdentity execute(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Missing token");
        }
        
        log.info("Validating session token");
        
        return tokenValidationGateway.validate(token)
                .orElseThrow(() -> new SecurityException("Invalid or expired token"));
    }
}
