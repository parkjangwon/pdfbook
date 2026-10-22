#!/usr/bin/env bash
# Checks scripts/anr-wait.py: dismiss other apps' ANR dialogs, never PDFBook's.
set -euo pipefail

cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
fail=0

dialog() {
  local title="$1" bounds="${2:-[66,1300][1014,1418]}"
  cat <<XML
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0"><node resource-id="" package="android" bounds="[0,0][1080,2400]">
<node resource-id="android:id/alertTitle" text="$title" package="android" bounds="[66,1000][1014,1100]" />
<node resource-id="android:id/aerr_close" text="Close app" package="android" bounds="[66,1180][1014,1298]" />
<node resource-id="android:id/aerr_wait" text="Wait" package="android" bounds="$bounds" />
</node></hierarchy>
XML
}

expect() {
  local name="$1" want="$2" file="$tmp/$1.xml" out got
  if out="$(python3 scripts/anr-wait.py "$file" 2>/dev/null)"; then got="tap $out"; else got=skip; fi
  if [[ "$got" == "$want" ]]; then
    echo "ok $name -> $got"
  else
    echo "FAIL $name: want '$want', got '$got'"
    fail=1
  fi
}

dialog "Pixel Launcher isn't responding" > "$tmp/launcher.xml"
dialog "PDFBook isn't responding" > "$tmp/own.xml"
dialog "pdfbook 앱이 응답하지 않습니다" > "$tmp/own-ko.xml"
dialog "" > "$tmp/no-title.xml"
dialog "Pixel Launcher isn't responding" "[66,1300]" > "$tmp/bad-bounds.xml"
printf '<hierarchy><node resource-id="library_screen" bounds="[0,0][1080,2400]" /></hierarchy>' > "$tmp/app.xml"
printf '<hierarchy><node' > "$tmp/broken.xml"

expect launcher "tap 540 1359 Pixel Launcher isn't responding"
expect own skip
expect own-ko skip
expect no-title skip
expect bad-bounds skip
expect app skip
expect broken skip
expect missing skip

exit "$fail"
