package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.external.rebrickable.dto.RebrickableColorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColorService {

    private final ColorRepository colorRepository;

    public ColorService(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional
    public Color resolveByExternalId(RebrickableColorResponse external) {
        Color color = colorRepository.findByExternalId(external.id())
                .orElseGet(Color::new);
        color.setExternalId(external.id());
        color.setName(external.name());
        color.setRgb(external.rgb());
        color.setTransparent(external.transparent());
        return colorRepository.save(color);
    }
}
