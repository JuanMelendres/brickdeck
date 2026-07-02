# BrickDeck Project Decisions

This file records important product and technical decisions.

---

## Decision 001 — Start with Documentation First

Status: Accepted

BrickDeck will start with documentation before coding.

Reason:

- The idea has multiple possible directions.
- Documentation helps prevent scope creep.
- AI coding assistants need strong context.
- The project may later become commercial.

---

## Decision 002 — Start with Catalog and Inventory Before AI

Status: Accepted

BrickDeck will not start with image recognition.

Reason:

- AI piece classification is valuable but complex.
- The app needs catalog and inventory data first.
- Manual inventory and set comparison already provide value.
- AI can be added later as a premium feature.

---

## Decision 003 — Use Rebrickable as First Catalog Source

Status: Accepted

Rebrickable will be the first external catalog source to evaluate and integrate.

Reason:

- It provides structured LEGO set, part, color, and inventory data.
- It fits the MVP requirements.
- It can support build recommendation features.

---

## Decision 004 — Treat Scraping as a Later Feature

Status: Accepted

Price tracking is important, but scraping will not be part of the MVP.

Reason:

- Scraping has legal, ethical, and maintenance risks.
- Store layouts change frequently.
- Some stores offer APIs or affiliate feeds.
- Manual price tracking can validate the idea first.

---

## Decision 005 — Use a Modular Monorepo

Status: Proposed

Recommended structure:

- Next.js frontend
- Spring Boot backend
- Optional Python AI service
- Shared documentation and infrastructure

Reason:

- Similar to BrewDeck.
- Easy to manage locally.
- Allows gradual growth.
