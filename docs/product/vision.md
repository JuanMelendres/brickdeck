# Product Vision

## What BrickDeck Is

BrickDeck is a LEGO collection intelligence platform. It helps collectors and
builders organize the sets they own, catalog loose pieces, understand a set's
full parts inventory, and — over time — compare sets, discover what they can
build, and track prices.

It starts as a personal/portfolio project with a clear path toward a possible
commercial SaaS product.

## The Problem

Most LEGO collectors hit the same recurring problems:

- They own sets but don't know exactly which pieces they have.
- They have loose pieces but don't know which sets or builds they belong to.
- They can't easily tell whether a new set is worth buying versus a previous version.
- They want to know what they can build with leftover pieces.
- They want to be alerted when a set has a genuine discount.

BrickDeck combines structured LEGO catalog data (from Rebrickable), a personal
inventory, comparison logic, and — later — AI-assisted recognition to solve this.

## Target Users

- **Collectors** who want an accurate digital record of owned sets and pieces.
- **Builders / MOC makers** who want to know what they can build from inventory.
- **Deal hunters** who want meaningful price signals, not fake "discount" labels.

## Current Scope (implemented)

- Search LEGO sets (Rebrickable-backed) and view set details.
- Import and view a set's full parts inventory (parts, colors, quantities).
- User authentication (register / login, stateless JWT).
- Add owned sets to a personal collection with status, purchase price, and date.
- Manually track loose pieces by part, color, quantity, and storage location.

See [features.md](./features.md) for the detailed feature list and status.

## Future Scope (planned)

- Missing-pieces engine (how close am I to completing a set?).
- Set comparison engine (version vs version, similarity score).
- Build recommendation engine.
- Price tracking and deal detection.
- AI-assisted piece recognition from photos.

The full phased plan lives in [roadmap.md](./roadmap.md).

## Positioning

> A smart LEGO collection assistant that helps you organize your pieces, compare
> sets, discover what you can build, and buy smarter.

## Product Principles

- Ship useful catalog and collection features before AI.
- Keep external API integrations isolated and cache their data locally.
- Respect store and marketplace Terms of Service (no aggressive scraping).
- Store only the user data that is needed.
- Keep the MVP simple; build with productization in mind, not ahead of it.
