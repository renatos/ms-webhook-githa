package com.githa.core.usecase.auth;

import com.githa.core.domain.SessionIdentity;
import com.githa.core.gateway.TokenValidationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateSessionUseCaseTest {

    @Mock
    TokenValidationGateway tokenValidationGateway;

    ValidateSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidateSessionUseCase(tokenValidationGateway);
    }

    @Test
    void shouldReturnIdentityWhenTokenIsValid() {
        // Given
        String token = "valid-token";
        SessionIdentity identity = SessionIdentity.builder()
                .login("user@test.com")
                .accountGroupId(1L)
                .build();
        
        when(tokenValidationGateway.validate(token)).thenReturn(Optional.of(identity));

        // When
        SessionIdentity result = useCase.execute(token);

        // Then
        assertNotNull(result);
        assertEquals("user@test.com", result.getLogin());
        assertEquals(1L, result.getAccountGroupId());
    }

    @Test
    void shouldThrowSecurityExceptionWhenTokenIsInvalid() {
        // Given
        String token = "invalid-token";
        when(tokenValidationGateway.validate(token)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(SecurityException.class, () -> useCase.execute(token));
    }

    @Test
    void shouldThrowSecurityExceptionWhenTokenIsEmpty() {
        // When & Then
        assertThrows(SecurityException.class, () -> useCase.execute(""));
        assertThrows(SecurityException.class, () -> useCase.execute(null));
        
        verifyNoInteractions(tokenValidationGateway);
    }
}
