# Technical Spike: Spring Boot 3.5 → 4.x Upgrade

> Status: **Proposed** · Author: engineering · Date: 2026-07-17
> **POC executed 2026-07-17 — see §12.** The Jackson/JWT unknown is now resolved: the
> central risk is confirmed with exact versions, and the defer recommendation is
> strengthened.
> Prompted by Dependabot #55 (`spring-boot-starter-parent` 3.5.16 → 4.1.0) and the
> backend vulnerability audit (`docs/security/spring-vulnerability-audit-api.md`),
> which deferred the Boot 4 decision as strategic rather than automatic.

## 1. Summary

**Main question:** Should `apps/api` upgrade from Spring Boot 3.5.16 to 4.x now, and
if so, on what path?

**Why now:** Dependabot surfaces Boot 4.1.0 weekly (#55, left un-ignored on purpose).
The audit also left three temporary version overrides in the pom whose lifecycle is
tied to this decision. We need a documented position so the recurring PR is a decision,
not a reflex.

**Expected decision:** Defer or schedule. This spike recommends **defer**, with a
staged plan and a time-boxed POC to retire the largest unknown before committing.

## 2. Background

- `apps/api` is Spring Boot **3.5.16**, Java **21**, Maven. The 3.5 line is actively
  maintained (3.5.16 shipped 2026-06-25, *after* Boot 4.1.0's 2026-06-10) — so staying
  put is not "running unsupported."
- Boot 3 already completed the `javax` → `jakarta` migration, so that historically
  painful hop is **not** part of 3.5 → 4.
- The security posture is clean as of the audit: 0 critical/high, all findings resolved.
  Nothing about Boot 4 is required for security today.
- Three audit overrides currently sit in `pom.xml` and interact with this upgrade:
  - `jackson-bom.version` = 2.21.5 (CVE-2026-54515)
  - `commons-lang3.version` = 3.18.0 (CVE-2025-48924)
  - `jjwt.version` = 0.13.0 (not BOM-managed)

## 3. Problem Statement

Boot 4.x is a major release (Spring Framework 7, Spring Security 7, Jackson 3, Hibernate
7, Flyway 12, Tomcat 11). We need to determine whether the upgrade is (a) low-risk enough
to take now, (b) worth a scheduled migration effort, or (c) best deferred — and to
identify the specific breakages in *this* codebase, not Boot 4 in general.

## 4. Goals

- Establish what Boot 4.1.0 actually pulls in (versions, Java baseline).
- Identify this project's concrete exposure — which of our APIs and dependencies break.
- Decide the fate of the three audit overrides under Boot 4.
- Recommend defer / schedule, with a staged path and a POC for the biggest unknown.

## 5. Non-Goals

- This spike does **not** perform the upgrade.
- It does not resolve the Jackson 2/3 coexistence question — it scopes a POC to do so.
- It does not evaluate Boot 4 features we might adopt later (e.g. the new HTTP client);
  the question here is migration cost, not opportunity.

## 6. Key Questions

- **Q-001:** What Java baseline and managed dependency versions does Boot 4.1.0 impose?
- **Q-002:** Does our Spring Security configuration survive Security 6.5 → 7.1?
- **Q-003:** What happens to JWT serialization when Boot moves to Jackson 3 while
  `jjwt-jackson` still depends on Jackson 2? *(the central risk)*
- **Q-004:** Which audit overrides must be removed, kept, or transformed?
- **Q-005:** What is the smallest safe staged path, and is a POC needed first?

## 7. Constraints

- **Stack:** Java 21, Maven wrapper 3.9.16, PostgreSQL 16, single module, no Spring Cloud.
- **Team:** small; low appetite for a multi-day migration without a concrete payoff.
- **Backward compatibility:** the published REST contract and `apps/web`'s generated
  types (`schema.d.ts`) must not change silently.
- **Security:** must not reintroduce the CVEs the audit just closed (see Q-003 — this is
  a live risk, not a hypothetical).
- **CI:** GitHub Actions on Java 21 temurin; Testcontainers-based integration tests.

## 8. Options Considered

| Option | Description | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|
| **A. Defer** (recommended) | Stay on 3.5.x; keep the springdoc-major ignore; re-evaluate when a payoff appears or 3.5 nears EOL | Zero effort; 3.5 actively maintained; no CVE regression risk | Boot 4 features unavailable; upgrade debt accrues slowly | None | Low |
| **B. Schedule staged migration** | Plan a deliberate 3.5 → 4.x migration in a dedicated slice, POC-first | Controlled; retires the Jackson unknown before committing; clean override cleanup | Real effort (est. 1–3 days incl. Jackson resolution); touches security-sensitive JWT path | Medium–High | Medium |
| **C. Take Dependabot #55 now** | Merge the automated bump and fix fallout reactively | Fast to start | Jackson 2/3 + Security 7 + Hibernate 7 fallout hits at once; high chance of a broken `develop`; overrides silently wrong | High | **High** |

Option C is rejected outright — it merges a four-major-version jump (Framework, Security,
Jackson, Hibernate) in one unreviewed step, exactly what the audit skill's remediation
rules warn against.

## 9. Evaluation Criteria

| Criterion | Description | Weight |
|---|---|---|
| Security continuity | Must not reintroduce closed CVEs | High |
| Effort vs payoff | Migration cost against concrete benefit | High |
| Contract stability | REST/OpenAPI/frontend-types unchanged | High |
| Reversibility | Can we roll back cleanly | Medium |
| Maintenance runway | How long 3.5.x remains viable | Medium |

## 10. Research Findings

*(All version facts pulled from Maven Central POMs on 2026-07-17. Facts are marked
FACT; engineering inference is marked INFERENCE.)*

### Finding 1: Boot 4.1.0 baseline and managed versions

**FACT** — from `spring-boot-dependencies:4.1.0` and `spring-boot-starter-parent:4.1.0`:

| Component | Boot 3.5.16 (current) | Boot 4.1.0 | Jump |
|---|---|---|---|
| Java baseline (`java.version`) | 17 | **17** | none — our Java 21 satisfies both |
| Spring Framework | 6.2.19 | **7.0.8** | major |
| Spring Security | 6.5.11 | **7.1.0** | major |
| Jackson | 2.21.x | **3.1.4** | **major, groupId change** |
| Hibernate | 6.6.x | **7.4.1** | major |
| Flyway | 11.7.2 | **12.4.0** | major |
| Tomcat | 10.1.55 | **11.0.22** | major (Servlet 6.1) |
| commons-lang3 | 3.17.0 (we override to 3.18.0) | **3.20.0** | — |

**Impact:** Java 21 needs no change. Every other core piece is a major bump. The
`javax`→`jakarta` migration is already behind us (done in Boot 3), removing the single
worst historical migration cost.

### Finding 2: Spring Security config is largely forward-compatible

**FACT** — `SecurityConfig` uses the lambda `SecurityFilterChain` DSL
(`authorizeHttpRequests(auth -> …)`) and `JwtAuthenticationFilter extends
OncePerRequestFilter`. It does **not** use `WebSecurityConfigurerAdapter` (removed back
in Security 6).

**INFERENCE:** Security 6.5 → 7.1 is a lower risk than the version jump suggests, because
we already write config the way Security 7 expects. Expect deprecation/method-rename
churn, not a rewrite. Still needs validation — Security 7 tightened several defaults.

### Finding 3: Jackson 2 → 3 collides with `jjwt-jackson` — the central risk

**FACT:**
- Boot 4 ships Jackson **3** (`tools.jackson`, `jackson-bom 3.1.4`).
- `jjwt-jackson:0.13.0` hard-depends on **`com.fasterxml.jackson.core:jackson-databind`**
  — Jackson **2**.

**INFERENCE:** Under Boot 4, both Jackson majors would be on the classpath: Jackson 3 for
Spring MVC's HTTP message conversion, Jackson 2 dragged in transitively by `jjwt-jackson`
for JWT claim (de)serialization. jjwt uses its own `ObjectMapper`, so functionally this
*should* work — but it creates two concrete problems:

1. **The CVE the audit just closed reappears, unmanaged.** Our `jackson-bom.version =
   2.21.5` override exists on the Boot 3 BOM. Under Boot 4, Boot no longer manages Jackson
   2 at all, so `jjwt-jackson` pulls whatever `jackson-databind` 2.x *its* POM resolves —
   potentially an older version still affected by CVE-2026-54515. The fix would have to
   move from a BOM property override to an explicit managed dependency on the transitive
   Jackson 2 artifact, or to dropping `jjwt-jackson` in favor of a Jackson-3 or
   Gson-based jjwt serializer.
2. **Two Jackson majors on one classpath** is a maintenance smell and a source of subtle
   serialization bugs if anything bridges the two.

**This is the finding that justifies a POC before any migration.** It is specific to this
project's JWT stack and is not covered by generic Boot 4 migration guides.

**→ CONFIRMED by the POC (§12):** the resolved Jackson 2 is `jackson-databind:2.21.4`,
which *is* CVE-2026-54515-affected, and it is unmanaged under Boot 4. The inference above
is now measured fact.

### Finding 4: Override cleanup under Boot 4

**FACT** — Boot 4.1.0 manages `commons-lang3` at **3.20.0** (> our 3.18.0 override, which
was for CVE-2025-48924, fixed in 3.18.0).

**INFERENCE:** On migration —
- `commons-lang3.version` override → **remove** (Boot 4 BOM already clears the CVE).
- `jackson-bom.version = 2.21.5` override → **remove / meaningless** (Boot 4 is Jackson 3;
  the Jackson-2 CVE moves to the `jjwt-jackson` transitive problem in Finding 3).
- `jjwt.version = 0.13.0` → **keep** (not BOM-managed either way).

### Finding 5: springdoc must jump to 3.0.x

**FACT** — springdoc 3.0.x targets Boot 4 (verified during backlog triage: its parent is
`spring-boot-starter-parent:4.x`). Latest is **3.0.3**. We are on 2.8.17, and
`.github/dependabot.yml` currently **ignores springdoc majors** precisely because 3.x is
Boot-4-only.

**Impact:** The Boot 4 migration is also a springdoc 2.8 → 3.0 migration, and it must
remove the springdoc-major ignore rule from `dependabot.yml`.

## 11. Proof of Concept Plan

**Purpose:** Retire the Finding 3 unknown (Jackson 2/3 + jjwt) before committing to a
migration — it is the one risk that could make the whole upgrade unattractive.

**Build (time-box: 2–4 hours, throwaway branch):**
- Bump parent to 4.1.0; bump springdoc to 3.0.3; remove the `commons-lang3` and
  `jackson-bom` overrides.
- Compile; resolve the Jackson classpath (`mvn dependency:tree` — confirm what Jackson 2
  version `jjwt-jackson` drags in and whether it is CVE-affected).
- Run `JwtServiceTest` + `AuthIntegrationTest` and a live register→login→`/api/v1/auth/me`
  round-trip.

**Do not build:** the full Security 7 / Hibernate 7 / Flyway 12 migration — this POC is
scoped only to the Jackson/JWT question. Everything else is deferred to the real slice.

**Validation / exit criteria:**
- JWT sign + verify works, tampered tokens rejected (reuse the #66 tamper matrix).
- The transitive Jackson 2 `jackson-databind` version is known and is **not**
  CVE-affected, or a mitigation is identified.
- `mvn dependency:tree` documented for the record.

**Decision gate:** if the JWT path survives with a non-vulnerable Jackson 2 (or an easy
switch to a Jackson-3 jjwt serializer), Boot 4 becomes Option B (schedule). If it needs
replacing the JWT serialization stack, the cost rises and defer strengthens.

## 12. Proof of Concept Results

**Executed 2026-07-17** on a throwaway branch (`poc/boot4-jackson-throwaway`, since
deleted). Applied exactly the §11 build: parent → 4.1.0, springdoc → 3.0.3, both audit
overrides removed, jjwt kept.

### Result 1 — Jackson 2/3 coexistence: CONFIRMED, and worse than feared

`mvn dependency:tree` under Boot 4.1.0 resolves **both** Jackson majors:

```
+- io.jsonwebtoken:jjwt-jackson:jar:0.13.0:runtime
|  \- com.fasterxml.jackson.core:jackson-databind:jar:2.21.4:compile   <- Jackson 2, jjwt
|  \- tools.jackson.core:jackson-databind:jar:3.1.4:compile            <- Jackson 3, Spring MVC
```

The critical detail: with the `jackson-bom` override gone, jjwt-jackson resolves its own
transitive **`jackson-databind:2.21.4`** — which **is affected by CVE-2026-54515** (fixed
in 2.21.5). Boot 4 does not manage Jackson 2 at all, so the property override that closed
this CVE on Boot 3 has no effect here. **The Boot 4 upgrade, done naively, silently
reintroduces the exact CVE the audit closed** — confirmed with exact versions, not
predicted. (Reachability is still gated by the audit's finding — no
`ACCEPT_CASE_INSENSITIVE_PROPERTIES` + `@JsonIgnoreProperties` in our code — but a scanner
flags it and it is now unmanaged.)

Jackson 2 and 3 are cleanly separated by groupId (`com.fasterxml` vs `tools.jackson`);
jjwt uses its own `ObjectMapper`, so the two do not interact. Functionally the JWT path
should work; the problem is the vulnerable, unmanaged Jackson 2 artifact, not a runtime
clash.

### Result 2 — Boot 4 does not compile as-is (secondary finding)

`mvn clean compile` under Boot 4 **fails**:

```
RebrickableConfig.java:[4] package org.springframework.boot.http.client does not exist
RebrickableConfig.java:[5] package org.springframework.boot.http.client does not exist
```

`RebrickableConfig` configures the Rebrickable `RestClient`'s connect/read timeouts via
`ClientHttpRequestFactorySettings` / `ClientHttpRequestFactoryBuilder` from
`org.springframework.boot.http.client` — a package **restructured in Boot 4**. This is a
real (if bounded) migration touchpoint, not an import rename we could resolve in the time
box, and it is unrelated to the Jackson question. It confirms Boot 4 is a genuine
migration with API churn, not a version bump.

### Result 3 — JWT runtime test: BLOCKED, not run

The live `register → login → /api/v1/auth/me` and tamper matrix (§11 exit criteria)
**could not be executed**: the app does not compile under Boot 4 until Result 2 is fixed,
which is outside this POC's scope. Honest status: the JWT path's *functional* behavior
under Boot 4 is **unverified at runtime**. The dependency evidence (isolated Jackson 2 for
jjwt) suggests it would work, but this is inference, not a runtime result.

### POC verdict against the §11 decision gate

The gate was: "if the JWT path survives with a non-vulnerable Jackson 2 → schedule; if it
needs replacing the serialization stack → defer strengthens."

**The Jackson 2 that resolves is vulnerable (2.21.4), and it is unmanaged.** The path does
*not* survive on a non-vulnerable Jackson 2 without extra work. The POC therefore
**strengthens the defer recommendation** and converts OQ-001/OQ-002 from open questions
into a required migration work item: on any Boot 4 upgrade, the JWT serialization must be
either (a) explicitly pinned to a fixed Jackson 2 `jackson-databind` via dependency
management, or (b) moved off `jjwt-jackson` to a Jackson-3 or Gson jjwt serializer that
removes the Jackson 2 dependency entirely. Option (b) is cleaner and is the recommended
migration approach when scheduled.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
|---|---|---|
| Defer vs upgrade now | No effort, no CVE-regression risk, 3.5 still maintained | Slowly accruing upgrade debt; Boot 4 features unavailable |
| Staged vs one-shot | Each major hop validated in isolation; clean rollback points | Slower; multiple PRs |
| Keep `jjwt-jackson` vs replace | No JWT code change | Forces Jackson 2 onto a Boot 4 classpath (Finding 3) |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Jackson 2 reintroduced unmanaged → CVE regression | High | Medium | POC resolves the tree; pin or replace `jjwt-jackson` serializer |
| Security 7 default/API changes break auth | High | Low–Medium | Config already lambda-DSL; validate with existing auth integration tests |
| Hibernate 7 / Flyway 12 schema or dialect changes | Medium | Medium | Testcontainers integration suite; validate migrations on a throwaway DB |
| springdoc 3.0 changes generated OpenAPI → frontend type drift | Medium | Medium | Diff `/v3/api-docs` before/after; regenerate `schema.d.ts` deliberately |
| Effort overruns with no user-facing payoff | Medium | Medium | Time-box; defer unless a concrete driver appears |

## 15. Recommendation

**Recommended option: A — Defer, with B (staged) pre-scoped and ready.**

- **Why defer:** Boot 3.5 is actively maintained, the security posture is clean, and
  nothing user-facing needs Boot 4 today. The one migration-specific risk (Finding 3)
  is non-trivial and security-sensitive — upgrading now trades a clean audit for an
  open question about a reintroduced Jackson-2 CVE.
- **Why not now (Option C):** merging four coincident majors (Framework 7, Security 7,
  Jackson 3, Hibernate 7) in one automated bump is high-risk and violates the staged
  remediation principle the audit followed throughout.
- **What flips the decision to B:** Boot 3.5 approaching end of maintenance, or a Boot 4
  feature we actually want. **The "clean Jackson/JWT path" trigger is now closed off —**
  the POC (§12) showed the path is *not* clean: it reintroduces a vulnerable, unmanaged
  Jackson 2, and Boot 4 needs a `RebrickableConfig` API migration just to compile. So a
  Boot 4 upgrade is now a known, non-trivial work item, not a maybe-easy bump.
- **The POC de-risked the decision by making the cost concrete** (§12): a scheduled Boot 4
  migration must include (a) replacing `jjwt-jackson` with a serializer that drops the
  Jackson 2 dependency, and (b) migrating the Rebrickable HTTP-client config. Both are
  bounded and known; neither is a blocker — but neither is free, which is precisely why
  defer holds until there is a driver.
- **Assumptions behind this:** see §20. Chiefly that no Boot-4-only security fix lands
  on our dependency graph in the interim — if one does, re-evaluate immediately.

## 16. Decision

- Decision: **Deferred** (recommended)
- Date: 2026-07-17
- Owner: backend
- Status: **Proposed** — pending team confirmation

Keep Dependabot #55 open and un-ignored as the standing reminder. Keep the
springdoc-major ignore in `dependabot.yml` until this decision flips.

## 17. Next Steps

1. Team confirms defer vs schedule on this spike. **The POC (§12) is done; its result
   argues for defer.**
2. If deferring: leave #55 open; no code change. Revisit on the triggers in §15.
3. ~~Run the Jackson/JWT POC~~ — **done 2026-07-17 (§12).** Central risk confirmed.
4. If/when scheduling: write a TDD for the staged migration that explicitly includes the
   two POC-surfaced work items — (a) drop `jjwt-jackson`'s Jackson 2 dependency (Jackson-3
   or Gson jjwt serializer), (b) migrate `RebrickableConfig` off
   `org.springframework.boot.http.client` — plus springdoc 2.8 → 3.0, override cleanup,
   and removing the `dependabot.yml` springdoc-major ignore. One major-concern per hop.
5. Promote the outcome to an ADR (§18).

## 18. ADR Candidate

- **ADR needed: Yes** (once decided).
- Suggested title: *ADR-012: Spring Boot 4 upgrade timing and Jackson/JWT strategy*
- Reason: affects the framework baseline, a security-sensitive dependency (JWT
  serialization), and the standing Dependabot policy — long-lived, cross-cutting.

## 19. Open Questions

- **OQ-001:** ~~What Jackson 2 `jackson-databind` version does `jjwt-jackson:0.13.0`
  resolve under Boot 4, and is it CVE-affected?~~ **ANSWERED (§12): `2.21.4`, and yes —
  CVE-2026-54515-affected, unmanaged.**
- **OQ-002:** Does jjwt offer a Jackson-3 or Gson serializer that removes the Jackson 2
  dependency entirely on Boot 4? *(Now a migration work item, not an open question — see
  §12 verdict. Still needs the specific serializer choice confirmed.)*
- **OQ-003:** Does Spring Security 7.1 change any default that affects our stateless-JWT,
  CSRF-disabled, `permitAll` config? *(Not reached — Boot 4 does not compile until the
  `RebrickableConfig` HTTP-client API is migrated; see §12 Result 2.)*
- **OQ-004:** Does the Boot 4 build still support Java 21 toolchain caching in CI as-is?
- **OQ-005 (new, from POC):** Where did `org.springframework.boot.http.client`
  (`ClientHttpRequestFactorySettings` / `Builder`) move in Boot 4, and is the
  `RebrickableConfig` migration an import swap or an API redesign?

## 20. Assumptions

- **Assumption-001:** Boot 3.5.x remains maintained for the near term (supported by its
  post-4.0 release cadence).
- **Assumption-002:** No Boot-4-only security fix lands on our graph before we upgrade;
  if one does, this defer is void.
- **Assumption-003:** The REST contract stays stable across the upgrade (springdoc 3.0
  emits an equivalent spec) — to be verified by diffing `/v3/api-docs`, as done for the
  2.8.17 bump.
- **Assumption-004:** Java 21 remains the target; Boot 4's Java 17 baseline imposes no
  change.
