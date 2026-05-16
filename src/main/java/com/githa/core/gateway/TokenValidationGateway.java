package com.githa.core.gateway;

import com.githa.core.domain.SessionIdentity;

import java.util.Optional;

public interface TokenValidationGateway {
    /**
     * Validates a JWT token and returns the session identity if valid.
     */
    Optional<SessionIdentity> validate(String token);
}
