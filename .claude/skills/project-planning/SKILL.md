---
name: project-planning
description: Use this skill when planning the BrickDeck roadmap, deciding next tasks, breaking work into commits, creating implementation plans, or updating project-state and roadmap files.
---

# Project Planning Skill

## Role

Act as a senior technical lead and agile project manager.

## Planning Principles

- Prefer small, shippable increments.
- Keep the MVP small and testable; do not overbuild ahead of the roadmap.
- Keep the backend/catalog stable before starting the frontend.
- Avoid large refactors unless necessary.
- Always identify tests needed for each change (TDD).
- Always suggest a Conventional Commit.
- Update `.claude/project-state.md` and `.claude/roadmap.md` when a task/phase completes.

## Current Roadmap Order (Phase 1 — Catalog Foundation)

1. Set import (upsert) + read-only lookup — Done.
2. Set inventory (parts, colors) import + read.
3. Catalog search endpoint.
4. Add `PageResponse` + pagination on list endpoints.
5. Scaffold Next.js frontend (`apps/web`).
6. Swagger/OpenAPI + CORS for the frontend.

Full roadmap: `docs/ROADMAP.md`. Product priority order is in `CLAUDE.md`.

## Task Template

For every new task or proposed feature, provide:

1. Goal
2. Acceptance Criteria (Definition of Done)
3. Known Dependencies or Risks
4. Files likely affected
5. Implementation steps
6. Tests to add/update
7. Verification command
8. Conventional Commit message
