package com.githa.core.exception;

/**
 * Exceção de recurso não encontrado do MS Webhook Githa.
 */
public class ResourceNotFoundException extends com.githa.error.core.ResourceNotFoundException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, String title) {
        super(message, title);
    }
}