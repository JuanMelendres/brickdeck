# Contribution Guide

## Branching (git-flow)

`develop` is the default branch and integration target; `master` stays releasable.
See [ADR-010](../decisions/ADR-010-git-flow-develop-default.md).

```bash
git checkout develop && git pull
git checkout -b feat/<short-name>      # branch from develop
# ...work + tests...
git push -u origin feat/<short-name>
gh pr create --base develop
```

Branch prefixes: `feat/`, `fix/`, `refactor/`, `test/`, `docs/`, `chore/`, `perf/`, `style/`, `ci/`.

## Commits (Conventional Commits)

Format: `type(scope): summary`. One logical change per commit.

Scopes: `catalog`, `collection`, `external`, `db`, `config`, `security`, `frontend`,
plus `docs` / `test` / `ci` / `chore` / `refactor` / `style` / `fix` / `perf`.

Examples:

```
feat(catalog): add set search endpoint
fix(external): add connect/read timeouts to Rebrickable client
test(collection): cover duplicate set 409 path
docs(architecture): document find-or-import flow
```

## Definition of Done

Before opening a PR:

1. Smallest safe change, in roadmap order — no overbuilding ahead of the roadmap.
2. Tests added/updated (TDD), and green:
   ```bash
   cd apps/api && ./mvnw clean verify
   cd apps/web && npm run test && npm run typecheck && npm run lint && npm run build
   ```
3. Docs updated when behavior/architecture/decisions change (see below).
4. No secrets committed; `.env.example` updated if new variables were introduced.
5. Conventional Commit message(s).

## Keeping Documentation Alive

- **New/changed endpoint:** update [../architecture/api-design.md](../architecture/api-design.md)
  and [../api/openapi.yaml](../api/openapi.yaml).
- **Schema change (new migration):** update [../architecture/database-design.md](../architecture/database-design.md).
- **New feature:** add/update a feature entry in [../product/features.md](../product/features.md)
  and an FDD under [../product/fdd/](../product/fdd/).
- **Significant technical decision:** add an ADR in [../decisions/](../decisions/).
- **Phase/task completion:** update `.claude/project-state.md` and `.claude/roadmap.md`.
- **User-facing release notes:** update the [CHANGELOG](../../CHANGELOG.md).

## AI-Assisted Development

Project context for AI tools lives in [`CLAUDE.md`](../../CLAUDE.md) (root). Keep it
in sync with these docs; avoid duplicating deep content between README, CLAUDE.md,
and `/docs`.
