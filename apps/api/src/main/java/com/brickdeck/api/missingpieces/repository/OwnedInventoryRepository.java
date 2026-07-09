package com.brickdeck.api.missingpieces.repository;

import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.collection.entity.CollectionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates a user's owned inventory by part+color. Owned = loose parts
 * (user_parts) plus the parts of the sets in the user's collection whose status
 * counts as physically owned. Spare parts count toward owned.
 */
public interface OwnedInventoryRepository extends Repository<SetPart, UUID> {

    @Query("""
            select up.part.id as partId, up.color.id as colorId,
                   sum(up.quantity) as totalQuantity
            from UserPart up
            where up.user.id = :userId
            group by up.part.id, up.color.id
            """)
    List<PartColorQuantity> sumLoosePartsByUser(@Param("userId") UUID userId);

    @Query("""
            select sp.part.id as partId, sp.color.id as colorId,
                   sum(sp.quantity) as totalQuantity
            from UserSet us, SetPart sp
            where sp.brickSet = us.brickSet
              and us.user.id = :userId
              and us.status in :statuses
            group by sp.part.id, sp.color.id
            """)
    List<PartColorQuantity> sumOwnedSetPartsByUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<CollectionStatus> statuses);
}
