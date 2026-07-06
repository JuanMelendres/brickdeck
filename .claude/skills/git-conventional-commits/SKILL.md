---
name: git-conventional-commits
description: Use this skill when generating commit messages, reviewing staged changes, preparing PR summaries, or deciding how to group commits for BrickDeck.
---

# Git Conventional Commits Skill

## Commit Format

Use:

```text
type(scope): description
```

Examples:

```text
feat(catalog): add set import endpoint
fix(catalog): add connect/read timeouts to Rebrickable client
refactor(catalog): extract theme resolution helper
test(catalog): add set import upsert coverage
docs(catalog): add set import design spec
chore: initialize Spring Boot API with PostgreSQL
```

## Allowed Types

- feat
- fix
- test
- refactor
- docs
- ci
- chore
- perf
- style

## Scope

Prefer these scopes:

- catalog
- external
- db
- config
- docs
- test
- frontend

## Rules

- Use imperative mood ("add", not "added"/"adds").
- Keep the subject concise (under 50 characters if possible).
- Do not end the description with a period.
- For breaking changes, append `!` after type/scope and optionally a `BREAKING CHANGE:` footer.
- Split unrelated changes into separate commits (disjoint file sets).
- Commit only when asked; if on `master`, branch first. Never push unless asked.

## Output

Return explicitly scoped add commands to avoid staging unintended files:

```bash
git add <specific-file-or-folder-1> <specific-file-or-folder-2>
git commit -m "type(scope): message"
```

If changes should be split, provide multiple commit commands.
