package com.brickdeck.api.missingpieces;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserPart;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserPartRepository;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.service.MissingPartsService;
import com.brickdeck.api.security.entity.User;
import com.brickdeck.api.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MissingPartsIntegrationTest {

    @Autowired
    private MissingPartsService missingPartsService;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private ColorRepository colorRepository;
    @Autowired
    private SetPartRepository setPartRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPartRepository userPartRepository;
    @Autowired
    private UserSetRepository userSetRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void combinesLooseAndOwnedSetsExcludingSparesAgainstRealDatabase() {
        BrickSet targetSet = brickSet("IT-MP-TARGET-" + suffix);
        BrickSet ownedSet = brickSet("IT-MP-OWNED-" + suffix);
        Part p1 = part("IT-MP-P1-" + suffix, "Brick 2 x 4");
        Part p2 = part("IT-MP-P2-" + suffix, "Brick 2 x 3");
        Color red = color("Red");
        Color blue = color("Blue");

        // Target inventory: p1x4 red, p2x2 blue, plus a spare p1 red that must be ignored.
        setPart(targetSet, p1, red, 4, false);
        setPart(targetSet, p2, blue, 2, false);
        setPart(targetSet, p1, red, 9, true);

        // Owned set contributes one p1 red.
        setPart(ownedSet, p1, red, 1, false);

        User user = user("mp-" + suffix + "@brickdeck.test");
        // Loose parts: one p1 red, two p2 blue.
        userPart(user, p1, red, 1);
        userPart(user, p2, blue, 2);
        // User owns the owned set.
        userSet(user, ownedSet, CollectionStatus.OWNED);

        MissingPartsReport report =
                missingPartsService.computeMissingParts(targetSet.getExternalSetNumber(), user.getId());

        assertThat(report.totalRequired()).isEqualTo(6);
        assertThat(report.totalOwned()).isEqualTo(4);
        assertThat(report.totalMissing()).isEqualTo(2);
        assertThat(report.completionPercentage()).isEqualTo(66.7);
        assertThat(report.lines()).hasSize(2);

        var p1Line = report.lines().stream()
                .filter(l -> l.partNumber().equals(p1.getExternalPartNumber()))
                .findFirst().orElseThrow();
        assertThat(p1Line.required()).isEqualTo(4);
        assertThat(p1Line.owned()).isEqualTo(2); // 1 loose + 1 from owned set
        assertThat(p1Line.missing()).isEqualTo(2);
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

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setRole("USER");
        return userRepository.saveAndFlush(user);
    }

    private void userPart(User user, Part part, Color color, int quantity) {
        UserPart userPart = new UserPart();
        userPart.setUser(user);
        userPart.setPart(part);
        userPart.setColor(color);
        userPart.setQuantity(quantity);
        userPartRepository.saveAndFlush(userPart);
    }

    private void userSet(User user, BrickSet set, CollectionStatus status) {
        UserSet userSet = new UserSet();
        userSet.setUser(user);
        userSet.setBrickSet(set);
        userSet.setStatus(status);
        userSetRepository.saveAndFlush(userSet);
    }
}
