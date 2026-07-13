package com.brickdeck.api.comparison.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetComparisonServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;
    @Mock
    private SetPartRepository setPartRepository;
    @InjectMocks
    private SetComparisonService service;

    private Color red;
    private Color blue;
    private Color white;
    private Part brickX;
    private Part plateY;
    private Part tileZ;

    @BeforeEach
    void setUp() {
        red = color(1, "Red");
        blue = color(2, "Blue");
        white = color(3, "White");
        brickX = part("X", "Brick X");
        plateY = part("Y", "Plate Y");
        tileZ = part("Z", "Tile Z");
    }

    @Test
    void computesWeightedSimilarityAndCategories() {
        // A: brickX/red x4, plateY/blue x2 ; B: brickX/red x2, tileZ/white x6
        stubSet("A-1", List.of(line("A-1", brickX, red, 4), line("A-1", plateY, blue, 2)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 2), line("B-1", tileZ, white, 6)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        // sum(min)=2, sum(max)=4+2+6=12 -> 0.17
        assertThat(report.similarityScore()).isEqualTo(0.17);
        assertThat(report.sharedLineCount()).isEqualTo(1);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(3);
        assertThat(report.lines()).hasSize(3);
        // BOTH sorts first
        SetComparisonLine firstLine = report.lines().get(0);
        assertThat(firstLine.category()).isEqualTo(ComparisonCategory.BOTH);
        assertThat(firstLine.partNumber()).isEqualTo("X");
        assertThat(firstLine.quantityA()).isEqualTo(4);
        assertThat(firstLine.quantityB()).isEqualTo(2);
        assertThat(firstLine.shared()).isEqualTo(2);
    }

    @Test
    void identicalInventoriesScoreOne() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 3)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 3)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        assertThat(report.similarityScore()).isEqualTo(1.0);
        assertThat(report.sharedLineCount()).isEqualTo(1);
    }

    @Test
    void disjointInventoriesScoreZero() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 3)));
        stubSet("B-1", List.of(line("B-1", tileZ, white, 3)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        assertThat(report.similarityScore()).isEqualTo(0.0);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
    }

    @Test
    void categoryFilterKeepsSummaryButFiltersLines() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 4), line("A-1", plateY, blue, 2)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 2), line("B-1", tileZ, white, 6)));

        SetComparisonReport report =
                service.compare("A-1", "B-1", ComparisonCategory.ONLY_A, 0, 50);

        assertThat(report.onlyALineCount()).isEqualTo(1); // summary unchanged
        assertThat(report.totalLines()).isEqualTo(1);      // filtered lines
        assertThat(report.lines()).hasSize(1);
        assertThat(report.lines().get(0).category()).isEqualTo(ComparisonCategory.ONLY_A);
        assertThat(report.lines().get(0).partNumber()).isEqualTo("Y");
    }

    @Test
    void normalizesBareSetNumbers() {
        stubSet("42232-1", List.of(line("42232-1", brickX, red, 1)));
        stubSet("10497-1", List.of(line("10497-1", brickX, red, 1)));

        SetComparisonReport report = service.compare("42232", "10497", null, 0, 50);

        assertThat(report.setNumberA()).isEqualTo("42232-1");
        assertThat(report.setNumberB()).isEqualTo("10497-1");
    }

    @Test
    void throwsWhenSetNotImported() {
        when(brickSetRepository.findByExternalSetNumber("A-1"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.compare("A-1", "B-1", null, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("A-1");
    }

    @Test
    void throwsWhenInventoryNotImported() {
        when(brickSetRepository.findByExternalSetNumber("A-1"))
                .thenReturn(java.util.Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("A-1"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.compare("A-1", "B-1", null, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("inventory");
    }

    @Test
    void paginatesLinesWhileKeepingWholeSetSummary() {
        // 3 distinct BOTH lines so ordering within a category is by part number.
        stubSet("A-1", List.of(
                line("A-1", brickX, red, 1),
                line("A-1", plateY, red, 1),
                line("A-1", tileZ, red, 1)));
        stubSet("B-1", List.of(
                line("B-1", brickX, red, 1),
                line("B-1", plateY, red, 1),
                line("B-1", tileZ, red, 1)));

        SetComparisonReport page0 = service.compare("A-1", "B-1", null, 0, 2);
        assertThat(page0.totalLines()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.lines()).hasSize(2);
        assertThat(page0.page()).isEqualTo(0);
        assertThat(page0.size()).isEqualTo(2);
        assertThat(page0.first()).isTrue();
        assertThat(page0.last()).isFalse();

        SetComparisonReport page1 = service.compare("A-1", "B-1", null, 1, 2);
        assertThat(page1.lines()).hasSize(1);
        assertThat(page1.first()).isFalse();
        assertThat(page1.last()).isTrue();
    }

    private void stubSet(String number, List<SetPart> parts) {
        when(brickSetRepository.findByExternalSetNumber(number))
                .thenReturn(java.util.Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(number))
                .thenReturn(parts);
    }

    private Color color(int externalId, String name) {
        Color c = new Color();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setName(name);
        c.setRgb("000000");
        return c;
    }

    private Part part(String number, String name) {
        Part p = new Part();
        p.setId(UUID.randomUUID());
        p.setExternalPartNumber(number);
        p.setName(name);
        return p;
    }

    private SetPart line(String setNumber, Part part, Color color, int quantity) {
        SetPart sp = new SetPart();
        sp.setPart(part);
        sp.setColor(color);
        sp.setQuantity(quantity);
        sp.setSpare(false);
        return sp;
    }
}
