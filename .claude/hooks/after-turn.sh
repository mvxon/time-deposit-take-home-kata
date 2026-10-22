#!/usr/bin/env bash
set -uo pipefail
source "$(dirname "$0")/lib.sh"

input=$(cat)
already_continued=$(json_field '.stop_hook_active' "$input" | grep -qE '^true$|"stop_hook_active": *true' && echo true || echo false)
root=$(project_dir)
state_file="${TMPDIR:-/tmp}/claude-after-turn-$(printf '%s' "$root" | shasum | cut -c1-12)"

java_state() {
  { git -C "$root" diff HEAD -- java; git -C "$root" ls-files --others --exclude-standard -- java | xargs -I{} git -C "$root" hash-object {} 2>/dev/null; } | shasum | cut -d' ' -f1
}

diff_summary() {
  local tracked untracked
  tracked=$(git -C "$root" diff HEAD --shortstat -- java | sed 's/^ *//')
  untracked=$(git -C "$root" ls-files --others --exclude-standard -- java | wc -l | tr -d ' ')
  echo "java/: ${tracked:-no tracked changes}; $untracked untracked files"
}

[[ "$(java_state)" == "$(cat "$state_file" 2>/dev/null)" ]] && exit 0

if ! use_jdk_25; then
  system_message "after-turn: JDK 25 not found, skipped formatting and compilation"
  exit 0
fi

cd "$root/java" || exit 0
./mvnw -q -B -ntp spotless:apply >/dev/null 2>&1
log=$(./mvnw -q -B -ntp test-compile 2>&1)
compiled=$?
java_state >"$state_file"

if [[ $compiled -ne 0 && "$already_continued" != "true" ]]; then
  echo "test-compile failed after this turn; fix it before finishing:" >&2
  grep -E '^\[ERROR\]' <<<"$log" | head -20 >&2
  exit 2
fi

[[ $compiled -eq 0 ]] && result="compile OK" || result="COMPILE FAILED"
system_message "after-turn: $(diff_summary); $result"
