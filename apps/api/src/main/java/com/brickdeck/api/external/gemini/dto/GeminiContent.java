package com.brickdeck.api.external.gemini.dto;

import java.util.List;

public record GeminiContent(
        List<GeminiPart> parts
) {
}
