# Create Commit Message

Review the current change context and suggest a Conventional Commit.

Rules:

- Use `feat` for new behavior.
- Use `fix` for bug fixes.
- Use `test` for test-only changes.
- Use `refactor` for behavior-preserving code cleanup.
- Use `docs` for documentation.
- Use `ci` for GitHub Actions / pipeline changes.
- Use `chore` for maintenance.

Prefer domain scopes: `catalog`, `external`, `db`, `config`, `docs`, `test`, `frontend`.

Output one command:

```bash
git add <specific-file-or-folder-1> <specific-file-or-folder-2>
git commit -m "type(scope): message"
```

If changes should be split into disjoint logical commits, suggest multiple commits.
