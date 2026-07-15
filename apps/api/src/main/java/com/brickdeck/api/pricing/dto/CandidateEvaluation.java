package com.brickdeck.api.pricing.dto;

import java.math.BigDecimal;

/**
 * Assessment of a candidate price against the user's observed history for a set.
 * {@code pricePerPiece} is null when the set has no part count.
 */
public record CandidateEvaluation(
        BigDecimal amount,
        BigDecimal pricePerPiece,
        BigDecimal percentBelowAverage,
        boolean atOrBelowLowest,
        DealVerdict verdict
) {
}
