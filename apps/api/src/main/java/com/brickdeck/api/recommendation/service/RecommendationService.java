package com.brickdeck.api.recommendation.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService.PartColorKey;
import com.brickdeck.api.recommendation.dto.BuildableSetRecommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recommends which of the user's WISHLIST sets they can build (or are closest
 * to building) from their owned inventory. Owned inventory (loose parts plus
 * the parts of owned sets) is computed once and reused across all candidates.
 * Sets whose inventory has not been imported are skipped (they cannot be
 * scored). Results are ordered most-complete first.
 */
@Service
public class RecommendationService {

    private static final Comparator<BuildableSetRecommendation> BY_BUILDABILITY =
            Comparator.comparingDouble(BuildableSetRecommendation::completionPercentage).reversed()
                    .thenComparing(BuildableSetRecommendation::setNumber);

    private final UserSetRepository userSetRepository;
    private final SetPartRepository setPartRepository;
    private final OwnedInventoryService ownedInventoryService;

    public RecommendationService(
            UserSetRepository userSetRepository,
            SetPartRepository setPartRepository,
            OwnedInventoryService ownedInventoryService) {
        this.userSetRepository = userSetRepository;
        this.setPartRepository = setPartRepository;
        this.ownedInventoryService = ownedInventoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BuildableSetRecommendation> recommendBuildable(
            UUID userId, boolean buildableOnly, Pageable pageable) {

        List<UserSet> wishlist =
                userSetRepository.findByUserIdAndStatus(userId, CollectionStatus.WISHLIST);

        Map<PartColorKey, Long> owned = wishlist.isEmpty()
                ? Map.of()
                : ownedInventoryService.buildOwnedMap(userId);

        List<BuildableSetRecommendation> scored = new ArrayList<>(wishlist.size());
        for (UserSet userSet : wishlist) {
            score(userSet.getBrickSet(), owned)
                    .filter(rec -> !buildableOnly || rec.buildable())
                    .ifPresent(scored::add);
        }
        scored.sort(BY_BUILDABILITY);

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int from = Math.min((int) pageable.getOffset(), scored.size());
        int to = Math.min(from + size, scored.size());
        List<BuildableSetRecommendation> pageContent = List.copyOf(scored.subList(from, to));

        return PageResponse.of(pageContent, page, size, scored.size());
    }

    /**
     * Scores one candidate set against the owned inventory. Empty if the set's
     * inventory has not been imported (nothing to score).
     */
    private java.util.Optional<BuildableSetRecommendation> score(
            BrickSet set, Map<PartColorKey, Long> owned) {

        List<SetPart> required = setPartRepository
                .findByBrickSet_ExternalSetNumberAndSpareFalse(set.getExternalSetNumber());
        if (required.isEmpty()) {
            return java.util.Optional.empty();
        }

        int totalRequired = 0;
        int totalOwned = 0;
        int totalMissing = 0;
        for (SetPart line : required) {
            int req = line.getQuantity();
            long ownedRaw = owned.getOrDefault(
                    new PartColorKey(line.getPart().getId(), line.getColor().getId()), 0L);
            int ownedQty = (int) Math.min(ownedRaw, Integer.MAX_VALUE);

            totalRequired += req;
            totalOwned += Math.min(ownedQty, req);
            totalMissing += Math.max(0, req - ownedQty);
        }

        double completion = totalRequired == 0
                ? 100.0
                : Math.round(totalOwned * 1000.0 / totalRequired) / 10.0;

        return java.util.Optional.of(new BuildableSetRecommendation(
                set.getExternalSetNumber(),
                set.getName(),
                themeName(set.getTheme()),
                totalRequired,
                totalOwned,
                totalMissing,
                completion,
                totalMissing == 0));
    }

    private String themeName(Theme theme) {
        return theme == null ? null : theme.getName();
    }
}
