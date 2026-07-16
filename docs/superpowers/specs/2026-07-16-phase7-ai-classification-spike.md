# Technical Spike: Phase 7 AI-Assisted Part Classification

Date: 2026-07-16 · Status: Proposed · Owner: Juan Melendres

## 1. Summary

Phase 7 (AI-Assisted Classification) needs to turn a user photo of a single LEGO piece into candidate part numbers plus a candidate color, each with a confidence score, which the user then confirms before anything is saved. This Spike investigates **how BrickDeck should perform that recognition**, given a Java/Spring backend, no ML infrastructure, no labeled dataset, and an explicit "AI assists, never pretends to be perfect" product rule.

- **Main question:** What is the lowest-cost, lowest-infrastructure way to get useful part/color candidates from a photo, without training a model or building a dataset?
- **Reason:** The catalog, collection, missing-pieces, comparison, recommendation, and pricing features are all keyed on `part_number` + `color_id`. Today the only way a user gets a loose part into their inventory is by typing both identifiers by hand (Phase 2c) — which is the single worst UX in the product and the main barrier to loose-piece inventory adoption.
- **Expected decision:** Pick the recognition source for Phase 7 slice 1, and the shape of the classification contract (request, candidates, confidence, confirm) so a second/better source can be swapped in without reworking the API or the UI.

## 2. Background

- `docs/product/ai-strategy.md` defines four AI levels. **Level 2 (assisted single-piece classification)** is Phase 7: candidate part IDs, candidate colors, confidence score, similar-looking alternatives, user confirmation. Level 3 (multi-piece detection) is explicitly a later premium feature.
- The same doc sketches `Next.js → Spring Boot API → Python AI Service → Model/Vision Pipeline` and says **"the AI service should be optional during early development"** — i.e. the Python hop is a possible destination, not a required first step.
- ADR-006 deferred AI (and scraping) out of the MVP. Phases 1–6 are done, so that deferral has expired on schedule; this Spike is the trigger to supersede the AI half of ADR-006.
- Existing local catalog: `parts` (`external_part_number`, name) and `colors` (`external_id`, name, RGB) tables, populated **only** as a side effect of importing a set's inventory (`SetInventoryService.importInventory`). There is no single-part fetch (`RebrickableClient.getPart/getColor` does not exist — it is a known open item in `.claude/project-state.md`).
- Rebrickable exposes part images (`part_img_url`) in the set-parts payload, but BrickDeck does not currently persist them.
- Precedent for a new external integration: `external.rebrickable` (isolated client + config + DTOs, timeouts, env-var key). Precedent for an AI-adjacent decision record: ADR-011 (price sourcing), which chose a two-track "cheap thing now, better source next" path.

## 3. Problem Statement

We must decide how BrickDeck obtains part/color candidates from a photo. The uncertainty is not "can a model recognize a brick" — it is **which recognition source we can adopt with no dataset, no GPU, no Python service, and predictable per-request cost, and whether its output can be mapped onto our catalog's `part_number` + `color_id` well enough for a confirm-before-save flow to be worth shipping.**

Two sub-problems make this harder than a normal integration:

1. **Identifier grounding.** A generic vision model can describe a brick ("2x4 red plate") but does not natively emit `3020` / color `4`. Any answer must land on identifiers that exist in *our* catalog.
2. **Catalog coverage.** Our `parts`/`colors` tables only hold what previous set imports pulled in. A correct suggestion for a part we have never imported currently 404s at save time (the Phase 2c constraint).

## 4. Goals

- Enumerate candidate recognition sources and classify each by infrastructure cost, dataset requirement, and identifier grounding.
- Compare them on accuracy plausibility, cost per classification, latency, and fit with the confirm-before-save rule.
- Recommend the Phase 7 slice-1 source and a source-agnostic classification contract (endpoint + DTOs).
- Define what must be verified by a small POC before writing production code.
- Decide whether the Python AI service from `ai-strategy.md` is needed now or deferred.

## 5. Non-Goals

- Not implementing the classifier, the endpoint, the upload storage, or the frontend capture UI.
- Not multi-piece detection (Level 3), organization suggestions (Level 4), or the text-assist features (Level 1) — those are separate slices/phases.
- Not building or labeling a training dataset.
- Not choosing a permanent image-storage backend (local disk vs object storage) — that is a slice-1 design question, not a source-selection question.
- Not confirming exact current vendor pricing, rate limits, or accuracy numbers — those are POC verification items, flagged below.

## 6. Key Questions

- **Q-001:** Which sources can emit (or be grounded to) a Rebrickable-compatible `part_number` + `color_id`, versus only a free-text description?
- **Q-002:** What is the per-classification cost and p95 latency of each source, at the image sizes a phone camera produces?
- **Q-003:** Can we avoid a Python service for slice 1 — i.e. is there a source callable directly from Spring Boot with an official Java SDK or plain REST?
- **Q-004:** How do we ground candidates to our catalog when the suggested part has never been imported? (The Phase 2c "missing ref → 404" rule collides with this.)
- **Q-005:** Is the confidence score meaningful enough to drive UI (rank, threshold, "not sure" state), or is it self-reported and uncalibrated?
- **Q-006:** Do the candidate sources' terms permit sending user-submitted photos, and what retention do they impose?

## 7. Constraints

- Stack: Java 21 / Spring Boot 3, PostgreSQL + Flyway, DDD + hexagonal; external integrations isolated in `external.*` (mirror `external.rebrickable`). No Python service exists in the repo today; adding one adds a deployable, a CI job, and a second runtime.
- Product rule: **every visual classification result must include confidence and require confirmation.** No silent auto-save (`ai-strategy.md`, "AI Product Rule").
- Secrets via env vars only; `.env.example` committed, never `.env`.
- Solo project — favor low operational cost, no GPU, no model hosting, no dataset labeling.
- Privacy: user photos are user data. Whatever we send leaves our infrastructure; whatever we store needs a retention answer.
- Catalog coupling: suggestions must resolve to `parts.external_part_number` + `colors.external_id`, and the current inventory-only import path is the sole way those rows appear.

## 8. Options Considered

| Option | Description | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|
| A. Claude vision (`claude-opus-4-8`) via `anthropic-java` | Send the photo to the Messages API with a system prompt describing the task; use **structured outputs** (`output_config.format`) to force a candidate array with `partNumber`/`colorId`/`confidence`. Ground by passing a candidate color enum from our `colors` table. | Official Java SDK — no Python hop; no dataset, no training, no hosting; structured outputs guarantee parseable candidates; naturally emits alternatives + rationale; same shape works later for Level 1 text features; high-res vision (up to 2576px long edge) on Opus 4.7+ | Per-request cost scales with image tokens; part-number accuracy on a 60k-part catalog is unproven (LLMs know common parts, not the long tail); confidence is self-reported, not calibrated | Low–Medium | Medium |
| B. Brickognize (public LEGO recognition API) | Purpose-built third-party LEGO part/set recognition service; POST an image, get ranked part candidates with scores. | Domain-specific — trained on LEGO parts; returns identifiers, not prose; plain REST (no SDK needed); free tier historically available | Third-party availability/ToS/rate limits must be verified; single point of failure with no vendor relationship; identifier namespace mapping to Rebrickable must be verified; color may not be returned at all | Low | Medium–High |
| C. Custom CNN / fine-tune (Python service) | Train or fine-tune an image classifier on part renders, serve from a Python FastAPI service behind the Spring API. | Best possible ceiling; no per-request vendor cost; matches the `ai-strategy.md` target architecture | Needs a dataset (licensing! — `ai-strategy.md` "Dataset Strategy"), labeling, training, GPU, a second deployable, and MLOps. Months, not a slice. | High | High |
| D. Generic cloud vision (Google Vision / AWS Rekognition) | Off-the-shelf label detection. | Cheap, mature, high availability | Not LEGO-aware — returns "toy", "plastic", "block". No identifier grounding at all. Would need C on top of it anyway. | Medium | High (useless output) |
| E. Hybrid: B primary, A re-rank/color | Brickognize for shape candidates; Claude vision to pick the color and re-rank against our catalog. | Plays each to its strength — specialist shape model + language model for color naming and grounding | Two integrations, two failure modes, two ToS reviews, two cost lines; premature before either is proven alone | High | Medium |
| F. Manual entry only (status quo) | Keep Phase 2c typing. | Zero cost, zero risk | Does not deliver Phase 7; leaves the worst UX in the product intact | — | — |

## 9. Evaluation Criteria

| Criterion | Description | Weight |
|---|---|---|
| Identifier grounding | Produces (or can be forced to produce) our `part_number` + `color_id` | High |
| Time-to-first-value | How fast a working, testable slice ships | High |
| Infrastructure cost | No GPU / no dataset / no second deployable | High |
| Accuracy plausibility | Good enough that confirm-before-save is a helpful step, not a chore | High |
| Cost per classification | Predictable, and bounded by client-side downscaling | Medium |
| Latency | Acceptable for an interactive capture → suggest flow | Medium |
| Privacy / ToS | Permits user-submitted photos; retention is answerable | Medium |
| Swappability | Source sits behind a port so a better one replaces it without touching the API/UI | Medium |

## 10. Research Findings

> Findings below combine repo evidence (verifiable) with general platform knowledge (hypotheses). **Vendor accuracy, exact pricing, current ToS, and rate limits are NOT verified here** — see the POC plan. Treat them as things to confirm, not settled facts.

### Finding 1: The confirm-before-save rule means recall matters more than precision

Description: The product rule already forces a human in the loop. A ranked top-5 with the right answer at position 3 is a *success*; a single confident wrong answer is a *failure*.
Evidence: `ai-strategy.md` → "Human-in-the-Loop Design" and "AI Product Rule"; roadmap Phase 7 line ("suggestion + confidence + user confirm").
Impact: This substantially lowers the accuracy bar for slice 1 and favors a source that returns several plausible alternatives with rationale (Option A) over one that must be exactly right.

### Finding 2: A Java-callable source avoids the Python service entirely for slice 1

Description: `ai-strategy.md` sketches a Python AI service, but explicitly marks it optional early. The Anthropic Java SDK (`com.anthropic:anthropic-java`) supports vision input and structured outputs directly from Spring Boot.
Evidence: `ai-strategy.md` → "Recommended AI Architecture" ("The AI service should be optional during early development"); the existing `external.rebrickable` package is the template for a Java-side external adapter.
Impact: Option A ships as one more `external.*` adapter — no new deployable, no new CI job, no second runtime. The Python service becomes a *later* option (needed only if we go to Option C), not a Phase 7 prerequisite.

### Finding 3: Structured outputs solve the grounding problem for colors, but only partly for parts

Description: Colors are a small, closed set (~200 rows in `colors`); we can pass them as an enum and force the model to pick one that exists. Parts are ~60k and cannot be enumerated in a prompt.
Evidence: `colors` / `parts` schema (V4 migration); structured-outputs `enum` support.
Impact: Color grounding is essentially solved (constrain to catalog `external_id`s). Part grounding is best-effort: the model emits candidate part numbers, and **we validate each against `PartRepository` before returning it** — flagging unknown candidates rather than dropping them (see OQ-002). This asymmetry should shape the DTO: `colorId` is a resolved reference, `partNumber` is a *claim* plus a resolution status.

### Finding 4: The catalog-coverage gap (Q-004) is a real blocker and is already a known open item

Description: Even a perfect suggestion for part `3020` fails to save if `3020` was never pulled in by a set import. `.claude/project-state.md` already lists "single-part find-or-import (`RebrickableClient.getPart/getColor`)" as an optional later item.
Evidence: Phase 2c notes ("part+color must be pre-imported in catalog (missing ref → 404)"); project-state "Immediate Next Steps" item 4.
Impact: **Single-part find-or-import is promoted from "optional later" to a Phase 7 dependency.** Without it, the classification flow suggests parts the user cannot then add — the feature would fail on exactly the long-tail loose pieces it exists to serve. It should be its own small slice, sequenced *before* the classifier endpoint.

### Finding 5: Image size is the cost and latency dial, and it lives on the client

Description: Vision cost scales with image tokens; high-res images on Opus 4.7+ can cost roughly 3x a downscaled one. A phone photo of a single brick does not need maximum resolution.
Evidence: General platform knowledge (verify with `count_tokens` on representative photos in the POC).
Impact: Downscale in the browser before upload. This bounds cost, upload time, and latency simultaneously, and it is a frontend decision — flag it in the slice-1 design, and measure before choosing a target resolution rather than guessing.

### Finding 6: Rebrickable already gives us reference images we do not store

Description: The set-parts payload carries `part_img_url`. We normalize the part but drop the image URL.
Evidence: `RebrickablePartResponse` DTO / `Part` entity (no image column).
Impact: Cheap, low-risk enrichment that makes the *confirm* step far better — the user compares their photo against the candidate's reference image instead of trusting a number. Worth folding into slice 1 (a nullable `part_img_url` column) regardless of which recognition source wins.

### Finding 7: Photos are user data and need a retention answer before the first upload

Description: Any option except F sends a user photo to a third party. Option A additionally requires 30-day data retention on some models (Fable 5), which constrains model choice for ZDR-style postures.
Evidence: `ai-strategy.md` "Dataset Strategy" contemplates "user-uploaded confirmed images" as a future dataset — which is a retention decision with consent implications, not just a storage decision.
Impact: Slice 1 should default to **transient** handling (classify, return, discard; no persistence of the raw photo) and defer the "keep confirmed images to improve future classification" idea to an explicit, consented later slice.

## 11. Proof of Concept Plan

Time-box: **one day.** Throwaway branch, no production code, no migration, no endpoint, no UI.

- **Verify (~2h):** Brickognize's current availability, ToS (user-submitted images, commercial use), rate limits, response schema, and whether its part identifiers are Rebrickable-compatible. Confirm Anthropic vision pricing/limits and the org's data-retention posture.
- **Build a mini eval set (~1h):** Photograph **15–20 real bricks** spanning easy (2x4 brick, 1x2 plate) → medium (slopes, tiles, technic pins) → hard (near-identical variants, e.g. 3020 vs 3021 plates; similar reds; a translucent piece). Record ground-truth `part_number` + `color_id` for each. **This eval set is the deliverable that outlives the POC** — the same set validates any future source swap.
- **Prototype A (~2h):** A `main()` (or a single throwaway test) using `anthropic-java` — `claude-opus-4-8`, image + prompt, structured output forcing `[{partNumber, partNameGuess, colorId, confidence, reasoning}]`, color enum seeded from the local `colors` table. Run the eval set. Record: top-1 hit rate, **top-5 hit rate**, color hit rate, p95 latency, tokens/cost per image via `count_tokens`, at two resolutions.
- **Prototype B (~2h):** Same eval set against Brickognize via plain REST. Record top-1 / top-5, latency, and whether color comes back at all.
- **Not built:** No persistence, no controller, no upload endpoint, no frontend, no Flyway migration.
- **Validation gate:** Slice 1 proceeds on **top-5 part hit rate ≥ 60% and color hit rate ≥ 80%** on the eval set, from a source callable without a Python service. Below that, the confirm step is noise and Phase 7 should defer rather than ship a frustrating feature.

## 12. Proof of Concept Results

TODO: POC not executed yet.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
|---|---|---|
| Vendor vision API (A/B) vs own model (C) | Ships in a slice; zero dataset/GPU/MLOps | Per-request cost forever; accuracy ceiling is theirs, not ours; vendor dependency |
| General model (A) vs LEGO specialist (B) | A grounds to our identifiers, returns alternatives + rationale, and has an official Java SDK | B is likely better at pure shape ID on the long tail — where it matters most |
| Java-side adapter vs Python AI service | No second deployable, no new CI job, no new runtime | Closes the door on Option C later — reopening it means the Python hop returns |
| Transient photos vs stored photos | No retention/consent surface; simplest privacy answer | Forfeits the "confirmed images improve future classification" flywheel from `ai-strategy.md` |
| Confidence-gated UI vs always showing top-5 | Fewer bad suggestions surfaced | Self-reported confidence is uncalibrated — a threshold may hide correct answers |
| Single-part find-or-import as a prerequisite | Suggestions become savable; unblocks the long tail | One extra slice before the visible feature lands |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Part-number accuracy is too low on the long tail to be useful | High | Medium | POC gate (§11) before any production code; top-5 + confirm lowers the bar; fall back to B or hybrid E |
| Confidence is self-reported and uncalibrated → misleading UI | Medium | High | Show it as a *rank hint*, never as a truth claim; always show reference images; never gate save on it |
| Suggested part not in local catalog → save 404s | High | High | Sequence single-part find-or-import (Finding 4) **before** the classifier slice |
| Vendor cost per classification is higher than expected | Medium | Medium | Client-side downscale (Finding 5); measure with `count_tokens`; consider a cheaper model for slice 1 |
| Brickognize ToS/rate limits forbid our use | Medium | Medium | Verify in POC before designing around it; A is the fallback that has no such dependency |
| Photo privacy / retention becomes a problem | Medium | Low | Transient-only in slice 1; defer stored-image dataset to a consented slice |
| Contract churn when the source is swapped | Medium | Low | Port/adapter (`PartClassifier` interface) + source-agnostic DTOs from day one; the eval set makes swaps measurable |

## 15. Recommendation

**Recommended: Option A (Claude vision via `anthropic-java`) for slice 1, behind a `PartClassifier` port — with Option B evaluated head-to-head in the POC and kept as the swap-in if it wins on shape accuracy.**

Why:

1. **It is the only option that grounds to our identifiers and needs no new infrastructure.** Structured outputs pin the response shape; a color enum from our `colors` table pins color to a real `colorId`; the Java SDK keeps it inside the existing Spring app as one more `external.*` adapter (Findings 2, 3).
2. **The confirm-before-save rule fits it well.** Ranked alternatives + a rationale are exactly what a human needs to pick correctly, and that is what a language model is good at producing (Finding 1).
3. **It composes forward.** The same adapter later serves Level 1 text features (set comparison summaries, missing-piece prose) with no new integration.

Why not the others: **B** is the strongest technical rival and may well beat A on pure shape ID — but it carries an unverified ToS/availability dependency and probably no color, so it enters as a POC contender, not a default. **C** is the long-term destination from `ai-strategy.md` but is months of dataset/GPU/MLOps work and cannot be a first slice. **D** produces no usable identifiers. **E** (hybrid) is likely correct *eventually*, but committing to two integrations before either is measured is premature. **F** does not deliver the phase.

Sequencing:

1. **Slice 0 — single-part find-or-import** (`RebrickableClient.getPart/getColor` + `PartService`/`ColorService` find-or-import). Small, independently valuable (it also fixes the Phase 2c manual-entry 404), and a hard prerequisite (Finding 4).
2. **Slice 1 — classification backend.** `classification` domain package + `external.anthropic` adapter behind a `PartClassifier` port; `POST /api/v1/classify/part` (authenticated, multipart image) → ranked `PartSuggestion` candidates with `confidence`, `resolutionStatus`, and reference image URLs. Transient photo handling. Fold in the nullable `part_img_url` column (Finding 6).
3. **Slice 2 — frontend capture + confirm.** Client-side downscale, capture/upload, candidate list with reference images + confidence, confirm → existing `POST /api/v1/collection/parts`.

Assumptions supporting this: the POC clears the §11 gate; the classification contract can be source-agnostic (Q-001/Q-003); users accept a confirm step.

## 16. Decision

Decision pending. (Recommendation proposed for review; confirm before running the POC.)

## 17. Next Steps

1. Approve the direction and the §11 validation gate (top-5 ≥ 60% part, ≥ 80% color).
2. Build the 15–20 brick eval set — it is reusable and outlives this Spike.
3. Run the one-day POC (A vs B on the same eval set); record results in §12.
4. Record the decision as an ADR (§18) and supersede the AI half of ADR-006.
5. Write a TDD for **slice 0** (single-part find-or-import), then one for slice 1 (classification contract + adapter).
6. Update `docs/product/roadmap.md`, `.claude/roadmap.md`, and `.claude/project-state.md` Phase 7 with the chosen approach.

## 18. ADR Candidate

- **ADR needed:** Yes.
- **Suggested title:** ADR-012 — AI part classification: vendor vision API first, no Python AI service in Phase 7.
- **Reason:** Introduces a new external integration and a recurring per-request cost, supersedes the AI half of ADR-006, and decides against (defers) the Python AI service in `ai-strategy.md`'s target architecture — an architecture + long-term-maintainability decision, which is exactly the ADR trigger.

## 19. Open Questions

- **OQ-001:** Do Brickognize's current ToS and rate limits permit our use, and are its part identifiers Rebrickable-compatible?
- **OQ-002:** When a suggested part number does not exist locally, do we (a) find-or-import it on the fly (needs slice 0), (b) return it flagged as unresolved, or (c) drop it? (Recommendation leans (a)+(b).)
- **OQ-003:** Do we store the user photo at all in slice 1, or classify-and-discard? (Recommendation: discard.)
- **OQ-004:** What model tier do we run — `claude-opus-4-8` for accuracy, or a cheaper tier if the POC shows it suffices?
- **OQ-005:** Does the frontend downscale before upload, and to what resolution? (Measure in the POC; don't guess.)
- **OQ-006:** Is there a rate/quota limit per user to bound cost abuse on an authenticated endpoint?

## 20. Assumptions

- **Assumption-001:** No dataset will be built or labeled for Phase 7 (rules out Option C as a first slice).
- **Assumption-002:** A single `PartClassifier` port can serve a general vision model, a LEGO specialist API, or a future in-house model without changing the REST contract.
- **Assumption-003:** Users will accept — and benefit from — a confirm step; a ranked top-5 with reference images beats typing a part number blind.
- **Assumption-004:** Single-part find-or-import is feasible against Rebrickable's part/color endpoints (needs a client-method check, not just an assumption).
- **Assumption-005:** Per-classification vendor cost stays low enough at downscaled resolution that no per-user quota is required in slice 1 (verify in POC; OQ-006).
