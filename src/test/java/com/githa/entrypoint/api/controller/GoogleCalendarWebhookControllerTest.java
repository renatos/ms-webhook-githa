package com.githa.entrypoint.api.controller;

import com.githa.core.domain.CalendarWebhookChannel;
import com.githa.core.usecase.calendar.ForwardWebhookPayloadUseCase;
import com.githa.core.usecase.calendar.ValidateWebhookUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

@QuarkusTest
class GoogleCalendarWebhookControllerTest {

    @InjectMock
    ValidateWebhookUseCase validateWebhookUseCase;

    @InjectMock
    ForwardWebhookPayloadUseCase forwardWebhookPayloadUseCase;

    @Test
    void shouldReturn401ForUnknownChannel() {
        when(validateWebhookUseCase.execute("unknown", "token")).thenThrow(new SecurityException("Unknown channel"));

        given()
                .header("X-Goog-Channel-ID", "unknown")
                .header("X-Goog-Channel-Token", "token")
                .header("X-Goog-Resource-State", "exists")
                .contentType("application/json")
                .when()
                .post("/api/webhooks/google/calendar")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn401ForTokenMismatch() {
        when(validateWebhookUseCase.execute("chan-1", "wrong-token")).thenThrow(new SecurityException("Token mismatch"));

        given()
                .header("X-Goog-Channel-ID", "chan-1")
                .header("X-Goog-Channel-Token", "wrong-token")
                .header("X-Goog-Resource-State", "exists")
                .contentType("application/json")
                .when()
                .post("/api/webhooks/google/calendar")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn200AndForwardForValidNotification() {
        when(validateWebhookUseCase.execute("chan-1", "valid-token")).thenReturn(CalendarWebhookChannel.builder().build());

        given()
                .header("X-Goog-Channel-ID", "chan-1")
                .header("X-Goog-Channel-Token", "valid-token")
                .header("X-Goog-Resource-State", "exists")
                .contentType("application/json")
                .when()
                .post("/api/webhooks/google/calendar")
                .then()
                .statusCode(200);

        verify(forwardWebhookPayloadUseCase).execute("chan-1");
    }

    @Test
    void shouldReturn200WithoutForwardForSyncState() {
        when(validateWebhookUseCase.execute("chan-1", "valid-token")).thenReturn(CalendarWebhookChannel.builder().build());

        given()
                .header("X-Goog-Channel-ID", "chan-1")
                .header("X-Goog-Channel-Token", "valid-token")
                .header("X-Goog-Resource-State", "sync")
                .contentType("application/json")
                .when()
                .post("/api/webhooks/google/calendar")
                .then()
                .statusCode(200);

        verify(forwardWebhookPayloadUseCase, never()).execute(anyString());
    }
}
