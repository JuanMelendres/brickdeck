package com.brickdeck.api.recommendation.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService;
import com.brickdeck.api.missingpieces.service.OwnedInventoryService.PartColorKey;
import com.brickdeck.api.recommendation.dto.BuildableSetRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserSetRepository userSetRepository;
    @Mock
    private SetPartRepository setPartRepository;
    @Mock
    private OwnedInventoryService ownedInventoryService;

    @InjectMocks
    private RecommendationService service;

    private final UUID userId = UUID.randomUUID();
    private Part partA;
    private Part partB;
    private Color red;
    private Color blue;

    @BeforeEach
    void setUp() {
        partA = part("3001", "Brick 2 x 4");
        partB = part("3002", "Brick 2 x 3");
        red = color(4, "Red");
        blue = color(1, "Blue");
    }

    @Test
    void marksAFullyOwnedWishlistSetAsBuildable() {
        wishlist(userSet("100-1", "Small Set", "City"));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("100-1"))
                .thenReturn(List.of(setPart(partA, red, 2)));
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 5L));

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, false, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        BuildableSetRecommendation r = page.content().get(0);
        assertThat(r.setNumber()).isEqualTo("100-1");
        assertThat(r.name()).isEqualTo("Small Set");
        assertThat(r.themeName()).isEqualTo("City");
        assertThat(r.totalRequired()).isEqualTo(2);
        assertThat(r.totalOwned()).isEqualTo(2);
        assertThat(r.totalMissing()).isZero();
        assertThat(r.completionPercentage()).isEqualTo(100.0);
        assertThat(r.buildable()).isTrue();
    }

    @Test
    void scoresAPartiallyOwnedSetAsNotBuildable() {
        wishlist(userSet("200-1", "Big Set", "Technic"));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("200-1"))
                .thenReturn(List.of(setPart(partA, red, 4), setPart(partB, blue, 2)));
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 1L)); // partB fully missing

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, false, PageRequest.of(0, 20));

        BuildableSetRecommendation r = page.content().get(0);
        assertThat(r.totalRequired()).isEqualTo(6);
        assertThat(r.totalOwned()).isEqualTo(1);
        assertThat(r.totalMissing()).isEqualTo(5);
        assertThat(r.completionPercentage()).isEqualTo(16.7);
        assertThat(r.buildable()).isFalse();
    }

    @Test
    void buildableOnlyFiltersOutIncompleteSets() {
        wishlist(
                userSet("100-1", "Buildable", "City"),
                userSet("200-1", "Incomplete", "Technic"));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("100-1"))
                .thenReturn(List.of(setPart(partA, red, 2)));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("200-1"))
                .thenReturn(List.of(setPart(partB, blue, 2)));
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 2L)); // 100-1 buildable, 200-1 not

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, true, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).setNumber()).isEqualTo("100-1");
    }

    @Test
    void ordersByCompletionDescending() {
        wishlist(
                userSet("200-1", "Half", "Technic"),
                userSet("100-1", "Full", "City"));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("100-1"))
                .thenReturn(List.of(setPart(partA, red, 2)));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("200-1"))
                .thenReturn(List.of(setPart(partB, blue, 4)));
        when(ownedInventoryService.buildOwnedMap(userId))
                .thenReturn(Map.of(key(partA, red), 2L, key(partB, blue), 2L)); // 100%: 100-1, 50%: 200-1

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, false, PageRequest.of(0, 20));

        assertThat(page.content()).extracting(BuildableSetRecommendation::setNumber)
                .containsExactly("100-1", "200-1");
    }

    @Test
    void skipsWishlistSetsWithNoImportedInventory() {
        wishlist(userSet("300-1", "Not Imported", "Ideas"));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("300-1"))
                .thenReturn(List.of());
        when(ownedInventoryService.buildOwnedMap(userId)).thenReturn(Map.of());

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, false, PageRequest.of(0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void returnsAnEmptyPageWhenTheWishlistIsEmpty() {
        wishlist();

        PageResponse<BuildableSetRecommendation> page =
                service.recommendBuildable(userId, false, PageRequest.of(0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
    }

    private void wishlist(UserSet... sets) {
        when(userSetRepository.findByUserIdAndStatus(userId, CollectionStatus.WISHLIST))
                .thenReturn(List.of(sets));
    }

    private UserSet userSet(String setNumber, String name, String themeName) {
        Theme theme = new Theme();
        theme.setName(themeName);
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(setNumber);
        set.setName(name);
        set.setTheme(theme);
        UserSet us = new UserSet();
        us.setId(UUID.randomUUID());
        us.setBrickSet(set);
        us.setStatus(CollectionStatus.WISHLIST);
        return us;
    }

    private Part part(String number, String name) {
        Part p = new Part();
        p.setId(UUID.randomUUID());
        p.setExternalPartNumber(number);
        p.setName(name);
        return p;
    }

    private Color color(int externalId, String name) {
        Color c = new Color();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setName(name);
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
