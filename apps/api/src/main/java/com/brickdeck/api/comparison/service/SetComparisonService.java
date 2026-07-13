package com.brickdeck.api.comparison.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SetComparisonService {

    private final BrickSetRepository brickSetRepository;
    private final SetPartRepository setPartRepository;

    public SetComparisonService(BrickSetRepository brickSetRepository,
                                SetPartRepository setPartRepository) {
        this.brickSetRepository = brickSetRepository;
        this.setPartRepository = setPartRepository;
    }

    /** Lines ordered BOTH first, then ONLY_A, then ONLY_B, then part, then color. */
    private static final Comparator<SetComparisonLine> LINE_ORDER =
            Comparator.comparingInt((SetComparisonLine l) -> rank(l.category()))
                    .thenComparing(l -> l.partNumber() == null ? "" : l.partNumber())
                    .thenComparing(l -> l.colorExternalId() == null
                            ? Integer.MIN_VALUE : l.colorExternalId());

    private static int rank(ComparisonCategory category) {
        return switch (category) {
            case BOTH -> 0;
            case ONLY_A -> 1;
            case ONLY_B -> 2;
        };
    }

    /**
     * Compare two catalog sets' non-spare inventories. Quantities are summed per
     * part+color; the similarity score is sum(min)/sum(max) over the union of
     * keys. Line counts are whole-set; returned lines are optionally filtered by
     * category and paginated.
     */
    @Transactional(readOnly = true)
    public SetComparisonReport compare(String setNumberA, String setNumberB,
                                       ComparisonCategory category, int page, int size) {
        String a = SetNumbers.normalize(setNumberA);
        String b = SetNumbers.normalize(setNumberB);

        List<SetPart> partsA = requireInventory(a);
        Map<PartColorKey, Integer> qtyA = sumByKey(partsA);
        List<SetPart> partsB = requireInventory(b);
        Map<PartColorKey, Integer> qtyB = sumByKey(partsB);

        Map<PartColorKey, SetPart> repr = new HashMap<>();
        for (SetPart sp : partsA) {
            repr.putIfAbsent(key(sp), sp);
        }
        for (SetPart sp : partsB) {
            repr.putIfAbsent(key(sp), sp);
        }

        Set<PartColorKey> keys = new HashSet<>();
        keys.addAll(qtyA.keySet());
        keys.addAll(qtyB.keySet());

        long sumMin = 0;
        long sumMax = 0;
        int sharedCount = 0;
        int onlyACount = 0;
        int onlyBCount = 0;
        List<SetComparisonLine> all = new ArrayList<>(keys.size());

        for (PartColorKey k : keys) {
            int va = qtyA.getOrDefault(k, 0);
            int vb = qtyB.getOrDefault(k, 0);
            sumMin += Math.min(va, vb);
            sumMax += Math.max(va, vb);

            ComparisonCategory cat = va > 0 && vb > 0
                    ? ComparisonCategory.BOTH
                    : va > 0 ? ComparisonCategory.ONLY_A : ComparisonCategory.ONLY_B;
            switch (cat) {
                case BOTH -> sharedCount++;
                case ONLY_A -> onlyACount++;
                case ONLY_B -> onlyBCount++;
            }

            SetPart r = repr.get(k);
            Part part = r.getPart();
            Color color = r.getColor();
            all.add(new SetComparisonLine(
                    part.getExternalPartNumber(),
                    part.getName(),
                    part.getImageUrl(),
                    color.getExternalId(),
                    color.getName(),
                    color.getRgb(),
                    va,
                    vb,
                    Math.min(va, vb),
                    cat));
        }

        double similarity = sumMax == 0 ? 0.0 : Math.round(sumMin * 100.0 / sumMax) / 100.0;

        List<SetComparisonLine> filtered = all.stream()
                .filter(l -> category == null || l.category() == category)
                .sorted(LINE_ORDER)
                .toList();

        int safeSize = size <= 0 ? 50 : size;
        int safePage = Math.max(0, page);
        long totalLines = filtered.size();
        int totalPages = (int) Math.ceil((double) totalLines / safeSize);

        int from = (int) Math.min((long) safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<SetComparisonLine> pageLines = List.copyOf(filtered.subList(from, to));

        boolean first = safePage == 0;
        boolean last = to >= filtered.size();

        return new SetComparisonReport(
                a, b, similarity, sharedCount, onlyACount, onlyBCount,
                pageLines, safePage, safeSize, totalLines, totalPages, first, last);
    }

    private List<SetPart> requireInventory(String normalized) {
        brickSetRepository.findByExternalSetNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Set not imported: " + normalized + " (import the set first)"));
        List<SetPart> parts =
                setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(normalized);
        if (parts.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Set inventory not imported: " + normalized
                            + " (import the inventory first)");
        }
        return parts;
    }

    private static Map<PartColorKey, Integer> sumByKey(List<SetPart> parts) {
        Map<PartColorKey, Integer> m = new HashMap<>();
        for (SetPart sp : parts) {
            m.merge(key(sp), sp.getQuantity(), Integer::sum);
        }
        return m;
    }

    private static PartColorKey key(SetPart sp) {
        return new PartColorKey(sp.getPart().getId(), sp.getColor().getId());
    }

    private record PartColorKey(UUID partId, UUID colorId) {
    }
}
