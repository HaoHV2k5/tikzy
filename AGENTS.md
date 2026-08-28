# Tikzy Agent Instructions

This file is the repository-level working agreement. Read it before making code changes.

## General Workflow

1. Inspect the repository before editing:
   - `git status --short --branch`
   - `git diff` and `git diff --cached`
   - `git log --oneline -10`
   - `git remote -v` and `git branch -vv`
2. Understand the existing implementation and project conventions before choosing a solution.
3. Make the smallest correct change that solves the request.
4. Preserve user changes. Never revert, reset, overwrite, or reformat unrelated work.
5. Keep secrets out of Git. Never commit `.env`, credentials, tokens, private keys, or local IDE/build files such as `.idea/` and `target/`.
6. Run the most relevant build, test, lint, or validation command after editing. Report failures honestly.

## Git Workflow

Follow this sequence when the user asks to commit or push:

1. Review the complete working tree and identify the exact files belonging to the request.
2. Separate unrelated changes. Do not stage them, and do not use `git add .` or `git add -A` without reviewing the result.
3. Stage only intended files with explicit paths.
4. Review the staged result:
   - `git status --short`
   - `git diff --cached --stat`
   - `git diff --cached`
   - `git diff --cached --check`
5. Run the relevant verification command before committing.
6. Create a new commit. Do not amend commits unless the user explicitly asks.
7. Push the current branch to its configured remote, normally `origin`:
   - `git push origin <branch>`
8. Verify the result with `git status --short --branch`, `git log -1 --oneline`, and `git ls-remote origin refs/heads/<branch>`.
9. Report the commit hash, commit message, branch, verification result, and any files intentionally left uncommitted.

Do not force-push. Do not use destructive commands such as `git reset --hard` or `git checkout --` unless the user explicitly approves them. Push only when the user explicitly requests a push.

## Conventional Commits

Use this format:

```text
<type>(<scope>): <short imperative description>
```

Use a lowercase subject without a trailing period. Keep each commit focused on one logical change. Split unrelated changes into separate commits when appropriate.

Allowed types:

- `feat`: add user-facing functionality
- `fix`: correct a bug or incorrect behavior
- `docs`: documentation-only changes
- `refactor`: restructure code without changing behavior
- `test`: add or change tests
- `chore`: maintenance and repository/tooling changes
- `build`: build or dependency changes
- `ci`: CI/CD changes
- `perf`: performance improvements
- `style`: formatting-only changes
- `revert`: revert a previous commit

Use a meaningful scope such as `backend`, `auth`, `security`, `database`, `frontend`, or `trello`.

Examples:

- `feat(auth): add refresh token rotation`
- `fix(backend): align entities with database schema`
- `fix(security): restrict CORS origins`
- `docs(trello): add project task list`
- `chore(config): update local Redis configuration`

## Communication

Before a substantial edit, briefly state the intended scope. At the end, summarize what changed, what was verified, the commit and push status, and any remaining risks or uncommitted files.
