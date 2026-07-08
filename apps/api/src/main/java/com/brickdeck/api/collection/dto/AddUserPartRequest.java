package com.brickdeck.api.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request to add a loose part to the authenticated user's inventory.
 *
 * <p>The part and color must already exist in the local catalog (imported via a set's
 * inventory); unknown references return 404. {@code storageLocation} is optional.
 */
public record AddUserPartRequest(
        @NotBlank String externalPartNumber,
        @NotNull Integer colorExternalId,
        @NotNull @Positive Integer quantity,
        String storageLocation
) {
}
