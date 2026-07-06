# Next Task

Based on BrickDeck `.claude/project-state.md` and `.claude/roadmap.md`, recommend the next task.

Return:

1. Recommended next task
2. Why it matters
3. Files likely affected
4. Implementation plan
5. Tests needed
6. Verification command
7. Conventional Commit message

Prefer the smallest safe increment in roadmap order. Do not suggest large refactors unless project-state says quality gates are green. Update `.claude/project-state.md` and `.claude/roadmap.md` when a phase/task completes.
