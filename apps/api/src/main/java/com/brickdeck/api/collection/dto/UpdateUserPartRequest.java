package com.brickdeck.api.collection.dto;

import jakarta.validation.constraints.Positive;

/**
 * Partial update for a loose part entry. Only non-null fields are applied;
 * omitted fields keep their current value (cannot clear a field to null).
 */
public record UpdateUserPartRequest(
        @Positive Integer quantity,
        String storageLocation
) {
}
