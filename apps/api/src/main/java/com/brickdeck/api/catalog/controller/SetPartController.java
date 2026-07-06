package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.SetPartResponse;
import com.brickdeck.api.catalog.service.SetInventoryService;
import com.brickdeck.api.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sets")
public class SetPartController {

    private final SetInventoryService setInventoryService;

    public SetPartController(SetInventoryService setInventoryService) {
        this.setInventoryService = setInventoryService;
    }

    @GetMapping("/{setNumber}/parts")
    public PageResponse<SetPartResponse> getInventory(
            @PathVariable String setNumber,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return setInventoryService.findInventory(setNumber, pageable);
    }
}
