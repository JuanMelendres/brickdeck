package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.missingpieces.repository.OwnedInventoryRepository;
import com.brickdeck.api.missingpieces.repository.PartColorQuantity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Computes what a user physically owns, keyed by part+color: their loose parts
 * plus the parts of every set they own (OWNED / BUILT / IN_PROGRESS). WISHLIST
 * sets are excluded. Shared by the missing-pieces and recommendation engines.
 */
@Service
public class OwnedInventoryService {

    /** Collection statuses that mean the user physically owns the set's parts. */
    public static final Set<CollectionStatus> OWNED_STATUSES =
            EnumSet.of(
                    CollectionStatus.OWNED,
                    CollectionStatus.BUILT,
                    CollectionStatus.IN_PROGRESS);

    private final OwnedInventoryRepository ownedInventoryRepository;

    public OwnedInventoryService(OwnedInventoryRepository ownedInventoryRepository) {
        this.ownedInventoryRepository = ownedInventoryRepository;
    }

    /** Total owned quantity per part+color across loose parts and owned sets. */
    @Transactional(readOnly = true)
    public Map<PartColorKey, Long> buildOwnedMap(UUID userId) {
        Map<PartColorKey, Long> owned = new HashMap<>();
        accumulate(owned, ownedInventoryRepository.sumLoosePartsByUser(userId));
        accumulate(owned, ownedInventoryRepository.sumOwnedSetPartsByUser(userId, OWNED_STATUSES));
        return owned;
    }

    private void accumulate(Map<PartColorKey, Long> owned, List<PartColorQuantity> rows) {
        for (PartColorQuantity row : rows) {
            owned.merge(
                    new PartColorKey(row.getPartId(), row.getColorId()),
                    row.getTotalQuantity(),
                    Long::sum);
        }
    }

    /** Identifies an inventory line by its part and color. */
    public record PartColorKey(UUID partId, UUID colorId) {
    }
}
