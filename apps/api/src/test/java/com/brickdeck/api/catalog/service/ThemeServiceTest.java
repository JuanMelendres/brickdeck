package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private RebrickableClient rebrickableClient;

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

    @Test
    void resolveByExternalIdReturnsNullWhenExternalIdIsNull() {
        Theme result = themeService.resolveByExternalId(null);

        assertThat(result).isNull();
        verifyNoInteractions(rebrickableClient);
        verify(themeRepository, never()).save(any(Theme.class));
    }

    @Test
    void resolveByExternalIdCreatesNewThemeWhenAbsent() {
        when(rebrickableClient.getThemeById(158))
                .thenReturn(new RebrickableThemeResponse(158, "Star Wars", null));
        when(themeRepository.findByExternalId("158")).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenAnswer(inv -> inv.getArgument(0));

        Theme result = themeService.resolveByExternalId(158);

        assertThat(result.getExternalId()).isEqualTo("158");
        assertThat(result.getName()).isEqualTo("Star Wars");

        ArgumentCaptor<Theme> captor = ArgumentCaptor.forClass(Theme.class);
        verify(themeRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("158");
        assertThat(captor.getValue().getName()).isEqualTo("Star Wars");
    }

    @Test
    void resolveByExternalIdReusesExistingThemeAndUpdatesName() {
        UUID existingId = UUID.randomUUID();
        Theme existing = new Theme();
        existing.setId(existingId);
        existing.setExternalId("158");
        existing.setName("Old Name");

        when(rebrickableClient.getThemeById(158))
                .thenReturn(new RebrickableThemeResponse(158, "Star Wars", null));
        when(themeRepository.findByExternalId("158")).thenReturn(Optional.of(existing));
        when(themeRepository.save(any(Theme.class))).thenAnswer(inv -> inv.getArgument(0));

        Theme result = themeService.resolveByExternalId(158);

        assertThat(result.getId()).isEqualTo(existingId);
        assertThat(result.getName()).isEqualTo("Star Wars");
        verify(themeRepository).save(existing);
    }
}
