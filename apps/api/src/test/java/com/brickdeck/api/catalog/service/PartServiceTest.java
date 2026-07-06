package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

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
}
