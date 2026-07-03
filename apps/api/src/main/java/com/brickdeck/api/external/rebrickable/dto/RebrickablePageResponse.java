package com.brickdeck.api.external.rebrickable.dto;

import java.util.List;

public record RebrickablePageResponse<T>(
        Integer count,
        String next,
        String previous,
        List<T> results
) {
}