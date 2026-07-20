package com.brickdeck.api.missingpieces.dto;

import java.util.List;

/**
 * How close a user is to completing a target set. The totals and completion are
 * whole-set aggregates (independent of filter/paging); {@code lines} is the
 * current page after the optional missing-only filter.
 */
public record MissingPartsReport(
        String setNumber,
        int totalRequired,
        int totalOwned,
        int totalMissing,
        double completionPercentage,
        int missingLineCount,
        List<MissingPartLine> lines,
        int page,
        int size,
        long totalLines,
        int totalPages,
        boolean first,
        boolean last
) {
}
