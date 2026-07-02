package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ThemeService {

    private final ThemeRepository themeRepository;

    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Transactional(readOnly = true)
    public ThemeResponse getById(UUID id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Theme not found: " + id));
        return toResponse(theme);
    }

    private ThemeResponse toResponse(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getExternalId(),
                theme.getName(),
                theme.getParentThemeId()
        );
    }
}
