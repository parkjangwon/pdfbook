#!/usr/bin/env bash
# Runs the named verify.sh features that this branch actually has.
#
# The emulator workflow splits its work into groups (core, page-fit,
# image-adjust), and a group name is fixed for every pull request, but a
# feature only exists on the branch that adds it. Asking verify.sh for a
# feature a branch does not have prints usage and exits 1, which used to fail
# the whole job, so this wrapper reads the branch's own FEATURES list and skips
# what is missing.
#
# Usage: scripts/verify-group.sh "library open-book ..." [--dry-run]
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN=0
requested=()
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    "") ;;
    *) read -r -a parts <<<"$arg"; requested+=("${parts[@]}") ;;
  esac
done

supported="$(sed -n 's/^FEATURES="\(.*\)"/\1/p' "$ROOT/scripts/verify.sh" | head -1)"
if [ -z "$supported" ]; then
  echo "verify-group: cannot read FEATURES from scripts/verify.sh" >&2
  exit 2
fi
if [ "${#requested[@]}" -eq 0 ]; then
  echo "verify-group: no feature requested" >&2
  exit 2
fi

failed=0
ran=0
for feature in "${requested[@]}"; do
  case " $supported " in
    *" $feature "*)
      ran=$((ran + 1))
      if [ "$DRY_RUN" = 1 ]; then
        echo "verify-group: would run $feature"
      else
        bash "$ROOT/scripts/verify.sh" "$feature" || failed=1
      fi
      ;;
    *)
      echo "verify-group: skip $feature (this branch does not have it)"
      ;;
  esac
done

if [ "$ran" -eq 0 ]; then
  echo "verify-group: none of the requested features exist on this branch"
fi
exit "$failed"
