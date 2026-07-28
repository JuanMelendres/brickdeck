package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableColorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColorServiceTest {

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private RebrickableClient rebrickableClient;

    @InjectMocks
    private ColorService colorService;

    @Test
    void resolveCreatesColorWhenMissing() {
        RebrickableColorResponse external = new RebrickableColorResponse(4, "Red", "C91A09", false);

        when(colorRepository.findByExternalId(4)).thenReturn(Optional.empty());
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> inv.getArgument(0));

        Color result = colorService.resolveByExternalId(external);

        assertThat(result.getExternalId()).isEqualTo(4);
        assertThat(result.getName()).isEqualTo("Red");
        assertThat(result.getRgb()).isEqualTo("C91A09");
        assertThat(result.isTransparent()).isFalse();
    }

    @Test
    void resolveUpdatesExistingColor() {
        UUID id = UUID.randomUUID();
        Color existing = new Color();
        existing.setId(id);
        existing.setExternalId(4);
        existing.setName("Stale");

        RebrickableColorResponse external = new RebrickableColorResponse(4, "Trans-Red", "C91A09", true);

        when(colorRepository.findByExternalId(4)).thenReturn(Optional.of(existing));
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> inv.getArgument(0));

        Color result = colorService.resolveByExternalId(external);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Trans-Red");
        assertThat(result.isTransparent()).isTrue();

        ArgumentCaptor<Color> captor = ArgumentCaptor.forClass(Color.class);
        verify(colorRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    void findOrImportReturnsLocalHitWithoutFetchingRebrickable() {
        Color existing = new Color();
        existing.setId(UUID.randomUUID());
        existing.setExternalId(4);
        existing.setName("Red");

        when(colorRepository.findByExternalId(4)).thenReturn(Optional.of(existing));

        Color result = colorService.findOrImport(4);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(rebrickableClient);
    }

    @Test
    void findOrImportFetchesAndPersistsOnMiss() {
        when(colorRepository.findByExternalId(4)).thenReturn(Optional.empty());
        when(rebrickableClient.getColor(4)).thenReturn(new RebrickableColorResponse(4, "Red", "C91A09", false));
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> inv.getArgument(0));

        Color result = colorService.findOrImport(4);

        assertThat(result.getExternalId()).isEqualTo(4);
        assertThat(result.getName()).isEqualTo("Red");
    }

    @Test
    void findOrImportThrowsWhenRebrickableAlsoMisses() {
        when(colorRepository.findByExternalId(999)).thenReturn(Optional.empty());
        when(rebrickableClient.getColor(999)).thenThrow(
                HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> colorService.findOrImport(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
