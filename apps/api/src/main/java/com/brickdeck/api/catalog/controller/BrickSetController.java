package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.service.BrickSetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sets")
public class BrickSetController {

    private final BrickSetService brickSetService;

    public BrickSetController(BrickSetService brickSetService) {
        this.brickSetService = brickSetService;
    }

    @GetMapping
    public List<BrickSetResponse> findAll() {
        return brickSetService.findAll();
    }

    @GetMapping("/by-number/{setNumber}")
    public BrickSetResponse findOrImportBySetNumber(@PathVariable String setNumber) {
        return brickSetService.findOrImportBySetNumber(setNumber);
    }
}