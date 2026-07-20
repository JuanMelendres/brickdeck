# ADR-011: Price Data Sourcing — User-Submitted Snapshots First, BrickLink API Next

## Status
Accepted

## Date
2026-07-15

## Context
Phase 6 (Price Tracking and Deals) needs set prices to store snapshots, build
history, and detect real discounts. Rebrickable — our catalog source (ADR-005) —
provides no pricing. The scraping policy (`docs/product/pricing-scraping-policy.md`)
and ADR-006 require official APIs / feeds / user-submitted data before scraping, and
state the MVP must not depend on scraping. Deal quality is defined against historical
average / lowest observed / MSRP / price-per-piece, so a time series of snapshots is
required — not a single live price.

The Phase 6 price-data-sources spike
(`docs/superpowers/specs/2026-07-15-phase6-price-data-sources-spike.md`) compared
user-submitted snapshots, BrickLink Price Guide API, BrickOwl API, Brickset RRP,
retail/affiliate APIs, and scraping.

## Decision
Source prices in two tracks:

1. **Now — user-submitted / manual snapshots + MSRP baseline.** Introduce a
   **source-agnostic `price_snapshot` model** (`set`, `source`, `currency`,
   `amount`, `condition` new/used, `observed_at`, optional `store`/`url`) and a
   deal-detection engine (current vs stored history / MSRP / price-per-piece).
   No external dependency; fully policy-compliant.
2. **Next — BrickLink Price Guide API** as the first automated source, added as an
   isolated `external.bricklink` adapter (mirroring `external.rebrickable`) that
   writes the **same** `price_snapshot` model — **only after** a time-boxed POC
   confirms BrickLink's Terms of Service permit storing/retaining prices.

Scraping remains excluded from the MVP.

## Consequences
- Positive: Phase 6 slice 1 ships now with zero legal/approval risk; the schema and
  deal engine are built once and reused by later sources.
- Positive: adding BrickLink (or BrickOwl) later is an isolated adapter, not a
  rewrite — `source`/`currency`/`condition` discriminators absorb new sources.
- Negative: coverage is sparse until users contribute prices or an API lands; early
  "deal" signals lean on MSRP and user targets.
- Negative: BrickLink integration is gated on a ToS check that may block price
  retention, in which case we stay on user-submitted history + MSRP.

## Alternatives Considered
- **BrickLink/BrickOwl API first:** richer market context, but needs auth + a ToS
  check on storage before any code — not a safe *first* slice.
- **Brickset RRP as a standalone source:** RRP is launch retail, not live market
  price; folded into slice 1 as the MSRP baseline instead.
- **Retail/affiliate APIs (Amazon PA-API, LEGO, Walmart, Mercado Libre):** gated on
  program approval / qualifying sales; revisit after user traction.
- **Scraping:** excluded by the scraping policy and ADR-006 for the MVP.

## Notes
Spike: `docs/superpowers/specs/2026-07-15-phase6-price-data-sources-spike.md`.
Slice-1 design: `docs/superpowers/specs/2026-07-15-price-tracking-slice1-design.md`.
Open items tracked in the spike (OQ-001 BrickLink retention rights; OQ-002 launch
currency scope; OQ-003 who submits snapshots).
