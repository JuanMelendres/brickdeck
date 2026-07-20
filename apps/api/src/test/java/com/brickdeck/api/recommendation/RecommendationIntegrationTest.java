package com.brickdeck.api.recommendation;

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
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.recommendation.dto.BuildableSetRecommendation;
import com.brickdeck.api.recommendation.service.RecommendationService;
import com.brickdeck.api.security.entity.User;
import com.brickdeck.api.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RecommendationIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;
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
    void scoresWishlistSetsFromOwnedInventoryAgainstRealDatabase() {
        Part p1 = part("IT-RC-P1-" + suffix, "Brick 2 x 4");
        Part p2 = part("IT-RC-P2-" + suffix, "Brick 2 x 3");
        Color red = color("Red");
        Color blue = color("Blue");

        // Buildable wishlist set: p1x2 red + p2x2 blue (owned exactly below).
        BrickSet buildable = brickSet("IT-RC-BUILD-" + suffix);
        setPart(buildable, p1, red, 2, false);
        setPart(buildable, p2, blue, 2, false);

        // Partial wishlist set: needs p1x10 red (user only owns 2 -> not buildable).
        BrickSet partial = brickSet("IT-RC-PART-" + suffix);
        setPart(partial, p1, red, 10, false);

        // Wishlist set with no imported inventory -> skipped.
        BrickSet noInventory = brickSet("IT-RC-NOINV-" + suffix);

        User user = user("rc-" + suffix + "@brickdeck.test");
        // Owned inventory: 2 loose p1 red + an OWNED set contributing 2 p2 blue.
        userPart(user, p1, red, 2);
        BrickSet ownedSet = brickSet("IT-RC-OWNED-" + suffix);
        setPart(ownedSet, p2, blue, 2, false);
        userSet(user, ownedSet, CollectionStatus.OWNED);

        // Wishlist entries.
        userSet(user, buildable, CollectionStatus.WISHLIST);
        userSet(user, partial, CollectionStatus.WISHLIST);
        userSet(user, noInventory, CollectionStatus.WISHLIST);

        PageResponse<BuildableSetRecommendation> all =
                recommendationService.recommendBuildable(user.getId(), false, PageRequest.of(0, 20));

        // noInventory skipped -> 2 scored, buildable first (100%).
        assertThat(all.totalElements()).isEqualTo(2);
        assertThat(all.content()).extracting(BuildableSetRecommendation::setNumber)
                .containsExactly(buildable.getExternalSetNumber(), partial.getExternalSetNumber());

        BuildableSetRecommendation first = all.content().get(0);
        assertThat(first.completionPercentage()).isEqualTo(100.0);
        assertThat(first.totalOwned()).isEqualTo(4);
        assertThat(first.totalMissing()).isZero();
        assertThat(first.buildable()).isTrue();

        BuildableSetRecommendation second = all.content().get(1);
        assertThat(second.buildable()).isFalse();
        assertThat(second.totalMissing()).isEqualTo(8); // needs 10 p1 red, owns 2

        PageResponse<BuildableSetRecommendation> onlyBuildable =
                recommendationService.recommendBuildable(user.getId(), true, PageRequest.of(0, 20));

        assertThat(onlyBuildable.content()).hasSize(1);
        assertThat(onlyBuildable.content().get(0).setNumber())
                .isEqualTo(buildable.getExternalSetNumber());
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
