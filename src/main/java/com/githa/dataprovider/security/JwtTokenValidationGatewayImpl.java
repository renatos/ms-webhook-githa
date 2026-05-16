package com.githa.dataprovider.security;

import com.githa.core.domain.SessionIdentity;
import com.githa.core.gateway.TokenValidationGateway;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class JwtTokenValidationGatewayImpl implements TokenValidationGateway {

    @Inject
    JWTParser parser;

    @ConfigProperty(name = "jwt.secret")
    String jwtSecret;

    @Override
    public Optional<SessionIdentity> validate(String token) {
        try {
            // Verify and parse the token
            JsonWebToken jwt = parser.verify(token, "githa-backend");
            
            String email = jwt.getSubject();
            // We'll use 0L as default accountGroupId for now if not present in token,
            // but we'll try to extract it if we decide to add it to claims later.
            // For now, most tokens in Githa have accountGroupId as a claim or we can infer it.
            Long accountGroupId = jwt.getClaim("accountGroupId") != null ? 
                    ((Number) jwt.getClaim("accountGroupId")).longValue() : 0L;

            log.info("Token validated for user: {} in group {}", email, accountGroupId);

            return Optional.of(SessionIdentity.builder()
                    .login(email)
                    .accountGroupId(accountGroupId)
                    .connectedAt(LocalDateTime.now())
                    .build());

        } catch (ParseException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            return Optional.empty();
        }
    }
}
