package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.missingpieces.repository.OwnedInventoryRepository;
import com.brickdeck.api.missingpieces.repository.PartColorQuantity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnedInventoryServiceTest {

    @Mock
    private OwnedInventoryRepository ownedInventoryRepository;

    @InjectMocks
    private OwnedInventoryService ownedInventoryService;

    private static PartColorQuantity row(UUID partId, UUID colorId, long qty) {
        return new PartColorQuantity() {
            @Override public UUID getPartId() { return partId; }
            @Override public UUID getColorId() { return colorId; }
            @Override public long getTotalQuantity() { return qty; }
        };
    }

    @Test
    void mergesLoosePartsAndOwnedSetPartsForTheSamePartColor() {
        UUID userId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID colorId = UUID.randomUUID();

        when(ownedInventoryRepository.sumLoosePartsByUser(userId))
                .thenReturn(List.of(row(partId, colorId, 3L)));
        when(ownedInventoryRepository.sumOwnedSetPartsByUser(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(row(partId, colorId, 5L)));

        Map<OwnedInventoryService.PartColorKey, Long> owned =
                ownedInventoryService.buildOwnedMap(userId);

        assertThat(owned)
                .containsEntry(new OwnedInventoryService.PartColorKey(partId, colorId), 8L)
                .hasSize(1);
    }
}
