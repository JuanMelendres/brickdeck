package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickableColorResponse(
        Integer id,

        String name,

        String rgb,

        @JsonProperty("is_trans")
        boolean transparent
) {
}
