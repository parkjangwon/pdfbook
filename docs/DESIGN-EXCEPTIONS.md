# PDFBook design exceptions

Project-specific rules on top of the team design system (`docs/DESIGN.md`). Anything not listed here follows `docs/DESIGN.md`.

## Reader (theme-independent)
The book page is the hero. The reader is always on black with neutral gray overlays, whatever the app theme, so overlays never tint the page.

| Token | Value |
|---|---|
| readerBackground | #000000 |
| readerOverlay | #161616 (bars at 94%) |
| readerOnOverlay | #EDEDED |
| readerOnOverlayVariant | #A8A8A8 |
| readerTrack | #3A3A3A |
| readerAccent | #FF8A50 |
| readerChip | #2A2A2A |
| readerListCurrent | #262626 |
| readerScrim | readerBackground at 50% |
| readerTapZoneSide / Center / Divider / Label | readerOverlay 55% / readerOverlay 18% / readerOnOverlay 55% / readerOverlay 86% |

- Overlay: top and bottom bars only, neutral gray, white-family icons. Orange only on the slider thumb and filled track. Overlays fade only.
- Contrast: overlay text 15.5:1, secondary 7.6:1, slider 4.9:1.
- The reader uses `PdfBookReaderProgressBar` and `PdfBookReaderSlider` (reader tokens, no stop indicator, no ticks).
- Reader dialogs (go to page) use `readerOnOverlayVariant` for hints and validation messages (7.6:1 on `readerOverlay`); there is no reader error color.

## Screens
- Folder list: each card shows the folder name, a one-line path, `책 %d권` / book count, a 4-cover preview and a continue-reading row (orange bar). Inaccessible folders dim their covers and show a "choose folder again" secondary button.
- Library: cover grid with title, progress bar and % (numeric font) under each cover. 2 columns compact, 4 or more expanded.
- Folder-add FAB: extended ink FAB, corner 12, height 72.
- Foldable and tabletop postures: book and tabletop layouts keep the reading position; side margins switch 16 → 24 when unfolded.

## Shapes and sizes (approved per feature)
- Cover corner 8 (`CoverCorner`).
- PIN pad keys and tabletop pads: corner 14 (`PadCorner`). PIN digits: numeric font at 22sp.
- Status banners at the top of the folder list (update available, peek hidden): surfaceContainerHigh, corner 10, no shadow, 13sp message + 11sp hint.
- Hidden badge: corner 6, 1dp hairlineStrong border, no background, eye-off icon 12dp + 11sp SemiBold.
- Reader tap-zone guide labels: readerOverlay 86%, corner 10, 13sp SemiBold. Tap-width preview: 120×168dp page, corner 6, 10sp zone labels.
- Jump-back pill: 44dp high, fully rounded, readerChip with 1dp readerTrack border, 14sp text, page number in the numeric font.

## Copy
- Never use "만화", "comic" or "manga" in the app. Use "책" / "book", e.g. "책 폴더 / Book folders", "책 폴더를 골라 주세요 / Choose your book folder".

## Naming
- Extended colors class: `PdfBookColors` via `LocalPdfBookColors`.

## Reader accent beyond the slider (approved exception)
- In the reader, orange is also used for the "open" text button on the next-book notice, the "reading" label in the book list, and the current-book bar. These come from approved designs (v0.3 and v0.3.2). Everywhere else in the reader, orange stays limited to the slider.

## Reader tool row
- Shared bottom-bar tools sit in one row directly under the page slider (above the page number). Tap-zone entry stays above the slider.
- Fixed left-to-right order: Brightness, AutoTurn, Bookmarks, AutoCrop. Only tools that are passed in are shown, always in that enum order. The row does **not** mirror with RTL reading direction (volume buttons and the page slider still do).
- Each item: 24dp line icon over `labelSmall`, `readerOnOverlay` content, ~56dp tall equal-width slots (min 48×48 touch target). No `readerAccent`, no elevation, no new shapes.
