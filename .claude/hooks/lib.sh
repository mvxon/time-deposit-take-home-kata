#!/usr/bin/env bash

readonly ORIGINAL_TIME_DEPOSIT_BLOB=2952ac14618ad804f435813a8c2a059107294cc7
readonly TIME_DEPOSIT=java/src/main/java/org/ikigaidigital/TimeDeposit.java

project_dir() {
  echo "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
}

java_major() {
  "${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1
}

use_jdk_25() {
  [[ "$(java_major)" -ge 25 ]] 2>/dev/null && return 0
  local wanted candidate
  wanted=$(sed -nE 's/^java=(.*)$/\1/p' "$(project_dir)/java/.sdkmanrc" 2>/dev/null)
  for candidate in \
    "${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/$wanted" \
    "$(/usr/libexec/java_home -v 25 2>/dev/null)"; do
    if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done
  return 1
}

blob_in_worktree() {
  git -C "$(project_dir)" hash-object "$TIME_DEPOSIT" 2>/dev/null
}

blob_in_index() {
  git -C "$(project_dir)" rev-parse ":$TIME_DEPOSIT" 2>/dev/null
}

has_jq() {
  command -v jq >/dev/null 2>&1
}

json_field() {
  if has_jq; then
    jq -r "$1 // empty" <<<"$2"
  else
    printf '%s' "$2"
  fi
}

system_message() {
  if has_jq; then
    jq -n --arg message "$1" '{systemMessage: $message}'
  else
    echo "$1"
  fi
}
