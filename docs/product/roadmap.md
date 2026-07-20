# BrickDeck Roadmap

This roadmap defines the planned evolution of BrickDeck from initial side project to a possible commercial product.

---

## Phase 0 — Foundation

Goal: define the project clearly and prepare the repository for development.

### Deliverables

- Repository created
- README completed
- Project structure defined
- Architecture documented
- Claude configuration added
- Environment strategy defined
- Docker Compose planned
- Initial GitHub issues created

### Success Criteria

- Any developer or AI assistant can understand the project vision, scope, and technical direction by reading the documentation.

---

## Phase 1 — Catalog Foundation

Goal: create the base LEGO catalog layer.

### Features

- Search LEGO sets
- View set details
- View set inventory
- Store sets, parts, colors, and themes locally
- Sync catalog data from external API
- Cache external API responses

### Technical Tasks

- Create backend project
- Create frontend project
- Configure PostgreSQL
- Add Flyway migrations
- Add external API client
- Add catalog sync service
- Add basic API endpoints

### Success Criteria

- User can search for a LEGO set and view its parts inventory.

---

## Phase 2 — User Collection

Goal: allow users to manage owned sets and loose pieces.

### Features

- User authentication
- Add set to collection
- Mark set status
- Register purchase price and purchase date
- Add loose pieces manually
- Track quantity by part and color
- Track storage location

### Success Criteria

- User can create a basic digital version of their LEGO collection.

---

## Phase 3 — Missing Pieces Engine

Goal: calculate what a user is missing to complete a set.

### Features

- Select a target set
- Compare required parts vs user inventory
- Show missing pieces
- Show pieces already available
- Show completion percentage

### Success Criteria

- User can know exactly how close they are to completing a specific set.

---

## Phase 4 — Set Comparison Engine

Status: In Progress (backend done).

Goal: compare similar sets across years or versions.

Backend shipped: `GET /api/v1/sets/compare?a=&b=&category=&page=&size=` (public) compares two catalog sets' non-spare inventories and returns a quantity-weighted similarity score (`sum(min)/sum(max)`), categorized per part+color diff lines (ONLY_A/ONLY_B/BOTH) with counts, and paginated lines with an optional category filter. 404 if a set or its inventory is not imported. Frontend compare page still pending.

### Features

- Compare two sets side by side
- Compare metadata
- Compare inventory overlap
- Detect unique pieces
- Detect color changes
- Compare minifigures
- Calculate similarity score
- Generate summary of differences

### Example Use Cases

- Compare a 2023 set vs a 2025 set
- Compare two versions of the same vehicle
- Compare a re-release against the original
- Decide whether a new set is worth buying

### Success Criteria

- User can understand whether a newer version is meaningfully different or mostly the same.

---

## Phase 5 — Build Recommendation Engine

Goal: recommend what the user can build with their existing pieces.

### Features

- Recommend sets user can fully build
- Recommend sets user can almost build
- Rank recommendations by completion percentage
- Show missing parts required
- Suggest efficient purchases to complete multiple builds

### Success Criteria

- User gets practical build ideas based on their actual inventory.

---

## Phase 6 — Price Tracking and Deals

Goal: help users buy smarter.

### Features

- Track prices over time
- Store price snapshots
- Show price history
- Detect real discounts
- Add wishlist alerts
- Compare current price vs historical average

### Technical Considerations

- Prefer official APIs and affiliate feeds.
- Scraping must respect Terms of Service and robots.txt.
- Avoid high-frequency scraping.
- Store only necessary pricing metadata.

### Success Criteria

- User can see whether a current price is actually a good deal.

---

## Phase 7 — AI-Assisted Classification

Goal: help users identify and organize loose pieces using images.

### Features

- Upload photo of one piece
- Suggest part type
- Suggest color
- Show confidence score
- Ask user to confirm result
- Save confirmed piece to inventory

### Technical Considerations

- Spike: `docs/superpowers/specs/2026-07-16-phase7-ai-classification-spike.md` (proposed; decision pending).
- Proposed source: Claude vision via the Anthropic Java SDK, behind a `PartClassifier` port — no Python AI service in this phase.
- Single-part find-or-import is a prerequisite: parts only enter the catalog via set-inventory import today, so suggestions for never-imported parts cannot be saved.
- Every result must carry confidence and require user confirmation. No silent auto-save.
- User photos are transient in the first slice — classify and discard.

### Later Enhancements

- Multi-piece detection
- Automatic grouping
- Storage recommendations
- Similar part detection
- AI explanations for uncertain matches

### Success Criteria

- AI can assist classification without pretending to be perfect.

---

## Phase 8 — Productization

Goal: prepare BrickDeck as a real product.

### Features

- Subscription model
- User limits
- Premium AI features
- Notifications
- Email alerts
- Analytics dashboard
- Deployment automation
- Terms and privacy policy

### Success Criteria

- BrickDeck can be tested by real users outside local development.
