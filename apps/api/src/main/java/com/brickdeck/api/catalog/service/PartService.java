package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
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
}
