package com.githa.core.usecase.calendar;

import com.githa.entrypoint.websocket.AppointmentSessionRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ClearConnectionsUseCase {

    private final AppointmentSessionRegistry sessionRegistry;

    public void execute() {
        log.info("Executing use case to clear all active connections");
        sessionRegistry.clearAll();
    }
}
