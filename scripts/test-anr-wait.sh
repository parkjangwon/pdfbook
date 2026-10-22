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
dialog "Pixel Launcher이(가) 응답하지 않음" > "$tmp/launcher-ko.xml"
dialog "Something went wrong" > "$tmp/other-title.xml"
dialog " isn't responding" > "$tmp/no-app.xml"
dialog "Pixel Launcher isn't responding" "[66,1300]" > "$tmp/bad-bounds.xml"
printf '<hierarchy><node resource-id="library_screen" bounds="[0,0][1080,2400]" /></hierarchy>' > "$tmp/app.xml"
printf '<hierarchy><node' > "$tmp/broken.xml"

expect launcher "tap 540 1359 Pixel Launcher isn't responding"
expect own skip
expect own-ko skip
expect no-title skip
expect launcher-ko "tap 540 1359 Pixel Launcher이(가) 응답하지 않음"
expect other-title skip
expect no-app skip
expect bad-bounds skip
expect app skip
expect broken skip
expect missing skip

# logcat filter: only PDFBook's crashes and ANRs fail the run.
cat > "$tmp/logcat.txt" <<'LOG'
10-22 16:10:36.364  900  950 E ActivityManager: ANR in com.google.android.apps.nexuslauncher (com.google.android.apps.nexuslauncher/.NexusLauncherActivity)
10-22 16:10:37.000 1200 1200 E AndroidRuntime: FATAL EXCEPTION: main
10-22 16:10:37.000 1200 1200 E AndroidRuntime: Process: com.example.other, PID: 1200
10-22 16:10:38.000 3100 3100 E AndroidRuntime: FATAL EXCEPTION: main
10-22 16:10:38.000 3100 3100 E AndroidRuntime: Process: org.parkjw.apps.pdfbook, PID: 3100
10-22 16:10:39.000  900  950 E ActivityManager: ANR in org.parkjw.apps.pdfbook (org.parkjw.apps.pdfbook/.MainActivity)
10-22 16:10:40.000 4000 4000 E AndroidRuntime: FATAL EXCEPTION: worker
LOG
awk -v pkg=org.parkjw.apps.pdfbook -v foreign="$tmp/foreign.txt" -f scripts/logcat-crashes.awk "$tmp/logcat.txt" > "$tmp/own.txt"
check_count() {
  local name="$1" file="$2" want="$3" got
  got="$(wc -l < "$file" | tr -d ' ')"
  if [[ "$got" == "$want" ]]; then echo "ok $name -> $got lines"; else echo "FAIL $name: want $want lines, got $got"; fail=1; fi
}
check_count own-crashes "$tmp/own.txt" 4
check_count foreign-crashes "$tmp/foreign.txt" 3
grep -q 'nexuslauncher' "$tmp/own.txt" && { echo "FAIL launcher ANR counted as own"; fail=1; } || echo "ok launcher ANR is foreign"

exit "$fail"
