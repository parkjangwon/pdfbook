#!/usr/bin/env python3
import pathlib
import subprocess
import sys
import tempfile
import zipfile

import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
FONT_DIR = ROOT / "app" / "src" / "main" / "res" / "font"
CHARSET_PATH = ROOT / "fonts" / "pretendard_subset_chars.txt"
SIZE_PATH = ROOT / "fonts" / "subset_sizes.txt"
SRC_CACHE = pathlib.Path(tempfile.gettempdir()) / "pdfbook-fonts" / "src"
PRETENDARD_URL = "https://github.com/orioncactus/pretendard/releases/download/v1.3.9/Pretendard-1.3.9.zip"
JETBRAINS_URL = "https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip"
PRETENDARD_VERSION = "1.3.9"
JETBRAINS_VERSION = "2.304"
PUNCT = "·…‘’“”₩→"
LATIN = "".join(chr(c) for c in range(0x20, 0x7F))
MONO_CHARS = "0123456789.,%/×:- "


def ksx1001_hangul() -> str:
    chars = []
    for lead in range(0xB0, 0xC9):
        for trail in range(0xA1, 0xFF):
            try:
                ch = bytes([lead, trail]).decode("euc-kr")
            except UnicodeDecodeError:
                continue
            if "\uac00" <= ch <= "\ud7a3":
                chars.append(ch)
    if len(chars) != 2350:
        raise SystemExit(f"expected 2350 hangul, got {len(chars)}")
    return "".join(chars)


def ensure_src() -> tuple[pathlib.Path, pathlib.Path, pathlib.Path]:
    SRC_CACHE.mkdir(parents=True, exist_ok=True)
    regular = SRC_CACHE / "Pretendard-Regular.ttf"
    semibold = SRC_CACHE / "Pretendard-SemiBold.ttf"
    mono = SRC_CACHE / "JetBrainsMono-Regular.ttf"
    if not regular.is_file() or not semibold.is_file():
        zpath = SRC_CACHE / "Pretendard-1.3.9.zip"
        if not zpath.is_file():
            urllib.request.urlretrieve(PRETENDARD_URL, zpath)
        with zipfile.ZipFile(zpath) as zf:
            with zf.open("public/static/alternative/Pretendard-Regular.ttf") as src, regular.open("wb") as dst:
                dst.write(src.read())
            with zf.open("public/static/alternative/Pretendard-SemiBold.ttf") as src, semibold.open("wb") as dst:
                dst.write(src.read())
    if not mono.is_file():
        zpath = SRC_CACHE / "JetBrainsMono-2.304.zip"
        if not zpath.is_file():
            urllib.request.urlretrieve(JETBRAINS_URL, zpath)
        with zipfile.ZipFile(zpath) as zf:
            with zf.open("fonts/ttf/JetBrainsMono-Regular.ttf") as src, mono.open("wb") as dst:
                dst.write(src.read())
    return regular, semibold, mono


def write_charset(text: str) -> None:
    CHARSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    CHARSET_PATH.write_text(text, encoding="utf-8")


def subset_ttf(src: pathlib.Path, dst: pathlib.Path, text: str, family: str | None) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False, suffix=".txt") as tmp:
        tmp.write(text)
        tmp_path = tmp.name
    cmd = [
        "pyftsubset",
        str(src),
        f"--text-file={tmp_path}",
        "--layout-features=*",
        "--glyph-names",
        "--symbol-cmap",
        "--legacy-cmap",
        "--notdef-glyph",
        "--notdef-outline",
        "--recommended-glyphs",
        "--name-IDs=*",
        "--name-legacy",
        "--name-languages=*",
        f"--output-file={dst}",
    ]
    subprocess.check_call(cmd)
    pathlib.Path(tmp_path).unlink(missing_ok=True)
    if family:
        from fontTools.ttLib import TTFont

        ps = family.replace(" ", "")
        font = TTFont(dst)
        for record in font["name"].names:
            try:
                value = record.toUnicode()
            except Exception:
                continue
            if "Pretendard" not in value:
                continue
            replaced = value.replace("Pretendard", ps if record.nameID == 6 else family)
            if record.platformID in (0, 3):
                record.string = replaced.encode("utf-16-be")
            else:
                record.string = replaced.encode("latin-1", errors="replace")
        font.save(dst)


def main() -> int:
    regular, semibold, mono = ensure_src()
    pretendard_chars = LATIN + PUNCT + ksx1001_hangul()
    write_charset(pretendard_chars)
    out_regular = FONT_DIR / "pdfbook_sans_regular.ttf"
    out_semibold = FONT_DIR / "pdfbook_sans_semibold.ttf"
    out_mono = FONT_DIR / "jetbrains_mono_regular.ttf"
    subset_ttf(regular, out_regular, pretendard_chars, "PDFBook Sans")
    subset_ttf(semibold, out_semibold, pretendard_chars, "PDFBook Sans")
    subset_ttf(mono, out_mono, MONO_CHARS, None)
    lines = [
        f"Pretendard {PRETENDARD_VERSION} Regular {regular.stat().st_size} -> {out_regular.stat().st_size}",
        f"Pretendard {PRETENDARD_VERSION} SemiBold {semibold.stat().st_size} -> {out_semibold.stat().st_size}",
        f"JetBrainsMono {JETBRAINS_VERSION} Regular {mono.stat().st_size} -> {out_mono.stat().st_size}",
        "RFN: Pretendard declared -> renamed file/name table to PDFBook Sans; JetBrains Mono no RFN declared -> kept name",
    ]
    SIZE_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
