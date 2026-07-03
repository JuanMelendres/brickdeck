package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BrickSetService {

    private static final String SOURCE_REBRICKABLE = "REBRICKABLE";

    private final BrickSetRepository brickSetRepository;
    private final RebrickableClient rebrickableClient;

    public BrickSetService(
            BrickSetRepository brickSetRepository,
            RebrickableClient rebrickableClient
    ) {
        this.brickSetRepository = brickSetRepository;
        this.rebrickableClient = rebrickableClient;
    }

    @Transactional(readOnly = true)
    public List<BrickSetResponse> findAll() {
        return brickSetRepository.findAll()
                .stream()
                .map(brickSet -> toResponse(brickSet, "LOCAL_CACHE_HIT"))
                .toList();
    }

    @Transactional
    public BrickSetResponse findOrImportBySetNumber(String setNumber) {
        return brickSetRepository.findByExternalSetNumber(setNumber)
                .map(brickSet -> toResponse(brickSet, "LOCAL_CACHE_HIT"))
                .orElseGet(() -> importFromRebrickable(setNumber));
    }

    private BrickSetResponse importFromRebrickable(String setNumber) {
        RebrickableSetResponse externalSet = rebrickableClient.getSetByNumber(setNumber);

        BrickSet brickSet = new BrickSet();
        brickSet.setExternalSetNumber(externalSet.setNum());
        brickSet.setName(externalSet.name());
        brickSet.setYearReleased(externalSet.year());
        brickSet.setExternalThemeId(externalSet.themeId());
        brickSet.setNumberOfParts(externalSet.numParts());
        brickSet.setImageUrl(externalSet.setImgUrl());
        brickSet.setExternalUrl(externalSet.setUrl());
        brickSet.setExternalLastModifiedAt(parseOffsetDateTime(externalSet.lastModifiedDt()));
        brickSet.setSource(SOURCE_REBRICKABLE);

        BrickSet savedSet = brickSetRepository.save(brickSet);

        return toResponse(savedSet, "IMPORTED_FROM_REBRICKABLE");
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