package com.brickdeck.api.recommendation.dto;

/**
 * How buildable a candidate set is from the user's owned inventory:
 * required vs owned piece counts, a completion percentage, and whether the
 * user has every piece ({@code buildable}). Quantities count non-spare
 * inventory only; owned counts are capped per line at what the set requires.
 */
public record BuildableSetRecommendation(
        String setNumber,
        String name,
        String themeName,
        int totalRequired,
        int totalOwned,
        int totalMissing,
        double completionPercentage,
        boolean buildable
) {
}
