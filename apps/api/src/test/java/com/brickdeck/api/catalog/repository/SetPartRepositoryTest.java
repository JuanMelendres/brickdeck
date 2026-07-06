package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SetPartRepositoryTest {

    @Autowired
    private BrickSetRepository brickSetRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private SetPartRepository setPartRepository;

    private BrickSet persistSet(String setNumber) {
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(setNumber);
        set.setName("Test Set " + setNumber);
        return brickSetRepository.save(set);
    }

    private Part persistPart(String partNumber) {
        Part part = new Part();
        part.setExternalPartNumber(partNumber);
        part.setName("Brick " + partNumber);
        return partRepository.save(part);
    }

    private Color persistColor(int externalId) {
        Color color = new Color();
        color.setExternalId(externalId);
        color.setName("Color " + externalId);
        color.setRgb("FF0000");
        color.setTransparent(false);
        return colorRepository.save(color);
    }

    @Test
    void persistsInventoryLineLinkingSetPartAndColor() {
        BrickSet set = persistSet("77777-1");
        Part part = persistPart("3001");
        Color color = persistColor(4);

        SetPart line = new SetPart();
        line.setBrickSet(set);
        line.setPart(part);
        line.setColor(color);
        line.setQuantity(6);
        line.setSpare(false);

        SetPart saved = setPartRepository.saveAndFlush(line);

        assertThat(saved.getId()).isNotNull();

        SetPart found = setPartRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getBrickSet().getExternalSetNumber()).isEqualTo("77777-1");
        assertThat(found.getPart().getExternalPartNumber()).isEqualTo("3001");
        assertThat(found.getColor().getExternalId()).isEqualTo(4);
        assertThat(found.getQuantity()).isEqualTo(6);
        assertThat(found.isSpare()).isFalse();
    }

    @Test
    void rejectsDuplicateInventoryLine() {
        BrickSet set = persistSet("88888-1");
        Part part = persistPart("3002");
        Color color = persistColor(5);

        SetPart first = new SetPart();
        first.setBrickSet(set);
        first.setPart(part);
        first.setColor(color);
        first.setQuantity(2);
        first.setSpare(false);
        setPartRepository.saveAndFlush(first);

        SetPart duplicate = new SetPart();
        duplicate.setBrickSet(set);
        duplicate.setPart(part);
        duplicate.setColor(color);
        duplicate.setQuantity(9);
        duplicate.setSpare(false);

        assertThatThrownBy(() -> setPartRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
