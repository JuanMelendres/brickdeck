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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPartServiceTest {

    @Mock
    private UserPartRepository userPartRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private ColorRepository colorRepository;

    @InjectMocks
    private UserPartService userPartService;

    private User owner() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        return user;
    }

    private Part brick() {
        Part part = new Part();
        part.setId(UUID.randomUUID());
        part.setExternalPartNumber("3001");
        part.setName("Brick 2 x 4");
        part.setImageUrl("https://cdn.rebrickable.com/media/parts/3001.jpg");
        return part;
    }

    private Color red() {
        Color color = new Color();
        color.setId(UUID.randomUUID());
        color.setExternalId(4);
        color.setName("Red");
        color.setRgb("C91A09");
        return color;
    }

    @Test
    void addPartResolvesCatalogRefsAndPersists() {
        User owner = owner();
        Part part = brick();
        Color color = red();

        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(part));
        when(colorRepository.findByExternalId(4)).thenReturn(Optional.of(color));
        when(userPartRepository.existsByUserIdAndPartIdAndColorId(owner.getId(), part.getId(), color.getId()))
                .thenReturn(false);
        when(userPartRepository.save(any(UserPart.class))).thenAnswer(inv -> {
            UserPart toSave = inv.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        AddUserPartRequest request = new AddUserPartRequest("3001", 4, 12, "Drawer A3");

        UserPartResponse response = userPartService.addPart(owner, request);

        assertThat(response.partNumber()).isEqualTo("3001");
        assertThat(response.colorName()).isEqualTo("Red");
        assertThat(response.quantity()).isEqualTo(12);
        assertThat(response.storageLocation()).isEqualTo("Drawer A3");

        ArgumentCaptor<UserPart> captor = ArgumentCaptor.forClass(UserPart.class);
        verify(userPartRepository).save(captor.capture());
        UserPart saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getPart()).isSameAs(part);
        assertThat(saved.getColor()).isSameAs(color);
    }

    @Test
    void addPartThrowsWhenPartNotInCatalog() {
        User owner = owner();
        when(partRepository.findByExternalPartNumber("9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPartService.addPart(owner, new AddUserPartRequest("9999", 4, 1, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");

        verify(userPartRepository, never()).save(any(UserPart.class));
    }

    @Test
    void addPartThrowsWhenColorNotInCatalog() {
        User owner = owner();
        Part part = brick();
        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(part));
        when(colorRepository.findByExternalId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPartService.addPart(owner, new AddUserPartRequest("3001", 999, 1, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(userPartRepository, never()).save(any(UserPart.class));
    }

    @Test
    void addPartRejectsDuplicate() {
        User owner = owner();
        Part part = brick();
        Color color = red();

        when(partRepository.findByExternalPartNumber("3001")).thenReturn(Optional.of(part));
        when(colorRepository.findByExternalId(4)).thenReturn(Optional.of(color));
        when(userPartRepository.existsByUserIdAndPartIdAndColorId(owner.getId(), part.getId(), color.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> userPartService.addPart(owner, new AddUserPartRequest("3001", 4, 1, null)))
                .isInstanceOf(DuplicateCollectionEntryException.class)
                .hasMessageContaining("3001");

        verify(userPartRepository, never()).save(any(UserPart.class));
    }

    @Test
    void findForUserReturnsPagedEntries() {
        User owner = owner();

        UserPart entry = new UserPart();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);
        entry.setPart(brick());
        entry.setColor(red());
        entry.setQuantity(5);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt"));
        when(userPartRepository.findByUserId(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        PageResponse<UserPartResponse> page = userPartService.findForUser(owner, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).partNumber()).isEqualTo("3001");
        assertThat(page.content().get(0).quantity()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void updateEntryAppliesNonNullFieldsOnly() {
        User owner = owner();

        UserPart entry = new UserPart();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);
        entry.setPart(brick());
        entry.setColor(red());
        entry.setQuantity(5);
        entry.setStorageLocation("Drawer A3");

        when(userPartRepository.findByIdAndUserId(entry.getId(), owner.getId()))
                .thenReturn(Optional.of(entry));
        when(userPartRepository.save(any(UserPart.class))).thenAnswer(inv -> inv.getArgument(0));

        // quantity changes; storageLocation omitted -> preserved
        UserPartResponse response = userPartService.updateEntry(
                owner, entry.getId(), new UpdateUserPartRequest(20, null));

        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.storageLocation()).isEqualTo("Drawer A3");
    }

    @Test
    void updateEntryThrowsWhenNotOwnedOrMissing() {
        User owner = owner();
        UUID entryId = UUID.randomUUID();
        when(userPartRepository.findByIdAndUserId(entryId, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPartService.updateEntry(owner, entryId, new UpdateUserPartRequest(1, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userPartRepository, never()).save(any(UserPart.class));
    }

    @Test
    void removeEntryDeletesOwnedEntry() {
        User owner = owner();
        UserPart entry = new UserPart();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);

        when(userPartRepository.findByIdAndUserId(entry.getId(), owner.getId()))
                .thenReturn(Optional.of(entry));

        userPartService.removeEntry(owner, entry.getId());

        verify(userPartRepository).delete(entry);
    }
}
