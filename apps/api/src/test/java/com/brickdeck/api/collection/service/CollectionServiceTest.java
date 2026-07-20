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

import java.math.BigDecimal;
import java.time.LocalDate;
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
class CollectionServiceTest {

    @Mock
    private UserSetRepository userSetRepository;

    @Mock
    private BrickSetService brickSetService;

    @InjectMocks
    private CollectionService collectionService;

    private User owner() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        return user;
    }

    private BrickSet falcon() {
        Theme theme = new Theme();
        theme.setId(UUID.randomUUID());
        theme.setName("Star Wars");

        BrickSet set = new BrickSet();
        set.setId(UUID.randomUUID());
        set.setExternalSetNumber("75375-1");
        set.setName("Millennium Falcon");
        set.setYearReleased(2024);
        set.setImageUrl("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg");
        set.setTheme(theme);
        return set;
    }

    @Test
    void addSetResolvesCatalogEntryAndPersistsEntry() {
        User owner = owner();
        BrickSet set = falcon();

        when(brickSetService.findOrImportEntity("75375-1")).thenReturn(set);
        when(userSetRepository.existsByUserIdAndBrickSetId(owner.getId(), set.getId())).thenReturn(false);
        when(userSetRepository.save(any(UserSet.class))).thenAnswer(inv -> {
            UserSet toSave = inv.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        AddUserSetRequest request = new AddUserSetRequest(
                "75375-1", CollectionStatus.BUILT, new BigDecimal("849.99"), LocalDate.of(2024, 5, 1));

        UserSetResponse response = collectionService.addSet(owner, request);

        assertThat(response.setNumber()).isEqualTo("75375-1");
        assertThat(response.setName()).isEqualTo("Millennium Falcon");
        assertThat(response.themeName()).isEqualTo("Star Wars");
        assertThat(response.status()).isEqualTo(CollectionStatus.BUILT);
        assertThat(response.purchasePrice()).isEqualByComparingTo("849.99");
        assertThat(response.purchaseDate()).isEqualTo(LocalDate.of(2024, 5, 1));

        ArgumentCaptor<UserSet> captor = ArgumentCaptor.forClass(UserSet.class);
        verify(userSetRepository).save(captor.capture());
        UserSet saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getBrickSet()).isSameAs(set);
        assertThat(saved.getStatus()).isEqualTo(CollectionStatus.BUILT);
    }

    @Test
    void addSetDefaultsStatusToOwnedWhenNull() {
        User owner = owner();
        BrickSet set = falcon();

        when(brickSetService.findOrImportEntity("75375-1")).thenReturn(set);
        when(userSetRepository.existsByUserIdAndBrickSetId(owner.getId(), set.getId())).thenReturn(false);
        when(userSetRepository.save(any(UserSet.class))).thenAnswer(inv -> inv.getArgument(0));

        AddUserSetRequest request = new AddUserSetRequest("75375-1", null, null, null);

        UserSetResponse response = collectionService.addSet(owner, request);

        assertThat(response.status()).isEqualTo(CollectionStatus.OWNED);
    }

    @Test
    void addSetRejectsDuplicateEntry() {
        User owner = owner();
        BrickSet set = falcon();

        when(brickSetService.findOrImportEntity("75375-1")).thenReturn(set);
        when(userSetRepository.existsByUserIdAndBrickSetId(owner.getId(), set.getId())).thenReturn(true);

        AddUserSetRequest request = new AddUserSetRequest("75375-1", null, null, null);

        assertThatThrownBy(() -> collectionService.addSet(owner, request))
                .isInstanceOf(DuplicateCollectionEntryException.class)
                .hasMessageContaining("75375-1");

        verify(userSetRepository, never()).save(any(UserSet.class));
    }

    @Test
    void findForUserReturnsPagedEntries() {
        User owner = owner();
        BrickSet set = falcon();

        UserSet entry = new UserSet();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);
        entry.setBrickSet(set);
        entry.setStatus(CollectionStatus.OWNED);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt"));
        when(userSetRepository.findByUserId(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        PageResponse<UserSetResponse> page = collectionService.findForUser(owner, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).setNumber()).isEqualTo("75375-1");
        assertThat(page.content().get(0).themeName()).isEqualTo("Star Wars");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
    }

    @Test
    void updateEntryAppliesNonNullFieldsOnly() {
        User owner = owner();
        BrickSet set = falcon();

        UserSet entry = new UserSet();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);
        entry.setBrickSet(set);
        entry.setStatus(CollectionStatus.OWNED);
        entry.setPurchasePrice(new BigDecimal("100.00"));
        entry.setPurchaseDate(LocalDate.of(2023, 1, 1));

        when(userSetRepository.findByIdAndUserId(entry.getId(), owner.getId()))
                .thenReturn(Optional.of(entry));
        when(userSetRepository.save(any(UserSet.class))).thenAnswer(inv -> inv.getArgument(0));

        // status changes; purchasePrice/purchaseDate omitted (null) -> preserved
        UpdateUserSetRequest request = new UpdateUserSetRequest(CollectionStatus.BUILT, null, null);

        UserSetResponse response = collectionService.updateEntry(owner, entry.getId(), request);

        assertThat(response.status()).isEqualTo(CollectionStatus.BUILT);
        assertThat(response.purchasePrice()).isEqualByComparingTo("100.00");
        assertThat(response.purchaseDate()).isEqualTo(LocalDate.of(2023, 1, 1));
    }

    @Test
    void updateEntryThrowsWhenNotOwnedOrMissing() {
        User owner = owner();
        UUID entryId = UUID.randomUUID();

        when(userSetRepository.findByIdAndUserId(entryId, owner.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.updateEntry(
                owner, entryId, new UpdateUserSetRequest(CollectionStatus.BUILT, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(entryId.toString());

        verify(userSetRepository, never()).save(any(UserSet.class));
    }

    @Test
    void removeEntryDeletesOwnedEntry() {
        User owner = owner();

        UserSet entry = new UserSet();
        entry.setId(UUID.randomUUID());
        entry.setUser(owner);
        entry.setBrickSet(falcon());

        when(userSetRepository.findByIdAndUserId(entry.getId(), owner.getId()))
                .thenReturn(Optional.of(entry));

        collectionService.removeEntry(owner, entry.getId());

        verify(userSetRepository).delete(entry);
    }

    @Test
    void removeEntryThrowsWhenNotOwnedOrMissing() {
        User owner = owner();
        UUID entryId = UUID.randomUUID();

        when(userSetRepository.findByIdAndUserId(entryId, owner.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.removeEntry(owner, entryId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userSetRepository, never()).delete(any(UserSet.class));
    }
}
