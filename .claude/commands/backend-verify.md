# Backend Verify

Run or suggest the BrickDeck backend verification flow.

## Verification Flow

Run from `apps/api` (append `.cmd` to `./mvnw` if explicitly on Windows CMD):

Steps:

1. Check Postgres is up (integration/`@SpringBootTest` need it on 5433):

```bash
nc -z -w2 localhost 5433
```

2. Run focused tests (optional): fast feedback on a changed slice. `@WebMvcTest`/unit slices do not need the DB:

```bash
./mvnw -Dtest=ClassName test
```

3. Run full verification (all unit + integration tests):

```bash
./mvnw clean verify
```

Parse noisy output:

```bash
./mvnw clean verify 2>&1 | grep -E "Tests run:|BUILD"
```

4. If failures occur:

- Identify the failing test
- Read the relevant file
- Explain the root cause
- Propose the smallest fix
- Update tests if needed
- Suggest a Conventional Commit

5. Post-success actions:

- Acknowledge the build is stable.
- If this completes a major feature, remind the user to review coverage/quality gates.
