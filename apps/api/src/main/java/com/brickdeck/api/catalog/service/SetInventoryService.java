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
import com.brickdeck.api.external.rebrickable.dto.RebrickablePageResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetPartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetInventoryService {

    private static final int PAGE_SIZE = 100;

    private final BrickSetRepository brickSetRepository;
    private final SetPartRepository setPartRepository;
    private final ColorService colorService;
    private final PartService partService;
    private final RebrickableClient rebrickableClient;

    public SetInventoryService(
            BrickSetRepository brickSetRepository,
            SetPartRepository setPartRepository,
            ColorService colorService,
            PartService partService,
            RebrickableClient rebrickableClient
    ) {
        this.brickSetRepository = brickSetRepository;
        this.setPartRepository = setPartRepository;
        this.colorService = colorService;
        this.partService = partService;
        this.rebrickableClient = rebrickableClient;
    }

    @Transactional
    public InventoryImportResult importInventory(String setNumber) {
        String normalized = SetNumbers.normalize(setNumber);
        BrickSet set = brickSetRepository.findByExternalSetNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Set not imported: " + normalized + " (import the set first)"));

        int linesProcessed = 0;
        int page = 1;

        while (true) {
            RebrickablePageResponse<RebrickableSetPartResponse> pageResponse =
                    rebrickableClient.getSetParts(normalized, page, PAGE_SIZE);

            List<RebrickableSetPartResponse> results =
                    pageResponse.results() != null ? pageResponse.results() : List.of();

            for (RebrickableSetPartResponse line : results) {
                upsertLine(set, line);
                linesProcessed++;
            }

            if (results.isEmpty() || pageResponse.next() == null) {
                break;
            }
            page++;
        }

        return new InventoryImportResult(set.getExternalSetNumber(), linesProcessed);
    }

    private void upsertLine(BrickSet set, RebrickableSetPartResponse line) {
        Color color = colorService.resolveByExternalId(line.color());
        Part part = partService.resolveByExternalPartNumber(line.part());

        SetPart setPart = setPartRepository
                .findByBrickSetAndPartAndColorAndSpare(set, part, color, line.isSpare())
                .orElseGet(SetPart::new);

        setPart.setBrickSet(set);
        setPart.setPart(part);
        setPart.setColor(color);
        setPart.setSpare(line.isSpare());
        setPart.setQuantity(line.quantity());
        setPart.setExternalElementId(line.elementId());

        setPartRepository.save(setPart);
    }
}
