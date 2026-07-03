package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrickSetServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;

    @Mock
    private RebrickableClient rebrickableClient;

    @InjectMocks
    private BrickSetService brickSetService;

    @Test
    void returnsAllSetsFromLocalCatalog() {
        UUID setId = UUID.randomUUID();

        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75375-1");
        set.setName("Millennium Falcon");
        set.setYearReleased(2024);
        set.setExternalThemeId(158);
        set.setNumberOfParts(921);
        set.setImageUrl("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg");
        set.setExternalUrl("https://rebrickable.com/sets/75375-1/millennium-falcon/");
        set.setSource("REBRICKABLE");

        when(brickSetRepository.findAll()).thenReturn(List.of(set));

        List<BrickSetResponse> responses = brickSetService.findAll();

        assertThat(responses).hasSize(1);

        BrickSetResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(setId);
        assertThat(response.externalSetNumber()).isEqualTo("75375-1");
        assertThat(response.name()).isEqualTo("Millennium Falcon");
        assertThat(response.yearReleased()).isEqualTo(2024);
        assertThat(response.externalThemeId()).isEqualTo(158);
        assertThat(response.numberOfParts()).isEqualTo(921);
        assertThat(response.imageUrl()).isEqualTo("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg");
        assertThat(response.externalUrl()).isEqualTo("https://rebrickable.com/sets/75375-1/millennium-falcon/");
        assertThat(response.source()).isEqualTo("REBRICKABLE");
        assertThat(response.cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");
    }

    @Test
    void returnsCachedSetWhenItAlreadyExistsLocally() {
        UUID themeId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(themeId);
        theme.setName("Star Wars");

        UUID setId = UUID.randomUUID();
        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75257-1");
        set.setName("Millennium Falcon");
        set.setYearReleased(2019);
        set.setExternalThemeId(158);
        set.setNumberOfParts(1351);
        set.setImageUrl("https://img/75257.jpg");
        set.setExternalUrl("https://rebrickable.com/sets/75257-1/millennium-falcon/");
        set.setSource("REBRICKABLE");
        set.setTheme(theme);

        when(brickSetRepository.findByExternalSetNumber("75257-1"))
                .thenReturn(Optional.of(set));

        BrickSetResponse response = brickSetService.findOrImportBySetNumber("75257-1");

        assertThat(response.id()).isEqualTo(setId);
        assertThat(response.externalSetNumber()).isEqualTo("75257-1");
        assertThat(response.name()).isEqualTo("Millennium Falcon");
        assertThat(response.yearReleased()).isEqualTo(2019);
        assertThat(response.externalThemeId()).isEqualTo(158);
        assertThat(response.numberOfParts()).isEqualTo(1351);
        assertThat(response.imageUrl()).isEqualTo("https://img/75257.jpg");
        assertThat(response.externalUrl()).isEqualTo("https://rebrickable.com/sets/75257-1/millennium-falcon/");
        assertThat(response.source()).isEqualTo("REBRICKABLE");
        assertThat(response.themeId()).isEqualTo(themeId);
        assertThat(response.themeName()).isEqualTo("Star Wars");
        assertThat(response.cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");

        verify(rebrickableClient, never()).getSetByNumber("75257-1");
        verify(brickSetRepository, never()).save(any(BrickSet.class));
    }

    @Test
    void importsSetFromRebrickableWhenItDoesNotExistLocally() {
        UUID savedId = UUID.randomUUID();

        RebrickableSetResponse externalSet = new RebrickableSetResponse(
                "75375-1",
                "Millennium Falcon",
                2024,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "2024-01-30T08:35:07.710189Z"
        );

        when(brickSetRepository.findByExternalSetNumber("75375-1"))
                .thenReturn(Optional.empty());

        when(rebrickableClient.getSetByNumber("75375-1"))
                .thenReturn(externalSet);

        when(brickSetRepository.save(any(BrickSet.class)))
                .thenAnswer(invocation -> {
                    BrickSet setToSave = invocation.getArgument(0);
                    setToSave.setId(savedId);
                    return setToSave;
                });

        BrickSetResponse response = brickSetService.findOrImportBySetNumber("75375-1");

        assertThat(response.id()).isEqualTo(savedId);
        assertThat(response.externalSetNumber()).isEqualTo("75375-1");
        assertThat(response.name()).isEqualTo("Millennium Falcon");
        assertThat(response.yearReleased()).isEqualTo(2024);
        assertThat(response.themeId()).isNull();
        assertThat(response.themeName()).isNull();
        assertThat(response.externalThemeId()).isEqualTo(158);
        assertThat(response.numberOfParts()).isEqualTo(921);
        assertThat(response.imageUrl()).isEqualTo("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg");
        assertThat(response.externalUrl()).isEqualTo("https://rebrickable.com/sets/75375-1/millennium-falcon/");
        assertThat(response.source()).isEqualTo("REBRICKABLE");
        assertThat(response.cacheStatus()).isEqualTo("IMPORTED_FROM_REBRICKABLE");

        ArgumentCaptor<BrickSet> brickSetCaptor = ArgumentCaptor.forClass(BrickSet.class);
        verify(brickSetRepository).save(brickSetCaptor.capture());

        BrickSet savedSet = brickSetCaptor.getValue();
        assertThat(savedSet.getExternalSetNumber()).isEqualTo("75375-1");
        assertThat(savedSet.getName()).isEqualTo("Millennium Falcon");
        assertThat(savedSet.getYearReleased()).isEqualTo(2024);
        assertThat(savedSet.getExternalThemeId()).isEqualTo(158);
        assertThat(savedSet.getNumberOfParts()).isEqualTo(921);
        assertThat(savedSet.getImageUrl()).isEqualTo("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg");
        assertThat(savedSet.getExternalUrl()).isEqualTo("https://rebrickable.com/sets/75375-1/millennium-falcon/");
        assertThat(savedSet.getExternalLastModifiedAt()).isNotNull();
        assertThat(savedSet.getSource()).isEqualTo("REBRICKABLE");
    }

    @Test
    void mapsNullThemeToNullThemeFields() {
        BrickSet set = new BrickSet();
        set.setId(UUID.randomUUID());
        set.setExternalSetNumber("10001-1");
        set.setName("Themeless");
        set.setSource("REBRICKABLE");
        set.setTheme(null);

        when(brickSetRepository.findByExternalSetNumber("10001-1"))
                .thenReturn(Optional.of(set));

        BrickSetResponse response = brickSetService.findOrImportBySetNumber("10001-1");

        assertThat(response.themeId()).isNull();
        assertThat(response.themeName()).isNull();
        assertThat(response.cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");

        verify(rebrickableClient, never()).getSetByNumber("10001-1");
    }
}
