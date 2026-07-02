package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.service.BrickSetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/sets")
public class BrickSetController {

    private final BrickSetService brickSetService;

    public BrickSetController(BrickSetService brickSetService) {
        this.brickSetService = brickSetService;
    }

    @GetMapping("/{externalSetNumber}")
    public BrickSetResponse getBySetNumber(@PathVariable String externalSetNumber) {
        return brickSetService.getByExternalSetNumber(externalSetNumber);
    }
}
