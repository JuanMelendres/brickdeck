# BrickDeck Architecture

BrickDeck should be designed as a modular application with clear separation between product areas: catalog, collection, comparison, recommendations, pricing, and AI.

---

## High-Level Architecture

```text
                 ┌────────────────────┐
                 │    Next.js Web     │
                 │  React + TypeScript│
                 └─────────┬──────────┘
                           │ REST/JSON
                 ┌─────────▼──────────┐
                 │  Spring Boot API   │
                 │ Java 21 / REST API │
                 └─────────┬──────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
┌─────────▼───────┐ ┌──────▼───────┐ ┌──────▼─────────┐
│  PostgreSQL DB  │ │ External APIs│ │   AI Service   │
│ Catalog + User  │ │ Rebrickable  │ │ Python optional│
└─────────────────┘ │ BrickLink etc│ └────────────────┘
                    └──────────────┘
```

---

## Recommended Repository Structure

```text
brickdeck/
├── apps/
│   └── web/
│       ├── src/
│       ├── public/
│       └── package.json
│
├── services/
│   ├── api/
│   │   ├── src/main/java/
│   │   ├── src/main/resources/
│   │   ├── src/test/java/
│   │   └── build.gradle or pom.xml
│   │
│   └── ai-service/
│       ├── app/
│       ├── notebooks/
│       ├── models/
│       └── requirements.txt
│
├── packages/
│   └── shared/
│
├── docs/
│   ├── ROADMAP.md
│   ├── ARCHITECTURE.md
│   ├── API_STRATEGY.md
│   ├── SCRAPING_POLICY.md
│   └── AI_STRATEGY.md
│
├── infra/
│   ├── docker-compose.yml
│   └── scripts/
│
├── postman/
│   └── brickdeck.postman_collection.json
│
├── .env.example
├── CLAUDE.md
└── README.md
```

---

## Backend Modules

Recommended Spring Boot package structure:

```text
com.brickdeck
├── catalog
│   ├── set
│   ├── part
│   ├── color
│   └── theme
├── collection
│   ├── userset
│   └── userpart
├── comparison
├── recommendation
├── pricing
├── integration
│   ├── rebrickable
│   ├── bricklink
│   └── brickset
├── security
├── common
└── config
```

---

## Database Areas

### Catalog Tables

- sets
- themes
- parts
- colors
- set_inventories
- minifigures

### User Tables

- users
- collections
- user_sets
- user_parts
- storage_locations

### Comparison Tables

- set_comparisons
- set_comparison_results

### Recommendation Tables

- build_recommendations
- recommendation_results

### Pricing Tables

- stores
- products
- price_snapshots
- deal_alerts

---

## External Integrations

### Rebrickable

Primary source for:

- Sets
- Parts
- Colors
- Inventories
- MOCs and alternate builds, if available

### BrickLink

Possible future source for:

- Marketplace prices
- Seller inventory
- Part availability

### Brickset

Possible future source for:

- Set metadata
- Themes
- Release information
- Additional collector context

---

## API Design Principles

- Keep external API clients isolated from domain services.
- Do not expose raw third-party API models directly to the frontend.
- Normalize external data into internal models.
- Cache catalog data locally.
- Use background jobs for large sync operations.
- Track source and sync timestamp for imported data.

---

## AI Service Strategy

The AI service should be separate from the main API until the model and feature value are proven.

Initial AI service responsibilities:

- Image upload handling
- Image preprocessing
- Piece classification prototype
- Confidence scoring
- Candidate part suggestions

The Spring Boot API should remain the source of truth for user inventory and catalog data.

---

## Security Principles

- Never commit API keys.
- Use environment variables for secrets.
- Add `.env.example` only with placeholder values.
- Validate all user input.
- Rate-limit public endpoints when deployed.
- Avoid storing unnecessary personal data.

---

## Performance Principles

- Cache frequently accessed catalog data.
- Avoid calling external APIs on every user request.
- Use pagination for large inventories.
- Use batch jobs for catalog sync.
- Consider indexes on set number, part number, color, theme, and user inventory.
