package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final RebrickableClient rebrickableClient;

    public ThemeService(ThemeRepository themeRepository, RebrickableClient rebrickableClient) {
        this.themeRepository = themeRepository;
        this.rebrickableClient = rebrickableClient;
    }

    @Transactional(readOnly = true)
    public ThemeResponse getById(UUID id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Theme not found: " + id));
        return toResponse(theme);
    }

    @Transactional
    public Theme resolveByExternalId(Integer externalThemeId) {
        if (externalThemeId == null) {
            return null;
        }

        String externalId = String.valueOf(externalThemeId);
        RebrickableThemeResponse external = rebrickableClient.getThemeById(externalThemeId);

        Theme theme = themeRepository.findByExternalId(externalId)
                .orElseGet(Theme::new);
        theme.setExternalId(externalId);
        theme.setName(external.name());

        return themeRepository.save(theme);
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
