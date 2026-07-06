package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.InventoryImportResult;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableColorResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePageResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickablePartResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetPartResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetInventoryServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;

    @Mock
    private SetPartRepository setPartRepository;

    @Mock
    private ColorService colorService;

    @Mock
    private PartService partService;

    @Mock
    private RebrickableClient rebrickableClient;

    @InjectMocks
    private SetInventoryService setInventoryService;

    private RebrickableSetPartResponse externalLine(String partNum, int colorId, int qty, boolean spare) {
        RebrickablePartResponse part = new RebrickablePartResponse(partNum, "Brick " + partNum, 11, null, null);
        RebrickableColorResponse color = new RebrickableColorResponse(colorId, "Color " + colorId, "C91A09", false);
        return new RebrickableSetPartResponse(part, color, qty, spare, "el-" + partNum);
    }

    private BrickSet localSet(String setNumber) {
        BrickSet set = new BrickSet();
        set.setId(UUID.randomUUID());
        set.setExternalSetNumber(setNumber);
        return set;
    }

    private void stubResolvers() {
        when(colorService.resolveByExternalId(any())).thenAnswer(inv -> {
            Color c = new Color();
            c.setId(UUID.randomUUID());
            return c;
        });
        when(partService.resolveByExternalPartNumber(any())).thenAnswer(inv -> {
            Part p = new Part();
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    @Test
    void importCreatesLinesAndUpsertsReferenceData() {
        BrickSet set = localSet("75375-1");
        when(brickSetRepository.findByExternalSetNumber("75375-1")).thenReturn(Optional.of(set));
        when(rebrickableClient.getSetParts("75375-1", 1, 100))
                .thenReturn(new RebrickablePageResponse<>(1, null, null, List.of(externalLine("3001", 4, 6, false))));
        stubResolvers();
        when(setPartRepository.findByBrickSetAndPartAndColorAndSpare(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        when(setPartRepository.save(any(SetPart.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryImportResult result = setInventoryService.importInventory("75375-1");

        assertThat(result.setNumber()).isEqualTo("75375-1");
        assertThat(result.linesProcessed()).isEqualTo(1);

        ArgumentCaptor<SetPart> captor = ArgumentCaptor.forClass(SetPart.class);
        verify(setPartRepository).save(captor.capture());
        SetPart saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(6);
        assertThat(saved.isSpare()).isFalse();
        assertThat(saved.getExternalElementId()).isEqualTo("el-3001");
        assertThat(saved.getBrickSet()).isSameAs(set);
    }

    @Test
    void importIsIdempotentUpdatingExistingLine() {
        BrickSet set = localSet("75375-1");
        UUID lineId = UUID.randomUUID();
        SetPart existing = new SetPart();
        existing.setId(lineId);
        existing.setQuantity(2);

        when(brickSetRepository.findByExternalSetNumber("75375-1")).thenReturn(Optional.of(set));
        when(rebrickableClient.getSetParts("75375-1", 1, 100))
                .thenReturn(new RebrickablePageResponse<>(1, null, null, List.of(externalLine("3001", 4, 8, false))));
        stubResolvers();
        when(setPartRepository.findByBrickSetAndPartAndColorAndSpare(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.of(existing));
        when(setPartRepository.save(any(SetPart.class))).thenAnswer(inv -> inv.getArgument(0));

        setInventoryService.importInventory("75375-1");

        ArgumentCaptor<SetPart> captor = ArgumentCaptor.forClass(SetPart.class);
        verify(setPartRepository).save(captor.capture());
        SetPart saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(lineId);
        assertThat(saved.getQuantity()).isEqualTo(8);
    }

    @Test
    void importFetchesAllPages() {
        BrickSet set = localSet("75375-1");
        when(brickSetRepository.findByExternalSetNumber("75375-1")).thenReturn(Optional.of(set));
        when(rebrickableClient.getSetParts("75375-1", 1, 100))
                .thenReturn(new RebrickablePageResponse<>(2, "http://next/page2", null,
                        List.of(externalLine("3001", 4, 6, false))));
        when(rebrickableClient.getSetParts("75375-1", 2, 100))
                .thenReturn(new RebrickablePageResponse<>(2, null, null,
                        List.of(externalLine("3002", 5, 1, true))));
        stubResolvers();
        when(setPartRepository.findByBrickSetAndPartAndColorAndSpare(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        when(setPartRepository.save(any(SetPart.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryImportResult result = setInventoryService.importInventory("75375-1");

        assertThat(result.linesProcessed()).isEqualTo(2);
        verify(rebrickableClient).getSetParts("75375-1", 1, 100);
        verify(rebrickableClient).getSetParts("75375-1", 2, 100);
    }

    @Test
    void importNormalizesMissingSuffix() {
        BrickSet set = localSet("75375-1");
        when(brickSetRepository.findByExternalSetNumber("75375-1")).thenReturn(Optional.of(set));
        when(rebrickableClient.getSetParts(eq("75375-1"), eq(1), eq(100)))
                .thenReturn(new RebrickablePageResponse<>(0, null, null, List.of()));

        InventoryImportResult result = setInventoryService.importInventory("75375");

        assertThat(result.setNumber()).isEqualTo("75375-1");
        assertThat(result.linesProcessed()).isZero();
    }

    @Test
    void importThrowsWhenSetNotImported() {
        when(brickSetRepository.findByExternalSetNumber("00000-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> setInventoryService.importInventory("00000-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("00000-1");

        verify(rebrickableClient, never()).getSetParts(anyString(), anyInt(), anyInt());
    }
}
