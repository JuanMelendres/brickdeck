package com.brickdeck.api.comparison.dto;

/**
 * One part+color line of a set comparison: how many set A requires, how many
 * set B requires, the shared minimum, and which set(s) the line appears in.
 * Quantities count non-spare inventory only.
 */
public record SetComparisonLine(
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        int quantityA,
        int quantityB,
        int shared,
        ComparisonCategory category
) {
}
