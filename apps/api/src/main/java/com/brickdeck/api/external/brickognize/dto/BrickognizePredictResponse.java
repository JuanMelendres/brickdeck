package com.brickdeck.api.external.brickognize.dto;

import java.util.List;

public record BrickognizePredictResponse(
        List<BrickognizeItem> items
) {
}
