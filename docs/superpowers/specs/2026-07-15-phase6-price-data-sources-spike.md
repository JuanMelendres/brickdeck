# Technical Spike: Phase 6 Price Data Sources

Date: 2026-07-15 · Status: Proposed · Owner: Juan Melendres

## 1. Summary

Phase 6 (Price Tracking and Deals) needs a source of set prices to store snapshots, build history, and detect real discounts. This Spike investigates **which price data source(s) BrickDeck should build on first**, given the project's explicit "official APIs before scraping, no scraping in the MVP" policy.

- **Main question:** What is the lowest-risk data source that lets us ship deal detection without scraping?
- **Reason:** Rebrickable (our catalog source) provides no pricing. Prices live in secondary-market and retail systems, each with its own terms, auth, and reliability.
- **Expected decision:** Pick the source for Phase 6 slice 1, and the shape of the price-snapshot schema so later sources plug in without rework.

## 2. Background

- Catalog data comes from Rebrickable and is cached locally (ADR-005). Rebrickable exposes sets/parts/themes/colors — **not prices**.
- `docs/product/pricing-scraping-policy.md` and ADR-006 ("defer scraping and AI") set the rule: prefer official APIs / feeds / user-submitted data; **the MVP must not depend on scraping**; start with manual price input + snapshots, add API integrations later.
- Roadmap Phase 6 success criterion: *"User can see whether a current price is actually a good deal."* Deal quality is computed from current price vs historical average / lowest observed / MSRP / price-per-piece — i.e. **we need a time series of snapshots**, not just a single live price.

## 3. Problem Statement

We must decide how BrickDeck obtains set prices for Phase 6. The uncertainty is not "can we fetch a price" but "which source can we depend on **legally, reliably, and cheaply**, and does it give us the historical/market context deal detection requires — without scraping in the MVP."

## 4. Goals

- Enumerate candidate price sources and classify each as official-API / feed / user-submitted / scraping.
- Compare them on legality (ToS), auth/cost, coverage, and fit for time-series deal detection.
- Recommend the Phase 6 slice-1 source and a source-agnostic snapshot schema.
- Define what must be verified before writing code (a small POC).

## 5. Non-Goals

- Not implementing price ingestion, the schema migration, or deal-detection logic.
- Not finalizing alert delivery (email/push) — that is a later Phase 6 slice.
- Not selecting retail affiliate partners or negotiating API access.
- Not confirming exact current ToS / rate limits / pricing — those are POC verification items, flagged below.

## 6. Key Questions

- **Q-001:** Which sources are official APIs vs scraping, and which are permitted by our policy for the MVP?
- **Q-002:** Which source gives secondary-market price context (avg / min / used vs new) needed for genuine deal detection, not just a single retail sticker price?
- **Q-003:** What auth, cost, rate limits, and per-region coverage does each viable source impose? (Region matters — policy examples use MXN / Mercado Libre / Liverpool.)
- **Q-004:** Can we design one `price_snapshot` schema that serves user-submitted prices now and API-sourced prices later without breaking changes?
- **Q-005:** Do any candidate ToS forbid storing/retaining price history or redistributing prices?

## 7. Constraints

- Stack: Java 21 / Spring Boot 3, PostgreSQL + Flyway, hexagonal packages; external integrations isolated in `external.*` (mirror `external.rebrickable`).
- Policy: no scraping in MVP; respect ToS/robots.txt; low frequency; cache; store only necessary pricing metadata.
- Secrets via env vars only; no keys in the repo.
- Solo/small project — favor low operational cost and low maintenance.
- Region relevance: users likely span US + Mexico (policy cites MXN, Mercado Libre, Liverpool).

## 8. Options Considered

| Option | Description | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|
| A. User-submitted / manual snapshots | Users (or admin) record observed prices + store + date. | Zero external dependency; policy's explicit MVP starting point; full control of schema; unblocks deal-detection engine immediately | No automatic coverage; sparse data until users contribute; self-reported accuracy | Low | Low |
| B. BrickLink Price Guide API | Official API for the largest LEGO secondary marketplace; price guide gives avg/min/max, new/used, qty. | Purpose-built market context (avg/min → real deal detection); official + documented | OAuth-style consumer key/token + IP registration; ToS limits on storage/redistribution (verify); primarily USD/marketplace, not local retail | Medium | Medium |
| C. BrickOwl API | Official API of a second LEGO marketplace (catalog + pricing). | Official; alternative/second opinion to BrickLink | Smaller market → thinner data; another integration to maintain | Medium | Medium |
| D. Brickset API | Set metadata API including RRP / retail price (region fields). | Official; gives MSRP/RRP for price-per-piece & baseline | RRP ≈ launch retail, not live market price; not a deal feed on its own | Low–Medium | Low |
| E. Retail store / affiliate APIs (LEGO, Amazon PA-API, Walmart, Mercado Libre) | Live retail prices via official/affiliate APIs. | Real purchase prices; region coverage (MXN) | Each needs separate approval/affiliate status; PA-API gated on sales; heterogeneous; retail ≠ secondary market | High | Medium–High |
| F. Scraping | Fetch store pages directly. | Broadest theoretical coverage | Explicitly against MVP policy; ToS/robots risk; brittle | High | High |

## 9. Evaluation Criteria

| Criterion | Description | Weight |
|---|---|---|
| Policy/legal fit | Allowed by scraping policy + source ToS | High |
| Deal-detection fit | Provides market context (avg/min/MSRP), not one sticker price | High |
| Time-to-first-value | How fast we can ship a working slice | High |
| Cost / access | Free-ish, no approval gate | Medium |
| Maintainability | Isolated adapter, low upkeep | Medium |
| Region coverage | US + MX relevance | Medium |

## 10. Research Findings

> Facts below are from general knowledge of these platforms and the repo. **Exact current ToS, auth, rate limits, and field names are NOT verified here** — see the POC plan. Treated as hypotheses to confirm, not settled facts.

### Finding 1: Rebrickable gives no prices — a second source is mandatory

Description: Our sole catalog integration returns sets/parts/colors/themes only.
Evidence: `external.rebrickable` client + DTOs (repo); ADR-005; CLAUDE.md "External API Rules".
Impact: Phase 6 requires a genuinely new integration or a user-submitted path; it cannot reuse the catalog client.

### Finding 2: Deal detection needs market context, which favors a marketplace price guide

Description: The policy defines a good deal via current vs historical average / lowest / MSRP / price-per-piece. A single retail price cannot establish "below average."
Evidence: `pricing-scraping-policy.md` → "Deal Detection Logic".
Impact: A marketplace price-guide source (BrickLink, Option B) or our own accumulated snapshot history is required. Until either exists, "deal" reduces to "below MSRP / below user target."

### Finding 3: The policy already prescribes the MVP starting point

Description: The policy's "MVP Recommendation" lists manual price input, wishlist target prices, store links, manually added snapshots — then "later API-based integrations."
Evidence: `pricing-scraping-policy.md` → "MVP Recommendation".
Impact: Option A is the sanctioned first step and lets us build the schema + deal engine now, decoupled from any external approval.

### Finding 4: MSRP/RRP is a cheap, low-risk baseline (Brickset / possibly Rebrickable metadata)

Description: A set's RRP enables price-per-piece and a "below retail" signal without any marketplace feed.
Evidence: Brickset API is known to expose retail-price fields (verify current schema); some catalog sources carry year/piece-count already cached.
Impact: RRP can enrich Option A cheaply and improve early deal signals before a marketplace API lands.

### Finding 5: Retail/affiliate APIs are powerful but gated

Description: Amazon PA-API requires qualifying affiliate sales to stay active; LEGO/Walmart/Mercado Libre need per-program approval.
Evidence: General platform knowledge (verify).
Impact: Option E is not a viable *first* slice; revisit once there is user traction.

## 11. Proof of Concept Plan

Time-box: **half a day.** No production code; a throwaway branch/script.

- **Verify (research, ~2h):** BrickLink API — current auth mechanism, price-guide endpoint fields (avg/min/max, new/used), IP registration, and ToS clauses on **storing/retaining** and **redistributing** prices. Same quick check for BrickOwl + Brickset RRP fields.
- **Prototype (~2h):** With a personal BrickLink key (env only), fetch the price guide for 2–3 known sets; capture the JSON; confirm we can extract avg/min for new + used. Record rate-limit headers.
- **Not built:** No persistence, no scheduler, no controller, no UI.
- **Validation:** We can (a) legally store a snapshot per the ToS, and (b) obtain avg/min price for a set number → deal detection is feasible via Option B.

## 12. Proof of Concept Results

TODO: POC not executed yet.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
|---|---|---|
| Manual snapshots (A) first vs marketplace API (B) first | Ship the schema + deal engine now, zero legal/approval risk | Sparse data until an API or users fill history |
| Marketplace price guide (B) vs retail (E) | Real "below average" deal signal | USD/market-centric; less local-retail (MXN) coverage |
| Storing history locally vs querying live each time | Enables trend/lowest-ever; resilient to source outages | Must confirm ToS permits retention; storage grows |
| One generic snapshot schema vs per-source tables | Sources plug in without migrations | Slightly looser typing; `source` + `currency` discriminators needed |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Marketplace ToS forbids storing/redistributing prices | High | Medium | Verify in POC before building B; if blocked, lean on A + user history + MSRP |
| Source auth/approval blocks automation | Medium | Medium | Start with A (no external dep); treat B as an isolated adapter added later |
| Region gap (no cheap MXN market data) | Medium | Medium | Support multi-currency in schema; allow user-submitted local prices |
| Snapshot schema churn when a real API lands | Medium | Low | Design source-agnostic schema now (Q-004); validate against B's fields in POC |
| Sparse manual data undermines "deal" signal early | Medium | High | Seed with MSRP baseline (Finding 4); "deal" = below-target/below-MSRP until history builds |

## 15. Recommendation

**Recommended: Two-track — Option A now, Option B next.**

1. **Slice 1 — Option A (user-submitted / manual snapshots) + MSRP baseline.** Build a source-agnostic `price_snapshot` model (`set`, `source`, `currency`, `amount`, `condition` new/used, `observed_at`, optional `store`/`url`) and the deal-detection engine (current vs stored history / MSRP / price-per-piece). Zero external dependency, fully policy-compliant, unblocks the whole feature.
2. **Slice 2 — Option B (BrickLink Price Guide API)** as the first automated source, added as an isolated `external.bricklink` adapter that writes the *same* snapshot model — **only after** the POC confirms ToS permits storing prices.

Why not the others first: C (BrickOwl) is a thinner second marketplace — add after B if needed. D (Brickset RRP) is an enrichment, folded into slice 1 as the MSRP baseline, not a standalone source. E (retail/affiliate) is gated on approvals/traction — defer. F (scraping) is excluded by policy.

Assumptions supporting this: the snapshot schema can be source-agnostic (Q-004); BrickLink's ToS permits limited local retention (to verify); users will contribute some prices early.

## 16. Decision

Decision pending. (Recommendation proposed for review; confirm before the POC.)

## 17. Next Steps

1. Approve the two-track direction.
2. Run the half-day POC (Section 11) — verify BrickLink ToS + price-guide fields.
3. Write a TDD for slice 1: `price_snapshot` schema (Flyway), `pricing` domain package, manual snapshot CRUD, deal-detection service, endpoints.
4. Record the source decision as an ADR (see §18).
5. Update `docs/product/roadmap.md` Phase 6 with the chosen approach.

## 18. ADR Candidate

- **ADR needed:** Yes.
- **Suggested title:** ADR-011 — Price data sourcing: user-submitted snapshots first, BrickLink API next.
- **Reason:** Affects a new external integration, persistence (price history), and long-term maintainability — exactly the ADR trigger.

## 19. Open Questions

- **OQ-001:** Does BrickLink's (and BrickOwl's) ToS permit storing and displaying retained price history?
- **OQ-002:** What currency/region granularity do we commit to at launch (USD only, or USD + MXN)?
- **OQ-003:** Who supplies manual snapshots in slice 1 — any authenticated user, or admin-curated?
- **OQ-004:** Is MSRP/RRP already available from a cached source, or does it need a Brickset call?

## 20. Assumptions

- **Assumption-001:** No scraping will be used for the MVP (per policy/ADR-006).
- **Assumption-002:** A single `price_snapshot` schema can serve manual and API-sourced prices with `source`/`currency`/`condition` discriminators.
- **Assumption-003:** Users accept manually contributing some prices early, before automated coverage exists.
- **Assumption-004:** Deal detection can launch on MSRP + accumulated snapshots, with marketplace averages added later.
