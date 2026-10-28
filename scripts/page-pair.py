#!/usr/bin/env python3
"""Print the page numbers drawn on the left and right half of a reader screenshot.

verify.sh renders the synthetic book from scripts/make-test-pdf.py, whose page
background is a fixed HSV colour per page number, so the dominant colour inside
each half identifies the page. The reader does not expose reader_page_* tags to
uiautomator, so this is how the emulator flow proves a spread really re-paired.

Usage: page-pair.py <screenshot.png>
Exit 1 when a half does not confidently match any page colour.
"""
import colorsys
import struct
import sys
import zlib

MAX_PAGE = 30  # scripts/verify.sh renders a 30-page book


def read_png(path):
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"not a PNG: {path}")
    pos = 8
    idat = bytearray()
    width = height = depth = ctype = interlace = 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", chunk[:13])
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
        pos += 12 + length
    if depth != 8 or ctype not in (2, 6) or interlace:
        raise SystemExit(f"unsupported PNG (depth {depth}, colour type {ctype}, interlace {interlace})")
    bpp = 4 if ctype == 6 else 3
    stride = width * bpp
    raw = zlib.decompress(bytes(idat))
    rows = []
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        flt = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride
        if flt == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif flt == 2:
            for x in range(stride):
                line[x] = (line[x] + previous[x]) & 255
        elif flt == 3:
            for x in range(stride):
                left = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((left + previous[x]) >> 1)) & 255
        elif flt == 4:
            for x in range(stride):
                left = line[x - bpp] if x >= bpp else 0
                up = previous[x]
                upleft = previous[x - bpp] if x >= bpp else 0
                p = left + up - upleft
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - upleft)
                predictor = left if pa <= pb and pa <= pc else (up if pb <= pc else upleft)
                line[x] = (line[x] + predictor) & 255
        rows.append(line)
        previous = line
    return width, height, bpp, rows


def dominant_color(width, height, bpp, rows, x0, x1, y0, y1):
    counts = {}
    for y in range(int(height * y0), int(height * y1)):
        row = rows[y]
        for x in range(int(width * x0), int(width * x1)):
            pixel = bytes(row[x * bpp:x * bpp + 3])
            counts[pixel] = counts.get(pixel, 0) + 1
    return max(counts, key=counts.get)


def page_color(page):
    r, g, b = colorsys.hsv_to_rgb((page * 0.13) % 1.0, 0.35, 0.95)
    return (round(r * 255), round(g * 255), round(b * 255))


def normalized(pixel):
    peak = max(pixel)
    return tuple(channel / peak for channel in pixel) if peak else (0.0, 0.0, 0.0)


def nearest_page(pixel):
    # The reader overlay dims the pages, which scales the channels; comparing
    # them after dividing by the peak channel keeps the hue readable.
    seen = normalized(pixel)
    best, best_distance = None, None
    for page in range(1, MAX_PAGE + 1):
        candidate = normalized(page_color(page))
        distance = sum((a - b) ** 2 for a, b in zip(seen, candidate))
        if best_distance is None or distance < best_distance:
            best, best_distance = page, distance
    return best, best_distance ** 0.5


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    width, height, bpp, rows = read_png(sys.argv[1])
    halves = ((0.08, 0.45, 0.40, 0.62), (0.55, 0.92, 0.40, 0.62))
    pages = []
    for x0, x1, y0, y1 in halves:
        pixel = dominant_color(width, height, bpp, rows, x0, x1, y0, y1)
        page, distance = nearest_page(pixel)
        if distance > 0.08:
            raise SystemExit(f"no page colour at x{x0}-{x1}: #{pixel.hex()}")
        pages.append(page)
    print(*pages)


if __name__ == "__main__":
    main()
