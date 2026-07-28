package com.brickdeck.api.classification.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.classification.PartClassifier;
import com.brickdeck.api.classification.dto.ColorSuggestion;
import com.brickdeck.api.classification.dto.PartClassificationResponse;
import com.brickdeck.api.classification.dto.PartResolutionStatus;
import com.brickdeck.api.classification.dto.PartSuggestion;
import com.brickdeck.api.external.brickognize.client.BrickognizeClient;
import com.brickdeck.api.external.brickognize.dto.BrickognizeItem;
import com.brickdeck.api.external.gemini.client.GeminiClient;
import com.brickdeck.api.external.gemini.dto.GeminiColorGuess;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Combines two free/low-cost vendor sources behind a single {@link PartClassifier}
 * port (ADR-013): Brickognize (LEGO-specialist) for part-number candidates, Gemini
 * for color — Brickognize's response has no color field at all.
 */
@Service
public class HybridPartClassifier implements PartClassifier {

    private static final int MAX_PART_SUGGESTIONS = 5;

    private final BrickognizeClient brickognizeClient;
    private final GeminiClient geminiClient;
    private final PartRepository partRepository;
    private final ColorRepository colorRepository;

    public HybridPartClassifier(BrickognizeClient brickognizeClient,
                                 GeminiClient geminiClient,
                                 PartRepository partRepository,
                                 ColorRepository colorRepository) {
        this.brickognizeClient = brickognizeClient;
        this.geminiClient = geminiClient;
        this.partRepository = partRepository;
        this.colorRepository = colorRepository;
    }

    @Override
    public PartClassificationResponse classify(byte[] imageBytes, String mimeType) {
        List<PartSuggestion> partSuggestions = brickognizeClient.identifyPart(imageBytes).items().stream()
                .limit(MAX_PART_SUGGESTIONS)
                .map(this::toPartSuggestion)
                .toList();

        ColorSuggestion colorSuggestion = guessColor(imageBytes, mimeType);

        return new PartClassificationResponse(colorSuggestion, partSuggestions);
    }

    private PartSuggestion toPartSuggestion(BrickognizeItem item) {
        Optional<Part> local = partRepository.findByExternalPartNumber(item.id());
        boolean resolved = local.isPresent();
        String referenceImageUrl = local.map(Part::getImageUrl).orElse(item.imgUrl());

        return new PartSuggestion(
                item.id(),
                item.name(),
                item.score(),
                resolved ? PartResolutionStatus.RESOLVED : PartResolutionStatus.UNRESOLVED,
                referenceImageUrl
        );
    }

    private ColorSuggestion guessColor(byte[] imageBytes, String mimeType) {
        List<String> colorNames = colorRepository.findAll().stream().map(Color::getName).toList();
        GeminiColorGuess guess = geminiClient.guessColor(imageBytes, mimeType, colorNames);

        Integer colorId = colorRepository.findByName(guess.colorName())
                .map(Color::getExternalId)
                .orElse(null);

        return new ColorSuggestion(colorId, guess.colorName(), guess.confidence());
    }
}
