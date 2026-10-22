#!/usr/bin/env bash
set -uo pipefail
source "$(dirname "$0")/lib.sh"

cd "$(project_dir)" || exit 0

check_jdk() {
  use_jdk_25 && echo "JDK $(java_major) OK" || echo "WARNING: JDK 25 not found; run 'sdk env' in java/ or set JAVA_HOME"
}

check_docker() {
  docker info >/dev/null 2>&1 && echo "Docker OK" || echo "WARNING: Docker is not running; integration tests need it"
}

check_time_deposit() {
  local tree index
  tree=$(blob_in_worktree)
  index=$(blob_in_index)
  [[ "$tree" == "$ORIGINAL_TIME_DEPOSIT_BLOB" ]] || echo "WARNING: TimeDeposit.java in the working tree differs from the original"
  [[ -z "$index" || "$index" == "$ORIGINAL_TIME_DEPOSIT_BLOB" ]] || echo "WARNING: TimeDeposit.java in the git index differs from the original; do not commit before resetting the index"
}

summarize_git() {
  local status staged unstaged untracked stale
  status=$(git status --porcelain)
  staged=$(grep -c '^[MADRC]' <<<"$status")
  unstaged=$(grep -c '^.[MD]' <<<"$status")
  untracked=$(grep -c '^??' <<<"$status")
  stale=$(grep -c '^[MA]M' <<<"$status")
  echo "git: $staged staged, $unstaged modified, $untracked untracked"
  [[ "$stale" -eq 0 ]] || echo "WARNING: $stale files changed again after staging; the index may be stale"
}

check_jq() {
  has_jq || echo "WARNING: jq is not installed; the commit guard blocks every commit until it is"
}

report=$(printf '%s\n' "$(check_jdk)" "$(check_docker)" "$(check_jq)" "$(check_time_deposit)" "$(summarize_git)" | sed '/^$/d')

has_jq || { echo "preflight"; echo "$report"; exit 0; }

jq -n --arg report "$report" '{
  systemMessage: ("preflight\n" + $report),
  hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: ("Preflight of this repository:\n" + $report)}
}'
