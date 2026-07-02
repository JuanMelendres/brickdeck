package com.brickdeck.api.common;

/**
 * Thrown when a requested domain resource does not exist locally.
 * Mapped to HTTP 404 by the global exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
