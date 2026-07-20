package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.missingpieces.dto.MissingPartLine;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService.PartColorKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MissingPartsService {

    private final BrickSetRepository brickSetRepository;
    private final SetPartRepository setPartRepository;
    private final OwnedInventoryService ownedInventoryService;

    public MissingPartsService(
            BrickSetRepository brickSetRepository,
            SetPartRepository setPartRepository,
            OwnedInventoryService ownedInventoryService) {
        this.brickSetRepository = brickSetRepository;
        this.setPartRepository = setPartRepository;
        this.ownedInventoryService = ownedInventoryService;
    }

    /** Lines ordered by most-missing first, then part number, then color. */
    private static final Comparator<MissingPartLine> LINE_ORDER =
            Comparator.comparingInt(MissingPartLine::missing).reversed()
                    .thenComparing(l -> l.partNumber() == null ? "" : l.partNumber())
                    .thenComparing(l -> l.colorExternalId() == null ? Integer.MIN_VALUE : l.colorExternalId());

    /**
     * Compare a target set's required (non-spare) inventory against the user's
     * owned inventory (loose parts plus the parts of owned sets) and report
     * missing pieces and completion percentage. Totals are whole-set; the
     * returned lines are optionally filtered to missing-only and paginated.
     */
    @Transactional(readOnly = true)
    public MissingPartsReport computeMissingParts(
            String setNumber, UUID userId, boolean missingOnly, int page, int size) {
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

        Map<PartColorKey, Long> owned = ownedInventoryService.buildOwnedMap(userId);

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

        int missingLineCount = (int) lines.stream().filter(l -> l.missing() > 0).count();

        List<MissingPartLine> filtered = lines.stream()
                .filter(l -> !missingOnly || l.missing() > 0)
                .sorted(LINE_ORDER)
                .toList();

        int safeSize = size <= 0 ? 50 : size;
        int safePage = Math.max(0, page);
        long totalLines = filtered.size();
        int totalPages = (int) Math.ceil((double) totalLines / safeSize);

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<MissingPartLine> pageLines = List.copyOf(filtered.subList(from, to));

        boolean first = safePage == 0;
        boolean last = to >= filtered.size();

        return new MissingPartsReport(
                normalized, totalRequired, totalOwned, totalMissing, completion,
                missingLineCount, pageLines, safePage, safeSize, totalLines, totalPages,
                first, last);
    }

}
