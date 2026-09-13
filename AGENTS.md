# Tikzy Agent Instructions

This file is the repository-level working agreement. Read it before making changes and read it again immediately before any commit or push.

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
7. Before committing or pushing, read this file again and follow the Git Workflow and Conventional Commits rules below.

## Git Workflow

Follow this sequence when the user asks to commit or push:

1. Read `AGENTS.md` again immediately before any commit or push command. Do not skip this step.
2. Review the complete working tree and identify the exact files belonging to the request.
3. Never commit to or push directly from the protected branches `main` or `develop`.
   - If the current branch is `main` or `develop`, create a topic branch before staging or committing, for example `docs/readme-ops`, `ci/pipeline-quality-gates`, or `fix/security-cors`.
   - Use `git switch -c <topic-branch>` so existing user changes are preserved. Never use a destructive command to force the branch change.
   - Push only the topic branch. Changes destined for `main` or `develop` must go through the repository's pull-request/merge process.
4. Separate unrelated changes. Do not stage them, and do not use `git add .` or `git add -A` without reviewing the result.
5. Stage only intended files with explicit paths.
6. Review the staged result:
   - `git status --short`
   - `git diff --cached --stat`
   - `git diff --cached`
   - `git diff --cached --check`
7. Run the relevant verification command before committing.
8. Create a new commit that follows Conventional Commits. Do not amend commits unless the user explicitly asks.
   - Attribute commits and pull requests only to the human user. Never add `Co-authored-by`, author, or contributor metadata for Cursor, an AI agent, or an automated assistant.
9. Push the verified topic branch to its configured remote, normally `origin`:
   - `git push -u origin <topic-branch>` for the first push
   - `git push origin <topic-branch>` for later pushes
10. Verify the result with `git status --short --branch`, `git log -1 --oneline`, and `git ls-remote origin refs/heads/<topic-branch>`.
11. Report the commit hash, commit message, topic branch, verification result, and any files intentionally left uncommitted.

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
