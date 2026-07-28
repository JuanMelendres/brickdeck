package com.brickdeck.api.external.gemini.dto;

import java.util.List;

public record GeminiGenerateContentResponse(
        List<GeminiCandidate> candidates
) {

    public record GeminiCandidate(GeminiCandidateContent content) {
    }

    public record GeminiCandidateContent(List<GeminiResponsePart> parts) {
    }

    public record GeminiResponsePart(String text) {
    }
}
