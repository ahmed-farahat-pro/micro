package com.cardiag.core.ecu;

import java.io.IOException;

/**
 * Exception thrown when an ECU returns a negative response or when a diagnostic
 * communication error occurs at the UDS application layer.
 */
public class DiagnosticException extends IOException {

    /**
     * Constructs a new {@code DiagnosticException} with the specified message.
     *
     * @param message the detail message
     */
    public DiagnosticException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DiagnosticException} with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public DiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }
}
