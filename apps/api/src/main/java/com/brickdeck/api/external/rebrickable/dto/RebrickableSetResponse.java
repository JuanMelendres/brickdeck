package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickableSetResponse(
        @JsonProperty("set_num")
        String setNum,

        String name,

        Integer year,

        @JsonProperty("theme_id")
        Integer themeId,

        @JsonProperty("num_parts")
        Integer numParts,

        @JsonProperty("set_img_url")
        String setImgUrl,

        @JsonProperty("set_url")
        String setUrl,

        @JsonProperty("last_modified_dt")
        String lastModifiedDt
) {
}