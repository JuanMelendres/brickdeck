package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickableSetPartResponse(
        RebrickablePartResponse part,

        RebrickableColorResponse color,

        Integer quantity,

        @JsonProperty("is_spare")
        boolean isSpare,

        @JsonProperty("element_id")
        String elementId
) {
}
