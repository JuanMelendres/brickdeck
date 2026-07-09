# ADR-008: Stateless JWT Authentication

## Status
Accepted

## Date
2026-07-06

## Context
Collection data is per-user and must be isolated. The backend is a stateless REST
API consumed by a separate Next.js SPA, so authentication should not rely on
server-side sessions.

## Decision
Use stateless authentication with signed JWTs (jjwt, HS256). On register/login the
API returns `AuthResponse { token, tokenType, user }`. A `JwtAuthenticationFilter`
resolves the Bearer token to a `User` principal. Spring Security is configured
stateless; auth, health, and Swagger routes are public, everything else requires a
token. The signing secret comes from `JWT_SECRET` (>= 32 bytes) and expiry from
`JWT_EXPIRATION_MINUTES` (default 120). Passwords are BCrypt-hashed.

## Consequences
- Positive: no session store; horizontally scalable; clean fit for an SPA.
- Positive: simple Bearer-header contract for the frontend.
- Negative: tokens can't be revoked before expiry (no refresh/blacklist yet).
- Negative: storing the token in browser `localStorage` is XSS-readable — acceptable
  for the MVP; revisit with httpOnly cookies + CSRF at productization.

## Alternatives Considered
- Server-side sessions: rejected — stateful, awkward for a separate SPA.
- OAuth/social login: deferred (heavier setup, not needed for MVP).

## Notes
See [product/fdd/authentication-fdd.md](../product/fdd/authentication-fdd.md).
Spec: `docs/superpowers/specs/2026-07-06-auth-foundation-design.md`.
