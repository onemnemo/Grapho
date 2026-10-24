#!/usr/bin/env python3
"""Repo hygiene checks over tracked files.

Fails on NUL bytes, em/en dashes and unresolved work markers in text files.
Warns (never fails) about .rs and .kt files over 400 lines.
"""
import os
import re
import subprocess
import sys

SKIP_PATHS = (
    "LICENSE",
    "android/app/src/main/assets/licenses/",
    "Cargo.lock",
    "android/gradlew",
    "android/gradlew.bat",
)
BINARY_EXTS = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ttf", ".otf", ".woff", ".woff2",
    ".jar", ".keystore", ".zip", ".so", ".ico", ".pdf",
}
MAX_LINES = 400

# Built from escapes so this file does not trip its own checks.
DASH = re.compile("[" + chr(0x2014) + chr(0x2013) + "]")
MARKER = re.compile(r"\b(" + "TO" + "DO|FIX" + "ME|HA" + "CK)\b")


def tracked_files():
    out = subprocess.run(
        ["git", "ls-files", "-z"], check=True, capture_output=True, text=True
    ).stdout
    return [p for p in out.split("\0") if p]


def is_binary(path, head):
    ext = "." + path.rsplit(".", 1)[-1].lower() if "." in path.rsplit("/", 1)[-1] else ""
    return b"\0" in head and ext in BINARY_EXTS


def main():
    failures = []
    warnings = []
    for path in tracked_files():
        if path in SKIP_PATHS or path.startswith(SKIP_PATHS[1]):
            continue
        if not os.path.exists(path):
            continue  # deleted or renamed in the working tree
        with open(path, "rb") as f:
            data = f.read()
        if is_binary(path, data[:8192]):
            continue
        if b"\0" in data:
            failures.append(f"{path}: NUL byte in text file")
            continue
        text = data.decode("utf-8", errors="replace")
        lines = text.splitlines()
        for n, line in enumerate(lines, 1):
            if DASH.search(line):
                failures.append(f"{path}:{n}: em or en dash")
            if MARKER.search(line):
                failures.append(f"{path}:{n}: work marker")
        if path.endswith((".rs", ".kt")) and len(lines) > MAX_LINES:
            warnings.append(f"{path}: {len(lines)} lines (over {MAX_LINES})")
    for w in warnings:
        print(f"warning: {w}")
    for f in failures:
        print(f"error: {f}")
    if failures:
        print(f"\n{len(failures)} problem(s) found")
        return 1
    print("repo checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
