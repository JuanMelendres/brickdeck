package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.AddPriceSnapshotRequest;
import com.brickdeck.api.pricing.dto.PriceSnapshotResponse;
import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.entity.PriceSource;
import com.brickdeck.api.pricing.repository.PriceSnapshotRepository;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class PriceSnapshotServiceTest {

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;
    @Mock
    private BrickSetService brickSetService;

    @InjectMocks
    private PriceSnapshotService service;

    private User owner;
    private BrickSet set;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        set = new BrickSet();
        set.setExternalSetNumber("75257-1");
        set.setName("Falcon");
    }

    private AddPriceSnapshotRequest request() {
        return new AddPriceSnapshotRequest(
                "75257-1", new BigDecimal("129.99"), "USD",
                PriceCondition.NEW, LocalDate.of(2026, 1, 10), "LEGO Store", null);
    }

    @Test
    void addResolvesTheSetAndPersistsAManualSnapshot() {
        when(brickSetService.findOrImportEntity("75257-1")).thenReturn(set);
        when(priceSnapshotRepository.save(any(PriceSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PriceSnapshotResponse response = service.addSnapshot(owner, request());

        ArgumentCaptor<PriceSnapshot> captor = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository).save(captor.capture());
        PriceSnapshot saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getBrickSet()).isSameAs(set);
        assertThat(saved.getSource()).isEqualTo(PriceSource.MANUAL);
        assertThat(saved.getAmount()).isEqualByComparingTo("129.99");
        assertThat(saved.getCurrency()).isEqualTo("USD");

        assertThat(response.setNumber()).isEqualTo("75257-1");
        assertThat(response.amount()).isEqualByComparingTo("129.99");
        assertThat(response.condition()).isEqualTo(PriceCondition.NEW);
        assertThat(response.source()).isEqualTo(PriceSource.MANUAL);
    }

    @Test
    void listsAllSnapshotsForTheUserWhenNoSetFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(priceSnapshotRepository.findByUserId(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(snapshot()), pageable, 1));

        PageResponse<PriceSnapshotResponse> page = service.findForUser(owner, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void listsSnapshotsFilteredBySetNumber() {
        Pageable pageable = PageRequest.of(0, 20);
        when(priceSnapshotRepository
                .findByUserIdAndBrickSet_ExternalSetNumber(owner.getId(), "75257-1", pageable))
                .thenReturn(new PageImpl<>(List.of(snapshot()), pageable, 1));

        PageResponse<PriceSnapshotResponse> page = service.findForUser(owner, "75257-1", pageable);

        assertThat(page.content()).hasSize(1);
        verify(priceSnapshotRepository, never()).findByUserId(any(), any());
    }

    @Test
    void deletesAnOwnedSnapshot() {
        PriceSnapshot snap = snapshot();
        when(priceSnapshotRepository.findByIdAndUserId(snap.getId(), owner.getId()))
                .thenReturn(Optional.of(snap));

        service.removeSnapshot(owner, snap.getId());

        verify(priceSnapshotRepository).delete(snap);
    }

    @Test
    void throwsWhenDeletingASnapshotNotOwned() {
        UUID id = UUID.randomUUID();
        when(priceSnapshotRepository.findByIdAndUserId(id, owner.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeSnapshot(owner, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(priceSnapshotRepository, never()).delete(any());
    }

    private PriceSnapshot snapshot() {
        PriceSnapshot p = new PriceSnapshot();
        p.setId(UUID.randomUUID());
        p.setUser(owner);
        p.setBrickSet(set);
        p.setSource(PriceSource.MANUAL);
        p.setCondition(PriceCondition.NEW);
        p.setCurrency("USD");
        p.setAmount(new BigDecimal("129.99"));
        p.setObservedAt(LocalDate.of(2026, 1, 10));
        return p;
    }
}
