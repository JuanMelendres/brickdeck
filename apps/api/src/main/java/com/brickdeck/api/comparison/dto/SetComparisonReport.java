package com.brickdeck.api.comparison.dto;

import java.util.List;

/**
 * A comparison of two catalog sets' non-spare inventories. The similarity score
 * and the three whole-set line counts are aggregates independent of
 * filter/paging; {@code lines} is the current page after the optional category
 * filter.
 */
public record SetComparisonReport(
        String setNumberA,
        String setNumberB,
        double similarityScore,
        int sharedLineCount,
        int onlyALineCount,
        int onlyBLineCount,
        List<SetComparisonLine> lines,
        int page,
        int size,
        long totalLines,
        int totalPages,
        boolean first,
        boolean last
) {
}
