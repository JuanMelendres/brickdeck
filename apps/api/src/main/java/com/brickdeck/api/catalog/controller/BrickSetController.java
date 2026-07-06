package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.common.PageResponse;
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
    public BrickSetResponse findBySetNumber(@PathVariable String setNumber) {
        return brickSetService.findBySetNumber(setNumber);
    }

    @GetMapping("/search")
    public PageResponse<BrickSetResponse> search(
            @RequestParam("q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return brickSetService.search(query, page, size);
    }
}
