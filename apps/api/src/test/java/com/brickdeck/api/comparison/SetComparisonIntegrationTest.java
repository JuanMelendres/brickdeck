package com.brickdeck.api.comparison;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SetComparisonIntegrationTest {

    @Autowired
    private SetComparisonService service;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private ColorRepository colorRepository;
    @Autowired
    private SetPartRepository setPartRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void comparesTwoSetsExcludingSparesAgainstRealDatabase() {
        BrickSet setA = brickSet("IT-CMP-A-" + suffix);
        BrickSet setB = brickSet("IT-CMP-B-" + suffix);
        Part brickX = part("IT-CMP-X-" + suffix, "Brick X");
        Part plateY = part("IT-CMP-Y-" + suffix, "Plate Y");
        Part tileZ = part("IT-CMP-Z-" + suffix, "Tile Z");
        Color red = color("Red");
        Color blue = color("Blue");
        Color white = color("White");

        // A: brickX/red x4, plateY/blue x2, plus a spare brickX/red x9 (ignored).
        setPart(setA, brickX, red, 4, false);
        setPart(setA, plateY, blue, 2, false);
        setPart(setA, brickX, red, 9, true);
        // B: brickX/red x2, tileZ/white x6.
        setPart(setB, brickX, red, 2, false);
        setPart(setB, tileZ, white, 6, false);

        SetComparisonReport report = service.compare(
                setA.getExternalSetNumber(), setB.getExternalSetNumber(), null, 0, 50);

        // sum(min)=2, sum(max)=12 -> 0.17
        assertThat(report.similarityScore()).isEqualTo(0.17);
        assertThat(report.sharedLineCount()).isEqualTo(1);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(3);

        var sharedLine = report.lines().stream()
                .filter(l -> l.category() == ComparisonCategory.BOTH)
                .findFirst().orElseThrow();
        assertThat(sharedLine.partNumber()).isEqualTo(brickX.getExternalPartNumber());
        assertThat(sharedLine.quantityA()).isEqualTo(4);
        assertThat(sharedLine.quantityB()).isEqualTo(2);
        assertThat(sharedLine.shared()).isEqualTo(2);
    }

    @Test
    void endpointIsReachableWithoutAuthentication() throws Exception {
        BrickSet setA = brickSet("IT-CMP-PUB-A-" + suffix);
        BrickSet setB = brickSet("IT-CMP-PUB-B-" + suffix);
        Part brickX = part("IT-CMP-PUB-X-" + suffix, "Brick X");
        Color red = color("Red");
        setPart(setA, brickX, red, 3, false);
        setPart(setB, brickX, red, 3, false);

        mockMvc.perform(get("/api/v1/sets/compare")
                        .param("a", setA.getExternalSetNumber())
                        .param("b", setB.getExternalSetNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarityScore").value(1.0));
    }

    private BrickSet brickSet(String number) {
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(number);
        set.setName("Set " + number);
        set.setSource("TEST");
        return brickSetRepository.saveAndFlush(set);
    }

    private Part part(String number, String name) {
        Part part = new Part();
        part.setExternalPartNumber(number);
        part.setName(name);
        part.setSource("TEST");
        return partRepository.saveAndFlush(part);
    }

    private Color color(String name) {
        Color color = new Color();
        color.setName(name);
        color.setSource("TEST");
        return colorRepository.saveAndFlush(color);
    }

    private void setPart(BrickSet set, Part part, Color color, int quantity, boolean spare) {
        SetPart line = new SetPart();
        line.setBrickSet(set);
        line.setPart(part);
        line.setColor(color);
        line.setQuantity(quantity);
        line.setSpare(spare);
        line.setSource("TEST");
        setPartRepository.saveAndFlush(line);
    }
}
