#!/usr/bin/env python3
import argparse
import colorsys


def page_stream(number, total, title, height=900):
    r, g, b = colorsys.hsv_to_rgb((number * 0.13) % 1.0, 0.35, 0.95)
    label = f"{number}"
    size = 220 if number < 100 else 160
    width = len(label) * size * 0.556
    lines = [
        f"{r:.3f} {g:.3f} {b:.3f} rg 0 0 600 {height} re f",
        f"0.1 0.1 0.1 rg 40 40 520 {height - 80} re S",
        "BT /F1 28 Tf 60 " + f"{height - 80}" + " Td (" + title + ") Tj ET",
        f"BT /F1 {size} Tf {300 - width / 2:.1f} 380 Td ({label}) Tj ET",
        f"BT /F1 24 Tf 60 80 Td (Page {number} of {total}) Tj ET",
    ]
    return "\n".join(lines).encode("latin-1")


def page_height(number, mixed):
    # A mixed book alternates a tall page (overflows a width fit) with a square one (fits).
    if not mixed:
        return 900
    return 1800 if number % 2 == 1 else 600


def build(pages, title, mixed=False):
    objects = []
    kids = []
    font_id = 3
    objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    objects.append(None)
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    for number in range(1, pages + 1):
        height = page_height(number, mixed)
        stream = page_stream(number, pages, title, height)
        content_id = len(objects) + 1
        objects.append(b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream")
        page_id = len(objects) + 1
        objects.append(
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 600 {height}] "
                f"/Resources << /Font << /F1 {font_id} 0 R >> >> /Contents {content_id} 0 R >>"
            ).encode("latin-1")
        )
        kids.append(page_id)
    objects[1] = ("<< /Type /Pages /Kids [" + " ".join(f"{k} 0 R" for k in kids) + f"] /Count {pages} >>").encode(
        "latin-1"
    )
    out = bytearray(b"%PDF-1.4\n")
    offsets = []
    for index, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % index + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 %d\n0000000000 65535 f \n" % (len(objects) + 1)
    for offset in offsets:
        out += b"%010d 00000 n \n" % offset
    out += b"trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (len(objects) + 1, xref)
    return bytes(out)


def main():
    parser = argparse.ArgumentParser(description="Write a generated test PDF with numbered pages.")
    parser.add_argument("output")
    parser.add_argument("--pages", type=int, default=30)
    parser.add_argument("--title", default="PDFBook test book")
    parser.add_argument(
        "--mixed",
        action="store_true",
        help="alternate a tall page with a square one, for page fit tests",
    )
    args = parser.parse_args()
    with open(args.output, "wb") as handle:
        handle.write(build(args.pages, args.title, args.mixed))


if __name__ == "__main__":
    main()
