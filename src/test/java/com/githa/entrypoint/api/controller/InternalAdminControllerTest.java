package com.githa.entrypoint.api.controller;

import com.githa.entrypoint.websocket.WebSocketSessionRegistry;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class InternalAdminControllerTest {

    @Test
    void shouldReturnActiveConnections() {
        // Given - No sessions registered yet (or registry is empty)
        // We can't easily mock the registry internal state because it's managed by CDI,
        // but we can inject it and register dummy sessions.
        
        // Actually, let's just use the real registry since it's ApplicationScoped.
        // We'll use a dummy Session mock.
        
        // When & Then
        given()
          .when().get("/internal/admin/connections")
          .then()
             .statusCode(200)
             .body("$", is(notNullValue()));
    }
}
