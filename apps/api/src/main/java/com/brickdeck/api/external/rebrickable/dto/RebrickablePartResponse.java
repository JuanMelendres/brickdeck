package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickablePartResponse(
        @JsonProperty("part_num")
        String partNum,

        String name,

        @JsonProperty("part_cat_id")
        Integer partCatId,

        @JsonProperty("part_url")
        String partUrl,

        @JsonProperty("part_img_url")
        String partImgUrl
) {
}
