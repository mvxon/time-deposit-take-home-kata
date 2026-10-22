# AI-assisted development

## Approach

One interactive Claude Code session does the work, and I make every design decision. Deterministic guarantees live in the build and in hooks; models are used where judgement is needed. Independent reviewers start with a clean context, and one of them is a model from another vendor. Every finding is verified against the code before I act on it.

## Tools

| Tool | Version | Role |
|---|---|---|
| [Claude Code](https://docs.anthropic.com/en/docs/claude-code) (CLI) with Claude Opus 5.5 | — | Main session: analysis, implementation, running builds and tests, live checks in Docker |
| Claude Code subagents | — | Reviewers and the documentation writer with a clean context: Sonnet for frequent checks, Opus for milestone reviews |
| [Codex CLI](https://github.com/openai/codex) | 0.162.1 | Second-vendor reviewer, run read-only |

## Setup

Everything is versioned, so cloning the repository reproduces it.

### Rules

| File | Purpose |
|---|---|
| [`CLAUDE.md`](../CLAUDE.md) | Loaded into every session: the kata invariants, the hexagonal architecture rules, code style, commands, and the "Skills and agents" table that says which tool to use when. |

### Subagents (`.claude/agents/`)

| Agent | Model | When | Purpose |
|---|---|---|---|
| [`requirements-reviewer`](../.claude/agents/requirements-reviewer.md) | Sonnet | Every change | Checks the pending diff (working tree and index) against the kata invariants and `CLAUDE.md`; answers `APPROVED` or `CHANGES REQUIRED` with file:line evidence. |
| [`solution-reviewer`](../.claude/agents/solution-reviewer.md) | Opus | Milestones | Independent review of the whole solution against `ASSIGNMENT.md`: readiness score, scores per area, findings with severity, evidence and confidence, conflicts with ADRs. The same prompt runs in Codex. |
| [`docs-writer`](../.claude/agents/docs-writer.md) | Sonnet | After a change or decision | Keeps README, ADRs and `docs/` in sync with the code and verifies every factual claim in the documents it touches. Never invents a decision. |

### Skills (`.claude/skills/`)

| Skill | Use |
|---|---|
| [`/change <task>`](../.claude/skills/change/SKILL.md) | Any behaviour change, test-first: clarify, plan, failing test, implementation, `./mvnw verify`, `requirements-reviewer`, real `git diff --stat`. Has recipes for a new interest plan, an API change, a schema change and a bug fix. |
| [`/verify [full]`](../.claude/skills/verify/SKILL.md) | Preflight, `clean verify` and coverage; `full` adds the Docker image and a live smoke test of both endpoints. |
| [`/review`](../.claude/skills/review/SKILL.md) | Milestone review: `solution-reviewer` and Codex in parallel, findings verified and classified, contradictions listed. It changes no files. Runs only when I invoke it. |
| [`/commit`](../.claude/skills/commit/SKILL.md) | Atomic commits with explicit paths and a build check per commit. Runs only when I invoke it. |

The commit history follows the `/commit` procedure: atomic commits staged by explicit paths, each one checked out and verified in a separate git worktree.

### Hooks (`.claude/hooks/`, configured in [`.claude/settings.json`](../.claude/settings.json))

| Hook | Event | What it checks | Failure it prevents |
|---|---|---|---|
| [`preflight.sh`](../.claude/hooks/preflight.sh) | `SessionStart` | JDK 25, Docker, `TimeDeposit.java` against the original in the working tree and the index, a git summary with a warning for files changed after staging | Starting from a broken state: a stale git index containing an old `TimeDeposit.java` while the build is green, or running on JDK 21 |
| [`guard-commit.sh`](../.claude/hooks/guard-commit.sh) | `PreToolUse` on `Bash` | On `git commit`: the staged `TimeDeposit.java` equals the original blob; `commit -a` is refused | Committing a modified shared class or unreviewed paths |
| [`after-turn.sh`](../.claude/hooks/after-turn.sh) | `Stop` | If `java/` changed during the turn: `spotless:apply`, then `test-compile`; a failure sends the turn back to be fixed; reports the real diff summary | An edit made in the IDE that silently broke the build; a change reported as not applied that was in fact applied |
| [`lib.sh`](../.claude/hooks/lib.sh) | shared | JDK 25 selection (`java/.sdkmanrc`, then `java_home`) and the original `TimeDeposit` blob | Running on the wrong JDK |

### Permissions (`.claude/settings.json`)

- **allow:** Maven wrapper, Docker Compose and build, read-only git commands, `git add -- <paths>`, `git commit -m`, `git worktree`, `codex exec -s read-only`, `curl` to `localhost:8080`.
- **ask:** edits of `TimeDepositCalculatorTest` and of Flyway migrations; `git reset`, `restore`, `stash`, `push`.
- **deny:** edits of `TimeDeposit.java` and `LegacyTimeDepositCalculator.java`; `git reset --hard`, `clean`, `checkout --`, `push --force`.

[`java/.sdkmanrc`](../java/.sdkmanrc) pins JDK 25. My global preferences (functional style, small methods, clarifying questions before designing) are repeated in `CLAUDE.md`, so the behaviour does not depend on my machine.

## Workflow

```
SessionStart: preflight (JDK, Docker, TimeDeposit blob, git state)
  |
  v
/change  clarify -> plan -> failing test -> implementation
            -> [Stop hook: spotless + test-compile + real diff]
            -> ./mvnw verify -> requirements-reviewer
  |
  v
docs-writer (README, ADRs, docs)
  |
  v   at milestones
/review  solution-reviewer || Codex -> verify findings -> fix or ADR -> re-review
  |
  v
/verify full -> /commit [guard-commit hook] -> CI
```

## Independent review loop

At each milestone the whole solution is reviewed against the assignment by Codex and by a fresh Claude subagent running the same prompt. Every finding is checked against the code and classified as a bug, a requirement violation, a trade-off already decided in an ADR, or taste. Where reviewers contradict each other or an ADR, I decide and record the reasoning in [the decision records](decisions). A re-review confirms the fixes.

Findings fixed through the loop include:

- a concurrency test that could pass without real concurrency;
- untested legacy rounding cases;
- a behaviour difference for a `null` plan type;
- a second, auto-generated API contract published by springdoc;
- `POST` answering `406` to `Accept: application/json`;
- Spring persistence exceptions leaking through the domain port.

## What was AI-assisted and why

| Area | AI involvement | Why |
|---|---|---|
| Production code, tests, build, Docker, CI | Written by Claude to my decisions, reviewed by me | Fast iteration on well-defined changes; every change is verified by the build |
| Design options and trade-offs | Options and arguments proposed by Claude, decisions mine | The choice depends on the context of the task. Examples: package structure, snake_case names taken from the requirements, no audit columns, no idempotency inside the service ([ADR 0003](decisions/0003-accrual-responsibility-and-idempotency.md)), paged single-transaction update ([ADR 0004](decisions/0004-paged-single-transaction-update.md)), contract-first without springdoc ([ADR 0005](decisions/0005-contract-first-api.md)) |
| Reviews | Codex and clean-context Claude subagents | Different models and clean contexts find different problems than the author |
| Documentation | Drafted by Claude from my decisions and reviewed by me; kept in sync by `docs-writer` | Keeps the records consistent with the code; `docs-writer` verifies every factual claim it touches |

## How the output is verified

- `./mvnw verify` runs 56 tests (unit and characterization tests, the equivalence test, Testcontainers integration tests, contract and ArchUnit tests), enforces line coverage of at least 90 % (currently 92.8 %) and checks formatting.
- `TimeDepositCalculatorEquivalenceTest` compares the refactored calculator with a verbatim copy of the original on 100 000 random deposits.
- `/verify full` runs live smoke tests in Docker: both endpoints, the expected balances, Swagger UI.
- The hooks verify the build, the shared class and the git state independently of what the model reports.
- Review findings are checked against the code before they are accepted.

## How the setup evolved

The setup took shape during development. Once the first independent reviews showed which failures actually occur, I formalised the process into the current agents, skills and hooks, each tied to one of those failures.

## Reproducing

1. Install [Claude Code](https://docs.anthropic.com/en/docs/claude-code), JDK 25 (`sdk env` in `java/` reads `.sdkmanrc`), Docker and [jq](https://jqlang.org) (the hooks need it; the commit guard blocks commits when it is missing).
2. Optional: install Codex CLI; `/review` uses it when it is available.
3. Start `claude` in the repository root. `CLAUDE.md` and `.claude/` are picked up automatically.

`.claude/settings.local.json` is personal and git-ignored.
