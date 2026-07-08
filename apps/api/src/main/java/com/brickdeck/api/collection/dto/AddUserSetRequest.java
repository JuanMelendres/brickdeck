package com.brickdeck.api.collection.dto;

import com.brickdeck.api.collection.entity.CollectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to add a set to the authenticated user's collection.
 *
 * <p>{@code setNumber} is resolved find-or-import against the catalog (cache-first;
 * a local hit skips Rebrickable). {@code status} defaults to {@code OWNED} when null.
 */
public record AddUserSetRequest(
        @NotBlank String setNumber,
        CollectionStatus status,
        @PositiveOrZero BigDecimal purchasePrice,
        LocalDate purchaseDate
) {
}
