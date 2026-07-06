# PR Review

Review the current BrickDeck changes as a senior backend engineer.

Check:

- Does the change match existing architecture (DDD + hexagonal, domain packages)?
- Are controllers thin and services focused?
- Are repositories clean?
- Are DTOs (records) used — no entities or raw Rebrickable responses exposed?
- Is external data normalized and cached (find-or-import cache-first)?
- Are missing resources mapped via `ResourceNotFoundException` → 404?
- Are validation messages consistent and sanitized?
- Are tests updated (service, controller `@WebMvcTest` with `@MockitoBean`, integration with Testcontainers)?
- Are integration tests stable (no single-record assumptions, Postgres on 5433)?
- Could this break coverage or quality gates?

Return:

1. Summary
2. Risks
3. Required fixes
4. Optional improvements
5. Suggested Conventional Commit or PR title
