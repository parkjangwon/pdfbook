#!/usr/bin/env python3
import argparse
import re
import sys


def label_globs(path, label):
    globs = []
    inside = False
    for raw in open(path, encoding="utf-8"):
        line = raw.rstrip("\n")
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if not line.startswith(" "):
            inside = line.split(":", 1)[0].strip() == label
            continue
        if inside:
            item = line.strip()
            if item.startswith("- ") and not item.endswith(":"):
                globs.append(item[2:].strip().strip("'\""))
    if not globs:
        sys.exit(f"no globs found for {label} in {path}")
    return globs


def glob_regex(glob):
    out = []
    i = 0
    while i < len(glob):
        if glob.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif glob.startswith("**", i):
            out.append(".*")
            i += 2
        elif glob[i] == "*":
            out.append("[^/]*")
            i += 1
        elif glob[i] == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(glob[i]))
            i += 1
    return re.compile("".join(out) + r"\Z")


def main():
    parser = argparse.ArgumentParser(description="Print true if any changed file matches a labeler rule.")
    parser.add_argument("files", nargs="*", help="changed files; read from stdin when omitted")
    parser.add_argument("--labeler", default=".github/labeler.yml")
    parser.add_argument("--label", default="needs-design")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    patterns = [(glob, glob_regex(glob)) for glob in label_globs(args.labeler, args.label)]
    files = args.files or [line.strip() for line in sys.stdin if line.strip()]
    matched = False
    for name in files:
        hit = next((glob for glob, rx in patterns if rx.match(name)), None)
        if hit:
            matched = True
            if args.verbose:
                print(f"{name} <- {hit}", file=sys.stderr)
    print("true" if matched else "false")


if __name__ == "__main__":
    main()
