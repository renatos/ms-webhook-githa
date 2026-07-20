package com.githa.entrypoint.api.config;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserMdcFilterTest {

    private UserMdcFilter filter;
    private SecurityIdentity securityIdentity;
    private ContainerRequestContext requestContext;
    private ContainerResponseContext responseContext;

    @BeforeEach
    void setUp() {
        filter = new UserMdcFilter();
        securityIdentity = mock(SecurityIdentity.class);
        filter.securityIdentity = securityIdentity;
        requestContext = mock(ContainerRequestContext.class);
        responseContext = mock(ContainerResponseContext.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPutLoginInMdcForAuthenticatedUser() throws IOException {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@user.com");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);

        filter.filter(requestContext);

        assertEquals("test@user.com", MDC.get("login"));
    }

    @Test
    void shouldNotPutLoginInMdcForAnonymousUser() throws IOException {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        filter.filter(requestContext);

        assertNull(MDC.get("login"));
    }

    @Test
    void shouldNotPutLoginInMdcIfPrincipalIsNull() throws IOException {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(null);

        filter.filter(requestContext);

        assertNull(MDC.get("login"));
    }

    @Test
    void shouldClearMdcOnResponseFilter() throws IOException {
        MDC.put("login", "test@user.com");

        filter.filter(requestContext, responseContext);

        assertNull(MDC.get("login"));
    }
}
