package com.brickdeck.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record BrickSetResponse(
        @Schema(nullable = true) UUID id,
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