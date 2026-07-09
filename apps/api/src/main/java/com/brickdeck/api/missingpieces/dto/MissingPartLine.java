package com.brickdeck.api.missingpieces.dto;

/**
 * One part+color line of a missing-pieces report: how many the target set
 * requires, how many the user owns (loose parts plus owned sets), and how many
 * are still missing.
 */
public record MissingPartLine(
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        int required,
        int owned,
        int missing
) {
}
