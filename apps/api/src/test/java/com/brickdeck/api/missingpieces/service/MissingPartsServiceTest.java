package com.brickdeck.api.missingpieces.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService.PartColorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissingPartsServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;
    @Mock
    private SetPartRepository setPartRepository;
    @Mock
    private OwnedInventoryService ownedInventoryService;

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
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 2L, key(partB, blue), 2L));

        MissingPartsReport report = service.computeMissingParts("75257-1", userId, false, 0, 50);

        assertThat(report.setNumber()).isEqualTo("75257-1");
        assertThat(report.totalRequired()).isEqualTo(6);
        assertThat(report.totalOwned()).isEqualTo(4);
        assertThat(report.totalMissing()).isEqualTo(2);
        assertThat(report.completionPercentage()).isEqualTo(66.7);
        assertThat(report.missingLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(2);
        assertThat(report.totalPages()).isEqualTo(1);
        assertThat(report.first()).isTrue();
        assertThat(report.last()).isTrue();
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
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 5L));

        MissingPartsReport report = service.computeMissingParts("100-1", userId, false, 0, 50);

        assertThat(report.totalMissing()).isZero();
        assertThat(report.totalOwned()).isEqualTo(2);
        assertThat(report.completionPercentage()).isEqualTo(100.0);
    }

    @Test
    void missingOnlyFilterReturnsOnlyIncompleteLines() {
        when(brickSetRepository.findByExternalSetNumber("200-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("200-1"))
                .thenReturn(List.of(setPart(partA, red, 4), setPart(partB, blue, 2)));
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partB, blue), 2L)); // partB complete, partA missing

        MissingPartsReport report = service.computeMissingParts("200-1", userId, true, 0, 50);

        assertThat(report.totalRequired()).isEqualTo(6); // whole-set, unaffected by filter
        assertThat(report.missingLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(1);
        assertThat(report.lines()).hasSize(1);
        assertThat(report.lines().get(0).partNumber()).isEqualTo("3001");
    }

    @Test
    void paginatesLines() {
        when(brickSetRepository.findByExternalSetNumber("300-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("300-1"))
                .thenReturn(List.of(setPart(partA, red, 4), setPart(partB, blue, 2)));
        when(ownedInventoryService.buildOwnedMap(userId)).thenReturn(Map.of());

        MissingPartsReport page0 = service.computeMissingParts("300-1", userId, false, 0, 1);
        assertThat(page0.lines()).hasSize(1);
        assertThat(page0.totalLines()).isEqualTo(2);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.first()).isTrue();
        assertThat(page0.last()).isFalse();

        MissingPartsReport page1 = service.computeMissingParts("300-1", userId, false, 1, 1);
        assertThat(page1.lines()).hasSize(1);
        assertThat(page1.first()).isFalse();
        assertThat(page1.last()).isTrue();
        assertThat(page1.lines().get(0).partNumber())
                .isNotEqualTo(page0.lines().get(0).partNumber());
    }

    @Test
    void throwsWhenSetNotImported() {
        when(brickSetRepository.findByExternalSetNumber("999-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeMissingParts("999-1", userId, false, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Set not imported");
    }

    @Test
    void throwsWhenInventoryNotImported() {
        when(brickSetRepository.findByExternalSetNumber("555-1"))
                .thenReturn(Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("555-1"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.computeMissingParts("555-1", userId, false, 0, 50))
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

    private PartColorKey key(Part part, Color color) {
        return new PartColorKey(part.getId(), color.getId());
    }
}
