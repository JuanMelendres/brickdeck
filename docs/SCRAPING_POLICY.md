# BrickDeck Scraping and Price Tracking Policy

BrickDeck may eventually track prices and deals, but scraping must be handled carefully and responsibly.

---

## Core Principle

Prefer official APIs, affiliate feeds, public product feeds, or manually supported integrations before scraping.

Scraping should never be the first option.

---

## Allowed Direction

Price tracking can be implemented through:

- Official store APIs
- Affiliate APIs
- Public product feeds
- Marketplace APIs
- User-submitted prices
- Low-frequency public page checks, only when allowed

---

## Stores to Evaluate

Possible stores for future price tracking:

- Official LEGO Store
- Amazon
- Walmart
- Liverpool
- Mercado Libre
- BrickLink
- Local hobby stores

Each source must be evaluated individually before implementation.

---

## Scraping Rules

Before scraping any site:

- Review Terms of Service.
- Review robots.txt.
- Avoid bypassing anti-bot systems.
- Avoid login-required scraping unless explicitly allowed.
- Avoid high-frequency scraping.
- Cache results.
- Identify the app responsibly when appropriate.
- Stop scraping if blocked.

---

## MVP Recommendation

The MVP should not depend on scraping.

Initial price tracking can start with:

- Manual price input
- Wishlist target prices
- Store links
- Price snapshots added manually
- Later API-based integrations

---

## Deal Detection Logic

A good deal should not simply mean “discount label shown by the store.”

BrickDeck should calculate deal quality using:

- Current price
- Historical average price
- Lowest observed price
- MSRP, when available
- Price per piece
- Rarity or retired status
- User wishlist priority

---

## Future Alert Examples

- Notify me when this set drops below $2,000 MXN.
- Notify me when this set is 20% below its average price.
- Notify me when a retired set appears below market average.
- Notify me when a wishlist set has a genuine discount.
