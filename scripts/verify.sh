#!/usr/bin/env bash
set -euo pipefail

FEATURES="library open-book sort-menu tap-width-sheet jump-back book-list settings keep-zoom e-ink"
PKG="org.parkjw.apps.pdfbook"
ACTIVITY="$PKG/.MainActivity"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_LABEL="$(sed -n 's|.*<string name="app_name">\(.*\)</string>.*|\1|p' "$ROOT/app/src/main/res/values/strings.xml" 2>/dev/null | head -1)"
APP_LABEL="${APP_LABEL:-PDFBook}"
AVD_NAME="${PDFBOOK_AVD:-pdfbook}"
APK="${PDFBOOK_APK:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
DEVICE_DIR="/sdcard/Download/pdfbook-verify"
BOOK_TITLE="PDFBook test book"

usage() {
  cat <<USAGE
Usage: scripts/verify.sh <feature>|all

Builds nothing; installs the debug APK, opens the app on an emulator or device,
drives it with adb + uiautomator, and saves evidence to build/verify/<feature>/<timestamp>.

Features: $FEATURES
"all" runs every feature, keeps going after failures, writes build/verify/summary.md,
and exits 1 if any feature failed.

Environment:
  ANDROID_HOME   Android SDK (needs platform-tools, emulator)
  PDFBOOK_AVD    AVD to boot when no device is attached (default: pdfbook)
  PDFBOOK_APK    APK to install (default: app/build/outputs/apk/debug/app-debug.apk)
  ANDROID_SERIAL Use this attached device instead of booting an emulator
USAGE
}

die() {
  echo "verify: $*" >&2
  exit 2
}

FEATURE="${1:-}"
if [ "$FEATURE" = all ]; then
  mkdir -p "$ROOT/build/verify"
  SUMMARY="$ROOT/build/verify/summary.md"
  printf '| Feature | Result | Seconds |\n|---|---|---|\n' >"$SUMMARY"
  failed=0
  for name in $FEATURES; do
    start=$(date +%s)
    if "${BASH_SOURCE[0]}" "$name"; then
      result=PASS
    else
      result=FAIL
      failed=1
    fi
    printf '| %s | %s | %s |\n' "$name" "$result" "$(($(date +%s) - start))" >>"$SUMMARY"
  done
  cat "$SUMMARY"
  exit "$failed"
fi
case " $FEATURES " in
  *" $FEATURE "*) ;;
  *)
    usage >&2
    exit 1
    ;;
esac

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android}}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
[ -x "$ADB" ] || ADB="$(command -v adb || true)"
[ -n "$ADB" ] && [ -x "$ADB" ] || die "adb not found. Install Android platform-tools and set ANDROID_HOME."
[ -f "$APK" ] || die "APK not found at $APK. Run ./gradlew assembleDebug first."
command -v python3 >/dev/null || die "python3 is required to read uiautomator dumps."

OUT="$ROOT/build/verify/$FEATURE/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
STEP=0
STARTED_EMULATOR=0
EMULATOR_PID=""

cleanup() {
  local status=$?
  if [ "$status" != 0 ] && [ -n "${ANDROID_SERIAL:-}" ]; then
    timeout 20 "$ADB" exec-out screencap -p >"$OUT/failure.png" 2>/dev/null || true
    [ -f "$OUT/ui.xml" ] && mv "$OUT/ui.xml" "$OUT/failure.xml"
    timeout 20 "$ADB" logcat -d -v threadtime >"$OUT/logcat.txt" 2>/dev/null || true
  fi
  if [ "$STARTED_EMULATOR" = 1 ]; then
    timeout 20 "$ADB" emu kill >/dev/null 2>&1 || true
    if [ -n "$EMULATOR_PID" ]; then
      for _ in $(seq 1 30); do
        kill -0 "$EMULATOR_PID" 2>/dev/null || break
        sleep 1
      done
      kill "$EMULATOR_PID" 2>/dev/null || true
      pkill -f "qemu-system.*-avd $AVD_NAME" 2>/dev/null || true
    fi
  fi
  if [ "$status" = 0 ]; then
    echo "verify: PASS $FEATURE -> $OUT"
  else
    echo "verify: FAIL $FEATURE (exit $status) -> $OUT" >&2
  fi
}
trap cleanup EXIT

attached_device() {
  "$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'
}

boot_emulator() {
  [ -x "$EMULATOR" ] || die "No device attached and no emulator at $EMULATOR. Install the 'emulator' SDK package or attach a device."
  "$EMULATOR" -list-avds | grep -qx "$AVD_NAME" || die "No device attached and AVD '$AVD_NAME' does not exist. Create it with avdmanager or set PDFBOOK_AVD."
  local args=(-avd "$AVD_NAME" -no-window -no-audio -no-boot-anim -memory 2560 -no-snapshot -gpu swiftshader_indirect)
  if [ -w /dev/kvm ]; then
    "$EMULATOR" "${args[@]}" >"$OUT/emulator.log" 2>&1 &
  elif id -nG "$(id -un)" | tr ' ' '\n' | grep -qx kvm && command -v sg >/dev/null; then
    sg kvm -c "$(printf '%q ' "$EMULATOR" "${args[@]}")" >"$OUT/emulator.log" 2>&1 &
  else
    die "/dev/kvm is not usable by $(id -un). Add the user to the kvm group; software emulation is not used."
  fi
  EMULATOR_PID=$!
  STARTED_EMULATOR=1
  echo "verify: booting AVD $AVD_NAME"
  timeout 300 "$ADB" wait-for-device || die "Emulator never came online in 5 minutes (see $OUT/emulator.log). Check that nested virtualization works for /dev/kvm."
  for _ in $(seq 1 150); do
    if [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
      "$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null
      "$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
      return 0
    fi
    sleep 2
  done
  die "Emulator did not finish booting in 5 minutes (see $OUT/emulator.log)."
}

dump_ui() {
  dump_raw
  dismiss_foreign_anr
}

# Remove the old dump first so a failed dump never re-reads stale XML.
dump_raw() {
  rm -f "$OUT/ui.xml"
  "$ADB" shell rm -f /sdcard/pdfbook-ui.xml >/dev/null 2>&1 || true
  "$ADB" shell uiautomator dump /sdcard/pdfbook-ui.xml >/dev/null 2>&1 || true
  "$ADB" pull /sdcard/pdfbook-ui.xml "$OUT/ui.xml" >/dev/null 2>&1 || true
}

# A slow emulator sometimes shows "<other app> isn't responding" over the
# screen under test (usually Pixel Launcher). Press Wait and dump again.
# scripts/anr-wait.py never matches PDFBook's own ANR, so that still fails.
dismiss_foreign_anr() {
  local found
  for _ in 1 2 3; do
    found="$(python3 "$ROOT/scripts/anr-wait.py" "$OUT/ui.xml" "$APP_LABEL" 2>/dev/null)" || return 0
    set -- $found
    echo "verify: dismissing system dialog: ${*:3}" >&2
    "$ADB" shell input tap "$1" "$2" >/dev/null 2>&1 || true
    sleep 2
    dump_raw
  done
  if python3 "$ROOT/scripts/anr-wait.py" "$OUT/ui.xml" "$APP_LABEL" >/dev/null 2>&1; then
    die "A system 'not responding' dialog is still on screen after 3 dismissals (last dump: $OUT/ui.xml)"
  fi
}

node_text() {
  python3 - "$OUT/ui.xml" "$1" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, tag = sys.argv[1:3]
try:
    nodes = ET.parse(path).getroot().iter("node")
except (ET.ParseError, FileNotFoundError):
    sys.exit(1)
for node in nodes:
    if node.get("resource-id") == tag:
        print(node.get("text", ""))
        sys.exit(0)
sys.exit(1)
PY
}

find_node() {
  python3 - "$OUT/ui.xml" "$1" "$2" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, attr, pattern = sys.argv[1:4]
try:
    nodes = ET.parse(path).getroot().iter("node")
except (ET.ParseError, FileNotFoundError):
    sys.exit(1)
rx = re.compile(pattern, re.IGNORECASE)
for node in nodes:
    if rx.fullmatch(node.get(attr, "")):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
        print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
        sys.exit(0)
sys.exit(1)
PY
}

wait_node() {
  local attr="$1" pattern="$2" tries="${3:-20}"
  for _ in $(seq 1 "$tries"); do
    dump_ui
    if find_node "$attr" "$pattern"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

tap_node() {
  local attr="$1" pattern="$2"
  local found
  found="$(wait_node "$attr" "$pattern")" || die "UI element $attr=$pattern not found (last dump: $OUT/ui.xml)"
  set -- $found
  "$ADB" shell input tap "$1" "$2"
  sleep 1
}

tap_id() {
  tap_node resource-id "$1"
}

expect_id() {
  wait_node resource-id "$1" >/dev/null || die "Expected $1 on screen (last dump: $OUT/ui.xml)"
}

shot() {
  STEP=$((STEP + 1))
  local name
  name="$(printf '%02d-%s' "$STEP" "$1")"
  dump_ui
  "$ADB" exec-out screencap -p >"$OUT/$name.png"
  cp "$OUT/ui.xml" "$OUT/$name.xml" 2>/dev/null || true
  echo "verify: saved $name.png"
}

prepare_book() {
  local tmp
  tmp="$(mktemp -d)"
  python3 "$ROOT/scripts/make-test-pdf.py" "$tmp/$BOOK_TITLE 01.pdf" --pages 30 --title "$BOOK_TITLE 01"
  python3 "$ROOT/scripts/make-test-pdf.py" "$tmp/$BOOK_TITLE 02.pdf" --pages 8 --title "$BOOK_TITLE 02"
  "$ADB" shell rm -rf "$DEVICE_DIR"
  "$ADB" shell mkdir -p "$DEVICE_DIR"
  "$ADB" push "$tmp/$BOOK_TITLE 01.pdf" "$tmp/$BOOK_TITLE 02.pdf" "$DEVICE_DIR/" >/dev/null
  "$ADB" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$DEVICE_DIR" >/dev/null 2>&1 || true
  rm -rf "$tmp"
}

pick_folder() {
  tap_id folder_pick_button
  wait_node resource-id '.*documentsui.*' 30 >/dev/null || die "Folder picker did not open (last dump: $OUT/ui.xml)"
  sleep 2
  local found=0
  for _ in 1 2 3 4 5 6; do
    dump_ui
    if find_node text 'pdfbook-verify' >/dev/null; then
      found=1
      break
    elif find_node text 'Download' >/dev/null; then
      tap_node text 'Download'
    elif find_node content-desc 'Show roots' >/dev/null; then
      tap_node content-desc 'Show roots'
      tap_node text 'sdk_gphone.*|Internal storage|Downloads'
    else
      sleep 1
    fi
    sleep 1
  done
  [ "$found" = 1 ] || die "Generated folder not found in the picker (last dump: $OUT/ui.xml)"
  tap_node text 'pdfbook-verify'
  sleep 1
  tap_node text 'Use this folder'
  tap_node text 'Allow'
  wait_node resource-id 'folder_shelf_screen|library_screen' 30 >/dev/null || die "App did not return to the shelf after picking the folder."
}

open_library() {
  if wait_node resource-id 'folder_pick_button' 15 >/dev/null; then
    pick_folder
  fi
  if wait_node resource-id 'folder_card' 15 >/dev/null; then
    tap_id folder_card
  fi
  expect_id library_screen
}

open_book() {
  open_library
  tap_id library_book
  expect_id reader_screen
  sleep 2
}

show_overlay() {
  if ! wait_node resource-id reader_overlay_bottom 1 >/dev/null; then
    tap_id reader_zone_Menu
  fi
  expect_id reader_overlay_bottom
}

slider_tap() {
  local fraction="$1" found
  found="$(wait_node resource-id reader_slider)" || die "Reader slider not found."
  set -- $found
  local x1="$3" x2="$5" y="$2"
  local x=$((x1 + (x2 - x1) * fraction / 100))
  "$ADB" shell input tap "$x" "$y"
  sleep 1
}

echo "verify: output -> $OUT"
DEVICE="${ANDROID_SERIAL:-$(attached_device)}"
if [ -z "$DEVICE" ]; then
  boot_emulator
  DEVICE="$(attached_device)"
fi
[ -n "$DEVICE" ] || die "No device available."
export ANDROID_SERIAL="$DEVICE"
echo "verify: device $DEVICE"

"$ADB" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
"$ADB" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
"$ADB" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
"$ADB" install -r -g "$APK" >/dev/null
"$ADB" shell pm clear "$PKG" >/dev/null
prepare_book
"$ADB" logcat -c
"$ADB" shell am start -W -n "$ACTIVITY" >"$OUT/am-start.txt"
sleep 2
"$ADB" shell dumpsys gfxinfo "$PKG" reset >/dev/null

case "$FEATURE" in
  library)
    open_library
    shot library
    ;;
  open-book)
    open_book
    shot reader-page
    show_overlay
    shot reader-overlay
    ;;
  sort-menu)
    open_library
    tap_id library_sort_button
    expect_id library_sort_option_Recent
    shot sort-menu-recent
    tap_id library_sort_option_Name
    tap_id library_sort_button
    expect_id library_sort_check
    shot sort-menu-name
    "$ADB" shell input keyevent KEYCODE_BACK
    ;;
  tap-width-sheet)
    open_book
    show_overlay
    tap_id reader_tap_zone_entry
    expect_id tap_zone_width_sheet
    shot tap-width-sheet
    tap_id tap_zone_width_done
    ;;
  jump-back)
    open_book
    show_overlay
    expect_id reader_progress
    origin="$(node_text reader_progress)"
    slider_tap 80
    pill="$(wait_node resource-id jump_back_pill 5)" || die "Jump back pill did not appear (last dump: $OUT/ui.xml)"
    STEP=$((STEP + 1))
    "$ADB" exec-out screencap -p >"$OUT/$(printf '%02d' "$STEP")-jump-back-pill.png"
    set -- $pill
    "$ADB" shell input tap "$1" "$2"
    sleep 1
    returned=""
    for _ in 1 2 3 4 5; do
      dump_ui
      returned="$(node_text reader_progress || true)"
      [ "$returned" = "$origin" ] && break
      sleep 1
    done
    [ "$returned" = "$origin" ] || die "Jump back did not return to $origin (now $returned)."
    shot jump-back-returned
    ;;
  book-list)
    open_book
    show_overlay
    tap_id reader_book_list_button
    expect_id book_list_row
    shot book-list
    ;;
  settings)
    open_library
    tap_id library_settings
    expect_id settings_screen
    shot settings
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    shot settings-reading
    ;;
  keep-zoom)
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    keep_zoom='Keep zoom when turning pages|넘겨도 확대 유지'
    for _ in 1 2 3; do
      wait_node text "$keep_zoom" 2 >/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    shot keep-zoom-off
    tap_node text "$keep_zoom"
    shot keep-zoom-on
    ;;
  e-ink)
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    e_ink='E-ink mode|전자잉크 모드'
    for _ in 1 2 3; do
      wait_node text "$e_ink" 2 >/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    shot e-ink-off
    tap_node text "$e_ink"
    shot e-ink-on
    ;;
esac

"$ADB" shell dumpsys gfxinfo "$PKG" >"$OUT/gfxinfo.txt"
grep -E 'Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile' "$OUT/gfxinfo.txt" | sed 's/^ *//' | head -6 >"$OUT/gfxinfo-summary.txt" || true
"$ADB" logcat -d -v threadtime >"$OUT/logcat.txt"
APP_PID="$("$ADB" shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$APP_PID" ]; then
  awk -v pid="$APP_PID" '$3 == pid' "$OUT/logcat.txt" >"$OUT/logcat-app.txt"
fi
# Every crash and PDFBook's own ANRs fail the run. Other apps' ANRs (for
# example a Pixel Launcher ANR dismissed above) are kept in foreign-anrs.txt.
awk -v pkg="$PKG" -v foreign="$OUT/foreign-anrs.txt" -f "$ROOT/scripts/logcat-crashes.awk" "$OUT/logcat.txt" >"$OUT/crashes.txt" \
  || die "Could not scan logcat for crashes (see $OUT/logcat.txt)."
rm -f "$OUT/ui.xml"
"$ADB" shell rm -rf "$DEVICE_DIR" /sdcard/pdfbook-ui.xml
if [ -s "$OUT/crashes.txt" ]; then
  die "Crash or ANR found in logcat (see $OUT/crashes.txt)."
fi
