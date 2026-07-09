package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.missingpieces.dto.MissingPartLine;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.repository.OwnedInventoryRepository;
import com.brickdeck.api.missingpieces.repository.PartColorQuantity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MissingPartsService {

    /** Collection statuses that mean the user physically owns the set's parts. */
    private static final Set<CollectionStatus> OWNED_STATUSES =
            EnumSet.of(
                    CollectionStatus.OWNED,
                    CollectionStatus.BUILT,
                    CollectionStatus.IN_PROGRESS);

    private final BrickSetRepository brickSetRepository;
    private final SetPartRepository setPartRepository;
    private final OwnedInventoryRepository ownedInventoryRepository;

    public MissingPartsService(
            BrickSetRepository brickSetRepository,
            SetPartRepository setPartRepository,
            OwnedInventoryRepository ownedInventoryRepository) {
        this.brickSetRepository = brickSetRepository;
        this.setPartRepository = setPartRepository;
        this.ownedInventoryRepository = ownedInventoryRepository;
    }

    /**
     * Compare a target set's required (non-spare) inventory against the user's
     * owned inventory (loose parts plus the parts of owned sets) and report
     * missing pieces and completion percentage.
     */
    @Transactional(readOnly = true)
    public MissingPartsReport computeMissingParts(String setNumber, UUID userId) {
        String normalized = SetNumbers.normalize(setNumber);

        brickSetRepository.findByExternalSetNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Set not imported: " + normalized + " (import the set first)"));

        List<SetPart> required =
                setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(normalized);
        if (required.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Set inventory not imported: " + normalized
                            + " (import the inventory first)");
        }

        Map<PartColorKey, Long> owned = new HashMap<>();
        accumulate(owned, ownedInventoryRepository.sumLoosePartsByUser(userId));
        accumulate(owned, ownedInventoryRepository.sumOwnedSetPartsByUser(userId, OWNED_STATUSES));

        List<MissingPartLine> lines = new ArrayList<>(required.size());
        int totalRequired = 0;
        int totalOwned = 0;
        int totalMissing = 0;

        for (SetPart line : required) {
            Part part = line.getPart();
            Color color = line.getColor();

            int req = line.getQuantity();
            long ownedRaw = owned.getOrDefault(
                    new PartColorKey(part.getId(), color.getId()), 0L);
            int ownedQty = (int) Math.min(ownedRaw, Integer.MAX_VALUE);
            int missing = Math.max(0, req - ownedQty);

            totalRequired += req;
            totalOwned += Math.min(ownedQty, req);
            totalMissing += missing;

            lines.add(new MissingPartLine(
                    part.getExternalPartNumber(),
                    part.getName(),
                    part.getImageUrl(),
                    color.getExternalId(),
                    color.getName(),
                    color.getRgb(),
                    req,
                    ownedQty,
                    missing));
        }

        double completion = totalRequired == 0
                ? 100.0
                : Math.round(totalOwned * 1000.0 / totalRequired) / 10.0;

        return new MissingPartsReport(
                normalized, totalRequired, totalOwned, totalMissing, completion, lines);
    }

    private void accumulate(Map<PartColorKey, Long> owned, List<PartColorQuantity> rows) {
        for (PartColorQuantity row : rows) {
            owned.merge(
                    new PartColorKey(row.getPartId(), row.getColorId()),
                    row.getTotalQuantity(),
                    Long::sum);
        }
    }

    private record PartColorKey(UUID partId, UUID colorId) {
    }
}
