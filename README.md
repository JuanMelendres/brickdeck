# BrickDeck

BrickDeck is a LEGO collection intelligence platform designed to help collectors, builders, and hobbyists organize sets, classify loose pieces, compare set versions, discover build possibilities, and track deals across stores.

The long-term goal is to combine structured LEGO catalog data, personal inventory management, price tracking, and AI-assisted piece recognition into a practical product that can evolve from a personal side project into a commercial SaaS platform.

---

## Vision

Most LEGO collectors eventually face the same problems:

- They own sets but do not know exactly which pieces they have.
- They have loose pieces but do not know what sets or builds they belong to.
- They want to know whether a new set is actually worth buying compared with a previous version.
- They want to discover what they can build with leftover pieces.
- They want alerts when a set has a real discount.
- They need a better way to organize, classify, and understand their collection.

BrickDeck aims to solve this with a combination of catalog APIs, local inventory data, comparison logic, recommendation algorithms, and AI.

---

## Core Features

### 1. Collection Management

Users can register the LEGO sets they own, including quantity, condition, purchase price, store, purchase date, and status.

Possible statuses:

- Owned
- Wishlist
- Missing pieces
- For sale
- Sold
- In progress
- Archived

---

### 2. Set Inventory Import

BrickDeck should integrate with public LEGO catalog data providers such as Rebrickable and, potentially, BrickLink or Brickset.

Main imported data:

- Set number
- Set name
- Theme
- Year
- Piece count
- Minifigures
- Colors
- Part inventory
- Alternate builds or related MOCs when available

---

### 3. Loose Piece Inventory

Users can register loose pieces manually or through AI-assisted image recognition.

Tracked piece attributes:

- Part number
- Part name
- Color
- Quantity
- Condition
- Storage location
- Source set, when known
- Notes

---

### 4. Build Recommendation Engine

Given the user's loose pieces and owned sets, BrickDeck should recommend:

- Sets the user can fully rebuild
- Sets the user can almost rebuild
- Missing pieces required to complete a set
- MOCs that can be built with existing pieces
- Similar builds based on available inventory

---

### 5. Set Comparison

BrickDeck should allow users to compare different versions of similar sets, for example:

- 2023 version vs 2025 version
- Previous generation vs current generation
- Similar sets from the same theme
- Original vs re-release

Comparison metrics:

- Piece count
- Price
- Price per piece
- Shared pieces
- Unique pieces
- Color differences
- Minifigure differences
- Build similarity score
- Exclusive or rare pieces
- Visual/design notes
- Release year
- Estimated value

---

### 6. Price Tracking and Deals

BrickDeck should track prices across supported stores and notify users when a set has a real discount.

Possible sources:

- Official LEGO Store
- Amazon
- Walmart
- Liverpool
- Mercado Libre
- BrickLink
- Other regional stores, where legally and technically allowed

Important: scraping should respect each site's Terms of Service and robots.txt. Prefer official APIs, affiliate APIs, or lightweight public price checks when possible.

---

### 7. AI-Assisted Piece Recognition

Future AI functionality can help users classify pieces from photos.

Possible AI capabilities:

- Detect a single LEGO piece from an image
- Detect color and part type
- Suggest likely part IDs
- Detect multiple pieces in a photo
- Group pieces by type or color
- Suggest storage categories
- Recommend possible sets based on detected pieces

This should be treated as an advanced feature after the core catalog and inventory system are stable.

---

## Suggested Tech Stack

### Frontend

- Next.js
- React
- TypeScript
- Tailwind CSS
- shadcn/ui

### Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Batch, optional for catalog sync jobs

### Database

- PostgreSQL 16+
- Flyway for migrations

### Infrastructure

- Docker Compose for local development
- GitHub Actions for CI
- Dependabot for dependency updates
- SonarQube or SonarCloud for quality checks

### AI / ML

- Python service for image processing and ML experiments
- OpenCV for preprocessing
- PyTorch or TensorFlow for model experiments
- Vector embeddings for similarity search
- Optional dedicated ML microservice once needed

---

## Proposed Architecture

```text
brickdeck/
├── apps/
│   └── web/                  # Next.js frontend
├── services/
│   ├── api/                  # Spring Boot backend
│   └── ai-service/           # Optional Python AI service
├── packages/
│   └── shared/               # Shared types, schemas, constants
├── docs/                     # Documentation and planning
├── infra/                    # Docker, deployment, scripts
├── postman/                  # API collections
└── README.md
```

---

## Initial Domain Model

Core entities:

- User
- Collection
- Set
- Theme
- Part
- Color
- SetInventory
- UserSet
- UserPart
- Store
- PriceSnapshot
- DealAlert
- SetComparison
- BuildRecommendation

---

## MVP Scope

The first version should avoid overbuilding. The goal is to deliver a working product that proves the core idea.

### MVP Features

- User can search LEGO sets.
- User can save sets to their collection.
- User can view the full inventory of a set.
- User can manually register loose pieces.
- User can compare two sets.
- User can see missing pieces needed to complete a set.
- User can receive simple build recommendations based on owned pieces.
- Admin/catalog job can sync selected sets and parts from external APIs.

### Not MVP Yet

- Full AI image recognition
- Aggressive scraping
- Marketplace transactions
- Public social features
- Mobile app
- Advanced monetization

---

## Roadmap

### Phase 0 — Project Setup

- Create repository
- Add README, roadmap, architecture, and Claude configuration
- Define stack and folder structure
- Configure Docker Compose
- Configure backend and frontend base projects
- Add CI pipeline

### Phase 1 — Catalog Foundation

- Integrate external catalog API
- Create database schema for sets, parts, colors, and inventories
- Add catalog search
- Add set detail page
- Add catalog sync jobs

### Phase 2 — User Collection

- Add authentication
- Add user collections
- Add owned sets
- Add wishlist
- Add manual loose piece inventory
- Add missing piece calculation

### Phase 3 — Set Comparison

- Compare two sets by metadata
- Compare inventories
- Calculate shared and unique parts
- Calculate similarity score
- Show version differences clearly

### Phase 4 — Build Recommendations

- Recommend sets users can complete
- Recommend sets with few missing pieces
- Recommend MOCs or alternate builds when data is available
- Show required missing parts

### Phase 5 — Price Tracking

- Add store model
- Add price snapshots
- Add price history
- Add deal alerts
- Integrate safe sources first
- Evaluate scraping only where allowed

### Phase 6 — AI Features

- Build image dataset strategy
- Add piece recognition prototype
- Add AI-assisted classification
- Add confidence scoring
- Add human confirmation flow
- Improve recommendations using AI explanations

### Phase 7 — Productization

- Add subscription tiers
- Add user limits
- Add premium AI features
- Add notifications
- Add analytics
- Add deployment pipeline

---

## Monetization Ideas

Possible future monetization:

- Free tier for small collections
- Premium tier for large collections
- AI piece recognition as premium feature
- Deal alerts as premium feature
- Advanced comparison reports
- Affiliate links for stores
- Collector valuation dashboard

---

## Product Positioning

BrickDeck is not just a LEGO catalog.

It should be positioned as:

> A smart LEGO collection assistant that helps you organize your pieces, compare sets, discover what you can build, and buy smarter.

---

## Development Principles

- Start with useful catalog and collection features before AI.
- Keep external API integrations isolated.
- Cache external data locally.
- Respect store and marketplace Terms of Service.
- Avoid storing unnecessary user data.
- Build with productization in mind, but keep the MVP simple.
- Prefer clean architecture over quick hacks.
- Document decisions early.

---

## License

To be defined.
