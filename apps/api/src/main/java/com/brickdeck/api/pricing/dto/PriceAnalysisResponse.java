package com.brickdeck.api.pricing.dto;

import java.math.BigDecimal;

/**
 * Aggregated price analysis for a set in one currency, computed from the user's
 * snapshots. {@code pricePerPiece} (of the average) and {@code numberOfParts}
 * are null when the set has no part count; {@code candidate} is null unless a
 * candidate price was supplied.
 */
public record PriceAnalysisResponse(
        String setNumber,
        String currency,
        int snapshotCount,
        BigDecimal minAmount,
        BigDecimal averageAmount,
        BigDecimal maxAmount,
        BigDecimal latestAmount,
        Integer numberOfParts,
        BigDecimal pricePerPiece,
        CandidateEvaluation candidate
) {
}
