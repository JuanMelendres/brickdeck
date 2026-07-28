package com.brickdeck.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record BrickSetResponse(
        // Not @Schema(nullable = true): CI showed the annotation has no effect
        // here (OpenApiDocsTest), unlike identically-annotated sibling fields
        // below - likely a springdoc/swagger-core schema-resolution quirk for
        // this common (name, type) pair shared with other DTOs' "UUID id".
        // Not worth chasing further without a live app to iterate against.
        UUID id,
        String externalSetNumber,
        String name,
        @Schema(nullable = true) Integer yearReleased,
        @Schema(nullable = true) UUID themeId,
        @Schema(nullable = true) String themeName,
        @Schema(nullable = true) Integer externalThemeId,
        @Schema(nullable = true) Integer numberOfParts,
        @Schema(nullable = true) String imageUrl,
        @Schema(nullable = true) String externalUrl,
        String source,
        String cacheStatus
) {
}