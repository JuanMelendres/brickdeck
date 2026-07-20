package com.brickdeck.api.collection.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single loose-part entry in a user's inventory, with denormalized catalog fields
 * so the client can render it without a second lookup.
 */
public record UserPartResponse(
        UUID id,
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        Integer quantity,
        String storageLocation,
        LocalDateTime createdAt
) {
}
