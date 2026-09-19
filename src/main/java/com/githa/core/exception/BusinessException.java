package com.githa.core.exception;

import com.githa.error.core.Severity;

/**
 * Exceção de negócio do MS Webhook Githa, estendendo a base do githa-error-core.
 */
public class BusinessException extends com.githa.error.core.BusinessException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, String title) {
        super(message, title);
    }

    public BusinessException(String message, String title, Severity severity) {
        super(message, title, severity);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(String message, String title, Severity severity, Throwable cause) {
        super(message, title, severity, cause);
    }
}