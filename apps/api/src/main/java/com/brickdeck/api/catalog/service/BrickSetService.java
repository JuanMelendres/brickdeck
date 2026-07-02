package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrickSetService {

    private final BrickSetRepository brickSetRepository;

    public BrickSetService(BrickSetRepository brickSetRepository) {
        this.brickSetRepository = brickSetRepository;
    }

    @Transactional(readOnly = true)
    public BrickSetResponse getByExternalSetNumber(String externalSetNumber) {
        BrickSet set = brickSetRepository.findByExternalSetNumber(externalSetNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Set not found: " + externalSetNumber));
        return toResponse(set);
    }

    private BrickSetResponse toResponse(BrickSet set) {
        Theme theme = set.getTheme();
        return new BrickSetResponse(
                set.getId(),
                set.getExternalSetNumber(),
                set.getName(),
                set.getYearReleased(),
                theme != null ? theme.getId() : null,
                theme != null ? theme.getName() : null,
                set.getNumberOfParts(),
                set.getImageUrl(),
                set.getSource()
        );
    }
}
