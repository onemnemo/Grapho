# Testing and verification

## What gets a test

- Core logic: stroke building, the stabiliser, tessellation, outlines, fill, geometry.
- Format round trips: write a document, read it back, compare. Include a file shaped like one
  an older version wrote.
- Edit and undo: every operation that changes a notebook, and its inverse.
- Pure shell logic such as viewport math, tool state and date formatting.
- Edge cases and failure paths, not only the happy path: empty strokes, one sample,
  truncated or corrupt files, a version higher than supported.
- UI wiring does not need a test. A new pure function does.
- A test should survive a rewrite of the thing it covers. Do not couple it to private detail.
- Do not require an external service or a device for a unit test.

## Running things

From the repository root:

```bash
cargo fmt --all --check && cargo clippy --all-targets -- -D warnings && cargo test
python3 scripts/check_repo.py
python3 scripts/check_boundaries.py
python3 scripts/check_commits.py origin/main..HEAD
scripts/ktlint.sh
```

- `check_repo.py` enforces the comment and dash rules.
- `check_boundaries.py` enforces the package dependency rules in `01-architecture.md`.
- `check_commits.py` enforces the commit format in `07-git.md`. It checks only new commits.

Android shell, from `android/`:

```bash
./gradlew lintRelease testReleaseUnitTest assembleRelease
```

This needs the Android SDK and `cargo-ndk`. Setup is in [android/README.md](../android/README.md).

Each new shell adds its own check commands to this section and to CI when it lands.

## Verifying a change for real

- Ink, input, rendering and latency changes are verified on a real device of the platform you
  touched: a Galaxy Tab with S Pen for Android, a Windows pen tablet such as a Surface, an iPad
  with Apple Pencil. An emulator or simulator does not exercise the fast ink path or pen
  pressure and tilt.
- Once a second shell exists, check files across platforms: save a notebook on one, open it on
  the other, and confirm it looks and edits identically. Do this for every format change.
- Open notebooks saved by older builds. Legacy files are where a format or model change
  breaks, and a fresh notebook has none of that history.
- When a visual change is reported as broken, ask whether the screenshot shows an old notebook
  before assuming the new rendering is at fault.
- Say what you ran and what it produced. If part of the suite was skipped, say that.

## Performance and latency claims

Two rules, both about not fooling yourself.

**A performance number needs a proof of correct output from the same run.**
Prove the strokes were actually drawn, from engine truth (a readback of the rendered result or
the stroke count in the document, not the renderer's own bookkeeping, and not "it looked fine
earlier"). Name the count and its expected value, and treat a near-zero as void rather than as
a result. A renderer that draws nothing always wins a latency benchmark, so the best-looking
result is exactly the one that most needs the proof. When comparing two configurations, the
drawn counts must match between them, or the comparison is not like for like.

**Never compare runs across process launches.**
Run both arms inside one process, separated by a state reset, with the order balanced
(A-B-B-A, then B-A-A-B on another pass). Thermal state, background load and clock scaling on a
tablet drift by more than most effects being measured, and a cold launch is not comparable to
a warm one. Record a work signal in the run and refuse to compare across a gap in it. Toggle
the arm at runtime and prove the toggle fired in the run that produced the numbers.

For frame timing, report the distribution (median, 95th and 99th percentile, dropped frames),
not one average, and read every thread in a trace rather than totalling one.
