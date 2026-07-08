package com.brickdeck.api.collection.dto;

import com.brickdeck.api.collection.entity.CollectionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single set entry in a user's collection. Carries denormalized catalog fields
 * so the client can render the entry without a second lookup.
 */
public record UserSetResponse(
        UUID id,
        String setNumber,
        String setName,
        Integer yearReleased,
        String themeName,
        String imageUrl,
        CollectionStatus status,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        LocalDateTime createdAt
) {
}
