# Set Comparison Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend engine + public read endpoint that compares two catalog sets' part inventories and returns a quantity-weighted similarity score plus a paginated, categorized diff.

**Architecture:** New `com.brickdeck.api.comparison` package (DDD, mirrors `missingpieces`): `dto`, `service`, `controller`. A pure service transforms two sets' non-spare `SetPart` lists (fetched via the existing `SetPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse`) into a `SetComparisonReport`. No new repository. The compare endpoint is made public via `SecurityConfig`.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Data JPA, JUnit 5, Mockito, MockMvc, Testcontainers/real Postgres.

## Global Constraints

- Java 21; base package `com.brickdeck.api`.
- Never return JPA entities from controllers — map to DTO records.
- Use `@MockitoBean`, NOT `@MockBean` (Spring Boot 3.5.x).
- Throw `ResourceNotFoundException` (in `common`) for missing resources → 404 `{message}`.
- No special symbols in messages (write `degrees Celsius`, not the symbol).
- Compare is catalog-only and **public** (no Bearer) — the rest of `/api/v1/sets/**` is authenticated (`anyRequest().authenticated()`), so the compare path MUST be added to `SecurityConfig.PUBLIC_ENDPOINTS`.
- Spares excluded: use only `spare = false` lines.
- Similarity = `round( sum(min(qtyA,qtyB)) / sum(max(qtyA,qtyB)) , 2 )`, range 0.0..1.0.
- Line sort: category `BOTH` → `ONLY_A` → `ONLY_B`, then part number, then color external id.
- Full `@SpringBootTest` needs Postgres on `localhost:5433` (`nc -z -w2 localhost 5433`).

---

### Task 1: Comparison DTOs + engine service

**Files:**
- Create: `apps/api/src/main/java/com/brickdeck/api/comparison/dto/ComparisonCategory.java`
- Create: `apps/api/src/main/java/com/brickdeck/api/comparison/dto/SetComparisonLine.java`
- Create: `apps/api/src/main/java/com/brickdeck/api/comparison/dto/SetComparisonReport.java`
- Create: `apps/api/src/main/java/com/brickdeck/api/comparison/service/SetComparisonService.java`
- Test: `apps/api/src/test/java/com/brickdeck/api/comparison/service/SetComparisonServiceTest.java`

**Interfaces:**
- Consumes: `SetPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(String) : List<SetPart>` (already `@EntityGraph` part+color); `BrickSetRepository.findByExternalSetNumber(String) : Optional<BrickSet>`; `SetNumbers.normalize(String) : String`; entity accessors `SetPart.getPart()/getColor()/getQuantity()`, `Part.getId()/getExternalPartNumber()/getName()/getImageUrl()`, `Color.getId()/getExternalId()/getName()/getRgb()`.
- Produces: `SetComparisonService.compare(String setNumberA, String setNumberB, ComparisonCategory category, int page, int size) : SetComparisonReport`; records `SetComparisonReport`, `SetComparisonLine`; enum `ComparisonCategory { ONLY_A, ONLY_B, BOTH }`.

- [ ] **Step 1: Create the enum and DTO records**

`comparison/dto/ComparisonCategory.java`:
```java
package com.brickdeck.api.comparison.dto;

/** Where a part+color line appears across the two compared sets. */
public enum ComparisonCategory {
    ONLY_A,
    ONLY_B,
    BOTH
}
```

`comparison/dto/SetComparisonLine.java`:
```java
package com.brickdeck.api.comparison.dto;

/**
 * One part+color line of a set comparison: how many set A requires, how many
 * set B requires, the shared minimum, and which set(s) the line appears in.
 * Quantities count non-spare inventory only.
 */
public record SetComparisonLine(
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        int quantityA,
        int quantityB,
        int shared,
        ComparisonCategory category
) {
}
```

`comparison/dto/SetComparisonReport.java`:
```java
package com.brickdeck.api.comparison.dto;

import java.util.List;

/**
 * A comparison of two catalog sets' non-spare inventories. The similarity score
 * and the three whole-set line counts are aggregates independent of
 * filter/paging; {@code lines} is the current page after the optional category
 * filter.
 */
public record SetComparisonReport(
        String setNumberA,
        String setNumberB,
        double similarityScore,
        int sharedLineCount,
        int onlyALineCount,
        int onlyBLineCount,
        List<SetComparisonLine> lines,
        int page,
        int size,
        long totalLines,
        int totalPages,
        boolean first,
        boolean last
) {
}
```

- [ ] **Step 2: Write the failing service test**

`comparison/service/SetComparisonServiceTest.java`:
```java
package com.brickdeck.api.comparison.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetComparisonServiceTest {

    @Mock
    private BrickSetRepository brickSetRepository;
    @Mock
    private SetPartRepository setPartRepository;
    @InjectMocks
    private SetComparisonService service;

    private Color red;
    private Color blue;
    private Color white;
    private Part brickX;
    private Part plateY;
    private Part tileZ;

    @BeforeEach
    void setUp() {
        red = color(1, "Red");
        blue = color(2, "Blue");
        white = color(3, "White");
        brickX = part("X", "Brick X");
        plateY = part("Y", "Plate Y");
        tileZ = part("Z", "Tile Z");
    }

    @Test
    void computesWeightedSimilarityAndCategories() {
        // A: brickX/red x4, plateY/blue x2 ; B: brickX/red x2, tileZ/white x6
        stubSet("A-1", List.of(line("A-1", brickX, red, 4), line("A-1", plateY, blue, 2)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 2), line("B-1", tileZ, white, 6)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        // sum(min)=2, sum(max)=4+2+6=12 -> 0.17
        assertThat(report.similarityScore()).isEqualTo(0.17);
        assertThat(report.sharedLineCount()).isEqualTo(1);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(3);
        assertThat(report.lines()).hasSize(3);
        // BOTH sorts first
        SetComparisonLine firstLine = report.lines().get(0);
        assertThat(firstLine.category()).isEqualTo(ComparisonCategory.BOTH);
        assertThat(firstLine.partNumber()).isEqualTo("X");
        assertThat(firstLine.quantityA()).isEqualTo(4);
        assertThat(firstLine.quantityB()).isEqualTo(2);
        assertThat(firstLine.shared()).isEqualTo(2);
    }

    @Test
    void identicalInventoriesScoreOne() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 3)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 3)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        assertThat(report.similarityScore()).isEqualTo(1.0);
        assertThat(report.sharedLineCount()).isEqualTo(1);
    }

    @Test
    void disjointInventoriesScoreZero() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 3)));
        stubSet("B-1", List.of(line("B-1", tileZ, white, 3)));

        SetComparisonReport report = service.compare("A-1", "B-1", null, 0, 50);

        assertThat(report.similarityScore()).isEqualTo(0.0);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
    }

    @Test
    void categoryFilterKeepsSummaryButFiltersLines() {
        stubSet("A-1", List.of(line("A-1", brickX, red, 4), line("A-1", plateY, blue, 2)));
        stubSet("B-1", List.of(line("B-1", brickX, red, 2), line("B-1", tileZ, white, 6)));

        SetComparisonReport report =
                service.compare("A-1", "B-1", ComparisonCategory.ONLY_A, 0, 50);

        assertThat(report.onlyALineCount()).isEqualTo(1); // summary unchanged
        assertThat(report.totalLines()).isEqualTo(1);      // filtered lines
        assertThat(report.lines()).hasSize(1);
        assertThat(report.lines().get(0).category()).isEqualTo(ComparisonCategory.ONLY_A);
        assertThat(report.lines().get(0).partNumber()).isEqualTo("Y");
    }

    @Test
    void normalizesBareSetNumbers() {
        stubSet("42232-1", List.of(line("42232-1", brickX, red, 1)));
        stubSet("10497-1", List.of(line("10497-1", brickX, red, 1)));

        SetComparisonReport report = service.compare("42232", "10497", null, 0, 50);

        assertThat(report.setNumberA()).isEqualTo("42232-1");
        assertThat(report.setNumberB()).isEqualTo("10497-1");
    }

    @Test
    void throwsWhenSetNotImported() {
        when(brickSetRepository.findByExternalSetNumber("A-1"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.compare("A-1", "B-1", null, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("A-1");
    }

    @Test
    void throwsWhenInventoryNotImported() {
        when(brickSetRepository.findByExternalSetNumber("A-1"))
                .thenReturn(java.util.Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse("A-1"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.compare("A-1", "B-1", null, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("inventory");
    }

    private void stubSet(String number, List<SetPart> parts) {
        when(brickSetRepository.findByExternalSetNumber(number))
                .thenReturn(java.util.Optional.of(new BrickSet()));
        when(setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(number))
                .thenReturn(parts);
    }

    private Color color(int externalId, String name) {
        Color c = new Color();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setName(name);
        c.setRgb("000000");
        return c;
    }

    private Part part(String number, String name) {
        Part p = new Part();
        p.setId(UUID.randomUUID());
        p.setExternalPartNumber(number);
        p.setName(name);
        return p;
    }

    private SetPart line(String setNumber, Part part, Color color, int quantity) {
        SetPart sp = new SetPart();
        sp.setPart(part);
        sp.setColor(color);
        sp.setQuantity(quantity);
        sp.setSpare(false);
        return sp;
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd apps/api && ./mvnw -Dtest=SetComparisonServiceTest test 2>&1 | grep -E "Tests run:|BUILD|ERROR"`
Expected: FAIL — `SetComparisonService` does not exist (compilation error).

- [ ] **Step 4: Implement the service**

`comparison/service/SetComparisonService.java`:
```java
package com.brickdeck.api.comparison.service;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SetComparisonService {

    private final BrickSetRepository brickSetRepository;
    private final SetPartRepository setPartRepository;

    public SetComparisonService(BrickSetRepository brickSetRepository,
                                SetPartRepository setPartRepository) {
        this.brickSetRepository = brickSetRepository;
        this.setPartRepository = setPartRepository;
    }

    /** Lines ordered BOTH first, then ONLY_A, then ONLY_B, then part, then color. */
    private static final Comparator<SetComparisonLine> LINE_ORDER =
            Comparator.comparingInt((SetComparisonLine l) -> rank(l.category()))
                    .thenComparing(l -> l.partNumber() == null ? "" : l.partNumber())
                    .thenComparing(l -> l.colorExternalId() == null
                            ? Integer.MIN_VALUE : l.colorExternalId());

    private static int rank(ComparisonCategory category) {
        return switch (category) {
            case BOTH -> 0;
            case ONLY_A -> 1;
            case ONLY_B -> 2;
        };
    }

    /**
     * Compare two catalog sets' non-spare inventories. Quantities are summed per
     * part+color; the similarity score is sum(min)/sum(max) over the union of
     * keys. Line counts are whole-set; returned lines are optionally filtered by
     * category and paginated.
     */
    @Transactional(readOnly = true)
    public SetComparisonReport compare(String setNumberA, String setNumberB,
                                       ComparisonCategory category, int page, int size) {
        String a = SetNumbers.normalize(setNumberA);
        String b = SetNumbers.normalize(setNumberB);

        Map<PartColorKey, Integer> qtyA = sumByKey(requireInventory(a));
        List<SetPart> partsB = requireInventory(b);
        Map<PartColorKey, Integer> qtyB = sumByKey(partsB);

        Map<PartColorKey, SetPart> repr = new HashMap<>();
        for (SetPart sp : requireInventory(a)) {
            repr.putIfAbsent(key(sp), sp);
        }
        for (SetPart sp : partsB) {
            repr.putIfAbsent(key(sp), sp);
        }

        Set<PartColorKey> keys = new HashSet<>();
        keys.addAll(qtyA.keySet());
        keys.addAll(qtyB.keySet());

        long sumMin = 0;
        long sumMax = 0;
        int sharedCount = 0;
        int onlyACount = 0;
        int onlyBCount = 0;
        List<SetComparisonLine> all = new ArrayList<>(keys.size());

        for (PartColorKey k : keys) {
            int va = qtyA.getOrDefault(k, 0);
            int vb = qtyB.getOrDefault(k, 0);
            sumMin += Math.min(va, vb);
            sumMax += Math.max(va, vb);

            ComparisonCategory cat = va > 0 && vb > 0
                    ? ComparisonCategory.BOTH
                    : va > 0 ? ComparisonCategory.ONLY_A : ComparisonCategory.ONLY_B;
            switch (cat) {
                case BOTH -> sharedCount++;
                case ONLY_A -> onlyACount++;
                case ONLY_B -> onlyBCount++;
            }

            SetPart r = repr.get(k);
            Part part = r.getPart();
            Color color = r.getColor();
            all.add(new SetComparisonLine(
                    part.getExternalPartNumber(),
                    part.getName(),
                    part.getImageUrl(),
                    color.getExternalId(),
                    color.getName(),
                    color.getRgb(),
                    va,
                    vb,
                    Math.min(va, vb),
                    cat));
        }

        double similarity = sumMax == 0 ? 0.0 : Math.round(sumMin * 100.0 / sumMax) / 100.0;

        List<SetComparisonLine> filtered = all.stream()
                .filter(l -> category == null || l.category() == category)
                .sorted(LINE_ORDER)
                .toList();

        int safeSize = size <= 0 ? 50 : size;
        int safePage = Math.max(0, page);
        long totalLines = filtered.size();
        int totalPages = (int) Math.ceil((double) totalLines / safeSize);

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<SetComparisonLine> pageLines = List.copyOf(filtered.subList(from, to));

        boolean first = safePage == 0;
        boolean last = to >= filtered.size();

        return new SetComparisonReport(
                a, b, similarity, sharedCount, onlyACount, onlyBCount,
                pageLines, safePage, safeSize, totalLines, totalPages, first, last);
    }

    private List<SetPart> requireInventory(String normalized) {
        brickSetRepository.findByExternalSetNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Set not imported: " + normalized + " (import the set first)"));
        List<SetPart> parts =
                setPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(normalized);
        if (parts.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Set inventory not imported: " + normalized
                            + " (import the inventory first)");
        }
        return parts;
    }

    private static Map<PartColorKey, Integer> sumByKey(List<SetPart> parts) {
        Map<PartColorKey, Integer> m = new HashMap<>();
        for (SetPart sp : parts) {
            m.merge(key(sp), sp.getQuantity(), Integer::sum);
        }
        return m;
    }

    private static PartColorKey key(SetPart sp) {
        return new PartColorKey(sp.getPart().getId(), sp.getColor().getId());
    }

    private record PartColorKey(UUID partId, UUID colorId) {
    }
}
```

Note: `requireInventory(a)` is intentionally called again to build `repr` from A's lines; the repeated call keeps the method simple and A's inventory is small. If a reviewer objects, hoist A's list into a local like `partsB` — behavior is identical.

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd apps/api && ./mvnw -Dtest=SetComparisonServiceTest test 2>&1 | grep -E "Tests run:|BUILD"`
Expected: PASS — `Tests run: 7, Failures: 0` and `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/jvilla/Documents/brickdeck
git add apps/api/src/main/java/com/brickdeck/api/comparison apps/api/src/test/java/com/brickdeck/api/comparison
git commit -m "feat(comparison): add set comparison engine service and DTOs

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_014QTrsTT5WzBK8SdyZouiXZ"
```

---

### Task 2: Public compare endpoint + security permit

**Files:**
- Create: `apps/api/src/main/java/com/brickdeck/api/comparison/controller/SetComparisonController.java`
- Modify: `apps/api/src/main/java/com/brickdeck/api/security/config/SecurityConfig.java` (add `/api/v1/sets/compare` to `PUBLIC_ENDPOINTS`)
- Test: `apps/api/src/test/java/com/brickdeck/api/comparison/controller/SetComparisonControllerTest.java`

**Interfaces:**
- Consumes: `SetComparisonService.compare(String, String, ComparisonCategory, int, int) : SetComparisonReport`.
- Produces: `GET /api/v1/sets/compare?a=&b=&category=&page=&size=` → `SetComparisonReport` JSON.

- [ ] **Step 1: Write the failing controller test**

`comparison/controller/SetComparisonControllerTest.java`:
```java
package com.brickdeck.api.comparison.controller;

import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class SetComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetComparisonService setComparisonService;

    @Test
    void returnsComparisonReport() throws Exception {
        SetComparisonReport report = new SetComparisonReport(
                "A-1", "B-1", 0.17, 1, 1, 1,
                List.of(new SetComparisonLine(
                        "X", "Brick X", null, 1, "Red", "B40000", 4, 2, 2,
                        ComparisonCategory.BOTH)),
                0, 50, 3L, 1, true, true);
        when(setComparisonService.compare(eq("A-1"), eq("B-1"), isNull(), eq(0), eq(50)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/compare").param("a", "A-1").param("b", "B-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumberA").value("A-1"))
                .andExpect(jsonPath("$.setNumberB").value("B-1"))
                .andExpect(jsonPath("$.similarityScore").value(0.17))
                .andExpect(jsonPath("$.sharedLineCount").value(1))
                .andExpect(jsonPath("$.lines[0].partNumber").value("X"))
                .andExpect(jsonPath("$.lines[0].category").value("BOTH"));
    }

    @Test
    void forwardsCategoryAndPaginationParams() throws Exception {
        SetComparisonReport report = new SetComparisonReport(
                "A-1", "B-1", 0.0, 0, 1, 0, List.of(), 2, 10, 1L, 1, false, true);
        when(setComparisonService.compare(
                eq("A-1"), eq("B-1"), eq(ComparisonCategory.ONLY_A), eq(2), eq(10)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/compare")
                        .param("a", "A-1").param("b", "B-1")
                        .param("category", "ONLY_A")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(setComparisonService).compare(
                eq("A-1"), eq("B-1"), eq(ComparisonCategory.ONLY_A), eq(2), eq(10));
    }

    @Test
    void returns404WhenSetOrInventoryMissing() throws Exception {
        when(setComparisonService.compare(eq("A-1"), eq("B-1"), isNull(), eq(0), eq(50)))
                .thenThrow(new ResourceNotFoundException("Set not imported: A-1"));

        mockMvc.perform(get("/api/v1/sets/compare").param("a", "A-1").param("b", "B-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not imported: A-1"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd apps/api && ./mvnw -Dtest=SetComparisonControllerTest test 2>&1 | grep -E "Tests run:|BUILD|ERROR"`
Expected: FAIL — `SetComparisonController` does not exist (compilation error).

- [ ] **Step 3: Implement the controller**

`comparison/controller/SetComparisonController.java`:
```java
package com.brickdeck.api.comparison.controller;

import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sets")
public class SetComparisonController {

    private final SetComparisonService setComparisonService;

    public SetComparisonController(SetComparisonService setComparisonService) {
        this.setComparisonService = setComparisonService;
    }

    @GetMapping("/compare")
    public SetComparisonReport compare(
            @RequestParam("a") String a,
            @RequestParam("b") String b,
            @RequestParam(name = "category", required = false) ComparisonCategory category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return setComparisonService.compare(a, b, category, page, size);
    }
}
```

- [ ] **Step 4: Add the compare path to `PUBLIC_ENDPOINTS`**

In `security/config/SecurityConfig.java`, extend the `PUBLIC_ENDPOINTS` array:
```java
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/sets/compare",
            "/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd apps/api && ./mvnw -Dtest=SetComparisonControllerTest test 2>&1 | grep -E "Tests run:|BUILD"`
Expected: PASS — `Tests run: 3, Failures: 0` and `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/jvilla/Documents/brickdeck
git add apps/api/src/main/java/com/brickdeck/api/comparison/controller apps/api/src/main/java/com/brickdeck/api/security/config/SecurityConfig.java apps/api/src/test/java/com/brickdeck/api/comparison/controller
git commit -m "feat(comparison): expose public GET /api/v1/sets/compare endpoint

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_014QTrsTT5WzBK8SdyZouiXZ"
```

---

### Task 3: Integration test (real database + public access)

**Files:**
- Test: `apps/api/src/test/java/com/brickdeck/api/comparison/SetComparisonIntegrationTest.java`

**Interfaces:**
- Consumes: `SetComparisonService.compare(...)`, `MockMvc` GET on `/api/v1/sets/compare`, catalog repositories for seeding.
- Produces: nothing (verification only).

- [ ] **Step 1: Confirm Postgres is up**

Run: `nc -z -w2 localhost 5433 && echo UP`
Expected: `UP`. If not, start it (`docker compose up -d` from repo root) before running the test.

- [ ] **Step 2: Write the failing integration test**

`comparison/SetComparisonIntegrationTest.java`:
```java
package com.brickdeck.api.comparison;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.catalog.repository.SetPartRepository;
import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SetComparisonIntegrationTest {

    @Autowired
    private SetComparisonService service;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private ColorRepository colorRepository;
    @Autowired
    private SetPartRepository setPartRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void comparesTwoSetsExcludingSparesAgainstRealDatabase() {
        BrickSet setA = brickSet("IT-CMP-A-" + suffix);
        BrickSet setB = brickSet("IT-CMP-B-" + suffix);
        Part brickX = part("IT-CMP-X-" + suffix, "Brick X");
        Part plateY = part("IT-CMP-Y-" + suffix, "Plate Y");
        Part tileZ = part("IT-CMP-Z-" + suffix, "Tile Z");
        Color red = color("Red");
        Color blue = color("Blue");
        Color white = color("White");

        // A: brickX/red x4, plateY/blue x2, plus a spare brickX/red x9 (ignored).
        setPart(setA, brickX, red, 4, false);
        setPart(setA, plateY, blue, 2, false);
        setPart(setA, brickX, red, 9, true);
        // B: brickX/red x2, tileZ/white x6.
        setPart(setB, brickX, red, 2, false);
        setPart(setB, tileZ, white, 6, false);

        SetComparisonReport report = service.compare(
                setA.getExternalSetNumber(), setB.getExternalSetNumber(), null, 0, 50);

        // sum(min)=2, sum(max)=12 -> 0.17
        assertThat(report.similarityScore()).isEqualTo(0.17);
        assertThat(report.sharedLineCount()).isEqualTo(1);
        assertThat(report.onlyALineCount()).isEqualTo(1);
        assertThat(report.onlyBLineCount()).isEqualTo(1);
        assertThat(report.totalLines()).isEqualTo(3);

        var sharedLine = report.lines().stream()
                .filter(l -> l.category() == ComparisonCategory.BOTH)
                .findFirst().orElseThrow();
        assertThat(sharedLine.partNumber()).isEqualTo(brickX.getExternalPartNumber());
        assertThat(sharedLine.quantityA()).isEqualTo(4);
        assertThat(sharedLine.quantityB()).isEqualTo(2);
        assertThat(sharedLine.shared()).isEqualTo(2);
    }

    @Test
    void endpointIsReachableWithoutAuthentication() throws Exception {
        BrickSet setA = brickSet("IT-CMP-PUB-A-" + suffix);
        BrickSet setB = brickSet("IT-CMP-PUB-B-" + suffix);
        Part brickX = part("IT-CMP-PUB-X-" + suffix, "Brick X");
        Color red = color("Red");
        setPart(setA, brickX, red, 3, false);
        setPart(setB, brickX, red, 3, false);

        mockMvc.perform(get("/api/v1/sets/compare")
                        .param("a", setA.getExternalSetNumber())
                        .param("b", setB.getExternalSetNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarityScore").value(1.0));
    }

    private BrickSet brickSet(String number) {
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(number);
        set.setName("Set " + number);
        set.setSource("TEST");
        return brickSetRepository.saveAndFlush(set);
    }

    private Part part(String number, String name) {
        Part part = new Part();
        part.setExternalPartNumber(number);
        part.setName(name);
        part.setSource("TEST");
        return partRepository.saveAndFlush(part);
    }

    private Color color(String name) {
        Color color = new Color();
        color.setName(name);
        color.setSource("TEST");
        return colorRepository.saveAndFlush(color);
    }

    private void setPart(BrickSet set, Part part, Color color, int quantity, boolean spare) {
        SetPart line = new SetPart();
        line.setBrickSet(set);
        line.setPart(part);
        line.setColor(color);
        line.setQuantity(quantity);
        line.setSpare(spare);
        line.setSource("TEST");
        setPartRepository.saveAndFlush(line);
    }
}
```

Note: the `endpointIsReachableWithoutAuthentication` test performs the MockMvc GET with no `authentication(...)` post-processor. With `@AutoConfigureMockMvc` (default filters ON) it exercises the real `SecurityConfig`; a 200 proves the `PUBLIC_ENDPOINTS` permit from Task 2. `@Transactional` rolls the seeded rows back after each test.

- [ ] **Step 3: Run the test to verify it passes**

Run: `cd apps/api && ./mvnw -Dtest=SetComparisonIntegrationTest test 2>&1 | grep -E "Tests run:|BUILD"`
Expected: PASS — `Tests run: 2, Failures: 0` and `BUILD SUCCESS`.

- [ ] **Step 4: Run the full comparison suite**

Run: `cd apps/api && ./mvnw -Dtest='SetComparison*Test' test 2>&1 | grep -E "Tests run:|BUILD"`
Expected: `Tests run: 12` total across the three classes, `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
cd /Users/jvilla/Documents/brickdeck
git add apps/api/src/test/java/com/brickdeck/api/comparison/SetComparisonIntegrationTest.java
git commit -m "test(comparison): integration test for set comparison over real Postgres

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_014QTrsTT5WzBK8SdyZouiXZ"
```

---

### Task 4: Docs + verification sweep

**Files:**
- Modify: `.claude/project-state.md` (add comparison engine to Completed + Recently Worked On)
- Modify: `.claude/roadmap.md` (Phase 4 → In Progress, backend slice Done)
- Modify: `docs/product/roadmap.md` if it carries the same Phase 4 entry (check first)

- [ ] **Step 1: Run the full backend verify**

Run: `cd apps/api && ./mvnw clean verify 2>&1 | grep -E "Tests run:|BUILD"`
Expected: final aggregate `Tests run: N` (previous total + 12), `BUILD SUCCESS`.

- [ ] **Step 2: Update `.claude/roadmap.md`**

Under `## Phase 4 — Set Comparison Engine`, change `Status: Not Started` to `Status: In Progress (backend done)` and add:
```markdown
- Backend engine + endpoint — Done: `GET /api/v1/sets/compare?a=&b=&category=&page=&size=` (public) compares two catalog sets' non-spare inventories; returns quantity-weighted similarity score (`sum(min)/sum(max)`), per part+color diff lines (`quantityA`/`quantityB`/`shared`/`category` ONLY_A|ONLY_B|BOTH), whole-set line counts, and paginated lines with an optional category filter. 404 if either set or its inventory is not imported.
- Frontend compare page — Not started.
```

- [ ] **Step 3: Update `.claude/project-state.md`**

Add to the top of `## Recently Worked On`:
```markdown
- Phase 4 set comparison engine (backend, TDD): new `comparison` package — `GET /api/v1/sets/compare?a=&b=&category=&page=&size=` (public; added to `SecurityConfig.PUBLIC_ENDPOINTS`) → `SetComparisonReport`. `SetComparisonService.compare` fetches both sets' non-spare `set_parts` (reuses `SetPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse`, no new repo), sums per part+color, and returns quantity-weighted similarity (`sum(min)/sum(max)`, 2 dp), per-line `ComparisonCategory` (ONLY_A/ONLY_B/BOTH), whole-set counts, and paginated+category-filtered lines (sort BOTH→ONLY_A→ONLY_B, then part, then color). 404 if a set or its inventory is missing. Tests: service (Mockito, 7), controller (`@WebMvcTest` addFilters=false, 3), integration (`@SpringBootTest` real Postgres + anonymous MockMvc proving public access, 2). Spec: `docs/superpowers/specs/2026-07-13-set-comparison-engine-design.md`.
```

Also update `## Current Phase` to note Phase 4 backend slice complete, and change the Phase 4 line in `## Immediate Next Steps` to "Frontend compare page".

- [ ] **Step 4: Commit**

```bash
cd /Users/jvilla/Documents/brickdeck
git add .claude/project-state.md .claude/roadmap.md docs/product/roadmap.md
git commit -m "docs(comparison): record Phase 4 backend slice in state and roadmap

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_014QTrsTT5WzBK8SdyZouiXZ"
```

---

## Self-Review

**Spec coverage:** endpoint (Task 2), two-set operands + normalization (Task 1 service), quantity-weighted similarity (Task 1 tests + impl), paginated + category filter (Task 1), spares excluded (Task 1 + Task 3 seed), 404 on missing set/inventory (Task 1 tests + Task 2), public access (Task 2 permit + Task 3 anonymous MockMvc), DTO shapes (Task 1), sort order (Task 1 `LINE_ORDER`), divide-by-zero guard (Task 1 `sumMax == 0`). All covered.

**Placeholders:** none — every step carries complete code or an exact command.

**Type consistency:** `compare(String, String, ComparisonCategory, int, int)` used identically in service impl, service test, controller, and controller test. `SetComparisonReport`/`SetComparisonLine`/`ComparisonCategory` field names consistent across DTO definitions, JSON path assertions, and constructors. Enum constants `ONLY_A, ONLY_B, BOTH` consistent; sort rank handled explicitly in `rank(...)`.
