package com.brickdeck.api.catalog.service;

/**
 * Helpers for LEGO set numbers.
 *
 * <p>Rebrickable identifies sets by a canonical number with a variant suffix
 * (e.g. {@code 42232-1}). Users typically know only the base number ({@code 42232}).
 * Exact Rebrickable lookups require the suffix, so a bare number is normalized to the
 * default first variant ({@code -1}).
 */
public final class SetNumbers {

    private SetNumbers() {
    }

    public static String normalize(String setNumber) {
        if (setNumber == null) {
            return null;
        }

        String trimmed = setNumber.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        return trimmed.matches("\\d+") ? trimmed + "-1" : trimmed;
    }
}
