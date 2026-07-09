# Diagrams

Central index of the project's Mermaid diagrams. Render on GitHub or any
Mermaid-aware viewer.

## System Context
See [overview.md — System Context](./overview.md#system-context).

## Find-or-Import Sequence
See [overview.md — Data Flow](./overview.md#data-flow-find-or-import-cache-first).

## Entity-Relationship
See [database-design.md — ER Overview](./database-design.md#entity-relationship-overview).

## Request Lifecycle (auth + collection)

```mermaid
flowchart LR
    Req[HTTP Request] --> CORS[CORS filter]
    CORS --> JWT[JwtAuthenticationFilter]
    JWT -->|Bearer valid| Ctrl[Controller]
    JWT -->|public route| Ctrl
    Ctrl --> Svc[Service]
    Svc --> Repo[(JPA Repository)]
    Repo --> DB[(PostgreSQL)]
    Ctrl -->|error| GEH[GlobalExceptionHandler]
    GEH -->|400/401/404/409| Req
```

## Frontend Data Flow

```mermaid
flowchart LR
    Comp[Component] --> Hook[TanStack Query hook]
    Hook --> Client[API client apiGet/apiPost]
    Client -->|fetch + Bearer| API[Spring Boot API]
    Hook -->|cache| Comp
```

> Keep new diagrams here or in the doc they explain, and link them from this index.
