# ADR-006: Defer Scraping and AI to Later Phases

## Status
Accepted

## Date
2026-07-02

## Context
Two of BrickDeck's most ambitious features — price tracking via scraping and
AI-assisted piece recognition — are high-value but high-risk (legal, ethical,
accuracy, and maintenance cost). The MVP must prove the core catalog + collection
idea first.

## Decision
Do not build web scraping or AI image recognition in the MVP. Prefer official APIs
and affiliate feeds for pricing when that phase arrives, respecting each site's
Terms of Service and `robots.txt`. Treat AI as an advanced feature after the
catalog, inventory, comparison, and recommendation systems are stable; the first
AI feature should be text-based, not vision-based.

## Consequences
- Positive: lower legal/maintenance risk; faster path to a usable product.
- Positive: keeps the MVP focused and testable.
- Negative: price tracking and recognition are delayed.

## Alternatives Considered
- Scraping in the MVP: rejected — fragile and legally risky.
- Vision AI first: rejected — complex, dataset/licensing constraints, high cost.

## Notes
Detailed policy: [product/pricing-scraping-policy.md](../product/pricing-scraping-policy.md)
and [product/ai-strategy.md](../product/ai-strategy.md).
