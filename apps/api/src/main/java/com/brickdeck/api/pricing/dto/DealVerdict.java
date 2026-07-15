package com.brickdeck.api.pricing.dto;

/** How good a candidate price is versus the user's observed history. */
public enum DealVerdict {
    GREAT_DEAL,
    GOOD_DEAL,
    FAIR,
    POOR
}
