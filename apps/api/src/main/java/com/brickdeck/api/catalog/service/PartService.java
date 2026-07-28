package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final RebrickableClient rebrickableClient;

    public PartService(PartRepository partRepository, RebrickableClient rebrickableClient) {
        this.partRepository = partRepository;
        this.rebrickableClient = rebrickableClient;
    }

    @Transactional
    public Part resolveByExternalPartNumber(RebrickablePartResponse external) {
        Part part = partRepository.findByExternalPartNumber(external.partNum())
                .orElseGet(Part::new);
        part.setExternalPartNumber(external.partNum());
        part.setName(external.name());
        part.setExternalCategoryId(external.partCatId());
        part.setPartUrl(external.partUrl());
        part.setImageUrl(external.partImgUrl());
        return partRepository.save(part);
    }

    /**
     * Returns the local {@link Part} for the given number, importing it from Rebrickable
     * on a cache miss. Cache-first: a local hit skips Rebrickable and skips saving.
     */
    @Transactional
    public Part findOrImport(String externalPartNumber) {
        return partRepository.findByExternalPartNumber(externalPartNumber)
                .orElseGet(() -> resolveByExternalPartNumber(fetchExternalPart(externalPartNumber)));
    }

    private RebrickablePartResponse fetchExternalPart(String externalPartNumber) {
        try {
            return rebrickableClient.getPart(externalPartNumber);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Part not found in Rebrickable: " + externalPartNumber);
        }
    }
}
