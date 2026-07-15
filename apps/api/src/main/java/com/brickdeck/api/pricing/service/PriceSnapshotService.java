package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.AddPriceSnapshotRequest;
import com.brickdeck.api.pricing.dto.PriceSnapshotResponse;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.entity.PriceSource;
import com.brickdeck.api.pricing.repository.PriceSnapshotRepository;
import com.brickdeck.api.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PriceSnapshotService {

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final BrickSetService brickSetService;
    private final PriceAlertService priceAlertService;

    public PriceSnapshotService(
            PriceSnapshotRepository priceSnapshotRepository,
            BrickSetService brickSetService,
            PriceAlertService priceAlertService) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.brickSetService = brickSetService;
        this.priceAlertService = priceAlertService;
    }

    @Transactional
    public PriceSnapshotResponse addSnapshot(User owner, AddPriceSnapshotRequest request) {
        BrickSet set = brickSetService.findOrImportEntity(request.setNumber());

        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setUser(owner);
        snapshot.setBrickSet(set);
        snapshot.setSource(PriceSource.MANUAL);
        snapshot.setCondition(request.condition());
        snapshot.setCurrency(request.currency());
        snapshot.setAmount(request.amount());
        snapshot.setStore(request.store());
        snapshot.setUrl(request.url());
        snapshot.setObservedAt(request.observedAt());

        PriceSnapshot saved = priceSnapshotRepository.save(snapshot);
        priceAlertService.evaluateForSnapshot(owner, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PriceSnapshotResponse> findForUser(
            User owner, String setNumber, Pageable pageable) {

        Page<PriceSnapshot> page = (setNumber == null || setNumber.isBlank())
                ? priceSnapshotRepository.findByUserId(owner.getId(), pageable)
                : priceSnapshotRepository.findByUserIdAndBrickSet_ExternalSetNumber(
                        owner.getId(), SetNumbers.normalize(setNumber), pageable);

        List<PriceSnapshotResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void removeSnapshot(User owner, UUID id) {
        PriceSnapshot snapshot = priceSnapshotRepository.findByIdAndUserId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Price snapshot not found: " + id));
        priceSnapshotRepository.delete(snapshot);
    }

    private PriceSnapshotResponse toResponse(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(
                snapshot.getId(),
                snapshot.getBrickSet().getExternalSetNumber(),
                snapshot.getAmount(),
                snapshot.getCurrency(),
                snapshot.getCondition(),
                snapshot.getSource(),
                snapshot.getObservedAt(),
                snapshot.getStore(),
                snapshot.getUrl(),
                snapshot.getCreatedAt());
    }
}
