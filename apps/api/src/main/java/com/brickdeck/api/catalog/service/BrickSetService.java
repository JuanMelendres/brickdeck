package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BrickSetService {

    private static final String SOURCE_REBRICKABLE = "REBRICKABLE";
    private static final String STATUS_LOCAL_CACHE_HIT = "LOCAL_CACHE_HIT";
    private static final String STATUS_IMPORTED = "IMPORTED_FROM_REBRICKABLE";
    private static final String STATUS_REFRESHED = "REFRESHED_FROM_REBRICKABLE";

    private final BrickSetRepository brickSetRepository;
    private final RebrickableClient rebrickableClient;
    private final ThemeService themeService;

    public BrickSetService(
            BrickSetRepository brickSetRepository,
            RebrickableClient rebrickableClient,
            ThemeService themeService
    ) {
        this.brickSetRepository = brickSetRepository;
        this.rebrickableClient = rebrickableClient;
        this.themeService = themeService;
    }

    @Transactional(readOnly = true)
    public List<BrickSetResponse> findAll() {
        return brickSetRepository.findAll()
                .stream()
                .map(brickSet -> toResponse(brickSet, STATUS_LOCAL_CACHE_HIT))
                .toList();
    }

    @Transactional(readOnly = true)
    public BrickSetResponse findBySetNumber(String setNumber) {
        return brickSetRepository.findByExternalSetNumber(setNumber)
                .map(brickSet -> toResponse(brickSet, STATUS_LOCAL_CACHE_HIT))
                .orElseThrow(() -> new ResourceNotFoundException("Set not found: " + setNumber));
    }

    @Transactional
    public ImportResult importSet(String setNumber) {
        RebrickableSetResponse external = fetchExternalSet(setNumber);
        Theme theme = themeService.resolveByExternalId(external.themeId());

        BrickSet brickSet = brickSetRepository.findByExternalSetNumber(external.setNum())
                .orElseGet(BrickSet::new);
        boolean created = brickSet.getId() == null;

        brickSet.setExternalSetNumber(external.setNum());
        brickSet.setName(external.name());
        brickSet.setYearReleased(external.year());
        brickSet.setExternalThemeId(external.themeId());
        brickSet.setNumberOfParts(external.numParts());
        brickSet.setImageUrl(external.setImgUrl());
        brickSet.setExternalUrl(external.setUrl());
        brickSet.setExternalLastModifiedAt(parseOffsetDateTime(external.lastModifiedDt()));
        brickSet.setTheme(theme);
        brickSet.setSource(SOURCE_REBRICKABLE);

        BrickSet saved = brickSetRepository.save(brickSet);
        String cacheStatus = created ? STATUS_IMPORTED : STATUS_REFRESHED;
        return new ImportResult(created, toResponse(saved, cacheStatus));
    }

    private RebrickableSetResponse fetchExternalSet(String setNumber) {
        try {
            return rebrickableClient.getSetByNumber(setNumber);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Set not found in Rebrickable: " + setNumber);
        }
    }

    private BrickSetResponse toResponse(BrickSet brickSet, String cacheStatus) {
        return new BrickSetResponse(
                brickSet.getId(),
                brickSet.getExternalSetNumber(),
                brickSet.getName(),
                brickSet.getYearReleased(),
                brickSet.getTheme() != null ? brickSet.getTheme().getId() : null,
                brickSet.getTheme() != null ? brickSet.getTheme().getName() : null,
                brickSet.getExternalThemeId(),
                brickSet.getNumberOfParts(),
                brickSet.getImageUrl(),
                brickSet.getExternalUrl(),
                brickSet.getSource(),
                cacheStatus
        );
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(value);
    }
}