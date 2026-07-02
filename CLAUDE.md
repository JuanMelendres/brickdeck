# CLAUDE.md — BrickDeck Project Context

This file provides project context and working rules for Claude or any AI coding assistant contributing to BrickDeck.

---

## Project Summary

BrickDeck is a LEGO collection intelligence platform.

It helps users:

- Organize LEGO sets and loose pieces
- Import set and part data from external catalog APIs
- Compare different versions of similar sets
- Find missing pieces required to complete builds
- Discover what sets or MOCs they can build with existing pieces
- Track prices and deals
- Eventually use AI to identify and classify LEGO pieces from photos

The project is intended to start as a side project but should be designed with enough quality to become a commercial SaaS product later.

---

## Current Project Phase

Current phase: **Phase 0 — Foundation**

Primary goals:

- Define the product vision
- Create the repository structure
- Add base documentation
- Configure local development
- Decide the initial architecture
- Start MVP implementation carefully

Do not overbuild advanced features before the foundation is stable.

---

## Product Priorities

Build in this order:

1. Catalog search and set details
2. Set inventory import
3. User collection management
4. Loose piece manual inventory
5. Missing piece calculation
6. Set comparison
7. Build recommendations
8. Price tracking
9. AI-assisted classification

AI image recognition is important but should not be implemented before the catalog and inventory system are working.

---

## Suggested Tech Stack

Frontend:

- Next.js
- React
- TypeScript
- Tailwind CSS
- shadcn/ui

Backend:

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- Flyway

Database:

- PostgreSQL 16+

Infrastructure:

- Docker Compose
- GitHub Actions
- Dependabot
- SonarQube or SonarCloud

AI service, later:

- Python
- FastAPI
- OpenCV
- PyTorch or TensorFlow

---

## Architecture Rules

- Keep frontend, backend, and AI service separated.
- Keep external API clients isolated inside integration packages.
- Do not expose third-party API responses directly to the frontend.
- Normalize external catalog data into internal database models.
- Cache external data locally.
- Use background jobs for sync operations when needed.
- Keep the MVP small and testable.

---

## Backend Package Direction

Recommended package structure:

```text
com.brickdeck
├── catalog
├── collection
├── comparison
├── recommendation
├── pricing
├── integration
├── security
├── common
└── config
```

---

## Coding Standards

- Prefer clear, boring, maintainable code.
- Avoid unnecessary abstractions.
- Add tests for business logic.
- Add integration tests for external API mapping when possible.
- Use DTOs for API responses.
- Use entities only for persistence.
- Keep controllers thin.
- Keep business logic in services.
- Validate inputs.
- Handle external API errors gracefully.

---

## Database Rules

- Use Flyway migrations.
- Do not modify applied migrations after they are committed.
- Use UUIDs for primary keys unless there is a strong reason not to.
- Use unique constraints for set numbers, part numbers, and color IDs where appropriate.
- Add indexes for frequent lookups.
- Track external source and sync timestamp for imported data.

---

## External API Rules

- Start with Rebrickable as the primary catalog source.
- Keep API keys in environment variables.
- Never commit real secrets.
- Add `.env.example` with placeholder values only.
- Add timeouts and retries for external calls.
- Respect API rate limits.
- Cache external data locally.

---

## Scraping Rules

- Do not implement scraping as part of the initial MVP.
- Prefer official APIs, affiliate feeds, marketplace APIs, or user-entered prices.
- Before scraping any site, review Terms of Service and robots.txt.
- Do not bypass anti-bot systems.
- Do not scrape aggressively.

---

## AI Rules

- Do not implement computer vision before the basic product is usable.
- Start with AI-generated text summaries before image recognition.
- For visual classification, always return confidence scores.
- Always require user confirmation before saving uncertain AI results.
- Treat AI as an assistant, not as a source of absolute truth.

---

## MVP Definition

The MVP is complete when a user can:

- Search a LEGO set
- View set details and parts inventory
- Add the set to their collection
- Add loose pieces manually
- Compare two sets
- See missing pieces for a target set
- Receive basic build recommendations

---

## Things Not to Do Yet

Do not start with:

- Full marketplace support
- Aggressive scraping
- Mobile app
- Complex social features
- Large AI model training
- Paid subscriptions
- Public launch infrastructure

These are later phases.

---

## Communication Style for AI Assistant

When helping with this project:

- Be direct and practical.
- Explain trade-offs clearly.
- Prefer incremental implementation.
- Point out risks early.
- Suggest production-ready patterns when useful.
- Avoid unnecessary complexity.
- Keep the project aligned with the roadmap.
