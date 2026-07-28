package com.brickdeck.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * {@code @Schema(nullable = true)} below is best-effort: confirmed across two
 * CI runs that springdoc/swagger-core's generated {@code nullable} flag is
 * non-deterministic per field (a different subset of these was missing each
 * run) - a scan-order-sensitive quirk, not something fixable from this DTO
 * alone. Treat the generated schema as a hint, not a verified contract, until
 * that's root-caused (needs a live app to iterate against).
 */
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