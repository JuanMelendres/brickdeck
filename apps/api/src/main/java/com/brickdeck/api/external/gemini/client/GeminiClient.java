package com.brickdeck.api.external.gemini.client;

import com.brickdeck.api.external.gemini.dto.GeminiColorGuess;
import com.brickdeck.api.external.gemini.dto.GeminiContent;
import com.brickdeck.api.external.gemini.dto.GeminiGenerateContentRequest;
import com.brickdeck.api.external.gemini.dto.GeminiGenerateContentResponse;
import com.brickdeck.api.external.gemini.dto.GeminiGenerationConfig;
import com.brickdeck.api.external.gemini.dto.GeminiInlineData;
import com.brickdeck.api.external.gemini.dto.GeminiPart;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public GeminiClient(RestClient geminiRestClient, ObjectMapper objectMapper, String model, String apiKey) {
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;
    }

    public GeminiColorGuess guessColor(byte[] imageBytes, String mimeType, List<String> colorNames) {
        GeminiGenerateContentRequest request = buildRequest(imageBytes, mimeType, colorNames);

        GeminiGenerateContentResponse response = geminiRestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .body(request)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);

        String candidateText = response.candidates().get(0).content().parts().get(0).text();
        return parseGuess(candidateText);
    }

    private GeminiColorGuess parseGuess(String candidateText) {
        try {
            return objectMapper.readValue(candidateText, GeminiColorGuess.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Gemini returned a non-JSON candidate: " + candidateText, ex);
        }
    }

    private GeminiGenerateContentRequest buildRequest(byte[] imageBytes, String mimeType, List<String> colorNames) {
        String base64Data = Base64.getEncoder().encodeToString(imageBytes);

        GeminiContent content = new GeminiContent(List.of(
                GeminiPart.ofText(
                        "You are identifying a single LEGO part from a photo. "
                                + "Guess the color from the given color list. Return JSON only."),
                GeminiPart.ofImage(new GeminiInlineData(mimeType, base64Data))
        ));

        Map<String, Object> colorNameSchema = Map.of(
                "type", "STRING",
                "enum", colorNames
        );
        Map<String, Object> properties = Map.of(
                "colorName", colorNameSchema,
                "confidence", Map.of("type", "NUMBER"),
                "reasoning", Map.of("type", "STRING")
        );
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", properties,
                "required", List.of("colorName", "confidence")
        );

        GeminiGenerationConfig generationConfig = new GeminiGenerationConfig("application/json", responseSchema);

        return new GeminiGenerateContentRequest(List.of(content), generationConfig);
    }
}
