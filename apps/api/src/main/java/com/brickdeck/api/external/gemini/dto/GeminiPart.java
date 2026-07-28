package com.brickdeck.api.external.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiPart(
        String text,
        @JsonProperty("inline_data") GeminiInlineData inlineData
) {
    public static GeminiPart ofText(String text) {
        return new GeminiPart(text, null);
    }

    public static GeminiPart ofImage(GeminiInlineData inlineData) {
        return new GeminiPart(null, inlineData);
    }
}
