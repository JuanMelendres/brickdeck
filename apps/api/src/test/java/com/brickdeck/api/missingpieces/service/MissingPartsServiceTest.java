package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.repository.OwnedInventoryRepository;
import com.brickdeck.api.missingpieces.repository.PartColorQuantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissingPartsServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;
    @Mock
    private SetPartRepository setPartRepository;
    @Mock
    private OwnedInventoryRepository ownedInventoryRepository;

    @InjectMocks
    private MissingPartsService service;

    private final UUID userId = UUID.randomUUID();
    private Part partA;
    private Part partB;
    private Color red;
    private Color blue;

    @BeforeEach
    void setUp() {
        partA = part("3001", "Brick 2 x 4");
        partB = part("3002", "Brick 2 x 3");
        red = color(4, "Red", "B40000");
        blue = color(1, "Blue", "0055BF");
    }

    @Test
    void combinesLoosePartsAndOwnedSetsIntoMissingReport() {
        when(brickSetRepository.findByExternalSetNumber("75257-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("75257-1"))
                .thenReturn(List.of(
                        setPart(partA, red, 4),
                        setPart(partB, blue, 2)));
        when(ownedInventoryRepository.sumLoosePartsByUser(userId))
                .thenReturn(List.of(pcq(partA, red, 1)));
        when(ownedInventoryRepository.sumOwnedSetPartsByUser(eq(userId), any()))
                .thenReturn(List.of(pcq(partA, red, 1), pcq(partB, blue, 2)));

        MissingPartsReport report = service.computeMissingParts("75257-1", userId);

        assertThat(report.setNumber()).isEqualTo("75257-1");
        assertThat(report.totalRequired()).isEqualTo(6);
        assertThat(report.totalOwned()).isEqualTo(4);
        assertThat(report.totalMissing()).isEqualTo(2);
        assertThat(report.completionPercentage()).isEqualTo(66.7);
        assertThat(report.lines()).hasSize(2);

        var lineA = report.lines().stream()
                .filter(l -> l.partNumber().equals("3001")).findFirst().orElseThrow();
        assertThat(lineA.required()).isEqualTo(4);
        assertThat(lineA.owned()).isEqualTo(2);
        assertThat(lineA.missing()).isEqualTo(2);
        assertThat(lineA.colorExternalId()).isEqualTo(4);
    }

    @Test
    void reportsFullCompletionWhenAllOwned() {
        when(brickSetRepository.findByExternalSetNumber("100-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("100-1"))
                .thenReturn(List.of(setPart(partA, red, 2)));
        when(ownedInventoryRepository.sumLoosePartsByUser(userId))
                .thenReturn(List.of(pcq(partA, red, 5)));
        when(ownedInventoryRepository.sumOwnedSetPartsByUser(eq(userId), any()))
                .thenReturn(List.of());

        MissingPartsReport report = service.computeMissingParts("100-1", userId);

        assertThat(report.totalMissing()).isZero();
        assertThat(report.totalOwned()).isEqualTo(2);
        assertThat(report.completionPercentage()).isEqualTo(100.0);
    }

    @Test
    void throwsWhenSetNotImported() {
        when(brickSetRepository.findByExternalSetNumber("999-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeMissingParts("999-1", userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Set not imported");
    }

    @Test
    void throwsWhenInventoryNotImported() {
        when(brickSetRepository.findByExternalSetNumber("555-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("555-1"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.computeMissingParts("555-1", userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("inventory not imported");
    }

    private Part part(String number, String name) {
        Part p = new Part();
        p.setId(UUID.randomUUID());
        p.setExternalPartNumber(number);
        p.setName(name);
        return p;
    }

    private Color color(int externalId, String name, String rgb) {
        Color c = new Color();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setName(name);
        c.setRgb(rgb);
        return c;
    }

    private SetPart setPart(Part part, Color color, int quantity) {
        SetPart sp = new SetPart();
        sp.setId(UUID.randomUUID());
        sp.setPart(part);
        sp.setColor(color);
        sp.setQuantity(quantity);
        sp.setSpare(false);
        return sp;
    }

    private PartColorQuantity pcq(Part part, Color color, long quantity) {
        return new PartColorQuantity() {
            @Override
            public UUID getPartId() {
                return part.getId();
            }

            @Override
            public UUID getColorId() {
                return color.getId();
            }

            @Override
            public long getTotalQuantity() {
                return quantity;
            }
        };
    }
}
