package com.brickdeck.api.collection.dto;

import com.brickdeck.api.collection.entity.CollectionStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update for a collection entry. Only non-null fields are applied;
 * omitted fields keep their current value (this endpoint cannot clear a field to null).
 */
public record UpdateUserSetRequest(
        CollectionStatus status,
        @PositiveOrZero BigDecimal purchasePrice,
        LocalDate purchaseDate
) {
}
