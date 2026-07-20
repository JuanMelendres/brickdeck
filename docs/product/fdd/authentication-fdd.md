# Feature: Authentication

## Summary
Email/password registration and login issuing a stateless JWT used to authorize
collection endpoints.

## Problem
Collection data (owned sets, loose pieces) is per-user and must be isolated. The
API needs an authentication mechanism that works for a stateless REST backend and
a separate Next.js frontend.

## Users
Any person who wants to manage a personal collection.

## Functional Requirements
- FR-001: `POST /api/v1/auth/register` creates a user and returns `201` with `AuthResponse { token, tokenType, user }`.
- FR-002: `POST /api/v1/auth/login` validates credentials and returns `200` with `AuthResponse`.
- FR-003: `GET /api/v1/auth/me` returns the authenticated user's `UserResponse`.
- FR-004: Protected endpoints require `Authorization: Bearer <token>`.

## Business Rules
- BR-001: Email is unique (`users.email UNIQUE`); duplicate registration returns `409`.
- BR-002: Passwords are stored as BCrypt hashes, never in plaintext.
- BR-003: Password must be 8-100 characters; email must be valid.
- BR-004: JWT is HS256, signed with `JWT_SECRET` (>= 32 bytes), expiring after `JWT_EXPIRATION_MINUTES` (default 120).
- BR-005: The API is stateless — no server-side session; the token is the source of identity.

## User Flow
1. User registers or logs in.
2. Backend returns a JWT.
3. Frontend stores the token and sends it as a Bearer header on subsequent calls.
4. `JwtAuthenticationFilter` resolves the token to a `User` principal.

## Edge Cases
- Invalid credentials: `401`.
- Duplicate email on register: `409`.
- Missing/expired/invalid token on protected route: `401`.
- Validation failure (bad email, short password): `400` with `validationErrors`.

## Out of Scope
- OAuth / social login.
- Refresh tokens and token revocation.
- Email verification and password reset.
- Role-based authorization beyond the stored `role` field.

## Open Questions
- Frontend token storage: `localStorage` (MVP) vs httpOnly cookie (see [ADR-008](../../decisions/ADR-008-jwt-stateless-auth.md)).
- Should refresh tokens be added before productization?
