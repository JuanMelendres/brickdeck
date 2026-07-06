package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.InventoryImportResult;
import com.brickdeck.api.catalog.service.SetInventoryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/sets")
public class SetInventoryImportController {

    private final SetInventoryService setInventoryService;

    public SetInventoryImportController(SetInventoryService setInventoryService) {
        this.setInventoryService = setInventoryService;
    }

    @PostMapping("/{setNumber}/inventory/import")
    public InventoryImportResult importInventory(@PathVariable String setNumber) {
        return setInventoryService.importInventory(setNumber);
    }
}
