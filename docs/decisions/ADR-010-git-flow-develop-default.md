# ADR-010: Git-Flow with `develop` as the Default Branch

## Status
Accepted

## Date
2026-07-06

## Context
The project uses feature branches and pull requests. A clear integration branch is
needed so `master` always reflects a releasable state while day-to-day work
integrates elsewhere.

## Decision
Adopt a git-flow-style model: `develop` is the default branch and integration
target. Feature branches (`feat/...`, `fix/...`, `docs/...`, `refactor/...`,
`test/...`, `chore/...`) branch from `develop` and merge back via PR. `develop`
merges into `master` for releases. Commits follow Conventional Commits with scopes
`catalog`, `collection`, `external`, `db`, `config`, `security`, `frontend`, plus
`docs`/`test`/`ci`/`chore`/`refactor`/`style`/`fix`/`perf`.

## Consequences
- Positive: `master` stays releasable; PRs give review checkpoints.
- Positive: consistent, tooling-friendly commit history.
- Negative: slightly more branching overhead than trunk-based development.

## Alternatives Considered
- Trunk-based (commit straight to `master`): rejected — no release-stable branch.

## Notes
See [development/contribution-guide.md](../development/contribution-guide.md).
