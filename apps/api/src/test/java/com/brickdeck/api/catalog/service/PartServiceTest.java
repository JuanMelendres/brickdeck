package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private RebrickableClient rebrickableClient;

    @InjectMocks
    private PartService partService;

    private RebrickablePartResponse brickExternal() {
        return new RebrickablePartResponse(
                "3001", "Brick 2 x 4", 11,
                "https://rebrickable.com/parts/3001/",
                "https://cdn.rebrickable.com/media/parts/3001.jpg");
    }

    @Test
    void resolveCreatesPartWhenMissing() {
        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.empty());
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        Part result = partService.resolveByExternalPartNumber(brickExternal());

        assertThat(result.getExternalPartNumber()).isEqualTo("3001");
        assertThat(result.getName()).isEqualTo("Brick 2 x 4");
        assertThat(result.getExternalCategoryId()).isEqualTo(11);
        assertThat(result.getPartUrl()).isEqualTo("https://rebrickable.com/parts/3001/");
        assertThat(result.getImageUrl()).isEqualTo("https://cdn.rebrickable.com/media/parts/3001.jpg");
    }

    @Test
    void resolveUpdatesExistingPart() {
        UUID id = UUID.randomUUID();
        Part existing = new Part();
        existing.setId(id);
        existing.setExternalPartNumber("3001");
        existing.setName("Stale");

        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(existing));
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        Part result = partService.resolveByExternalPartNumber(brickExternal());

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Brick 2 x 4");
    }

    @Test
    void findOrImportReturnsLocalHitWithoutFetchingRebrickable() {
        Part existing = new Part();
        existing.setId(UUID.randomUUID());
        existing.setExternalPartNumber("3001");
        existing.setName("Brick 2 x 4");

        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(existing));

        Part result = partService.findOrImport("3001");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(rebrickableClient);
    }

    @Test
    void findOrImportFetchesAndPersistsOnMiss() {
        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.empty());
        when(rebrickableClient.getPart("3001")).thenReturn(brickExternal());
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        Part result = partService.findOrImport("3001");

        assertThat(result.getExternalPartNumber()).isEqualTo("3001");
        assertThat(result.getName()).isEqualTo("Brick 2 x 4");
    }

    @Test
    void findOrImportThrowsWhenRebrickableAlsoMisses() {
        when(partRepository.findByExternalPartNumber("9999")).thenReturn(Optional.empty());
        when(rebrickableClient.getPart("9999")).thenThrow(
                HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> partService.findOrImport("9999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }
}
