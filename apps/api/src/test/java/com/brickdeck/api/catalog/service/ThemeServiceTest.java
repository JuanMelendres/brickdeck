package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
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
class ThemeServiceTest {

    @Mock
    private ThemeRepository themeRepository;

    @InjectMocks
    private ThemeService themeService;

    @Test
    void returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(id);
        theme.setExternalId("158");
        theme.setName("Star Wars");
        theme.setParentThemeId(parentId);

        when(themeRepository.findById(id)).thenReturn(Optional.of(theme));

        ThemeResponse response = themeService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.externalId()).isEqualTo("158");
        assertThat(response.name()).isEqualTo("Star Wars");
        assertThat(response.parentThemeId()).isEqualTo(parentId);
    }

    @Test
    void throwsWhenThemeNotFound() {
        UUID id = UUID.randomUUID();
        when(themeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> themeService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
