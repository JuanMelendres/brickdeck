# Wishlist Price Alerts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user set price-alert rules on wishlist sets and get in-app triggered alerts when a recorded price meets a rule, evaluated when a snapshot is added.

**Architecture:** Extend the `com.brickdeck.api.pricing` package. A pure `PriceAlertRuleEvaluator` decides if a rule fires; `PriceAlertService` owns rule CRUD (wishlist-guarded) and `evaluateForSnapshot`, which `PriceSnapshotService.addSnapshot` calls after saving. Two new tables (V9). Reuses `PriceAnalysisService` for average/lowest context.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL (localhost:5433), JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers-free `@SpringBootTest` against the shared DB.

## Global Constraints

- Package base `com.brickdeck.api`; feature lives in `pricing` (sub-concern of pricing).
- Catalog set table is `sets` (NOT `brick_sets`); price table is `price_snapshots`.
- `currency` columns are `VARCHAR(3)` (Hibernate rejects CHAR for `String`).
- Entities set timestamps via `@PrePersist`/`@PreUpdate` (id + createdAt + updatedAt), like `UserPart`/`PriceSnapshot`.
- Enums persisted `@Enumerated(EnumType.STRING)`, stored as `VARCHAR`.
- Use `@MockitoBean` (not `@MockBean`). Controller slices: `@WebMvcTest` + `authentication()`/`csrf()` post-processors (default filters), mirroring `PriceSnapshotControllerTest`.
- Owner-scoped reads/writes via `findByIdAndUserId`; missing/not-owned → `ResourceNotFoundException` (404), no existence leak.
- Money is `BigDecimal`; compare with `compareTo`.
- No `SecurityConfig` change (`/api/v1/price-alerts/**` already authenticated).
- Run integration/full tests only with Postgres up: `nc -z -w2 localhost 5433`.
- Parse Maven output: `./mvnw ... 2>&1 | grep -E "Tests run:|BUILD"`.
- Conventional Commits, scope `pricing`.

---

## File Structure

Create under `apps/api/src/main/java/com/brickdeck/api/pricing/`:
- `entity/PriceAlertType.java` — enum `BELOW_TARGET_PRICE | PERCENT_BELOW_AVERAGE | AT_OR_BELOW_LOWEST`
- `entity/PriceAlertRule.java`, `entity/TriggeredAlert.java`
- `repository/PriceAlertRuleRepository.java`, `repository/TriggeredAlertRepository.java`
- `dto/AddPriceAlertRuleRequest.java`, `dto/PriceAlertRuleResponse.java`, `dto/TriggeredAlertResponse.java`
- `service/PriceAlertRuleEvaluator.java`, `service/PriceAlertService.java`
- `controller/PriceAlertController.java`
- Modify `service/PriceSnapshotService.java` (call evaluator after save)

Migration: `apps/api/src/main/resources/db/migration/V9__add_price_alerts.sql`

Tests mirror packages under `src/test/java/com/brickdeck/api/pricing/`.

Docs: `docs/api/openapi.yaml`, `docs/api/postman/brickdeck.postman_collection.json`, `.claude/project-state.md`, `.claude/roadmap.md`.

---

### Task 1: PriceAlertRuleEvaluator (pure logic)

**Files:**
- Create: `pricing/service/PriceAlertRuleEvaluator.java`, `pricing/entity/PriceAlertType.java`
- Test: `pricing/service/PriceAlertRuleEvaluatorTest.java`

**Interfaces:**
- Produces: `enum PriceAlertType { BELOW_TARGET_PRICE, PERCENT_BELOW_AVERAGE, AT_OR_BELOW_LOWEST }`
- Produces: `Optional<String> PriceAlertRuleEvaluator.evaluate(PriceAlertType type, BigDecimal thresholdValue, BigDecimal amount, BigDecimal average, BigDecimal lowest)` — present message = fired.

- [ ] **Step 1: Write the failing test** (`PriceAlertRuleEvaluatorTest.java`)

```java
package com.brickdeck.api.pricing.service;

import com.brickdeck.api.pricing.entity.PriceAlertType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class PriceAlertRuleEvaluatorTest {
    private final PriceAlertRuleEvaluator evaluator = new PriceAlertRuleEvaluator();

    @Test
    void belowTargetFiresWhenStrictlyUnder() {
        assertThat(evaluator.evaluate(PriceAlertType.BELOW_TARGET_PRICE,
                new BigDecimal("100"), new BigDecimal("99.99"), null, null)).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.BELOW_TARGET_PRICE,
                new BigDecimal("100"), new BigDecimal("100"), null, null)).isEmpty();
    }

    @Test
    void percentBelowAverageFiresAtOrUnderThreshold() {
        // avg 100, 20% => fires at <= 80
        assertThat(evaluator.evaluate(PriceAlertType.PERCENT_BELOW_AVERAGE,
                new BigDecimal("20"), new BigDecimal("80"), new BigDecimal("100"), null)).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.PERCENT_BELOW_AVERAGE,
                new BigDecimal("20"), new BigDecimal("80.01"), new BigDecimal("100"), null)).isEmpty();
    }

    @Test
    void atOrBelowLowestFiresWhenTyingOrBeating() {
        assertThat(evaluator.evaluate(PriceAlertType.AT_OR_BELOW_LOWEST,
                null, new BigDecimal("80"), null, new BigDecimal("80"))).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.AT_OR_BELOW_LOWEST,
                null, new BigDecimal("80.01"), null, new BigDecimal("80"))).isEmpty();
    }
}
```

- [ ] **Step 2: Run — expect RED**

`./mvnw -Dtest=PriceAlertRuleEvaluatorTest test 2>&1 | grep -E "BUILD|cannot find symbol"` → cannot find symbol.

- [ ] **Step 3: Implement** — `PriceAlertType` enum + evaluator:

```java
package com.brickdeck.api.pricing.service;

import com.brickdeck.api.pricing.entity.PriceAlertType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PriceAlertRuleEvaluator {
    public Optional<String> evaluate(PriceAlertType type, BigDecimal thresholdValue,
                                     BigDecimal amount, BigDecimal average, BigDecimal lowest) {
        return switch (type) {
            case BELOW_TARGET_PRICE -> amount.compareTo(thresholdValue) < 0
                    ? Optional.of(amount + " is below your target " + thresholdValue) : Optional.empty();
            case PERCENT_BELOW_AVERAGE -> {
                BigDecimal cutoff = average.multiply(
                        BigDecimal.ONE.subtract(thresholdValue.movePointLeft(2)));
                yield amount.compareTo(cutoff) <= 0
                        ? Optional.of(amount + " is at least " + thresholdValue + "% below average " + average)
                        : Optional.empty();
            }
            case AT_OR_BELOW_LOWEST -> amount.compareTo(lowest) <= 0
                    ? Optional.of(amount + " is at or below your lowest " + lowest) : Optional.empty();
        };
    }
}
```

- [ ] **Step 4: Run — expect GREEN** (`Tests run: 3`).
- [ ] **Step 5: Commit** — `feat(pricing): add price-alert rule evaluator`.

---

### Task 2: Schema, entities, repositories, DTOs

**Files:**
- Create: `V9__add_price_alerts.sql`, `entity/PriceAlertRule.java`, `entity/TriggeredAlert.java`, `repository/PriceAlertRuleRepository.java`, `repository/TriggeredAlertRepository.java`, `dto/*`
- Test: `pricing/repository/PriceAlertRuleRepositoryTest.java` (`@SpringBootTest` + `@Transactional`, real Postgres)

**Interfaces (Produces):**
- `PriceAlertRule` (Lombok `@Getter/@Setter`): `id`, `user` (User), `brickSet` (BrickSet), `currency` (String), `type` (PriceAlertType), `thresholdValue` (BigDecimal, nullable), `active` (boolean), `createdAt`/`updatedAt`.
- `TriggeredAlert`: `id`, `user` (User), `rule` (PriceAlertRule), `snapshot` (PriceSnapshot), `amount` (BigDecimal), `currency` (String), `message` (String), `triggeredAt` (LocalDateTime).
- `PriceAlertRuleRepository`: `Page<PriceAlertRule> findByUserId(UUID, Pageable)` (`@EntityGraph brickSet`); `Optional<PriceAlertRule> findByIdAndUserId(UUID, UUID)`; `List<PriceAlertRule> findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(UUID, String, String)`.
- `TriggeredAlertRepository`: `Page<TriggeredAlert> findByUserId(UUID, Pageable)`; `Optional<TriggeredAlert> findByIdAndUserId(UUID, UUID)`.
- `AddPriceAlertRuleRequest(String setNumber, String currency, PriceAlertType type, BigDecimal thresholdValue)` with `@NotBlank setNumber`, `@NotBlank @Pattern("^[A-Z]{3}$") currency`, `@NotNull type`, `thresholdValue` (validated in service).
- `PriceAlertRuleResponse(UUID id, String setNumber, String currency, PriceAlertType type, BigDecimal thresholdValue, boolean active, LocalDateTime createdAt)`.
- `TriggeredAlertResponse(UUID id, UUID ruleId, String setNumber, BigDecimal amount, String currency, String message, LocalDateTime triggeredAt)`.

- [ ] **Step 1: Write the migration** `V9__add_price_alerts.sql`

```sql
CREATE TABLE price_alert_rules (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    set_id UUID NOT NULL REFERENCES sets (id),
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(30) NOT NULL,
    threshold_value NUMERIC(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_price_alert_rules_user_set_currency
    ON price_alert_rules (user_id, set_id, currency);

CREATE TABLE triggered_alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    rule_id UUID NOT NULL REFERENCES price_alert_rules (id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL REFERENCES price_snapshots (id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    message VARCHAR(500) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_triggered_alerts_user ON triggered_alerts (user_id);
```

- [ ] **Step 2: Write entities, repositories, DTOs** (per the Interfaces block; copy the `@PrePersist` pattern from `PriceSnapshot` — set `id`, `createdAt`, `updatedAt`; `TriggeredAlert` sets `id` + `triggeredAt` only).

- [ ] **Step 3: Write the failing repository test** (`PriceAlertRuleRepositoryTest`): seed a user + `sets` row, persist a rule, assert `findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue` returns it and `findByIdAndUserId` with a different user is empty. (Use `saveAndFlush` via the JPA repos; mirror `RecommendationIntegrationTest` seeding.)

- [ ] **Step 4: Run — expect RED then implement to GREEN.** `nc -z -w2 localhost 5433 && ./mvnw -Dtest=PriceAlertRuleRepositoryTest test 2>&1 | grep -E "Tests run:|BUILD|does not exist"`. If Flyway errors on an already-applied V9 during iteration, reset it: `docker exec brickdeck-postgres psql -U brickdeck -d brickdeck -c "DROP TABLE IF EXISTS triggered_alerts, price_alert_rules CASCADE; DELETE FROM flyway_schema_history WHERE version='9';"`.

- [ ] **Step 5: Commit** — `feat(pricing): add price-alert schema, entities, repositories, DTOs`.

---

### Task 3: PriceAlertService (CRUD + evaluateForSnapshot)

**Files:**
- Create: `pricing/service/PriceAlertService.java`
- Test: `pricing/service/PriceAlertServiceTest.java` (Mockito)

**Interfaces:**
- Consumes: `PriceAlertRuleRepository`, `TriggeredAlertRepository`, `UserSetRepository` (from `collection.repository`), `PriceAnalysisService`, `PriceAlertRuleEvaluator`, `BrickSetService`.
- Produces:
  - `PriceAlertRuleResponse createRule(User owner, AddPriceAlertRuleRequest req)` — set must be in owner's WISHLIST (`userSetRepository.findByUserIdAndStatus(owner.getId(), WISHLIST)` contains it) else `ResourceNotFoundException`; threshold required (`> 0`, and `<= 100` for PERCENT) for target/percent types else `IllegalArgumentException`; persists rule (resolve set via `brickSetService.findOrImportEntity`).
  - `PageResponse<PriceAlertRuleResponse> listRules(User owner, Pageable)`.
  - `void deleteRule(User owner, UUID id)` — owner-scoped.
  - `PageResponse<TriggeredAlertResponse> listTriggered(User owner, Pageable)`.
  - `void deleteTriggered(User owner, UUID id)` — owner-scoped.
  - `void evaluateForSnapshot(User owner, PriceSnapshot snapshot)` — load active rules for `(owner, snapshot.brickSet.externalSetNumber, snapshot.currency)`; if none return; `analysis = priceAnalysisService.analyze(owner.getId(), setNumber, currency, null)`; per rule `evaluator.evaluate(type, threshold, snapshot.amount, analysis.averageAmount(), analysis.minAmount())`; on a hit persist a `TriggeredAlert`.

- [ ] **Step 1: Failing tests** (`PriceAlertServiceTest`, Mockito) — cover:
  - `createRule` rejects a set not in wishlist → `ResourceNotFoundException`.
  - `createRule` rejects missing/zero threshold for `BELOW_TARGET_PRICE` and percent > 100 → `IllegalArgumentException`.
  - `createRule` persists an `AT_OR_BELOW_LOWEST` rule with null threshold.
  - `evaluateForSnapshot` with a matching `BELOW_TARGET_PRICE` rule saves one `TriggeredAlert`; with no rules saves none (and does not call `analyze`); with a non-matching rule saves none.
  - `deleteRule`/`deleteTriggered` owner-scoped → `ResourceNotFoundException` when absent.

  Mock `priceAnalysisService.analyze(...)` to return a `PriceAnalysisResponse` with `averageAmount`/`minAmount`. Stub `userSetRepository.findByUserIdAndStatus(id, CollectionStatus.WISHLIST)` to return a `UserSet` whose `brickSet.externalSetNumber` matches. Verify `triggeredAlertRepository.save(...)` invocation counts with `ArgumentCaptor`.

- [ ] **Step 2: Run — expect RED.** `./mvnw -Dtest=PriceAlertServiceTest test 2>&1 | grep -E "BUILD|cannot find symbol"`.

- [ ] **Step 3: Implement `PriceAlertService`.** Threshold validation:

```java
private void validateThreshold(PriceAlertType type, BigDecimal threshold) {
    switch (type) {
        case BELOW_TARGET_PRICE -> require(threshold != null && threshold.signum() > 0,
                "thresholdValue must be a positive amount");
        case PERCENT_BELOW_AVERAGE -> require(threshold != null && threshold.signum() > 0
                && threshold.compareTo(new BigDecimal("100")) <= 0,
                "thresholdValue must be a percent in (0, 100]");
        case AT_OR_BELOW_LOWEST -> { /* threshold ignored */ }
    }
}
```

Wishlist guard: `boolean owned = userSetRepository.findByUserIdAndStatus(owner.getId(), CollectionStatus.WISHLIST).stream().anyMatch(us -> us.getBrickSet().getExternalSetNumber().equals(SetNumbers.normalize(req.setNumber())));` else 404. `evaluateForSnapshot` stores `TriggeredAlert` with `owner`, `rule`, `snapshot`, `snapshot.getAmount()`, `snapshot.getCurrency()`, message from the evaluator.

- [ ] **Step 4: Run — expect GREEN.**
- [ ] **Step 5: Commit** — `feat(pricing): add price-alert service with snapshot evaluation`.

---

### Task 4: Wire evaluation into snapshot creation

**Files:**
- Modify: `pricing/service/PriceSnapshotService.java` (inject `PriceAlertService`; after `save`, call `evaluateForSnapshot(owner, saved)`)
- Modify test: `pricing/service/PriceSnapshotServiceTest.java` (add `@Mock PriceAlertService`; verify it is called with the saved snapshot)

- [ ] **Step 1: Update `PriceSnapshotServiceTest`** — add `@Mock private PriceAlertService priceAlertService;` and, in the add test, `verify(priceAlertService).evaluateForSnapshot(eq(owner), any(PriceSnapshot.class));`.
- [ ] **Step 2: Run — expect RED** (constructor arity / missing verify).
- [ ] **Step 3: Implement** — add the dependency + one call:

```java
PriceSnapshot saved = priceSnapshotRepository.save(snapshot);
priceAlertService.evaluateForSnapshot(owner, saved);
return toResponse(saved);
```

- [ ] **Step 4: Run — expect GREEN** (`PriceSnapshotServiceTest`, and re-run `PriceAlertServiceTest`).
- [ ] **Step 5: Commit** — `feat(pricing): evaluate alert rules on snapshot add`.

---

### Task 5: PriceAlertController

**Files:**
- Create: `pricing/controller/PriceAlertController.java`
- Test: `pricing/controller/PriceAlertControllerTest.java` (`@WebMvcTest(PriceAlertController.class)`, `@MockitoBean PriceAlertService`)

**Interfaces (Produces):** `@RequestMapping("/api/v1/price-alerts")`:
- `POST ""` → 201 + Location `/{id}` + `PriceAlertRuleResponse` (`@Valid @RequestBody AddPriceAlertRuleRequest`, `@AuthenticationPrincipal User`).
- `GET ""` → `PageResponse<PriceAlertRuleResponse>` (`@PageableDefault(size=20, sort="createdAt", direction=DESC)`).
- `DELETE "/{id}"` → 204.
- `GET "/triggered"` → `PageResponse<TriggeredAlertResponse>` (`@PageableDefault(size=20, sort="triggeredAt", direction=DESC)`).
- `DELETE "/triggered/{id}"` → 204.

- [ ] **Step 1: Failing tests** — mirror `PriceSnapshotControllerTest`:
  - POST 201 + `header().string("Location", containsString("/api/v1/price-alerts/"))` + `$.type` value.
  - POST 400 when the service throws `IllegalArgumentException` (bad threshold) — **requires** a `GlobalExceptionHandler` mapping for `IllegalArgumentException` → 400; if absent, add it in this task (Step 3a).
  - GET rules 200 `$.content[0].setNumber`, `$.page`.
  - GET `/triggered` 200 `$.content[0].message`.
  - DELETE `/{id}` and `/triggered/{id}` → 204, verify service called.
  Use `.with(authentication(principal()))` and `.with(csrf())` for POST/DELETE.

- [ ] **Step 2: Run — expect RED.**
- [ ] **Step 3: Implement the controller.**
- [ ] **Step 3a (if needed): add `IllegalArgumentException` → 400** to `common/GlobalExceptionHandler` (check first: `grep -n IllegalArgument apps/api/src/main/java/com/brickdeck/api/common/GlobalExceptionHandler.java`). Return the standard error body (status 400, error "Bad Request", message = exception message). Add/adjust a handler test if the class has one.
- [ ] **Step 4: Run — expect GREEN.**
- [ ] **Step 5: Commit** — `feat(pricing): add price-alert endpoints`.

---

### Task 6: Integration test (real Postgres)

**Files:**
- Test: `pricing/PriceAlertIntegrationTest.java` (`@SpringBootTest`, `@Transactional`)

- [ ] **Step 1: Write the test.** Seed (mirror `PriceTrackingIntegrationTest`): a `sets` row (`numberOfParts` set), a user, and a `UserSet` with `CollectionStatus.WISHLIST` for that set (via `UserSetRepository`). Create a `BELOW_TARGET_PRICE` rule (`thresholdValue = 100`, USD) through `PriceAlertService.createRule`. Add a snapshot at `80 USD` via `PriceSnapshotService.addSnapshot`. Assert:
  - `priceAlertService.listTriggered(owner, PageRequest.of(0,20)).totalElements() == 1` and the alert `amount` is `80`.
  - Adding a snapshot at `150 USD` creates no new triggered alert (still 1).
  - A second user with no rules sees `listTriggered == 0`.
- [ ] **Step 2: Run — expect RED, implement any gaps, then GREEN.** `nc -z -w2 localhost 5433 && ./mvnw -Dtest=PriceAlertIntegrationTest test 2>&1 | grep -E "Tests run:|BUILD"`.
- [ ] **Step 3: Full verify.** `./mvnw clean verify 2>&1 | grep -E "Tests run: [0-9]+, Fail|BUILD"` → BUILD SUCCESS.
- [ ] **Step 4: Commit** — `test(pricing): add price-alert integration test`.

---

### Task 7: Docs (openapi, Postman, state)

**Files:**
- Modify: `docs/api/openapi.yaml`, `docs/api/postman/brickdeck.postman_collection.json`, `docs/api/postman/README.md`, `.claude/project-state.md`, `.claude/roadmap.md`

- [ ] **Step 1: openapi** — add the five paths under the `Pricing` tag and schemas `AddPriceAlertRuleRequest`, `PriceAlertRuleResponse`, `PagePriceAlertRuleResponse`, `TriggeredAlertResponse`, `PageTriggeredAlertResponse`, `PriceAlertType` enum. Validate: `python3 -c "import yaml; yaml.safe_load(open('docs/api/openapi.yaml'))"`.
- [ ] **Step 2: Postman** — add alert requests to the `Pricing` folder (create rule captures `{{priceAlertRuleId}}`; list rules; delete rule; list triggered; dismiss triggered). Validate JSON parses.
- [ ] **Step 3: state/roadmap** — mark Phase 6 alerts backend done; note the frontend alerts slice as next.
- [ ] **Step 4: Commit** — `docs(api): document price-alert endpoints; mark Phase 6 alerts`.

---

## Self-Review

- **Spec coverage:** rule types → Task 1; schema/entities → Task 2; CRUD + wishlist guard + evaluation → Task 3; snapshot-add hook → Task 4; endpoints → Task 5; integration/owner-isolation → Task 6; openapi/Postman/state → Task 7. All spec sections mapped.
- **Placeholders:** none — evaluator, migration, threshold validation, and wiring shown as concrete code; other classes fully specified via Interfaces blocks + mirror-references to existing files (`PriceSnapshot*`).
- **Type consistency:** `evaluate(type, thresholdValue, amount, average, lowest)` used identically in Tasks 1 and 3; `evaluateForSnapshot(User, PriceSnapshot)` used in Tasks 3 and 4; repository finder name `findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue` consistent in Tasks 2 and 3.
- **Risk noted:** Task 5 depends on an `IllegalArgumentException`→400 mapping; Step 3a adds it if missing.
