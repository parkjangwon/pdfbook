#!/usr/bin/env bash
set -euo pipefail

FEATURES="library open-book sort-menu tap-width-sheet jump-back book-list bookmarks settings keep-zoom one-finger-zoom display-mode page-fit spread-shift"
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
    # A failed feature must not leave the device rotated, at 320dp/font 2 or in dark
    # mode: verify.sh all runs the next feature right after this one.
    "$ADB" shell settings put system accelerometer_rotation 1 >/dev/null 2>&1 || true
    "$ADB" shell settings put system user_rotation 0 >/dev/null 2>&1 || true
    "$ADB" shell wm density reset >/dev/null 2>&1 || true
    "$ADB" shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
    "$ADB" shell cmd uimode night no >/dev/null 2>&1 || true
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
  # Mixed page sizes for the page fit turns; never opened, so it sorts after 01 and 02.
  python3 "$ROOT/scripts/make-test-pdf.py" "$tmp/$BOOK_TITLE 03.pdf" --pages 6 \
    --title "$BOOK_TITLE 03" --mixed
  "$ADB" shell rm -rf "$DEVICE_DIR"
  "$ADB" shell mkdir -p "$DEVICE_DIR"
  "$ADB" push "$tmp/$BOOK_TITLE 01.pdf" "$tmp/$BOOK_TITLE 02.pdf" \
    "$tmp/$BOOK_TITLE 03.pdf" "$DEVICE_DIR/" >/dev/null
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
  # Optional title pattern: opens that library row instead of the first book.
  local title="${1:-}"
  open_library
  if [ -n "$title" ]; then
    wait_node text "$title" 20 >/dev/null || die "Book '$title' not in the library (last dump: $OUT/ui.xml)"
    tap_node text "$title"
  else
    tap_id library_book
  fi
  local found
  for _ in $(seq 1 20); do
    dump_ui
    if found="$(find_node resource-id 'android:id/ok')"; then
      echo "verify: dismissing the full-screen hint"
      set -- $found
      "$ADB" shell input tap "$1" "$2"
      sleep 1
      continue
    fi
    if find_node resource-id reader_screen >/dev/null; then
      sleep 2
      return 0
    fi
    sleep 1
  done
  die "Expected reader_screen on screen (last dump: $OUT/ui.xml)"
}

show_overlay() {
  if ! wait_node resource-id reader_overlay_bottom 1 >/dev/null; then
    tap_id reader_zone_Menu
  fi
  expect_id reader_overlay_bottom
}

# Scrolls up within the current screen, using the dumped window bounds so it also
# works in landscape where the fixed portrait coordinates are off-screen.
swipe_up() {
  local coords
  coords="$(python3 - "$OUT/ui.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except (ET.ParseError, FileNotFoundError):
    sys.exit(1)
x1, y1, x2, y2 = map(int, re.findall(r"\d+", root.get("bounds", "")))
cx = (x1 + x2) // 2
height = y2 - y1
print(cx, y2 - height // 5, y1 + height // 5)
PY
)" || return 1
  set -- $coords
  "$ADB" shell input swipe "$1" "$2" "$1" "$3" 300
}

# Scrolls the current screen until a node with this text is visible, then stops.
scroll_to_text() {
  local pattern="$1" tries="${2:-6}"
  for _ in $(seq 1 "$tries"); do
    dump_ui
    if find_node text "$pattern" >/dev/null; then
      return 0
    fi
    swipe_up
    sleep 1
  done
  return 1
}

# Reading settings from a deterministic start: restart, then library -> settings -> reading.
reading_settings() {
  "$ADB" shell am force-stop "$PKG" >/dev/null 2>&1 || true
  "$ADB" shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
  sleep 2
  open_library
  tap_id library_settings
  expect_id settings_screen
  # The reading row is below the fold in landscape and at 320dp / font scale 2.
  for _ in $(seq 1 6); do
    dump_ui
    if find_node resource-id settings_reading_entry >/dev/null; then
      break
    fi
    swipe_up
    sleep 1
  done
  tap_id settings_reading_entry
  expect_id settings_reading_screen
}

leave_settings() {
  # Restart instead of guessing how many Backs the current screen needs; the library is
  # where every caller expects to be next.
  "$ADB" shell am force-stop "$PKG" >/dev/null 2>&1 || true
  "$ADB" shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
  sleep 2
}

# reader_progress lives in the main window. While the more-menu popup is open,
# uiautomator dumps only the popup, so close it, read the page, then reopen.
progress_with_menu_closed() {
  local value
  "$ADB" shell input keyevent KEYCODE_BACK
  wait_node resource-id reader_progress 10 >/dev/null \
    || die "reader_progress is not on screen with the more menu closed (last dump: $OUT/ui.xml)"
  value="$(node_text reader_progress || true)"
  [ -n "$value" ] || die "reader_progress was empty with the more menu closed (last dump: $OUT/ui.xml)"
  printf '%s\n' "$value"
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
# Skip the one-time "Viewing full screen" cling that would cover the reader.
"$ADB" shell settings put secure immersive_mode_confirmations confirmed >/dev/null 2>&1 || true
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
    tap_id reader_more_button
    tap_id reader_more_book_list
    expect_id book_list_row
    shot book-list
    ;;
  bookmarks)
    open_book
    show_overlay
    tap_id reader_bookmark_toggle
    shot bookmark-added
    tap_id reader_bookmark_list_button
    expect_id bookmark_sheet
    expect_id bookmark_row
    shot bookmark-list
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
  one-finger-zoom)
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    one_finger_zoom='One-finger zoom|한 손가락 확대'
    for _ in 1 2 3 4; do
      wait_node text "$one_finger_zoom" 2>/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    shot one-finger-zoom-off
    tap_node text "$one_finger_zoom"
    shot one-finger-zoom-on
    ;;
  display-mode)
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    display_mode='Page color|페이지 색'
    for _ in 1 2 3; do
      wait_node text "$display_mode" 2 >/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    shot display-mode-default
    tap_node text "$display_mode"
    tap_node text 'Inverted|색 반전'
    "$ADB" shell input keyevent KEYCODE_BACK
    "$ADB" shell input keyevent KEYCODE_BACK
    open_book
    shot display-mode-invert
    ;;
  page-fit)
    page_fit='쪽 맞춤|Page fit'
    fit_width='너비 맞춤|Fit width'
    fit_height='높이 맞춤|Fit height'
    two_page='두쪽 보기|Two-page view'
    turn_animation='넘김 애니메이션|Page turn animation'
    curl_animation='책장 넘김|Page curl'

    # 1. No book read yet: the row is disabled, light and dark.
    reading_settings
    scroll_to_text "$page_fit" || die "Page fit row not found (last dump: $OUT/ui.xml)"
    shot page-fit-row-disabled
    "$ADB" shell cmd uimode night yes
    sleep 3
    reading_settings
    scroll_to_text "$page_fit" || true
    shot page-fit-row-disabled-dark
    "$ADB" shell cmd uimode night no
    sleep 3

    # 2. Read a book so the row targets it; one page and fit width, light dialog (RTL).
    reading_settings
    leave_settings
    open_book
    "$ADB" shell input keyevent KEYCODE_BACK
    reading_settings
    tap_node text "$two_page"
    tap_node text '항상 한쪽|Always single'
    scroll_to_text "$page_fit" || die "Page fit row not found (last dump: $OUT/ui.xml)"
    tap_node text "$page_fit"
    wait_node text "$fit_width" 20 >/dev/null || die "Page fit dialog did not open (last dump: $OUT/ui.xml)"
    shot page-fit-dialog
    tap_node text "$fit_width"
    sleep 1
    shot page-fit-row-width

    # 3. Dark dialog (ui mode recreates the activity, so start the scene again).
    "$ADB" shell cmd uimode night yes
    sleep 3
    reading_settings
    scroll_to_text "$page_fit" || true
    tap_node text "$page_fit"
    wait_node text "$fit_height" 20 >/dev/null || die "Page fit dialog did not open in dark (last dump: $OUT/ui.xml)"
    shot page-fit-dialog-dark
    "$ADB" shell input keyevent KEYCODE_BACK
    "$ADB" shell cmd uimode night no
    sleep 3

    # 4. Width fit, landscape single page: start, one screen down, end, next, previous.
    leave_settings
    open_book
    "$ADB" shell settings put system accelerometer_rotation 0
    "$ADB" shell settings put system user_rotation 1
    sleep 4
    shot page-fit-width-start
    tap_id reader_zone_Next
    shot page-fit-width-one-screen-down
    tap_id reader_zone_Next
    tap_id reader_zone_Next
    shot page-fit-width-end
    tap_id reader_zone_Next
    shot page-fit-width-next-page-start
    tap_id reader_zone_Previous
    shot page-fit-width-previous-page-end

    # 5. Auto-crop plus width fit: the cropped page must still fill the width.
    show_overlay
    tap_id reader_auto_crop
    expect_id reader_crop_sheet
    tap_id reader_crop_switch
    tap_id reader_crop_done
    show_overlay
    tap_id reader_back
    open_book
    sleep 1
    shot page-fit-crop-width-start

    # 6. Two-page view in landscape falls back to Fit screen.
    "$ADB" shell settings put system user_rotation 0
    sleep 3
    reading_settings
    tap_node text "$two_page"
    tap_node text '항상 두쪽|Always double'
    leave_settings
    "$ADB" shell settings put system user_rotation 1
    sleep 3
    open_book
    sleep 1
    # Page 1 is the cover alone; one turn shows a real two-page spread in the fallback.
    tap_id reader_zone_Next
    shot page-fit-two-page-fallback
    "$ADB" shell settings put system user_rotation 0
    sleep 3

    # 7. Height fit portrait RTL starts at the right edge.
    reading_settings
    tap_node text "$two_page"
    tap_node text '자동|Auto'
    "$ADB" shell settings put system user_rotation 0
    sleep 3
    reading_settings
    scroll_to_text "$page_fit" || true
    tap_node text "$page_fit"
    wait_node text "$fit_height" 20 >/dev/null || die "Page fit dialog did not open (last dump: $OUT/ui.xml)"
    tap_node text "$fit_height"
    leave_settings
    open_book
    sleep 1
    shot page-fit-height-rtl-start

    # 8. LTR book: height fit starts at the left edge, and the dialog mirrors.
    show_overlay
    tap_id reader_direction_toggle
    tap_id reader_back
    open_book
    sleep 1
    shot page-fit-height-ltr-start
    "$ADB" shell input keyevent KEYCODE_BACK
    reading_settings
    scroll_to_text "$page_fit" || true
    tap_node text "$page_fit"
    wait_node text "$fit_height" 20 >/dev/null || die "Page fit dialog did not open (last dump: $OUT/ui.xml)"
    shot page-fit-dialog-ltr
    "$ADB" shell input keyevent KEYCODE_BACK

    # 9. 320dp width at font scale 2: nothing in the dialog is clipped.
    "$ADB" shell settings put system font_scale 2.0
    "$ADB" shell wm density 540
    sleep 4
    reading_settings
    scroll_to_text "$page_fit" 10 || true
    tap_node text "$page_fit"
    # At 320dp / font 2 the third option sits below the dialog fold.
    scroll_to_text "$fit_height" 6 || true
    wait_node text "$fit_height" 30 >/dev/null || die "Page fit dialog did not open at 320dp/font 2 (last dump: $OUT/ui.xml)"
    shot page-fit-dialog-320dp-font2
    "$ADB" shell input keyevent KEYCODE_BACK
    "$ADB" shell wm density reset
    "$ADB" shell settings put system font_scale 1.0
    "$ADB" shell settings put system user_rotation 0
    "$ADB" shell settings put system accelerometer_rotation 1
    sleep 2

    # 10. Mixed page sizes with the curl preference: a turn from a fitting page onto an
    #     overflowing one falls back to Slide and lands at the start corner; the backward
    #     turn onto the overflowing page lands at its end corner.
    # Read it once so the per-book fit row targets it, then width fit and page curl.
    open_book "$BOOK_TITLE 03"
    reading_settings
    scroll_to_text "$page_fit" || die "Page fit row not found (last dump: $OUT/ui.xml)"
    tap_node text "$page_fit"
    wait_node text "$fit_width" 20 >/dev/null || die "Page fit dialog did not open (last dump: $OUT/ui.xml)"
    tap_node text "$fit_width"
    sleep 1
    scroll_to_text "$turn_animation" || die "Page turn row not found (last dump: $OUT/ui.xml)"
    tap_node text "$turn_animation"
    wait_node text "$curl_animation" 20 >/dev/null || die "Page turn dialog did not open (last dump: $OUT/ui.xml)"
    tap_node text "$curl_animation"
    leave_settings
    open_book "$BOOK_TITLE 03"
    sleep 1
    # Page 1 is tall (overflows), page 2 is square (fits): two next taps reach page 2.
    tap_id reader_zone_Next
    tap_id reader_zone_Next
    sleep 1
    shot page-fit-mixed-fitting-page
    tap_id reader_zone_Next
    sleep 1
    shot page-fit-mixed-forward-start
    tap_id reader_zone_Previous
    sleep 1
    tap_id reader_zone_Previous
    sleep 1
    shot page-fit-mixed-backward-end
    ;;
  spread-shift)
    open_book
    show_overlay
    tap_id reader_more_button
    expect_id reader_more_status
    wait_node resource-id reader_more_spread_shift 1 >/dev/null \
      && die "Shift row must stay hidden in single-page view (last dump: $OUT/ui.xml)"
    shot spread-shift-single-page
    # Back closes the more menu popup first; the top bar's back button then leaves the reader.
    "$ADB" shell input keyevent KEYCODE_BACK
    tap_id reader_back
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    two_page='Two-page view|두쪽 보기'
    for _ in 1 2 3; do
      wait_node text "$two_page" 2 >/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    tap_node text "$two_page"
    tap_node text 'Always double|항상 두쪽'
    "$ADB" shell input keyevent KEYCODE_BACK
    "$ADB" shell input keyevent KEYCODE_BACK
    open_book
    # Page 1 is [1], then [2·3], [4·5] ... so six turns land on [12·13].
    for _ in $(seq 1 6); do tap_id reader_zone_Next; done
    show_overlay
    expect_id reader_progress
    page_before="$(node_text reader_progress || true)"
    [ -n "$page_before" ] || die "reader_progress was empty before the shift (last dump: $OUT/ui.xml)"
    [ "$page_before" = "12 / 30" ] \
      || die "Expected the reader at page 12 before the shift (got '$page_before')."
    tap_id reader_more_button
    expect_id reader_more_status
    expect_id reader_more_spread_shift
    shot spread-shift-menu-off
    # The reader does not expose reader_page_* to uiautomator, so read the two
    # pages from their colours (scripts/make-test-pdf.py paints one per page).
    # The book is RTL: the right-hand page comes first in the pair.
    pair_off="$(python3 "$ROOT/scripts/page-pair.py" "$OUT/"*-spread-shift-menu-off.png)" \
      || die "Could not read the pair before the shift (last dump: $OUT/ui.xml)"
    case "$pair_off" in
      "13 12") ;;
      *) die "Expected the plain RTL pair 13 12 before the shift, saw '$pair_off' (last dump: $OUT/ui.xml)" ;;
    esac
    tap_id reader_more_spread_shift
    shot spread-shift-menu-on
    pair_on="$(python3 "$ROOT/scripts/page-pair.py" "$OUT/"*-spread-shift-menu-on.png)" \
      || die "Could not read the pair after the shift (last dump: $OUT/ui.xml)"
    case "$pair_on" in
      "12 11") ;;
      *) die "Shift must pair 11 and 12 as 12 11, saw '$pair_on' (last dump: $OUT/ui.xml)" ;;
    esac
    page_on="$(progress_with_menu_closed)"
    [ "$page_on" = "$page_before" ] \
      || die "Shift must keep the page being read ($page_before -> $page_on)."
    tap_id reader_more_button
    expect_id reader_more_status
    expect_id reader_more_spread_shift
    tap_id reader_more_spread_shift
    shot spread-shift-menu-back
    pair_back="$(python3 "$ROOT/scripts/page-pair.py" "$OUT/"*-spread-shift-menu-back.png)" \
      || die "Could not read the pair after toggling back (last dump: $OUT/ui.xml)"
    case "$pair_back" in
      "13 12") ;;
      *) die "Toggling back must pair 12 and 13 as 13 12 again, saw '$pair_back' (last dump: $OUT/ui.xml)" ;;
    esac
    page_back="$(progress_with_menu_closed)"
    [ "$page_back" = "$page_before" ] \
      || die "Toggling back must keep the page being read ($page_before -> $page_back)."
    # Put the reading settings back so a rerun starts where this one did.
    tap_id reader_back
    open_library
    tap_id library_settings
    expect_id settings_screen
    tap_id settings_reading_entry
    expect_id settings_reading_screen
    for _ in 1 2 3; do
      wait_node text "$two_page" 2 >/dev/null && break
      "$ADB" shell input swipe 540 1600 540 900 300
    done
    tap_node text "$two_page"
    tap_node text 'Auto|자동'
    "$ADB" shell input keyevent KEYCODE_BACK
    "$ADB" shell input keyevent KEYCODE_BACK
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
