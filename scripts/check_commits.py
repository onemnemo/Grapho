#!/usr/bin/env python3
"""Checks commit messages in a revision range: check_commits.py <rev-range>.

Subjects follow `type(scope): lowercase summary`, at most 72 characters, no
trailing period. Messages carry no dashes (em/en). Merge commits are skipped. An empty range passes.
"""
import re
import subprocess
import sys

TYPES = "feat|fix|perf|refactor|test|docs|build|ci|chore"
SCOPES = {
    "core", "format", "ink", "fill", "ffi", "android", "canvas", "library",
    "note", "wheel", "ui", "theme", "design", "docs", "ci", "repo", "release", "windows",
    "apple",
}
SUBJECT = re.compile(rf"^({TYPES})\(([a-z0-9-]+)\): [a-z0-9].*")
DASH = re.compile("[" + chr(0x2014) + chr(0x2013) + "]")
MAX_SUBJECT = 72


def git(*args):
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout


def problems(message):
    subject, _, body = message.partition("\n")
    found = []
    m = SUBJECT.match(subject)
    if not m:
        found.append("subject must look like `type(scope): summary` (types: "
                     + ", ".join(TYPES.split("|")) + ")")
    elif m.group(2) not in SCOPES:
        found.append(f"unknown scope `{m.group(2)}` (allowed: {', '.join(sorted(SCOPES))})")
    if len(subject) > MAX_SUBJECT:
        found.append(f"subject is {len(subject)} characters, limit is {MAX_SUBJECT}")
    if subject.endswith("."):
        found.append("subject ends with a period")
    if DASH.search(message):
        found.append("message contains an em or en dash")
    return found


def main(argv):
    if len(argv) != 2:
        print(__doc__.strip().splitlines()[0], file=sys.stderr)
        return 2
    shas = git("log", "--no-merges", "--format=%H", argv[1]).split()
    failed = 0
    for sha in shas:
        message = git("log", "-1", "--format=%B", sha).strip("\n")
        found = problems(message)
        if found:
            failed += 1
            subject = message.partition("\n")[0]
            print(f"{sha[:8]} {subject}")
            for p in found:
                print(f"    {p}")
    if failed:
        print(f"\n{failed} of {len(shas)} commit(s) failed")
        return 1
    print(f"{len(shas)} commit(s) checked, all good")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
