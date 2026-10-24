#!/usr/bin/env python3
"""Enforces module boundaries in the Kotlin app and the Rust workspace.

Kotlin (android/app/src/main/java/com/onemnemo/grapho):
  engine   imports no other app package (R is fine)
  note     imports engine and R, never ui
  ui       imports note, ui and R, never engine
  (root)   MainActivity and friends may import anything
Rust:
  grapho-core stays platform independent: no platform or path dependencies,
  no JNI exports. grapho-android may depend only on grapho-core.
"""
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KOTLIN_ROOT = ROOT / "android/app/src/main/java/com/onemnemo/grapho"
APP = "com.onemnemo.grapho"

ALLOWED = {
    "engine": {"engine", "R"},
    "note": {"note", "engine", "R"},
    "ui": {"ui", "note", "R"},
}
HINTS = {
    "engine": "engine is the native bridge and must not know about the app",
    "note": "note must not depend on ui",
    "ui": "ui talks to the engine through note, never directly",
}

APP_REF = re.compile(re.escape(APP) + r"\.(\w+)")
FORBIDDEN_DEPS = re.compile(r"^(jni|ndk|android.*|windows.*|objc.*)$")
FORBIDDEN_RUST = (
    ('extern "system"', "JNI exports belong in ffi-android"),
    ("#[no_mangle]", "exported symbols belong in ffi-android"),
    ("jni::", "JNI belongs in ffi-android"),
)
DEP_TABLES = ("dependencies", "dev-dependencies", "build-dependencies")


def check_kotlin(errors):
    for path in sorted(KOTLIN_ROOT.rglob("*.kt")):
        rel = path.relative_to(KOTLIN_ROOT)
        if len(rel.parts) == 1:
            continue
        layer = rel.parts[0]
        allowed = ALLOWED.get(layer)
        if allowed is None:
            continue
        shown = path.relative_to(ROOT)
        for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if line.startswith("package "):
                continue
            for m in APP_REF.finditer(line):
                target = m.group(1)
                if target in allowed:
                    continue
                kind = "import" if line.startswith("import ") else "qualified reference"
                errors.append(
                    f"{shown}:{n}: [{layer} must not use {target}] "
                    f"{kind} of {APP}.{target} ({HINTS[layer]})"
                )


def dep_tables(manifest):
    yield from ((t, manifest.get(t, {})) for t in DEP_TABLES)
    for target in manifest.get("target", {}).values():
        yield from ((t, target.get(t, {})) for t in DEP_TABLES)


def workspace_crate_names():
    root = tomllib.loads((ROOT / "Cargo.toml").read_text(encoding="utf-8"))
    names = set()
    for member in root["workspace"]["members"]:
        data = tomllib.loads((ROOT / member / "Cargo.toml").read_text(encoding="utf-8"))
        names.add(data["package"]["name"])
    return names


def check_rust(errors):
    core_toml = ROOT / "core/Cargo.toml"
    core = tomllib.loads(core_toml.read_text(encoding="utf-8"))
    for table, deps in dep_tables(core):
        for name, spec in deps.items():
            if FORBIDDEN_DEPS.match(name):
                errors.append(
                    f"core/Cargo.toml: [grapho-core stays platform independent] "
                    f"[{table}] depends on {name}"
                )
            if isinstance(spec, dict) and "path" in spec:
                errors.append(
                    f"core/Cargo.toml: [grapho-core takes no path dependencies] "
                    f"[{table}] {name} is a path dependency"
                )

    ffi = tomllib.loads((ROOT / "ffi-android/Cargo.toml").read_text(encoding="utf-8"))
    siblings = workspace_crate_names() - {"grapho-core", ffi["package"]["name"]}
    for table, deps in dep_tables(ffi):
        for name in deps:
            if name in siblings:
                errors.append(
                    f"ffi-android/Cargo.toml: [grapho-android may depend only on "
                    f"grapho-core] [{table}] depends on workspace crate {name}"
                )

    for path in sorted((ROOT / "core/src").rglob("*.rs")):
        for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for needle, why in FORBIDDEN_RUST:
                if needle in line:
                    errors.append(
                        f"{path.relative_to(ROOT)}:{n}: [core must not contain "
                        f"{needle}] {why}"
                    )


def main():
    errors = []
    check_kotlin(errors)
    check_rust(errors)
    for e in errors:
        print(f"error: {e}")
    if errors:
        print(f"\n{len(errors)} boundary violation(s)")
        return 1
    print("boundary checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
