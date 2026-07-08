package com.brickdeck.api.collection.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.collection.DuplicateCollectionEntryException;
import com.brickdeck.api.collection.dto.AddUserSetRequest;
import com.brickdeck.api.collection.dto.UpdateUserSetRequest;
import com.brickdeck.api.collection.dto.UserSetResponse;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.security.entity.User;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectionService {

    private final UserSetRepository userSetRepository;
    private final BrickSetService brickSetService;

    public CollectionService(UserSetRepository userSetRepository, BrickSetService brickSetService) {
        this.userSetRepository = userSetRepository;
        this.brickSetService = brickSetService;
    }

    @Transactional
    public UserSetResponse addSet(User owner, AddUserSetRequest request) {
        BrickSet set = brickSetService.findOrImportEntity(request.setNumber());

        if (userSetRepository.existsByUserIdAndBrickSetId(owner.getId(), set.getId())) {
            throw new DuplicateCollectionEntryException(
                    "Set already in collection: " + set.getExternalSetNumber());
        }

        UserSet entry = new UserSet();
        entry.setUser(owner);
        entry.setBrickSet(set);
        entry.setStatus(request.status() != null ? request.status() : CollectionStatus.OWNED);
        entry.setPurchasePrice(request.purchasePrice());
        entry.setPurchaseDate(request.purchaseDate());

        return toResponse(userSetRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSetResponse> findForUser(User owner, Pageable pageable) {
        Page<UserSet> page = userSetRepository.findByUserId(owner.getId(), pageable);
        List<UserSetResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public UserSetResponse updateEntry(User owner, UUID entryId, UpdateUserSetRequest request) {
        UserSet entry = requireOwnedEntry(owner, entryId);

        if (request.status() != null) {
            entry.setStatus(request.status());
        }
        if (request.purchasePrice() != null) {
            entry.setPurchasePrice(request.purchasePrice());
        }
        if (request.purchaseDate() != null) {
            entry.setPurchaseDate(request.purchaseDate());
        }

        return toResponse(userSetRepository.save(entry));
    }

    @Transactional
    public void removeEntry(User owner, UUID entryId) {
        userSetRepository.delete(requireOwnedEntry(owner, entryId));
    }

    private UserSet requireOwnedEntry(User owner, UUID entryId) {
        return userSetRepository.findByIdAndUserId(entryId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collection entry not found: " + entryId));
    }

    private UserSetResponse toResponse(UserSet entry) {
        BrickSet set = entry.getBrickSet();
        Theme theme = set.getTheme();
        return new UserSetResponse(
                entry.getId(),
                set.getExternalSetNumber(),
                set.getName(),
                set.getYearReleased(),
                theme != null ? theme.getName() : null,
                set.getImageUrl(),
                entry.getStatus(),
                entry.getPurchasePrice(),
                entry.getPurchaseDate(),
                entry.getCreatedAt()
        );
    }
}
