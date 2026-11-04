#!/usr/bin/env bash
# Guards scripts/verify-group.sh: features a branch lacks are skipped, not failed.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GROUP="$ROOT/scripts/verify-group.sh"

out="$("$GROUP" "library not-a-feature" --dry-run)"
grep -q "would run library" <<<"$out" || { echo "fail: library should run" >&2; exit 1; }
grep -q "skip not-a-feature" <<<"$out" || { echo "fail: missing feature should skip" >&2; exit 1; }

out="$("$GROUP" "not-a-feature" --dry-run)"
grep -q "none of the requested features" <<<"$out" || { echo "fail: empty group should say so" >&2; exit 1; }

if "$GROUP" --dry-run >/dev/null 2>&1; then
  echo "fail: no features should be an error" >&2
  exit 1
fi
echo "ok verify-group"
