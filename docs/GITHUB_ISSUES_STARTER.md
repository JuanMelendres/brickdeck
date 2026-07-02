# BrickDeck Starter GitHub Issues

Use this file to create the initial GitHub issues for the project.

---

## 1. Create Monorepo Structure

Create the initial folder structure for BrickDeck.

Acceptance criteria:

- apps/web folder exists
- services/api folder exists
- docs folder exists
- infra folder exists
- README is in root

---

## 2. Add Spring Boot API Base Project

Create the backend API using Java 21 and Spring Boot 3.

Acceptance criteria:

- Application starts locally
- Health endpoint exists
- Local profile is configured
- Basic test passes

---

## 3. Add Next.js Web Base Project

Create the frontend application.

Acceptance criteria:

- App starts locally
- Basic homepage exists
- API base URL comes from environment variable

---

## 4. Add PostgreSQL and Docker Compose

Create local database setup.

Acceptance criteria:

- docker-compose starts PostgreSQL
- Database credentials come from environment variables
- Backend can connect to database

---

## 5. Add Flyway Migrations

Configure database migrations.

Acceptance criteria:

- Flyway runs on startup
- Initial schema migration exists
- Migration includes core catalog tables

---

## 6. Add Rebrickable API Client

Create the first external catalog integration.

Acceptance criteria:

- API key comes from environment variable
- Client can search or fetch set details
- Timeout and error handling are configured
- Unit tests cover mapping logic

---

## 7. Add Catalog Search Endpoint

Expose catalog search through backend.

Acceptance criteria:

- Endpoint accepts query parameter
- Returns normalized DTOs
- Does not expose raw external API response

---

## 8. Add Set Detail Page

Create frontend page for viewing set details.

Acceptance criteria:

- User can search a set
- User can open set detail
- User can see metadata and piece count

---

## 9. Add Set Inventory View

Display the parts inventory of a set.

Acceptance criteria:

- User can view parts list
- Parts show quantity and color
- Pagination or grouping is considered for large inventories

---

## 10. Add Collection MVP

Allow users to save sets to collection.

Acceptance criteria:

- User can add a set to collection
- User can list owned sets
- User can remove or archive a set
