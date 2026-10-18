#!/usr/bin/env bash
set -euo pipefail

tag="${1:-${GITHUB_REF_NAME:-}}"
props="${2:-gradle.properties}"

if ! [[ "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "::error::태그 형식이 잘못됐어요($tag). vX.Y.Z 형식(예: v0.4.0)이어야 해요." >&2
  exit 1
fi

declared="$(sed -n 's/^version=[[:space:]]*\([^[:space:]]*\)[[:space:]]*$/\1/p' "$props" | tail -n 1)"
if [[ -z "$declared" ]]; then
  echo "::error::$props에서 version 값을 찾지 못했어요." >&2
  exit 1
fi

if [[ "${tag#v}" != "$declared" ]]; then
  echo "::error::태그($tag)와 앱 버전($declared)이 달라요. 버전 올림 PR을 먼저 main에 합친 뒤, 그 커밋에 태그를 붙여 주세요." >&2
  exit 1
fi

echo "태그($tag)와 앱 버전($declared)이 같아요."
