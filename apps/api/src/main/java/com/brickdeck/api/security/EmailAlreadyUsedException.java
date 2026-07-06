package com.brickdeck.api.security;

/**
 * Thrown when registering with an email that already exists.
 * Mapped to HTTP 409 by the global exception handler.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String message) {
        super(message);
    }
}
