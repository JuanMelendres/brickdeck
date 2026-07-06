# Phase 2a — Auth Foundation (JWT) — Design

Date: 2026-07-06
Status: Approved

## Goal

Backend auth foundation for BrickDeck: user registration, login, and an authenticated `/me` endpoint, using stateless JWT. Prerequisite for the collection features (2b add-set, 2c loose pieces).

## Decision

Spring Security + **stateless JWT** (HS256). Chosen over session cookies (avoids CORS-credentials + CSRF setup across the `localhost:3000 → :8080` origin split; matches the already-stateless API) and over an external IdP (overkill for MVP). Migration to cookie/IdP later is a contained swap (auth filter + login endpoint), Strangler-friendly.

## Scope (this slice)

In: users table, `User` entity/repo, register + login + `GET /me`, BCrypt hashing, JWT issue/validate, Spring Security config. Out: any collection/`user_sets` endpoints, refresh tokens, roles beyond a single default, frontend login wiring, password reset, email verification.

## Package layout (`com.brickdeck.api.security`)

```
security/
  config/    SecurityConfig, JwtProperties
  jwt/       JwtService, JwtAuthenticationFilter
  entity/    User
  repository/UserRepository
  service/   AuthService, AppUserDetailsService
  controller/AuthController
  dto/       RegisterRequest, LoginRequest, AuthResponse, UserResponse
  EmailAlreadyUsedException
```

## Data model — V5 migration

`users`: `id UUID PK`, `email VARCHAR UNIQUE NOT NULL`, `password_hash VARCHAR NOT NULL`, `display_name VARCHAR`, `role VARCHAR NOT NULL DEFAULT 'USER'`, `created_at`, `updated_at`. Email stored lowercased. Entity mirrors existing style (Lombok `@Getter/@Setter`, UUID assigned in `@PrePersist`, `LocalDateTime` timestamps).

## Endpoints

- `POST /api/v1/auth/register` — body `{email, password, displayName?}` (Bean Validation: email format, password min 8). 201 → `AuthResponse{token, tokenType:"Bearer", user:UserResponse}`. Duplicate email → 409.
- `POST /api/v1/auth/login` — body `{email, password}`. 200 → `AuthResponse`. Bad credentials → 401.
- `GET /api/v1/auth/me` — requires `Authorization: Bearer <jwt>`. 200 → `UserResponse`. Missing/invalid token → 401.

`UserResponse{id, email, displayName, role, createdAt}` — never expose `password_hash`.

## JWT

`JwtService`: issue token with subject = user id (UUID string), `email` claim, HS256 signed with a secret from `JWT_SECRET` (`brickdeck.security.jwt.secret`), TTL `expiration-minutes` (default 120). Validate: verify signature + expiry, extract subject. Secret required (>=32 bytes for HS256); app fails fast if too short. Dev default provided in `application.yaml`; production must override via env.

## Security config

`SecurityFilterChain`: `SessionCreationPolicy.STATELESS`, CSRF disabled (stateless, no cookies), permit `/api/v1/auth/register`, `/api/v1/auth/login`, `/health`, `/v3/api-docs/**`, `/swagger-ui/**`; everything else authenticated. `JwtAuthenticationFilter` (`OncePerRequestFilter`) reads the Bearer header, validates, loads the user, sets `SecurityContext`. `BCryptPasswordEncoder` bean. Reuse existing CORS config.

## Error handling

Extend `GlobalExceptionHandler`: `EmailAlreadyUsedException` → 409, `BadCredentialsException` → 401 (generic "Invalid email or password"), `MethodArgumentNotValidException` → 400 with field errors. Auth-entry-point/filter failures → 401.

## Testing (TDD)

- `JwtService` (unit): round-trips subject; rejects tampered token; rejects expired token.
- `AuthService` (unit, mocked repo/encoder/jwt): register hashes + persists + returns token; duplicate email throws; login verifies password + issues token; wrong password throws `BadCredentialsException`.
- `UserRepository` (`@DataJpaTest` / Testcontainers): `findByEmail`.
- `AuthController` (`@WebMvcTest` + security): register 201, validation 400, login 200, duplicate 409, bad creds 401, `/me` 401 without token.
- Integration (`@SpringBootTest`): register → login → call `/me` with token → 200.

## Verification

```bash
cd apps/api
nc -z -w2 localhost 5433
./mvnw -Dtest=JwtServiceTest,AuthServiceTest test
./mvnw clean verify
```

## Follow-ups

Frontend login/register + token storage + auth'd API client; 2b add-set-to-collection; refresh tokens; mark backend DTO nullability (unrelated). Consider moving `User` to a dedicated `user` domain if it grows.
