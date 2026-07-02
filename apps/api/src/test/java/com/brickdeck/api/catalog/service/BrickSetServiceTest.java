package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrickSetServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;

    @InjectMocks
    private BrickSetService brickSetService;

    @Test
    void returnsMappedResponseIncludingTheme() {
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
        set.setNumberOfParts(1351);
        set.setImageUrl("https://img/75257.jpg");
        set.setSource("REBRICKABLE");
        set.setTheme(theme);

        when(brickSetRepository.findByExternalSetNumber("75257-1"))
                .thenReturn(Optional.of(set));

        BrickSetResponse response = brickSetService.getByExternalSetNumber("75257-1");

        assertThat(response.id()).isEqualTo(setId);
        assertThat(response.externalSetNumber()).isEqualTo("75257-1");
        assertThat(response.name()).isEqualTo("Millennium Falcon");
        assertThat(response.yearReleased()).isEqualTo(2019);
        assertThat(response.numberOfParts()).isEqualTo(1351);
        assertThat(response.imageUrl()).isEqualTo("https://img/75257.jpg");
        assertThat(response.source()).isEqualTo("REBRICKABLE");
        assertThat(response.themeId()).isEqualTo(themeId);
        assertThat(response.themeName()).isEqualTo("Star Wars");
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

        BrickSetResponse response = brickSetService.getByExternalSetNumber("10001-1");

        assertThat(response.themeId()).isNull();
        assertThat(response.themeName()).isNull();
    }

    @Test
    void throwsWhenSetNotFound() {
        when(brickSetRepository.findByExternalSetNumber("nope"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> brickSetService.getByExternalSetNumber("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }
}
