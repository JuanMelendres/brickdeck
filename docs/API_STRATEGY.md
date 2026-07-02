# BrickDeck API Strategy

BrickDeck should rely on external LEGO-related data sources where possible instead of manually building the full catalog from scratch.

---

## Primary API Candidate: Rebrickable

Rebrickable should be the first integration because it provides structured data for:

- LEGO sets
- Parts
- Colors
- Set inventories
- Themes
- Minifigures
- User-related inventory features, depending on API usage

Recommended first use cases:

- Search sets
- Fetch set details
- Fetch set inventory
- Fetch part details
- Fetch colors
- Sync catalog data into local database

---

## Secondary API Candidate: BrickLink

BrickLink can be evaluated later for marketplace-related functionality.

Possible use cases:

- Price estimates
- Part availability
- Marketplace listings
- Store inventory

BrickLink should not be part of the initial MVP unless marketplace price data becomes a core requirement early.

---

## Secondary API Candidate: Brickset

Brickset can be evaluated for additional set metadata and collector-focused information.

Possible use cases:

- Theme metadata
- Release year
- Set descriptions
- Collection context

---

## Local Data Strategy

BrickDeck should not depend on live external API calls for every request.

Preferred approach:

1. Fetch data from external API.
2. Normalize it into internal models.
3. Store it locally.
4. Use local data for user-facing reads.
5. Refresh data through scheduled sync jobs.

---

## Sync Strategy

### Initial MVP Sync

- Sync only searched sets on demand.
- Store set, parts, colors, and inventory data locally.
- Avoid full catalog sync at the beginning.

### Later Sync

- Add scheduled jobs.
- Add manual admin sync.
- Add full catalog import if API limits and licensing allow it.

---

## API Key Management

All API keys must be configured through environment variables.

Do not commit real API keys.

Use `.env.example` with placeholders only:

```env
REBRICKABLE_API_KEY=your_api_key_here
BRICKLINK_CONSUMER_KEY=your_consumer_key_here
BRICKLINK_CONSUMER_SECRET=your_consumer_secret_here
BRICKSET_API_KEY=your_api_key_here
```

---

## Internal API Examples

Possible backend endpoints:

```text
GET    /api/catalog/sets/search?query=x-wing
GET    /api/catalog/sets/{setNumber}
GET    /api/catalog/sets/{setNumber}/inventory
POST   /api/collections/sets
GET    /api/collections/sets
POST   /api/collections/parts
GET    /api/collections/parts
POST   /api/comparisons/sets
GET    /api/recommendations/builds
GET    /api/prices/sets/{setNumber}
POST   /api/alerts/deals
```

---

## API Design Rules

- Never expose third-party response models directly.
- Use DTOs for frontend responses.
- Use domain services for business logic.
- Add tests for mapping external API data.
- Add retry and timeout logic for external calls.
- Add rate-limit awareness.
- Add source tracking to imported records.
