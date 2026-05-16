package com.githa.dataprovider.security;

import com.githa.core.domain.SessionIdentity;
import com.githa.core.gateway.TokenValidationGateway;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class JwtTokenValidationGatewayImpl implements TokenValidationGateway {

    @Inject
    JWTParser jwtParser;

    @Override
    public Optional<SessionIdentity> validate(String token) {
        try {
            // JWTParser uses the configuration from application.properties 
            // (mp.jwt.verify.issuer, smallrye.jwt.verify.key.location, etc.)
            JsonWebToken jwt = jwtParser.parse(token);
            
            String email = jwt.getSubject();
            Object claimValue = jwt.getClaim("accountGroupId");
            Long accountGroupId = claimValue != null ? ((Number) claimValue).longValue() : 1L;
            
            if (email == null) {
                log.warn("Token valid but missing subject (email)");
                return Optional.empty();
            }

            log.info("Token validated for user: {} (Group: {})", email, accountGroupId);

            return Optional.of(SessionIdentity.builder()
                    .login(email)
                    .accountGroupId(accountGroupId)
                    .connectedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("JWT validation failed via SmallRye Parser: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
