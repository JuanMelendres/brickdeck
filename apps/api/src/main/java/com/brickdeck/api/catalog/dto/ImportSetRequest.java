package com.brickdeck.api.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportSetRequest(
        @NotBlank String setNumber
) {
}
