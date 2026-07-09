# ADR-002: Java 21 + Spring Boot 3 for the Backend

## Status
Accepted

## Date
2026-07-02

## Context
The backend must expose a REST API, integrate with an external catalog API, manage
a relational schema, and support strong testing. It should use a stack the author
knows well and that is production-credible for a possible SaaS.

## Decision
Build the backend on Java 21 and Spring Boot 3 (3.5.x) with Spring Web, Spring Data
JPA/Hibernate, Bean Validation, and Spring Security. Build with the Maven Wrapper
(`./mvnw`).

## Consequences
- Positive: mature ecosystem, first-class JPA/validation/security, strong testing
  (JUnit 5, Mockito, MockMvc, Testcontainers).
- Positive: Java 21 LTS with modern language features (records for DTOs).
- Negative: heavier runtime/startup than a lightweight framework (acceptable).

## Alternatives Considered
- Node.js/NestJS: viable but the author prefers the JVM for the core domain.
- Gradle instead of Maven: Maven Wrapper chosen for familiarity and simplicity.

## Notes
DTOs use Java records; entities are never exposed to controllers.
