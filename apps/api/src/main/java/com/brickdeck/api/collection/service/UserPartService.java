package com.brickdeck.api.collection.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.collection.DuplicateCollectionEntryException;
import com.brickdeck.api.collection.dto.AddUserPartRequest;
import com.brickdeck.api.collection.dto.UpdateUserPartRequest;
import com.brickdeck.api.collection.dto.UserPartResponse;
import com.brickdeck.api.collection.entity.UserPart;
import com.brickdeck.api.collection.repository.UserPartRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserPartService {

    private final UserPartRepository userPartRepository;
    private final PartRepository partRepository;
    private final ColorRepository colorRepository;

    public UserPartService(UserPartRepository userPartRepository,
                           PartRepository partRepository,
                           ColorRepository colorRepository) {
        this.userPartRepository = userPartRepository;
        this.partRepository = partRepository;
        this.colorRepository = colorRepository;
    }

    @Transactional
    public UserPartResponse addPart(User owner, AddUserPartRequest request) {
        Part part = partRepository.findByExternalPartNumber(request.externalPartNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Part not found in catalog: " + request.externalPartNumber()));
        Color color = colorRepository.findByExternalId(request.colorExternalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Color not found in catalog: " + request.colorExternalId()));

        if (userPartRepository.existsByUserIdAndPartIdAndColorId(
                owner.getId(), part.getId(), color.getId())) {
            throw new DuplicateCollectionEntryException(
                    "Part already in inventory: " + part.getExternalPartNumber()
                            + " in color " + color.getExternalId());
        }

        UserPart entry = new UserPart();
        entry.setUser(owner);
        entry.setPart(part);
        entry.setColor(color);
        entry.setQuantity(request.quantity());
        entry.setStorageLocation(request.storageLocation());

        return toResponse(userPartRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserPartResponse> findForUser(User owner, Pageable pageable) {
        Page<UserPart> page = userPartRepository.findByUserId(owner.getId(), pageable);
        List<UserPartResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public UserPartResponse updateEntry(User owner, UUID entryId, UpdateUserPartRequest request) {
        UserPart entry = requireOwnedEntry(owner, entryId);

        if (request.quantity() != null) {
            entry.setQuantity(request.quantity());
        }
        if (request.storageLocation() != null) {
            entry.setStorageLocation(request.storageLocation());
        }

        return toResponse(userPartRepository.save(entry));
    }

    @Transactional
    public void removeEntry(User owner, UUID entryId) {
        userPartRepository.delete(requireOwnedEntry(owner, entryId));
    }

    private UserPart requireOwnedEntry(User owner, UUID entryId) {
        return userPartRepository.findByIdAndUserId(entryId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loose part entry not found: " + entryId));
    }

    private UserPartResponse toResponse(UserPart entry) {
        Part part = entry.getPart();
        Color color = entry.getColor();
        return new UserPartResponse(
                entry.getId(),
                part.getExternalPartNumber(),
                part.getName(),
                part.getImageUrl(),
                color.getExternalId(),
                color.getName(),
                color.getRgb(),
                entry.getQuantity(),
                entry.getStorageLocation(),
                entry.getCreatedAt()
        );
    }
}
