# Feature map for agents

How to reach every screen and feature from a cold launch, what to target, and where the code lives.
Source paths are relative to `app/src/main/java/org/parkjw/apps/pdfbook/`; tests are under `app/src/test/java/org/parkjw/apps/pdfbook/`.

## Targeting

- The root `Surface` in `MainActivity.kt` sets `testTagsAsResourceId = true`, so every `Modifier.testTag("x")` shows up in `uiautomator dump` as `resource-id="x"` (no package prefix).
- Tags repeated in lists (`folder_card`, `library_book`, `book_list_row`, `library_subfolder`) match the first visible item. Pick a specific item by its text.
- System screens (folder picker, permission dialogs) have no tags; match them by text.
- `scripts/verify.sh <feature>` drives the flows below and saves screenshots, UI dumps, logcat and `dumpsys gfxinfo` to `build/verify/<feature>/<timestamp>/`.
- `scripts/verify.sh all` runs every feature and writes `build/verify/summary.md`. The "Verify on emulator" workflow runs it on PRs whose changed files match the `needs-design` globs in `.github/labeler.yml` (checked by `scripts/needs-design.py`) or that carry the label and uploads `build/verify/` as the `verify-evidence` artifact.

## Launch and folders

| Feature | Entry path (taps from launch) | testTag / resource-id | Source | Tests |
|---|---|---|---|---|
| First run: pick a folder | Launch with no folders | `folder_pick_button` | `LibraryScreen.kt` (`FolderPickScreen`), `MainActivity.kt` | `FolderBrowseTest` |
| System folder picker | `folder_pick_button` → open `Download/<folder>` → "Use this folder" → "Allow" | text only | `MainActivity.kt` (`OpenDocumentTree`) | – |
| Folder shelf (서재) | Launch with at least one folder | `folder_shelf_screen`, `folder_card`, `folder_shelf_add`, `folder_shelf_settings` | `FolderShelfScreen.kt`, `FolderShelf.kt` | `FolderShelfTest`, `MainScreenshotsTest` |
| Continue reading (shelf) | Shelf, after reading any book | `folder_shelf_resume` | `FolderShelfScreen.kt`, `FolderShelf.kt` | `FolderShelfTest` |
| Folder menu: rename, re-pick, hide, remove | Shelf → `folder_card_menu` (or long-press `folder_card`) | `folder_card_menu`, then menu text | `FolderShelfScreen.kt`, `folders/FolderRepository.kt` | `FolderRepositoryTest`, `FolderWorkspaceTest` |
| Folder reorder | Shelf with 2+ folders → long-press and drag `folder_reorder_handle` (TalkBack: move up/down actions) | `folder_reorder_handle` | `FolderReorder.kt` | `FolderReorderComposeTest` |
| Lost folder access | Shelf → card with "no access" badge → re-pick | `folder_card`, `folder_pick_button` | `FolderShelfScreen.kt`, `folders/UriPermissions.kt` | `FolderRepositoryTest` |
| Double back to exit | Shelf → Back twice within 2 s | snackbar text | `DoubleBackExit.kt`, `MainActivity.kt` | `DoubleBackExitTest`, `FolderShelfExitHintTest` |
| Update banner | Shelf, when a newer release exists | text only | `update/UpdateUi.kt`, `update/UpdateFlow.kt` | `UpdateBannerUiTest`, `UpdateFlowTest` |

## Library (book list inside a folder)

| Feature | Entry path | testTag / resource-id | Source | Tests |
|---|---|---|---|---|
| Library | Shelf → `folder_card` | `library_screen`, `library_book`, `library_back`, `library_settings`, `library_more` | `LibraryScreen.kt`, `Library.kt`, `LibraryScan.kt` | `LibraryTest`, `BookWordingTest` |
| Continue reading (folder) | Library, sort = recent, after reading | `library_resume_card` | `LibraryScreen.kt`, `hidden/HiddenPeek.kt` | `LibraryTest` |
| Sort menu (SPEC-1) | Library → `library_sort_button` | `library_sort_button`, `library_sort_option_Recent`, `library_sort_option_Name`, `library_sort_check` | `LibraryScreen.kt`, `SortStore.kt`, `NaturalOrder.kt` | `SortCheckMenuComposeTest`, `SortPersistComposeTest`, `SortStoreTest`, `NaturalOrderTest` |
| Subfolders | Library of a folder with subfolders → `library_subfolder` | `library_subfolder`, `library_back` | `SubfolderCell.kt`, `Subfolders.kt`, `FolderBrowse.kt` | `SubfolderComposeTest`, `SubfoldersTest`, `SubfolderScreenshotsTest` |
| Book info | Long-press `library_book` → "책 정보" | `book_info_sheet` | `BookInfoSheet.kt`, `LibraryScreen.kt` | `BookInfoTest`, `BookInfoComposeTest`, `BookInfoScreenshotsTest` |
| Hide a book or folder | Long-press `library_book` / `library_subfolder` → "Hide" | menu text | `hidden/HiddenRepository.kt`, `MainActivity.kt` | `HiddenRepositoryTest`, `HiddenBookStateTest` |
| Peek hidden items | `library_more` (or shelf ⋮) → peek toggle | menu text | `hidden/HiddenPeek.kt`, `hidden/PeekHiddenUi.kt` | `HiddenPeekComposeTest`, `HiddenPeekVisibilityTest` |

## Reader

| Feature | Entry path | testTag / resource-id | Source | Tests |
|---|---|---|---|---|
| Open book | Library → `library_book` | `reader_screen` | `ReaderRoute.kt`, `ReaderScreen.kt`, `PageRenderer.kt` | `PrerenderReaderTest` |
| Tap zones | Reader → tap left / centre / right | `reader_zone_Previous`, `reader_zone_Menu`, `reader_zone_Next` | `TapZones.kt`, `ReaderScreen.kt` | `TapZonesTest`, `TapZoneTimingTest` |
| Reader overlay | Reader → `reader_zone_Menu` | `reader_overlay_top`, `reader_overlay_bottom`, `reader_back`, `reader_progress`, `reader_page_mode` | `ReaderScreen.kt` | `ReaderVolumesComposeTest` |
| Reading direction toggle | Overlay → `reader_direction_toggle` | `reader_direction_toggle` | `ReadingDirection.kt`, `BookStore.kt` | `PagerNavTest` |
| Page slider | Overlay → drag or tap `reader_slider` | `reader_slider` | `ReaderScreen.kt`, `PagerNav.kt` | `PagerNavTest` |
| Go to page (#116) | Overlay → tap `reader_progress` → type a page → "이동" | `reader_progress`, `go_to_page_dialog`, `go_to_page_field` | `ReaderScreen.kt`, `GoToPageDialog.kt` | `GoToPageTest`, `GoToPageComposeTest`, `GoToPageScreenshotsTest` |
| Jump back pill (SPEC-4) | Overlay → tap `reader_slider` far from the current page → close overlay | `jump_back_pill` | `JumpBack.kt`, `JumpBackPill.kt` | `JumpBackTest`, `JumpBackComposeTest`, `JumpBackScreenshotsTest` |
| Tap zone guide | Overlay open with "Tap zone guide" on in reading settings | `tap_zone_guide` | `TapZoneGuide.kt` | `TapZoneGuideTest`, `TapZoneGuidePixelTest` |
| Tap zone width sheet, reader (SPEC-2 TW-3) | Overlay → `reader_tap_zone_entry` | `tap_zone_width_sheet`, `tap_zone_width_slider`, `tap_zone_width_reset`, `tap_zone_width_done`, `tap_zone_width_preview` | `TapZoneWidthSheet.kt`, `ReaderScreen.kt` | `TapZoneWidthSheetTest`, `TapZoneWidthPixelTest`, `TapZoneWidthTest` |
| Reader tool row | Overlay bottom → `reader_tool_row` | `reader_tool_row`, `reader_brightness`, `reader_bookmark_list_button` (+ later auto-turn / auto-crop) | `ReaderToolRow.kt`, `ReaderScreen.kt` | `ReaderToolRowTest`, `ReaderToolsScreenshotsTest` |
| Reader brightness (#120) | Overlay → `reader_brightness` | `reader_brightness`, `reader_brightness_sheet`, `reader_brightness_switch`, `reader_brightness_slider`, `reader_brightness_value` | `ReaderBrightness.kt`, `ReaderBrightnessEffect.kt`, `ReaderBrightnessSheet.kt`, `ReaderScreen.kt`, `ReaderToolRow.kt`, `settings/AppSettings.kt` | `ReaderBrightnessTest`, `ReaderBrightnessSettingsTest`, `ReaderBrightnessSheetTest`, `ReaderBrightnessWindowTest`, `ReaderBrightnessScreenshotsTest` |
| Previous / next volume buttons | Overlay → `reader_volume_left` / `reader_volume_right` | `reader_volume_left`, `reader_volume_right`, `reader_book_changed_toast` | `VolumeNav.kt`, `ReaderScreen.kt` | `VolumeNavTest`, `ReaderVolumesScreenshotsTest` |
| Book list sheet (SPEC-3) | Overlay → `reader_book_list_button` | `book_list_sheet`, `book_list_row` | `BookListSheet.kt` | `ReaderVolumesComposeTest` |
| Bookmark toggle (#56) | Overlay → `reader_bookmark_toggle` (two-page: acts on the whole spread) | `reader_bookmark_toggle` | `Bookmarks.kt`, `ReaderScreen.kt`, `ReaderRoute.kt`, `BookStore.kt` | `BookmarksTest` |
| Bookmark list sheet (#56) | Overlay → tool row `reader_bookmark_list_button` → tap `bookmark_row` to jump, `bookmark_delete` to remove | `reader_bookmark_list_button`, `bookmark_sheet`, `bookmark_row`, `bookmark_delete` | `Bookmarks.kt`, `ReaderScreen.kt`, `ReaderToolRow.kt` | `BookmarksTest`, `ReaderBookmarksScreenshotsTest` |
| Next book notice at the last page (SPEC-8) | Last page → `reader_zone_Next` (again to open) | `end_notice`, `end_notice_open` | `NextBook.kt`, `NextBookNotice.kt`, `NoticeSlot.kt` | `NextBookTest`, `NextBookComposeTest`, `NextBookScreenshotsTest` |
| Two-page spreads and foldable posture | Wide screen or unfolded device; setting under Reading → two-page | `reader_page_mode` | `TwoPage.kt`, `Spreads.kt`, `FoldPosture.kt`, `ReadingAnchor.kt` | `TwoPageTest`, `SpreadsTest`, `SpreadLayoutTest`, `SpreadIndexOfTest`, `FoldPostureTest`, `ReadingAnchorTest` |
| Tabletop posture pads (SPEC-7) | Foldable half-opened, horizontal hinge | `tabletop_pads`, `tabletop_pad_Previous`, `tabletop_pad_Menu`, `tabletop_pad_Next`, `tabletop_page_number`, `reader_page_area` | `TabletopPads.kt`, `FoldLayout.kt`, `ReaderScreen.kt` | `FoldPostureComposeTest`, `FoldPostureScreenshotsTest`, `FoldLayoutTest` |
| Book posture fold gap (SPEC-7) | Foldable half-opened, vertical hinge | `book_posture_fold_gap`, `reader_page_*` | `FoldLayout.kt`, `ReaderScreen.kt` | `FoldPostureComposeTest`, `FoldPostureScreenshotsTest` |
| Pinch / double-tap zoom | Reader → pinch, or double-tap a page | `reader_screen` | `ZoomPan.kt` | `ZoomPanTest` |
| Zoom lock and keep zoom (SPEC-6) | Reader → zoom in; swipes only pan, tap zones and volume keys still turn | `reader_screen` (`ReaderZoomKey` semantics), `reader_zone_Next` | `ZoomPan.kt`, `ReaderScreen.kt` | `ZoomLockComposeTest`, `ZoomPanTest`, `TapZoneTimingTest` |
| Volume key paging | Reader → volume up / down | – | `MainActivity.kt`, `VolumeNav.kt` | `VolumeNavTest` |
| Full screen reading (#115) | Reader with full screen reading on (default) | – | `ReaderSystemBars.kt`, `ReaderScreen.kt` | `ReaderSystemBarsTest`, `ReaderImmersiveComposeTest` |
| Prerender and memory trim (SPEC-5) | Reader → turn pages | – | `PageCache.kt`, `MemoryTrim.kt`, `PageRenderer.kt` | `PageCacheTest`, `PrerenderTargetsTest`, `LargeBookPrerenderTest` |

## Settings

| Feature | Entry path | testTag / resource-id | Source | Tests |
|---|---|---|---|---|
| Settings | Shelf → `folder_shelf_settings`, or library → `library_settings` | `settings_screen` | `settings/SettingsScreen.kt`, `settings/SettingsRepository.kt` | `SettingsRepositoryTest`, `AppSettingsTest` |
| General: start screen, theme, language | Settings → rows by text | text only | `settings/SettingsScreen.kt`, `settings/AppLocale.kt` | `AppLocaleTest`, `SettingsBUiTest` |
| Reading settings | Settings → `settings_reading_entry` | `settings_reading_screen`, `settings_reading_back` | `settings/SettingsScreen.kt` | `SettingsBUiTest` |
| Keep zoom when turning pages (SPEC-6) | Reading settings → "Keep zoom when turning pages" switch | text only | `settings/SettingsScreen.kt`, `settings/AppSettings.kt` | `KeepZoomSettingsTest`, `SettingsBUiTest` |
| Page curl turn animation (#103) | Reading settings → "넘김 애니메이션" → "책장 넘김"; reader tap/swipe/volume at scale 1, single-page | `settings_animation_option_Curl`, `reader_page_curl` | `PageCurl.kt`, `PageCurlOverlay.kt`, `ReaderScreen.kt`, `settings/AppSettings.kt` | `PageCurlTest`, `PageCurlComposeTest`, `PageCurlSettingsTest`, `PageCurlScreenshotsTest` |
| Full screen reading (#115) | Reading settings → "전체 화면으로 읽기" switch | text only | `settings/SettingsScreen.kt`, `settings/AppSettings.kt` | `FullScreenReadingSettingsTest` |
| Page color: original, inverted, sepia | Reading settings → "페이지 색 / Page color" row → choose | text only | `settings/SettingsScreen.kt`, `settings/AppSettings.kt`, `DisplayMode.kt`, `ReaderScreen.kt` | `DisplayModeTest`, `SettingsBUiTest`, `PageColorScreenshotsTest` |
| Keep screen on | Reading settings → "화면 켜짐 유지" switch | text only | `settings/SettingsScreen.kt`, `settings/AppSettings.kt`, `ReaderScreen.kt` | `KeepScreenOnTest` |
| Tap zone width, settings (SPEC-2 TW-1/2/4) | Reading settings → `settings_tap_width_row` | `settings_tap_width_row`, `tap_zone_width_sheet` | `settings/SettingsScreen.kt`, `TapZoneWidthSheet.kt` | `TapZoneWidthSettingsTest`, `TapZoneWidthSheetThemeTest` |
| Screen orientation lock (#117) | Reading settings → "화면 방향" row → choose 자동/세로/가로 | text only | `settings/SettingsScreen.kt`, `settings/AppSettings.kt`, `ReaderRoute.kt` | `ScreenOrientationTest`, `ScreenOrientationSettingsTest`, `ReaderOrientationTest`, `SettingsOrientationScreenshotsTest` |
| Storage: hidden items, dot files | Settings → `settings_hidden_items` | `settings_hidden_items` | `settings/StorageSettings.kt`, `hidden/HiddenItemsScreen.kt` | `StorageSettingsTest`, `HiddenItemsScreenTest` |
| App lock / PIN | Settings → "App lock" row → choose PIN or biometric | `pin_dot`, `pin_next`, PIN pad text | `lock/AppLockSettingsScreen.kt`, `lock/PinPad.kt`, `lock/PinEntry.kt`, `lock/LockGate.kt` | `LockScreensTest`, `LockPolicyTest`, `LockRepositoryTest`, `LockScreenshotsTest` |
| Update check | Settings → Update section | text only | `update/UpdateUi.kt`, `update/UpdateViewModel.kt` | `SettingsUpdateUiTest`, `UpdateCheckerTest` |
| About: version, open-source licenses, GitHub | Settings → `settings_open_source` | `settings_open_source` | `settings/SettingsScreen.kt`, `settings/OpenSourceLicenses.kt` | `SettingsAboutUiTest`, `AboutStringsTest` |

## verify.sh commands and SPEC acceptance tests

| Command | What it shows | Acceptance tests |
|---|---|---|
| `scripts/verify.sh library` | Picks the generated folder and opens the library | – |
| `scripts/verify.sh open-book` | Opens the generated 30-page book, then the overlay | – |
| `scripts/verify.sh sort-menu` | Sort menu with "Recent" checked, then with "Name" checked | SC-1, SC-2 |
| `scripts/verify.sh tap-width-sheet` | Tap zone width sheet opened from the reader | TW-3 |
| `scripts/verify.sh jump-back` | Slider jump to about 80%, the pill, then the return | JB-1, JB-2 |
| `scripts/verify.sh book-list` | Book list sheet with both generated books | BM-1 |
| `scripts/verify.sh bookmarks` | Bookmarks the current page, then opens the bookmark list | – |
| `scripts/verify.sh settings` | Settings, then reading settings | – |
| `scripts/verify.sh keep-zoom` | Reading settings with "Keep zoom when turning pages" off, then on | ZL-3/ZL-4 setting |
| `scripts/verify.sh display-mode` | Reading settings with "페이지 색 / Page color" on original, then a book opened in inverted | #57 |

The test books are generated by `scripts/make-test-pdf.py` (numbered, coloured pages). Never use real or copyrighted books for evidence.
