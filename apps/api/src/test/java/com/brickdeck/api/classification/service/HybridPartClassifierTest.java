package com.brickdeck.api.classification.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.classification.dto.PartClassificationResponse;
import com.brickdeck.api.classification.dto.PartResolutionStatus;
import com.brickdeck.api.external.brickognize.client.BrickognizeClient;
import com.brickdeck.api.external.brickognize.dto.BrickognizeItem;
import com.brickdeck.api.external.brickognize.dto.BrickognizePredictResponse;
import com.brickdeck.api.external.gemini.client.GeminiClient;
import com.brickdeck.api.external.gemini.dto.GeminiColorGuess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridPartClassifierTest {

    @Mock
    private BrickognizeClient brickognizeClient;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PartRepository partRepository;

    @Mock
    private ColorRepository colorRepository;

    @InjectMocks
    private HybridPartClassifier classifier;

    private Part resolvedPart(String number) {
        Part part = new Part();
        part.setId(UUID.randomUUID());
        part.setExternalPartNumber(number);
        part.setName("Brick 2 x 4");
        part.setImageUrl("https://cdn.rebrickable.com/media/parts/elements/300121.jpg");
        return part;
    }

    private Color redColor() {
        Color color = new Color();
        color.setId(UUID.randomUUID());
        color.setExternalId(4);
        color.setName("Red");
        return color;
    }

    @Test
    void classifyMarksLocallyKnownPartAsResolvedWithCatalogReferenceImage() {
        BrickognizePredictResponse predictResponse = new BrickognizePredictResponse(List.of(
                new BrickognizeItem("3001", "Brick 2 x 4", 0.84, "Brick", "https://brickognize.example/3001.webp")
        ));
        when(brickognizeClient.identifyPart(any())).thenReturn(predictResponse);
        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(resolvedPart("3001")));

        when(colorRepository.findAll()).thenReturn(List.of(redColor()));
        when(geminiClient.guessColor(any(), eq("image/jpeg"), anyList()))
                .thenReturn(new GeminiColorGuess("Red", 0.95, "solid red"));
        when(colorRepository.findByName("Red")).thenReturn(Optional.of(redColor()));

        PartClassificationResponse response = classifier.classify("photo".getBytes(), "image/jpeg");

        assertThat(response.partSuggestions()).hasSize(1);
        assertThat(response.partSuggestions().get(0).partNumber()).isEqualTo("3001");
        assertThat(response.partSuggestions().get(0).resolutionStatus()).isEqualTo(PartResolutionStatus.RESOLVED);
        assertThat(response.partSuggestions().get(0).referenceImageUrl())
                .isEqualTo("https://cdn.rebrickable.com/media/parts/elements/300121.jpg");
    }

    @Test
    void classifyMarksUnknownPartAsUnresolvedWithBrickognizeReferenceImage() {
        BrickognizePredictResponse predictResponse = new BrickognizePredictResponse(List.of(
                new BrickognizeItem("99999", "Unknown Part", 0.60, "Other", "https://brickognize.example/99999.webp")
        ));
        when(brickognizeClient.identifyPart(any())).thenReturn(predictResponse);
        when(partRepository.findByExternalPartNumber("99999")).thenReturn(Optional.empty());

        when(colorRepository.findAll()).thenReturn(List.of(redColor()));
        when(geminiClient.guessColor(any(), eq("image/jpeg"), anyList()))
                .thenReturn(new GeminiColorGuess("Red", 0.95, "solid red"));
        when(colorRepository.findByName("Red")).thenReturn(Optional.of(redColor()));

        PartClassificationResponse response = classifier.classify("photo".getBytes(), "image/jpeg");

        assertThat(response.partSuggestions().get(0).resolutionStatus()).isEqualTo(PartResolutionStatus.UNRESOLVED);
        assertThat(response.partSuggestions().get(0).referenceImageUrl())
                .isEqualTo("https://brickognize.example/99999.webp");
    }

    @Test
    void classifyResolvesColorIdFromLocalColorsTable() {
        when(brickognizeClient.identifyPart(any())).thenReturn(new BrickognizePredictResponse(List.of()));
        when(colorRepository.findAll()).thenReturn(List.of(redColor()));
        when(geminiClient.guessColor(any(), eq("image/jpeg"), anyList()))
                .thenReturn(new GeminiColorGuess("Red", 0.95, "solid red"));
        when(colorRepository.findByName("Red")).thenReturn(Optional.of(redColor()));

        PartClassificationResponse response = classifier.classify("photo".getBytes(), "image/jpeg");

        assertThat(response.colorSuggestion().colorId()).isEqualTo(4);
        assertThat(response.colorSuggestion().colorName()).isEqualTo("Red");
        assertThat(response.colorSuggestion().confidence()).isEqualTo(0.95);
    }

    @Test
    void classifyReturnsNullColorIdWhenGuessedNameIsNotInLocalCatalog() {
        when(brickognizeClient.identifyPart(any())).thenReturn(new BrickognizePredictResponse(List.of()));
        when(colorRepository.findAll()).thenReturn(List.of(redColor()));
        when(geminiClient.guessColor(any(), eq("image/jpeg"), anyList()))
                .thenReturn(new GeminiColorGuess("Not-A-Real-Color", 0.5, "unsure"));
        when(colorRepository.findByName("Not-A-Real-Color")).thenReturn(Optional.empty());

        PartClassificationResponse response = classifier.classify("photo".getBytes(), "image/jpeg");

        assertThat(response.colorSuggestion().colorId()).isNull();
    }

    @Test
    void classifyLimitsPartSuggestionsToTopFive() {
        List<BrickognizeItem> items = List.of(
                new BrickognizeItem("1", "P1", 0.9, "Brick", null),
                new BrickognizeItem("2", "P2", 0.8, "Brick", null),
                new BrickognizeItem("3", "P3", 0.7, "Brick", null),
                new BrickognizeItem("4", "P4", 0.6, "Brick", null),
                new BrickognizeItem("5", "P5", 0.5, "Brick", null),
                new BrickognizeItem("6", "P6", 0.4, "Brick", null)
        );
        when(brickognizeClient.identifyPart(any())).thenReturn(new BrickognizePredictResponse(items));
        when(partRepository.findByExternalPartNumber(any())).thenReturn(Optional.empty());

        when(colorRepository.findAll()).thenReturn(List.of(redColor()));
        when(geminiClient.guessColor(any(), eq("image/jpeg"), anyList()))
                .thenReturn(new GeminiColorGuess("Red", 0.95, "solid red"));
        when(colorRepository.findByName("Red")).thenReturn(Optional.of(redColor()));

        PartClassificationResponse response = classifier.classify("photo".getBytes(), "image/jpeg");

        assertThat(response.partSuggestions()).hasSize(5);
    }
}
