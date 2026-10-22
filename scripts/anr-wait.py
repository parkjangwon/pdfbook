#!/usr/bin/env python3
"""Find the Wait button of another app's "isn't responding" dialog.

Usage: anr-wait.py <ui.xml>
Prints "<x> <y> <title>" and exits 0 when the dump shows a system ANR dialog
for an app other than PDFBook. Exits 1 otherwise, including when the title is
missing or empty, so PDFBook's own ANR is never dismissed.
"""
import re
import sys
import xml.etree.ElementTree as ET


def main(path: str) -> int:
    try:
        nodes = list(ET.parse(path).getroot().iter("node"))
    except (ET.ParseError, OSError):
        return 1
    ids = {}
    for node in nodes:
        ids.setdefault(node.get("resource-id", ""), node)
    wait = ids.get("android:id/aerr_wait")
    title = ids.get("android:id/alertTitle")
    if wait is None or title is None:
        return 1
    text = title.get("text", "").strip()
    if not text or re.search(r"pdfbook", text, re.IGNORECASE):
        return 1
    coords = [int(n) for n in re.findall(r"\d+", wait.get("bounds", ""))]
    if len(coords) != 4:
        return 1
    x1, y1, x2, y2 = coords
    print((x1 + x2) // 2, (y1 + y2) // 2, text)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]) if len(sys.argv) == 2 else 2)
