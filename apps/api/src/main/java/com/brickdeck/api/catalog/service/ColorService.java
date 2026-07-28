package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableColorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class ColorService {

    private final ColorRepository colorRepository;
    private final RebrickableClient rebrickableClient;

    public ColorService(ColorRepository colorRepository, RebrickableClient rebrickableClient) {
        this.colorRepository = colorRepository;
        this.rebrickableClient = rebrickableClient;
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

    /**
     * Returns the local {@link Color} for the given external id, importing it from Rebrickable
     * on a cache miss. Cache-first: a local hit skips Rebrickable and skips saving.
     */
    @Transactional
    public Color findOrImport(Integer externalId) {
        return colorRepository.findByExternalId(externalId)
                .orElseGet(() -> resolveByExternalId(fetchExternalColor(externalId)));
    }

    private RebrickableColorResponse fetchExternalColor(Integer externalId) {
        try {
            return rebrickableClient.getColor(externalId);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Color not found in Rebrickable: " + externalId);
        }
    }
}
