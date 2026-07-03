package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private ThemeService themeService;

    @InjectMocks
    private BrickSetService brickSetService;

    private RebrickableSetResponse falconExternal() {
        return new RebrickableSetResponse(
                "75375-1",
                "Millennium Falcon",
                2024,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "2024-01-30T08:35:07.710189Z"
        );
    }

    @Test
    void returnsAllSetsFromLocalCatalog() {
        UUID setId = UUID.randomUUID();

        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75375-1");
        set.setName("Millennium Falcon");
        set.setSource("REBRICKABLE");

        when(brickSetRepository.findAll()).thenReturn(List.of(set));

        List<BrickSetResponse> responses = brickSetService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(setId);
        assertThat(responses.get(0).cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");
    }

    @Test
    void findBySetNumberReturnsCachedSet() {
        UUID themeId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(themeId);
        theme.setName("Star Wars");

        UUID setId = UUID.randomUUID();
        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75257-1");
        set.setName("Millennium Falcon");
        set.setSource("REBRICKABLE");
        set.setTheme(theme);

        when(brickSetRepository.findByExternalSetNumber("75257-1"))
                .thenReturn(Optional.of(set));

        BrickSetResponse response = brickSetService.findBySetNumber("75257-1");

        assertThat(response.id()).isEqualTo(setId);
        assertThat(response.themeId()).isEqualTo(themeId);
        assertThat(response.themeName()).isEqualTo("Star Wars");
        assertThat(response.cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");

        verify(rebrickableClient, never()).getSetByNumber("75257-1");
    }

    @Test
    void findBySetNumberThrowsWhenMissing() {
        when(brickSetRepository.findByExternalSetNumber("00000-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> brickSetService.findBySetNumber("00000-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("00000-1");

        verify(rebrickableClient, never()).getSetByNumber("00000-1");
    }

    @Test
    void importSetCreatesNewSetAndLinksTheme() {
        UUID savedId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(themeId);
        theme.setName("Star Wars");

        when(brickSetRepository.findByExternalSetNumber("75375-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("75375-1")).thenReturn(falconExternal());
        when(themeService.resolveByExternalId(158)).thenReturn(theme);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> {
            BrickSet toSave = inv.getArgument(0);
            toSave.setId(savedId);
            return toSave;
        });

        ImportResult result = brickSetService.importSet("75375-1");

        assertThat(result.created()).isTrue();
        assertThat(result.body().id()).isEqualTo(savedId);
        assertThat(result.body().externalSetNumber()).isEqualTo("75375-1");
        assertThat(result.body().themeId()).isEqualTo(themeId);
        assertThat(result.body().themeName()).isEqualTo("Star Wars");
        assertThat(result.body().externalThemeId()).isEqualTo(158);
        assertThat(result.body().cacheStatus()).isEqualTo("IMPORTED_FROM_REBRICKABLE");

        ArgumentCaptor<BrickSet> captor = ArgumentCaptor.forClass(BrickSet.class);
        verify(brickSetRepository).save(captor.capture());
        BrickSet saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Millennium Falcon");
        assertThat(saved.getExternalLastModifiedAt()).isNotNull();
        assertThat(saved.getTheme()).isSameAs(theme);
        assertThat(saved.getSource()).isEqualTo("REBRICKABLE");
    }

    @Test
    void importSetRefreshesExistingSet() {
        UUID existingId = UUID.randomUUID();
        BrickSet existing = new BrickSet();
        existing.setId(existingId);
        existing.setExternalSetNumber("75375-1");
        existing.setName("Stale Name");
        existing.setSource("REBRICKABLE");

        when(brickSetRepository.findByExternalSetNumber("75375-1"))
                .thenReturn(Optional.of(existing));
        when(rebrickableClient.getSetByNumber("75375-1")).thenReturn(falconExternal());
        when(themeService.resolveByExternalId(158)).thenReturn(null);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = brickSetService.importSet("75375-1");

        assertThat(result.created()).isFalse();
        assertThat(result.body().id()).isEqualTo(existingId);
        assertThat(result.body().name()).isEqualTo("Millennium Falcon");
        assertThat(result.body().cacheStatus()).isEqualTo("REFRESHED_FROM_REBRICKABLE");
        verify(brickSetRepository).save(existing);
    }

    @Test
    void importSetWithNullThemeDoesNotLinkTheme() {
        RebrickableSetResponse themeless = new RebrickableSetResponse(
                "10001-1", "Themeless", 1999, null, 10,
                null, null, null);

        when(brickSetRepository.findByExternalSetNumber("10001-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("10001-1")).thenReturn(themeless);
        when(themeService.resolveByExternalId(null)).thenReturn(null);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = brickSetService.importSet("10001-1");

        assertThat(result.created()).isTrue();
        assertThat(result.body().themeId()).isNull();
        assertThat(result.body().themeName()).isNull();
        assertThat(result.body().externalThemeId()).isNull();
    }

    @Test
    void importSetMapsRebrickableNotFoundToResourceNotFound() {
        when(brickSetRepository.findByExternalSetNumber("99999-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("99999-1"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> brickSetService.importSet("99999-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99999-1");

        verify(brickSetRepository, never()).save(any(BrickSet.class));
    }
}
