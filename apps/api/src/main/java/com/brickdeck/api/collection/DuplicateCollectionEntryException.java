package com.brickdeck.api.collection;

/**
 * Thrown when adding a set that already exists in the user's collection.
 * Mapped to HTTP 409 by the global exception handler.
 */
public class DuplicateCollectionEntryException extends RuntimeException {

    public DuplicateCollectionEntryException(String message) {
        super(message);
    }
}
