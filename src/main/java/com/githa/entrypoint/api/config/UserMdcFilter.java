package com.githa.entrypoint.api.config;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.io.IOException;

@Provider
@ApplicationScoped
@Priority(Priorities.USER)
public class UserMdcFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String MDC_KEY = "login";

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MDC.remove(MDC_KEY);
        if (securityIdentity != null && !securityIdentity.isAnonymous() && securityIdentity.getPrincipal() != null) {
            String login = securityIdentity.getPrincipal().getName();
            if (login != null && !login.isBlank()) {
                MDC.put(MDC_KEY, login);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.remove(MDC_KEY);
    }
}
