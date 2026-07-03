package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickableThemeResponse(
        Integer id,

        String name,

        @JsonProperty("parent_id")
        Integer parentId
) {
}
