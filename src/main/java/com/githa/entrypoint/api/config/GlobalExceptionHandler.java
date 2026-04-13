package com.githa.entrypoint.api.config;

import com.githa.core.exception.BusinessException;
import com.githa.core.exception.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);


    @ServerExceptionMapper(ResourceNotFoundException.class)
    public Response handleResourceNotFoundException(ResourceNotFoundException ex) {
        LOG.warnf("Resource not found: %s", ex.getMessage());
        return buildResponse(Response.Status.NOT_FOUND.getStatusCode(), Response.Status.NOT_FOUND.getReasonPhrase(),
                ex.getMessage());
    }

    @ServerExceptionMapper(BusinessException.class)
    public Response handleBusinessException(BusinessException ex) {
        LOG.warnf("Business exception: %s", ex.getMessage(), ex);
        // 422 Unprocessable Entity
        return buildResponse(422, "Unprocessable Entity", ex.getMessage());
    }

    @ServerExceptionMapper(IllegalArgumentException.class)
    public Response handleIllegalArgumentException(IllegalArgumentException ex) {
        LOG.error("Illegal argument exception", ex);
        return buildResponse(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request", ex.getMessage());
    }

    @ServerExceptionMapper(jakarta.ws.rs.WebApplicationException.class)
    public Response handleWebApplicationException(jakarta.ws.rs.WebApplicationException ex) {
        LOG.error("Web application exception", ex);
            LOG.errorf("Web Application Error (%d): %s", ex.getResponse().getStatus(), ex.getMessage());
        return buildResponse(ex.getResponse().getStatus(),
                ex.getResponse().getStatusInfo().getReasonPhrase(),
                ex.getMessage());
    }

    @ServerExceptionMapper(Exception.class)
    public Response handleException(Exception ex) {
        LOG.error("Unexpected error occurred", ex);
        LOG.error("Unexpected Critical Error", ex);
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Erro: " + ex.getMessage());
    }

    @ServerExceptionMapper(jakarta.persistence.PersistenceException.class)
    public Response handlePersistenceException(jakarta.persistence.PersistenceException ex) {
        Throwable cause = ex;
        String message = ex.getMessage();
        int maxDepth = 10;
        int currentDepth = 0;

        // Loop to find the detailed message from the database
        while (cause != null && currentDepth < maxDepth) {
            if (cause.getMessage() != null && cause.getMessage().contains("duplicate key value")) {
                message = cause.getMessage();
                break;
            }
            cause = cause.getCause();
            currentDepth++;
        }

        if (message != null && message.contains("duplicate key value")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern
                    .compile("Key \\((.*?)\\)=\\((.*?)\\) already exists");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String field = matcher.group(1);
                String value = matcher.group(2);
                return buildResponse(Response.Status.CONFLICT.getStatusCode(), "Conflict",
                        String.format("O código '%s' para esse registro já existe na base de dados.", value));
            }
            // Fallback if regex doesn't match but we know it's a duplicate key
            return buildResponse(Response.Status.CONFLICT.getStatusCode(), "Conflict",
                    "Registro duplicado na base de dados.");
        }

        LOG.error("Persistence exception", ex);
        LOG.error("Persistence Error", ex);
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Erro de persistência: " + ex.getMessage());
    }

    private Response buildResponse(int statusCode, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", statusCode);
        body.put("error", error);
        body.put("message", message);
        return Response.status(statusCode).entity(body).build();
    }
}