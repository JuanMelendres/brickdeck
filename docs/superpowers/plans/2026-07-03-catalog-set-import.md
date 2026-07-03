# Catalog Set Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `POST /api/v1/catalog/sets/import` to import a LEGO set (and its theme) from Rebrickable into the local catalog with idempotent upsert semantics, and make the existing set read endpoint read-only.

**Architecture:** A new thin import controller under `/api/v1/catalog/sets` delegates to `BrickSetService.importSet`, which fetches the set from Rebrickable, resolves/upserts its Theme through `ThemeService`, and upserts the `BrickSet` row. Theme resolution owns the Theme aggregate; set import orchestrates. Reads no longer write: `findOrImportBySetNumber` becomes `findBySetNumber` (404 on miss).

**Tech Stack:** Java 21, Spring Boot 3 (Spring Web `RestClient`, Spring Data JPA, Bean Validation), Flyway, PostgreSQL 16, JUnit 5, Mockito, AssertJ, `@WebMvcTest`, `MockRestServiceServer`.

## Global Constraints

- Package root: `com.brickdeck.api`; module path `apps/api`.
- Never expose entities to controllers — return `BrickSetResponse` DTO. Use Java records for DTOs.
- Import is idempotent upsert: `201 Created` on new row, `200 OK` on refresh of an existing row.
- Import endpoint path is exactly `POST /api/v1/catalog/sets/import`.
- `cacheStatus` values: `IMPORTED_FROM_REBRICKABLE` (create), `REFRESHED_FROM_REBRICKABLE` (refresh), `LOCAL_CACHE_HIT` (local read). Exact strings.
- `source` constant is `"REBRICKABLE"`.
- Theme `parent_theme_id` is left `null` — hierarchy is out of scope.
- Rebrickable set `404` maps to `ResourceNotFoundException` → HTTP `404`; blank `setNumber` → HTTP `400`.
- Do not modify already-applied Flyway migrations (`V1`, `V2`); add new migrations only.
- Existing tests are pure unit tests (Mockito) or `@WebMvcTest`; new logic tests follow that pattern. DB-backed verification runs through the existing `@SpringBootTest` (`ApiApplicationTests.contextLoads`), which requires local Postgres via `docker-compose up -d`.

---

### Task 1: Theme persistence foundation (migration, lifecycle, lookup)

Adds the DB constraint, entity lifecycle callbacks, and repository lookup that make Theme upsert possible. Verified by the existing Spring Boot context test, which applies Flyway migrations, validates the JPA schema mapping (`ddl-auto: validate`), and validates the derived query at startup.

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V3__add_unique_constraint_to_themes_external_id.sql`
- Modify: `apps/api/src/main/java/com/brickdeck/api/catalog/entity/Theme.java`
- Modify: `apps/api/src/main/java/com/brickdeck/api/catalog/repository/ThemeRepository.java`
- Test (existing, run only): `apps/api/src/test/java/com/brickdeck/api/ApiApplicationTests.java`

**Interfaces:**
- Consumes: nothing (first task).
- Produces:
  - `themes.external_id` has a unique constraint `uq_themes_external_id`.
  - `Theme` populates `id`, `createdAt`, `updatedAt` on persist and `updatedAt` on update.
  - `ThemeRepository.findByExternalId(String externalId) -> Optional<Theme>`.

- [ ] **Step 1: Write the migration**

Create `apps/api/src/main/resources/db/migration/V3__add_unique_constraint_to_themes_external_id.sql`:

```sql
ALTER TABLE themes
    ADD CONSTRAINT uq_themes_external_id UNIQUE (external_id);
```

- [ ] **Step 2: Add lifecycle callbacks to `Theme`**

In `apps/api/src/main/java/com/brickdeck/api/catalog/entity/Theme.java`, add the import for `UUID` (already present) and add `@PrePersist`/`@PreUpdate` methods after the timestamp fields (mirroring `BrickSet`). Replace the closing brace region so the class ends with:

```java
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

(`jakarta.persistence.*` is already imported, so `@PrePersist`/`@PreUpdate` resolve.)

- [ ] **Step 3: Add the lookup to `ThemeRepository`**

Replace `apps/api/src/main/java/com/brickdeck/api/catalog/repository/ThemeRepository.java` with:

```java
package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {

    Optional<Theme> findByExternalId(String externalId);
}
```

- [ ] **Step 4: Start Postgres and run the context test**

Run: `docker compose up -d` (from repo root `/Users/jvilla/Documents/brickdeck`)
Then run: `cd apps/api && ./mvnw test -Dtest=ApiApplicationTests`
Expected: PASS — Flyway applies `V3`, Hibernate validates the schema (including the `themes` mapping), and Spring Data validates the `findByExternalId` derivation at startup.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/resources/db/migration/V3__add_unique_constraint_to_themes_external_id.sql \
        apps/api/src/main/java/com/brickdeck/api/catalog/entity/Theme.java \
        apps/api/src/main/java/com/brickdeck/api/catalog/repository/ThemeRepository.java
git commit -m "feat(catalog): add theme upsert persistence foundation"
```

---

### Task 2: Rebrickable theme client

Adds the external DTO and client method to fetch a theme by id from Rebrickable.

**Files:**
- Create: `apps/api/src/main/java/com/brickdeck/api/external/rebrickable/dto/RebrickableThemeResponse.java`
- Modify: `apps/api/src/main/java/com/brickdeck/api/external/rebrickable/client/RebrickableClient.java`
- Create: `apps/api/src/test/java/com/brickdeck/api/external/rebrickable/client/RebrickableClientTest.java`

**Interfaces:**
- Consumes: nothing from prior tasks.
- Produces:
  - `RebrickableThemeResponse(Integer id, String name, Integer parentId)` — JSON `parent_id` maps to `parentId`.
  - `RebrickableClient.getThemeById(Integer themeId) -> RebrickableThemeResponse` — calls `GET /lego/themes/{themeId}/`.

- [ ] **Step 1: Write the failing client test**

Create `apps/api/src/test/java/com/brickdeck/api/external/rebrickable/client/RebrickableClientTest.java`:

```java
package com.brickdeck.api.external.rebrickable.client;

import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class RebrickableClientTest {

    @Test
    void getThemeByIdReturnsMappedTheme() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RebrickableClient client = new RebrickableClient(builder.build());

        server.expect(requestTo("/lego/themes/158/"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"id\":158,\"name\":\"Star Wars\",\"parent_id\":null}",
                        MediaType.APPLICATION_JSON));

        RebrickableThemeResponse response = client.getThemeById(158);

        assertThat(response.id()).isEqualTo(158);
        assertThat(response.name()).isEqualTo("Star Wars");
        assertThat(response.parentId()).isNull();
        server.verify();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd apps/api && ./mvnw test -Dtest=RebrickableClientTest`
Expected: FAIL — compilation error, `RebrickableThemeResponse` and `getThemeById` do not exist.

- [ ] **Step 3: Create the DTO**

Create `apps/api/src/main/java/com/brickdeck/api/external/rebrickable/dto/RebrickableThemeResponse.java`:

```java
package com.brickdeck.api.external.rebrickable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RebrickableThemeResponse(
        Integer id,

        String name,

        @JsonProperty("parent_id")
        Integer parentId
) {
}
```

- [ ] **Step 4: Add `getThemeById` to the client**

In `apps/api/src/main/java/com/brickdeck/api/external/rebrickable/client/RebrickableClient.java`, add the import `import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;` alongside the existing DTO imports, and add this method after `getSetByNumber`:

```java
    public RebrickableThemeResponse getThemeById(Integer themeId) {
        return rebrickableRestClient
                .get()
                .uri("/lego/themes/{themeId}/", themeId)
                .retrieve()
                .body(RebrickableThemeResponse.class);
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd apps/api && ./mvnw test -Dtest=RebrickableClientTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/java/com/brickdeck/api/external/rebrickable/dto/RebrickableThemeResponse.java \
        apps/api/src/main/java/com/brickdeck/api/external/rebrickable/client/RebrickableClient.java \
        apps/api/src/test/java/com/brickdeck/api/external/rebrickable/client/RebrickableClientTest.java
git commit -m "feat(catalog): add Rebrickable theme fetch client method"
```

---

### Task 3: Theme resolution service

Adds `ThemeService.resolveByExternalId`: fetch a theme from Rebrickable and upsert the local Theme row, returning the managed entity (or `null` when no external theme id).

**Files:**
- Modify: `apps/api/src/main/java/com/brickdeck/api/catalog/service/ThemeService.java`
- Modify: `apps/api/src/test/java/com/brickdeck/api/catalog/service/ThemeServiceTest.java`

**Interfaces:**
- Consumes: `ThemeRepository.findByExternalId` (Task 1), `RebrickableClient.getThemeById` + `RebrickableThemeResponse` (Task 2).
- Produces: `ThemeService.resolveByExternalId(Integer externalThemeId) -> Theme` (returns `null` when `externalThemeId` is `null`; otherwise a saved managed `Theme`).

- [ ] **Step 1: Write the failing tests**

Replace `apps/api/src/test/java/com/brickdeck/api/catalog/service/ThemeServiceTest.java` with:

```java
package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private RebrickableClient rebrickableClient;

    @InjectMocks
    private ThemeService themeService;

    @Test
    void returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(id);
        theme.setExternalId("158");
        theme.setName("Star Wars");
        theme.setParentThemeId(parentId);

        when(themeRepository.findById(id)).thenReturn(Optional.of(theme));

        ThemeResponse response = themeService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.externalId()).isEqualTo("158");
        assertThat(response.name()).isEqualTo("Star Wars");
        assertThat(response.parentThemeId()).isEqualTo(parentId);
    }

    @Test
    void throwsWhenThemeNotFound() {
        UUID id = UUID.randomUUID();
        when(themeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> themeService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void resolveByExternalIdReturnsNullWhenExternalIdIsNull() {
        Theme result = themeService.resolveByExternalId(null);

        assertThat(result).isNull();
        verifyNoInteractions(rebrickableClient);
        verify(themeRepository, never()).save(any(Theme.class));
    }

    @Test
    void resolveByExternalIdCreatesNewThemeWhenAbsent() {
        when(rebrickableClient.getThemeById(158))
                .thenReturn(new RebrickableThemeResponse(158, "Star Wars", null));
        when(themeRepository.findByExternalId("158")).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenAnswer(inv -> inv.getArgument(0));

        Theme result = themeService.resolveByExternalId(158);

        assertThat(result.getExternalId()).isEqualTo("158");
        assertThat(result.getName()).isEqualTo("Star Wars");

        ArgumentCaptor<Theme> captor = ArgumentCaptor.forClass(Theme.class);
        verify(themeRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("158");
        assertThat(captor.getValue().getName()).isEqualTo("Star Wars");
    }

    @Test
    void resolveByExternalIdReusesExistingThemeAndUpdatesName() {
        UUID existingId = UUID.randomUUID();
        Theme existing = new Theme();
        existing.setId(existingId);
        existing.setExternalId("158");
        existing.setName("Old Name");

        when(rebrickableClient.getThemeById(158))
                .thenReturn(new RebrickableThemeResponse(158, "Star Wars", null));
        when(themeRepository.findByExternalId("158")).thenReturn(Optional.of(existing));
        when(themeRepository.save(any(Theme.class))).thenAnswer(inv -> inv.getArgument(0));

        Theme result = themeService.resolveByExternalId(158);

        assertThat(result.getId()).isEqualTo(existingId);
        assertThat(result.getName()).isEqualTo("Star Wars");
        verify(themeRepository).save(existing);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd apps/api && ./mvnw test -Dtest=ThemeServiceTest`
Expected: FAIL — compilation error, `resolveByExternalId` does not exist and the constructor does not take a `RebrickableClient`.

- [ ] **Step 3: Implement `resolveByExternalId`**

Replace `apps/api/src/main/java/com/brickdeck/api/catalog/service/ThemeService.java` with:

```java
package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.ThemeRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final RebrickableClient rebrickableClient;

    public ThemeService(ThemeRepository themeRepository, RebrickableClient rebrickableClient) {
        this.themeRepository = themeRepository;
        this.rebrickableClient = rebrickableClient;
    }

    @Transactional(readOnly = true)
    public ThemeResponse getById(UUID id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Theme not found: " + id));
        return toResponse(theme);
    }

    @Transactional
    public Theme resolveByExternalId(Integer externalThemeId) {
        if (externalThemeId == null) {
            return null;
        }

        String externalId = String.valueOf(externalThemeId);
        RebrickableThemeResponse external = rebrickableClient.getThemeById(externalThemeId);

        Theme theme = themeRepository.findByExternalId(externalId)
                .orElseGet(Theme::new);
        theme.setExternalId(externalId);
        theme.setName(external.name());

        return themeRepository.save(theme);
    }

    private ThemeResponse toResponse(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getExternalId(),
                theme.getName(),
                theme.getParentThemeId()
        );
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd apps/api && ./mvnw test -Dtest=ThemeServiceTest`
Expected: PASS — all five tests green.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/java/com/brickdeck/api/catalog/service/ThemeService.java \
        apps/api/src/test/java/com/brickdeck/api/catalog/service/ThemeServiceTest.java
git commit -m "feat(catalog): add theme resolution and upsert service"
```

---

### Task 4: Set import service and read-only lookup

Adds `BrickSetService.importSet` (upsert + theme link + 404 mapping) and the `ImportResult` DTO, and refactors `findOrImportBySetNumber` into a read-only `findBySetNumber`.

**Files:**
- Create: `apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportResult.java`
- Modify: `apps/api/src/main/java/com/brickdeck/api/catalog/service/BrickSetService.java`
- Modify: `apps/api/src/test/java/com/brickdeck/api/catalog/service/BrickSetServiceTest.java`

**Interfaces:**
- Consumes: `ThemeService.resolveByExternalId` (Task 3).
- Produces:
  - `ImportResult(boolean created, BrickSetResponse body)`.
  - `BrickSetService.importSet(String setNumber) -> ImportResult` (upsert; `created=true` → `IMPORTED_FROM_REBRICKABLE`/`201`; `created=false` → `REFRESHED_FROM_REBRICKABLE`/`200`; Rebrickable `404` → `ResourceNotFoundException`).
  - `BrickSetService.findBySetNumber(String setNumber) -> BrickSetResponse` (local only; `ResourceNotFoundException` on miss; no Rebrickable call).
  - `findOrImportBySetNumber` no longer exists.

- [ ] **Step 1: Create the `ImportResult` DTO**

Create `apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportResult.java`:

```java
package com.brickdeck.api.catalog.dto;

public record ImportResult(boolean created, BrickSetResponse body) {
}
```

- [ ] **Step 2: Write the failing service tests**

Replace `apps/api/src/test/java/com/brickdeck/api/catalog/service/BrickSetServiceTest.java` with:

```java
package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

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
class BrickSetServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;

    @Mock
    private RebrickableClient rebrickableClient;

    @Mock
    private ThemeService themeService;

    @InjectMocks
    private BrickSetService brickSetService;

    private RebrickableSetResponse falconExternal() {
        return new RebrickableSetResponse(
                "75375-1",
                "Millennium Falcon",
                2024,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "2024-01-30T08:35:07.710189Z"
        );
    }

    @Test
    void returnsAllSetsFromLocalCatalog() {
        UUID setId = UUID.randomUUID();

        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75375-1");
        set.setName("Millennium Falcon");
        set.setSource("REBRICKABLE");

        when(brickSetRepository.findAll()).thenReturn(List.of(set));

        List<BrickSetResponse> responses = brickSetService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(setId);
        assertThat(responses.get(0).cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");
    }

    @Test
    void findBySetNumberReturnsCachedSet() {
        UUID themeId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(themeId);
        theme.setName("Star Wars");

        UUID setId = UUID.randomUUID();
        BrickSet set = new BrickSet();
        set.setId(setId);
        set.setExternalSetNumber("75257-1");
        set.setName("Millennium Falcon");
        set.setSource("REBRICKABLE");
        set.setTheme(theme);

        when(brickSetRepository.findByExternalSetNumber("75257-1"))
                .thenReturn(Optional.of(set));

        BrickSetResponse response = brickSetService.findBySetNumber("75257-1");

        assertThat(response.id()).isEqualTo(setId);
        assertThat(response.themeId()).isEqualTo(themeId);
        assertThat(response.themeName()).isEqualTo("Star Wars");
        assertThat(response.cacheStatus()).isEqualTo("LOCAL_CACHE_HIT");

        verify(rebrickableClient, never()).getSetByNumber("75257-1");
    }

    @Test
    void findBySetNumberThrowsWhenMissing() {
        when(brickSetRepository.findByExternalSetNumber("00000-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> brickSetService.findBySetNumber("00000-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("00000-1");

        verify(rebrickableClient, never()).getSetByNumber("00000-1");
    }

    @Test
    void importSetCreatesNewSetAndLinksTheme() {
        UUID savedId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        Theme theme = new Theme();
        theme.setId(themeId);
        theme.setName("Star Wars");

        when(brickSetRepository.findByExternalSetNumber("75375-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("75375-1")).thenReturn(falconExternal());
        when(themeService.resolveByExternalId(158)).thenReturn(theme);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> {
            BrickSet toSave = inv.getArgument(0);
            toSave.setId(savedId);
            return toSave;
        });

        ImportResult result = brickSetService.importSet("75375-1");

        assertThat(result.created()).isTrue();
        assertThat(result.body().id()).isEqualTo(savedId);
        assertThat(result.body().externalSetNumber()).isEqualTo("75375-1");
        assertThat(result.body().themeId()).isEqualTo(themeId);
        assertThat(result.body().themeName()).isEqualTo("Star Wars");
        assertThat(result.body().externalThemeId()).isEqualTo(158);
        assertThat(result.body().cacheStatus()).isEqualTo("IMPORTED_FROM_REBRICKABLE");

        ArgumentCaptor<BrickSet> captor = ArgumentCaptor.forClass(BrickSet.class);
        verify(brickSetRepository).save(captor.capture());
        BrickSet saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Millennium Falcon");
        assertThat(saved.getExternalLastModifiedAt()).isNotNull();
        assertThat(saved.getTheme()).isSameAs(theme);
        assertThat(saved.getSource()).isEqualTo("REBRICKABLE");
    }

    @Test
    void importSetRefreshesExistingSet() {
        UUID existingId = UUID.randomUUID();
        BrickSet existing = new BrickSet();
        existing.setId(existingId);
        existing.setExternalSetNumber("75375-1");
        existing.setName("Stale Name");
        existing.setSource("REBRICKABLE");

        when(brickSetRepository.findByExternalSetNumber("75375-1"))
                .thenReturn(Optional.of(existing));
        when(rebrickableClient.getSetByNumber("75375-1")).thenReturn(falconExternal());
        when(themeService.resolveByExternalId(158)).thenReturn(null);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = brickSetService.importSet("75375-1");

        assertThat(result.created()).isFalse();
        assertThat(result.body().id()).isEqualTo(existingId);
        assertThat(result.body().name()).isEqualTo("Millennium Falcon");
        assertThat(result.body().cacheStatus()).isEqualTo("REFRESHED_FROM_REBRICKABLE");
        verify(brickSetRepository).save(existing);
    }

    @Test
    void importSetWithNullThemeDoesNotLinkTheme() {
        RebrickableSetResponse themeless = new RebrickableSetResponse(
                "10001-1", "Themeless", 1999, null, 10,
                null, null, null);

        when(brickSetRepository.findByExternalSetNumber("10001-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("10001-1")).thenReturn(themeless);
        when(themeService.resolveByExternalId(null)).thenReturn(null);
        when(brickSetRepository.save(any(BrickSet.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = brickSetService.importSet("10001-1");

        assertThat(result.created()).isTrue();
        assertThat(result.body().themeId()).isNull();
        assertThat(result.body().themeName()).isNull();
        assertThat(result.body().externalThemeId()).isNull();
    }

    @Test
    void importSetMapsRebrickableNotFoundToResourceNotFound() {
        when(brickSetRepository.findByExternalSetNumber("99999-1"))
                .thenReturn(Optional.empty());
        when(rebrickableClient.getSetByNumber("99999-1"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> brickSetService.importSet("99999-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99999-1");

        verify(brickSetRepository, never()).save(any(BrickSet.class));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd apps/api && ./mvnw test -Dtest=BrickSetServiceTest`
Expected: FAIL — compilation error, `importSet`/`findBySetNumber`/`ImportResult` and the `ThemeService` constructor dependency do not exist yet.

- [ ] **Step 4: Implement the service**

Replace `apps/api/src/main/java/com/brickdeck/api/catalog/service/BrickSetService.java` with:

```java
package com.brickdeck.api.catalog.service;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Theme;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BrickSetService {

    private static final String SOURCE_REBRICKABLE = "REBRICKABLE";
    private static final String STATUS_LOCAL_CACHE_HIT = "LOCAL_CACHE_HIT";
    private static final String STATUS_IMPORTED = "IMPORTED_FROM_REBRICKABLE";
    private static final String STATUS_REFRESHED = "REFRESHED_FROM_REBRICKABLE";

    private final BrickSetRepository brickSetRepository;
    private final RebrickableClient rebrickableClient;
    private final ThemeService themeService;

    public BrickSetService(
            BrickSetRepository brickSetRepository,
            RebrickableClient rebrickableClient,
            ThemeService themeService
    ) {
        this.brickSetRepository = brickSetRepository;
        this.rebrickableClient = rebrickableClient;
        this.themeService = themeService;
    }

    @Transactional(readOnly = true)
    public List<BrickSetResponse> findAll() {
        return brickSetRepository.findAll()
                .stream()
                .map(brickSet -> toResponse(brickSet, STATUS_LOCAL_CACHE_HIT))
                .toList();
    }

    @Transactional(readOnly = true)
    public BrickSetResponse findBySetNumber(String setNumber) {
        return brickSetRepository.findByExternalSetNumber(setNumber)
                .map(brickSet -> toResponse(brickSet, STATUS_LOCAL_CACHE_HIT))
                .orElseThrow(() -> new ResourceNotFoundException("Set not found: " + setNumber));
    }

    @Transactional
    public ImportResult importSet(String setNumber) {
        RebrickableSetResponse external = fetchExternalSet(setNumber);
        Theme theme = themeService.resolveByExternalId(external.themeId());

        BrickSet brickSet = brickSetRepository.findByExternalSetNumber(external.setNum())
                .orElseGet(BrickSet::new);
        boolean created = brickSet.getId() == null;

        brickSet.setExternalSetNumber(external.setNum());
        brickSet.setName(external.name());
        brickSet.setYearReleased(external.year());
        brickSet.setExternalThemeId(external.themeId());
        brickSet.setNumberOfParts(external.numParts());
        brickSet.setImageUrl(external.setImgUrl());
        brickSet.setExternalUrl(external.setUrl());
        brickSet.setExternalLastModifiedAt(parseOffsetDateTime(external.lastModifiedDt()));
        brickSet.setTheme(theme);
        brickSet.setSource(SOURCE_REBRICKABLE);

        BrickSet saved = brickSetRepository.save(brickSet);
        String cacheStatus = created ? STATUS_IMPORTED : STATUS_REFRESHED;
        return new ImportResult(created, toResponse(saved, cacheStatus));
    }

    private RebrickableSetResponse fetchExternalSet(String setNumber) {
        try {
            return rebrickableClient.getSetByNumber(setNumber);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Set not found in Rebrickable: " + setNumber);
        }
    }

    private BrickSetResponse toResponse(BrickSet brickSet, String cacheStatus) {
        return new BrickSetResponse(
                brickSet.getId(),
                brickSet.getExternalSetNumber(),
                brickSet.getName(),
                brickSet.getYearReleased(),
                brickSet.getTheme() != null ? brickSet.getTheme().getId() : null,
                brickSet.getTheme() != null ? brickSet.getTheme().getName() : null,
                brickSet.getExternalThemeId(),
                brickSet.getNumberOfParts(),
                brickSet.getImageUrl(),
                brickSet.getExternalUrl(),
                brickSet.getSource(),
                cacheStatus
        );
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(value);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd apps/api && ./mvnw test -Dtest=BrickSetServiceTest`
Expected: PASS — all tests green.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportResult.java \
        apps/api/src/main/java/com/brickdeck/api/catalog/service/BrickSetService.java \
        apps/api/src/test/java/com/brickdeck/api/catalog/service/BrickSetServiceTest.java
git commit -m "feat(catalog): add set import upsert and read-only lookup"
```

---

### Task 5: Import controller and read endpoint wiring

Adds the request DTO and `POST /api/v1/catalog/sets/import` controller (201/200/400/404), and updates `BrickSetController` to call the renamed read-only method.

**Files:**
- Create: `apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportSetRequest.java`
- Create: `apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetImportController.java`
- Create: `apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetImportControllerTest.java`
- Modify: `apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetController.java`
- Modify: `apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetControllerTest.java`

**Interfaces:**
- Consumes: `BrickSetService.importSet` + `ImportResult` (Task 4), `BrickSetService.findBySetNumber` (Task 4), existing `BrickSetResponse`.
- Produces: `POST /api/v1/catalog/sets/import` accepting `{ "setNumber": "..." }`.

- [ ] **Step 1: Write the failing controller tests**

Create `apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetImportControllerTest.java`:

```java
package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrickSetImportController.class)
class BrickSetImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrickSetService brickSetService;

    private BrickSetResponse falconResponse(String cacheStatus) {
        return new BrickSetResponse(
                UUID.randomUUID(),
                "75375-1",
                "Millennium Falcon",
                2024,
                null,
                null,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "REBRICKABLE",
                cacheStatus
        );
    }

    @Test
    void importReturnsCreatedForNewSet() throws Exception {
        when(brickSetService.importSet("75375-1"))
                .thenReturn(new ImportResult(true, falconResponse("IMPORTED_FROM_REBRICKABLE")));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"75375-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalSetNumber").value("75375-1"))
                .andExpect(jsonPath("$.cacheStatus").value("IMPORTED_FROM_REBRICKABLE"));
    }

    @Test
    void importReturnsOkForExistingSet() throws Exception {
        when(brickSetService.importSet("75375-1"))
                .thenReturn(new ImportResult(false, falconResponse("REFRESHED_FROM_REBRICKABLE")));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"75375-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheStatus").value("REFRESHED_FROM_REBRICKABLE"));
    }

    @Test
    void importReturnsBadRequestWhenSetNumberBlank() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(brickSetService, never()).importSet(any());
    }

    @Test
    void importReturnsNotFoundWhenSetMissingInRebrickable() throws Exception {
        when(brickSetService.importSet("99999-1"))
                .thenThrow(new ResourceNotFoundException("Set not found in Rebrickable: 99999-1"));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"99999-1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not found in Rebrickable: 99999-1"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd apps/api && ./mvnw test -Dtest=BrickSetImportControllerTest`
Expected: FAIL — compilation error, `BrickSetImportController` and `ImportSetRequest` do not exist.

- [ ] **Step 3: Create the request DTO**

Create `apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportSetRequest.java`:

```java
package com.brickdeck.api.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportSetRequest(
        @NotBlank String setNumber
) {
}
```

- [ ] **Step 4: Create the import controller**

Create `apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetImportController.java`:

```java
package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.dto.ImportSetRequest;
import com.brickdeck.api.catalog.service.BrickSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/sets")
public class BrickSetImportController {

    private final BrickSetService brickSetService;

    public BrickSetImportController(BrickSetService brickSetService) {
        this.brickSetService = brickSetService;
    }

    @PostMapping("/import")
    public ResponseEntity<BrickSetResponse> importSet(@Valid @RequestBody ImportSetRequest request) {
        ImportResult result = brickSetService.importSet(request.setNumber());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.body());
    }
}
```

- [ ] **Step 5: Update `BrickSetController` to the read-only method**

In `apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetController.java`, replace the `findOrImportBySetNumber` handler so it calls the renamed service method:

```java
    @GetMapping("/by-number/{setNumber}")
    public BrickSetResponse findBySetNumber(@PathVariable String setNumber) {
        return brickSetService.findBySetNumber(setNumber);
    }
```

- [ ] **Step 6: Update `BrickSetControllerTest` for the read-only behavior**

In `apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetControllerTest.java`, replace the `returnsSetByNumberWithOk` test with the two tests below (add the imports `import com.brickdeck.api.common.ResourceNotFoundException;` and `static org.mockito.Mockito.when` is already imported):

```java
    @Test
    void returnsSetByNumberWithOk() throws Exception {
        UUID id = UUID.randomUUID();

        BrickSetResponse response = new BrickSetResponse(
                id,
                "75375-1",
                "Millennium Falcon",
                2024,
                null,
                null,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "REBRICKABLE",
                "LOCAL_CACHE_HIT"
        );

        when(brickSetService.findBySetNumber("75375-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/sets/by-number/{setNumber}", "75375-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalSetNumber").value("75375-1"))
                .andExpect(jsonPath("$.cacheStatus").value("LOCAL_CACHE_HIT"));
    }

    @Test
    void returnsNotFoundWhenSetMissing() throws Exception {
        when(brickSetService.findBySetNumber("00000-1"))
                .thenThrow(new ResourceNotFoundException("Set not found: 00000-1"));

        mockMvc.perform(get("/api/v1/sets/by-number/{setNumber}", "00000-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not found: 00000-1"));
    }
```

- [ ] **Step 7: Run the affected tests to verify they pass**

Run: `cd apps/api && ./mvnw test -Dtest=BrickSetImportControllerTest,BrickSetControllerTest`
Expected: PASS — 201/200/400/404 import cases and the read-only 200/404 cases all green.

- [ ] **Step 8: Run the full unit suite (excluding the DB context test) and commit**

Run: `cd apps/api && ./mvnw test -Dtest='!ApiApplicationTests'`
Expected: PASS — all service and controller unit tests green.

```bash
git add apps/api/src/main/java/com/brickdeck/api/catalog/dto/ImportSetRequest.java \
        apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetImportController.java \
        apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetImportControllerTest.java \
        apps/api/src/main/java/com/brickdeck/api/catalog/controller/BrickSetController.java \
        apps/api/src/test/java/com/brickdeck/api/catalog/controller/BrickSetControllerTest.java
git commit -m "feat(catalog): add set import endpoint and make read endpoint read-only"
```

---

### Final verification (whole feature)

- [ ] **Full build with the DB context test**

Run: `docker compose up -d` (repo root), then `cd apps/api && ./mvnw verify`
Expected: PASS — Flyway applies `V1`–`V3`, Hibernate validates the schema, and all unit + context tests pass.

- [ ] **Manual smoke (optional, requires `REBRICKABLE_API_KEY`)**

```bash
# import (201 first time, 200 second time)
curl -i -X POST localhost:8080/api/v1/catalog/sets/import \
     -H 'Content-Type: application/json' -d '{"setNumber":"75375-1"}'
# read-only lookup (200 now that it is local)
curl -i localhost:8080/api/v1/sets/by-number/75375-1
# unknown set (404)
curl -i -X POST localhost:8080/api/v1/catalog/sets/import \
     -H 'Content-Type: application/json' -d '{"setNumber":"00000-0"}'
```
