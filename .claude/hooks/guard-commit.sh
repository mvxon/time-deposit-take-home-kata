#!/usr/bin/env bash
set -uo pipefail
source "$(dirname "$0")/lib.sh"

command=$(json_field '.tool_input.command' "$(cat)")

is_git_commit() {
  grep -qE '(^|[;&|("]|[[:space:]])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+commit([[:space:]]|$)' <<<"$command"
}

commit_options() {
  local options=${command#*commit}
  options=${options%%[;&|]*}
  sed -E 's/[[:space:]](-m|--message|-F|--file)([[:space:]=].*|$)//' <<<"$options"
}

stages_everything() {
  grep -qE '(^|[[:space:]])(-a|--all|-[a-zA-Z]*a[a-zA-Z]*)([[:space:]]|$)' <<<"$(commit_options)"
}

block() {
  echo "Commit blocked: $1" >&2
  exit 2
}

is_git_commit || exit 0

has_jq || block "jq is required by the commit guard; install it (e.g. 'brew install jq') and retry."

index=$(blob_in_index)
[[ -z "$index" || "$index" == "$ORIGINAL_TIME_DEPOSIT_BLOB" ]] \
  || block "the staged TimeDeposit.java differs from the original kata file. Restore it with 'git restore --staged $TIME_DEPOSIT' and check the working tree."

stages_everything \
  && block "stage explicit paths with 'git add -- <paths>' instead of 'git commit -a'."

exit 0
