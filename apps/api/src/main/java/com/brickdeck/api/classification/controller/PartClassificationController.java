package com.brickdeck.api.classification.controller;

import com.brickdeck.api.classification.PartClassifier;
import com.brickdeck.api.classification.dto.PartClassificationResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Classify-and-discard: the uploaded photo is never persisted, only passed to
 * {@link PartClassifier} for the duration of this request (ADR-013, Finding 7).
 */
@RestController
@RequestMapping("/api/v1/classify")
public class PartClassificationController {

    private final PartClassifier partClassifier;

    public PartClassificationController(PartClassifier partClassifier) {
        this.partClassifier = partClassifier;
    }

    @PostMapping(path = "/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PartClassificationResponse classifyPart(@RequestParam("image") MultipartFile image) {
        try {
            return partClassifier.classify(image.getBytes(), image.getContentType());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded image", ex);
        }
    }
}
