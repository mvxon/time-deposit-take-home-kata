---
name: review
description: Run an independent milestone review — the solution-reviewer subagent, plus Codex CLI read-only if installed — verify every finding against the code, classify it against the ADRs and list contradictions for the developer to decide. Use only when the developer asks for an independent review. Never applies fixes by itself.
disable-model-invocation: true
---

1. **Run the reviewers in parallel.**
   - The `solution-reviewer` subagent.
   - If `codex` is installed, the same prompt in Codex CLI, from the repository root, in the background:
     `codex exec -s read-only -o "$TMPDIR/codex-review.md" "$(awk 'c>=2;/^---$/{c++}' .claude/agents/solution-reviewer.md)"`
     Codex cannot build in a read-only sandbox; its review is static.
2. **Merge** the findings into one table: reviewer, severity, file:line, summary.
3. **Verify every finding** against the code (read the lines, run a targeted test or command) and mark it `confirmed` or `not reproduced`. Check `git status` too: a finding may come from a change made outside the session.
4. **Classify** each confirmed finding: bug / requirement violation / trade-off already decided in an ADR (name it) / taste.
5. **Contradictions.** List findings where reviewers disagree with each other or with an ADR, with the options and your recommendation.
6. **Stop.** Present the scores side by side and the classified list. Do not change any file; the developer decides what to fix. Accepted trade-offs become ADRs via `docs-writer`.
