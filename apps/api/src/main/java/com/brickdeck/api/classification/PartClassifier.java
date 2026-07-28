package com.brickdeck.api.classification;

import com.brickdeck.api.classification.dto.PartClassificationResponse;

/**
 * Source-agnostic port for turning a single-part photo into ranked part/color
 * candidates. Implementations may call one or more vendor vision APIs; callers
 * (the controller) depend only on this contract.
 */
public interface PartClassifier {

    PartClassificationResponse classify(byte[] imageBytes, String mimeType);
}
