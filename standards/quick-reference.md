# Contributor quick reference

The short version of how Grapho is built. Read it before your first edit. The full standard is
in [this folder](.), and [CONTRIBUTING.md](../CONTRIBUTING.md) covers how changes are
proposed and reviewed.

## What Grapho is

A free, open source handwriting notes app for tablets, meant to be a polished "2.0" of Samsung
Notes and GoodNotes: no subscription, an open file format, and no second-class platforms.
Android on the Samsung Galaxy Tab with S Pen comes first. Windows, iPad and macOS follow as
native shells over the same Rust core. Handwriting is the product, so craftsmanship and ink
feel matter more than shipping quickly.

Stack: a Rust core (`core/`, crate `grapho-core`), one ffi crate per platform, and a thin native
shell per platform. Today that is JNI bindings (`ffi-android/`) and a Kotlin and Jetpack Compose
Android app (`android/`). Planned: a C# and WinUI 3 Windows shell (`ffi-windows/`, `windows/`) and
a Swift Apple shell for iPad and macOS (`ffi-apple/`, `apple/`).

The visual source of truth for every shell is the design workspace in `design/`: tokens,
icons, components and screens in Storybook. Read `design/CONVENTIONS.md` before changing it.

## The standard

The files in this folder are the source of truth. Read the file that covers what you are about to touch.

- `standards/00-principles.md` engineering philosophy
- `standards/01-architecture.md` layers, dependency rules, persisted data, seams
- `standards/02-naming-and-structure.md` folder layout and the naming table
- `standards/03-rust.md` Rust core and ffi crates
- `standards/04-platform-shells.md` rules for every shell, plus Android, Windows and Apple
- `standards/05-testing-and-verification.md` tests, devices, and what a performance claim needs
- `standards/06-comments-and-copy.md` comments and user-facing copy
- `standards/07-git.md` commits and pull requests

## Non-negotiables

1. **No em dashes or en dashes.** Not in comments, commit messages, or any string a user can
   read. Use a comma, parentheses, or a new sentence.
2. **No to-do, fix-me or hack markers.** The tree has none. File the follow-up as an issue.
3. **No internal document references in code or commits.** No milestone identifiers, section
   numbers, or plan filenames. Comments and commits are read by strangers.
4. **Commit format is `type(scope): subject`**, lowercase imperative, checked on pull requests.
5. **The core stays platform free.** `grapho-core` has no JNI, no platform API, no UI, no global
   mutable state, and `#![forbid(unsafe_code)]`. Drawing and geometry logic lives in Rust, not
   in a shell.
6. **`.grapho` files from any released version must keep opening.** Persisted enums and ids
   are append only. A format change updates `docs/file-format.md` in the same pull request.
7. **No raw colours in UI chrome on any platform.** Use the theme tokens. Icons go through
   `GraphoIcon`.
8. **A latency or performance number needs a proof of correct output from the same run.** A
   renderer that draws nothing always wins the benchmark.
9. **Never swallow an exception, and never let a panic unwind across an ffi boundary.**

## Working rules

- Comment the why, and only when it is not obvious. Most code needs no comment.
- Prefer a new file over a growing one. Around 400 lines is the point to split.
- Build for what the product is becoming. Do not ship a stopgap without isolating it behind
  one seam and filing the follow-up.
- Verify against real saved notebooks and on a real device of the platform you touched for ink and input changes.
- Report what you actually ran and what it produced. If something was skipped or fails, say so.

## Verification

From the repository root:

```bash
cargo fmt --all --check && cargo clippy --all-targets -- -D warnings && cargo test
python3 scripts/check_repo.py
python3 scripts/check_boundaries.py
python3 scripts/check_commits.py origin/main..HEAD
scripts/ktlint.sh
```

Android shell, from `android/` (needs the Android SDK and cargo-ndk, see [android/README.md](../android/README.md)):

```bash
./gradlew lintRelease testReleaseUnitTest assembleRelease
```
