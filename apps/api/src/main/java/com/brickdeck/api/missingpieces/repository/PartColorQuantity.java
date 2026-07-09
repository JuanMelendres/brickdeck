package com.brickdeck.api.missingpieces.repository;

import java.util.UUID;

/**
 * Aggregated owned quantity for one part in one color, keyed by the internal
 * part and color ids. Spring Data projection for grouped sum queries.
 */
public interface PartColorQuantity {
    UUID getPartId();

    UUID getColorId();

    long getTotalQuantity();
}
