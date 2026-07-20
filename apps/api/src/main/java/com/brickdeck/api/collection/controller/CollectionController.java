package com.brickdeck.api.collection.controller;

import com.brickdeck.api.collection.dto.AddUserSetRequest;
import com.brickdeck.api.collection.dto.UpdateUserSetRequest;
import com.brickdeck.api.collection.dto.UserSetResponse;
import com.brickdeck.api.collection.service.CollectionService;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.security.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collection/sets")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping
    public ResponseEntity<UserSetResponse> addSet(@AuthenticationPrincipal User user,
                                                  @Valid @RequestBody AddUserSetRequest request) {
        UserSetResponse response = collectionService.addSet(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<UserSetResponse> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return collectionService.findForUser(user, pageable);
    }

    @PatchMapping("/{id}")
    public UserSetResponse update(@AuthenticationPrincipal User user,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody UpdateUserSetRequest request) {
        return collectionService.updateEntry(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal User user,
                                       @PathVariable UUID id) {
        collectionService.removeEntry(user, id);
        return ResponseEntity.noContent().build();
    }
}
