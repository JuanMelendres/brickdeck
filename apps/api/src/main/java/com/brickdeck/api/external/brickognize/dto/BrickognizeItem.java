package com.brickdeck.api.external.brickognize.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrickognizeItem(
        String id,
        String name,
        Double score,
        String category,
        @JsonProperty("img_url") String imgUrl
) {
}
