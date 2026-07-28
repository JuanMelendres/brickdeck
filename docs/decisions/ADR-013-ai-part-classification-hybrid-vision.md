# ADR-013: AI Part Classification — Hybrid Vendor Vision (Brickognize + Gemini), No Python AI Service

## Status
Accepted

## Date
2026-07-28

## Context
Phase 7 (AI-Assisted Classification) needs to turn a user's photo of a single loose
LEGO piece into candidate `part_number` + `color_id` suggestions with a confidence
score, confirmed by the user before anything saves (`docs/product/ai-strategy.md`
"AI Product Rule"). ADR-006 deferred vision AI out of the MVP; Phases 1–6 are done,
so that deferral has expired on schedule.

The Phase 7 spike
(`docs/superpowers/specs/2026-07-16-phase7-ai-classification-spike.md`) recommended
Claude vision (`anthropic-java`) for slice 1, with Brickognize (a LEGO-specialist
recognition API) benchmarked head-to-head in a one-day POC, gated on top-5 part hit
rate >= 60% and color hit rate >= 80% on a small eval set.

The POC ran 2026-07-28 against 18 real Rebrickable part+color combinations (easy →
hard, including near-identical size variants and a translucent piece). Two changes
from the spike's plan:

1. **Claude vision was replaced with Google Gemini** (`gemini-flash-latest`, AI
   Studio free tier) for Option A. This is a personal, no-budget project; Anthropic
   requires a prepaid credit balance with no free tier, while Gemini's AI Studio
   tier is free (rate-limited to 5 requests/minute on this model).
2. **The eval set used Rebrickable's own catalog renders** (`part_img_url`) instead
   of physically photographed bricks, for speed. This is a known methodology gap
   (see Finding 8, spike §12) — the renders' glossy look plausibly biased the model
   toward guessing translucent (`Trans-`) variants of solid colors.

## Decision
Ship Phase 7 slice 1 as a **hybrid of two free/low-cost vendor APIs**, no Python AI
service, behind a source-agnostic `PartClassifier` port:

- **Part-number candidates: Brickognize** (`POST /predict/parts/`, public, free, no
  key). LEGO-specialist, POC part hit rate 77.8% exact / 94.4% base-normalized
  (mold-suffix-tolerant) — beat Gemini on pure shape id, as expected for a
  domain-specialist model. Returns no color at all (structural), so it cannot be
  the sole source.
- **Color candidates: Google Gemini** (`gemini-flash-latest`, free tier), structured
  JSON output constrained to a closed enum of the local `colors` table's names.
  POC color hit rate 72.2%, below the spike's 80% gate.

**The gate is treated as advisory, not a hard blocker, for this decision:** the
product's confirm-before-save rule already puts a human in the loop with a
reference image, so a 72.2% automated color hit rate is workable rather than
disqualifying — the user corrects the ~28% of misses instead of the feature
silently saving a wrong color. This is an explicit risk acceptance, not a claim
that the gate was met.

Part-number grounding follows the spike's asymmetric design: `colorId` is a
resolved catalog reference (constrained by the enum), `partNumber` is a claim
validated against `PartRepository` — unresolved candidates are flagged, not
dropped (needs slice 0's find-or-import, already shipped).

No Python AI service is introduced. Both sources are plain REST, callable directly
from Spring Boot as two more `external.*` adapters (mirrors `external.rebrickable`).

This supersedes the AI half of ADR-006.

## Consequences
- Positive: zero recurring vendor cost at current volumes (both sources are free
  tier); no new deployable, CI job, or runtime.
- Positive: swapping either source later (e.g. Claude vision once budget allows) is
  isolated to one adapter behind `PartClassifier` — the port/DTO contract doesn't
  change.
- Negative: two external dependencies instead of one — two ToS postures, two
  failure modes, two ToS/availability risks to monitor (Brickognize in particular
  has no formal published ToS found; low-volume personal use is the accepted risk).
- Negative: Gemini's free tier is rate-limited (5 req/min on `gemini-flash-latest`
  at POC time) — slice 1 needs a request queue/backoff, not just a retry-once.
- Negative: the 72.2% color hit rate is unverified against real photos (Finding 8);
  actual production accuracy on user-submitted phone photos may differ from the
  POC's render-based estimate, in either direction.
- Deferred: re-running the color check against a handful of real brick photos to
  confirm or refute the render-artifact theory — cheap, not blocking, worth doing
  before or shortly after slice 1 ships.

## Alternatives Considered
- **Claude vision (`anthropic-java`), per the original spike recommendation:**
  rejected for now on cost grounds — no free tier, requires prepaid credits, not
  justified for a personal project at this stage. Remains the natural upgrade path
  if usage or budget changes; the `PartClassifier` port keeps that swap cheap.
- **Brickognize alone:** rejected — returns no color at all, and color is a
  required half of the classification contract.
- **Gemini alone:** rejected for slice 1 — Brickognize measurably beat it on pure
  part-shape accuracy in the POC (77.8%/94.4% vs 72.2%/77.8%), and Brickognize's
  part-only result is free to keep alongside Gemini's color result.
- **Wait for a real-photo POC re-run before deciding:** considered; rejected as
  overly conservative given confirm-before-save already bounds the downside of a
  wrong automated suggestion, and re-verification can happen non-blocking.
- **Defer Phase 7 entirely until gate is formally met:** rejected — same reasoning;
  the gate exists to prevent shipping a *frustrating*, unconfirmed feature, and
  confirm-before-save already prevents that regardless of the exact hit rate.
- **Custom CNN / Python AI service:** ruled out as a first slice per the spike —
  no dataset, no GPU, no MLOps budget for a solo project.

## Notes
Spike: `docs/superpowers/specs/2026-07-16-phase7-ai-classification-spike.md` (§12
has the full POC results table and raw findings).
Slice 0 (single-part find-or-import, prerequisite for saving classifier
suggestions): shipped 2026-07-28, see `.claude/project-state.md`.
Next: slice 1 TDD — `classification` package + `external.brickognize` +
`external.gemini` adapters behind `PartClassifier`, `POST /api/v1/classify/part`,
transient photo handling, nullable `part_img_url` column.
