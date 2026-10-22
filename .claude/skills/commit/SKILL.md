---
name: commit
description: Turn the working tree into atomic commits — detect a stale index, propose a grouping with messages, stage explicit paths only and keep the build green at every commit. Use only when the developer asks to commit.
disable-model-invocation: true
---

1. **Inspect.** Run `git status`, `git diff --stat` and `git diff --cached --stat`. If the index differs from the working tree for the same files, show it: a stale index can contain old versions. Propose `git reset` (mixed: the working tree is not touched) and run it only after the developer confirms.
2. **Check the shared class.** `git hash-object java/src/main/java/org/ikigaidigital/TimeDeposit.java` must be `2952ac14618ad804f435813a8c2a059107294cc7`.
3. **Propose a grouping** into logical, atomic commits in dependency order, each with a message (imperative mood, ≤ 72 characters, a body explaining why when it is not obvious). Wait for confirmation or changes.
4. **For each commit:**
   - `git add -- <explicit paths>` — never `git add -A` or `git add .`;
   - show `git diff --cached --stat`;
   - `git commit -m "<message>"`.
5. **Build at every commit** (when asked, or for a first history): for each new commit run `git worktree add "$TMPDIR/verify-<sha>" <sha>` and `./mvnw -q -B -ntp verify` in its `java/`, then `git worktree remove`. Report commits that do not build.
6. **Report** `git log --oneline` of the new commits. Never push unless asked.
