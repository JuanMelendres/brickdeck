# ADR-012: Spring Boot 4 Upgrade — Deferred, Staged When Scheduled

## Status
Accepted

## Date
2026-07-17

## Context
Dependabot surfaces Spring Boot `4.1.0` weekly (`spring-boot-starter-parent` 3.5.16 →
4.1.0, PR #55), and the backend vulnerability audit
(`docs/security/spring-vulnerability-audit-api.md`) deferred the Boot 4 question as a
strategic decision rather than an automated bump. `apps/api` is on Boot **3.5.16** /
Java **21**.

The Boot 4 upgrade spike (`docs/superpowers/specs/2026-07-17-spring-boot-4-upgrade-spike.md`)
compared defer / schedule / take-now, and a **POC was executed** to resolve the one
project-specific unknown — the interaction between Boot 4's Jackson 3 and the JWT stack.

Key facts (POC-verified, 2026-07-17):

- Boot 3.5 is **actively maintained** — 3.5.16 shipped 2026-06-25, *after* Boot 4.1.0
  (2026-06-10). Staying is not "running unsupported."
- Java 21 satisfies Boot 4's Java 17 baseline; the `javax`→`jakarta` migration is already
  done (Boot 3). So those historically painful hops are not in play.
- **Boot 4 ships Jackson 3** (`tools.jackson`), but **`jjwt-jackson:0.13.0` depends on
  Jackson 2** (`com.fasterxml.jackson`). Under Boot 4, jjwt-jackson resolves its own
  transitive `jackson-databind:2.21.4` — the version **affected by CVE-2026-54515** (fixed
  in 2.21.5) — and Boot 4 does not manage Jackson 2, so the property override that closed
  this CVE on Boot 3 has no effect. **A naive Boot 4 upgrade silently reintroduces a CVE
  the audit just closed.**
- Boot 4 does **not compile as-is**: `RebrickableConfig` uses
  `org.springframework.boot.http.client` (`ClientHttpRequestFactorySettings`/`Builder`),
  a package restructured in Boot 4.
- Nothing user-facing requires Boot 4 today; the security posture is clean on 3.5.16.

## Decision
**Defer the Spring Boot 4 upgrade.** Stay on the maintained 3.5.x line.

- Keep Dependabot #55 **open and un-ignored** as the standing reminder (Spring Boot's own
  4.x major is deliberately *not* added to the `dependabot.yml` ignore list).
- Keep the **`springdoc` major-version ignore** in `.github/dependabot.yml` — springdoc
  3.x is Boot-4-only; it is removed as part of the upgrade, not before.
- When a driver appears (Boot 3.5 nearing end of maintenance, a needed Boot 4 feature, or
  a Boot-4-only security fix landing on our graph), execute a **staged** migration via a
  TDD, not the automated bump. The migration must include, at minimum:
  1. Drop `jjwt-jackson`'s Jackson 2 dependency — move JWT (de)serialization to a
     Jackson-3 or Gson jjwt serializer (preferred over pinning a fixed Jackson 2).
  2. Migrate `RebrickableConfig` off `org.springframework.boot.http.client`.
  3. springdoc 2.8 → 3.0.x; remove the audit's `jackson-bom`/`commons-lang3` overrides
     (Boot 4 supersedes both); remove the `springdoc` major ignore from `dependabot.yml`.
  4. Validate Spring Security 7.1 defaults against the stateless-JWT config, and
     Hibernate 7 / Flyway 12 against the Testcontainers integration suite.

## Consequences
- Positive: no effort now, no CVE regression, no risk of a broken `develop` from a
  four-major bump (Framework 7 + Security 7 + Jackson 3 + Hibernate 7 at once).
- Positive: the eventual migration is **pre-costed** — the POC turned unknowns into a
  bounded, ordered work list, so scheduling it later is low-surprise.
- Negative: Boot 4 features stay unavailable; upgrade debt accrues slowly.
- Negative: the decision must be **revisited immediately** if a Boot-4-only security fix
  appears on our dependency graph (Assumption-002 in the spike).

## Alternatives Considered
- **Take Dependabot #55 now (one-shot bump):** rejected. Merges four coincident majors in
  one unreviewed step; the POC showed it reintroduces CVE-2026-54515 and does not even
  compile. Highest risk, no payoff.
- **Schedule the staged migration now:** viable but premature — there is no current driver,
  and the effort (jjwt serializer swap + HTTP-client migration + four major-version
  validations) is real. Held ready, not started.

## Notes
Spike (with full findings and POC results):
`docs/superpowers/specs/2026-07-17-spring-boot-4-upgrade-spike.md`.
Security audit that deferred this: `docs/security/spring-vulnerability-audit-api.md`.
Standing policy encoded in `.github/dependabot.yml` (springdoc major ignore; jjwt group).
Revisit triggers and open questions (OQ-002 serializer choice, OQ-003 Security 7 defaults,
OQ-005 HTTP-client API relocation) tracked in the spike.
