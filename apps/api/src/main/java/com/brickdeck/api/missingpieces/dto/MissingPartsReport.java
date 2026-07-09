package com.brickdeck.api.missingpieces.dto;

import java.util.List;

/**
 * How close a user is to completing a target set. Required counts non-spare
 * inventory lines; owned combines loose parts and owned sets. Completion is the
 * share of required pieces the user already has (capped per line).
 */
public record MissingPartsReport(
        String setNumber,
        int totalRequired,
        int totalOwned,
        int totalMissing,
        double completionPercentage,
        List<MissingPartLine> lines
) {
}
