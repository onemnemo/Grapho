# Architecture and layering

## The map

| Layer | What it is | May depend on |
|---|---|---|
| `core/` (`grapho-core`) | Platform-independent engine: stroke modelling, stabiliser, tessellation, outlines, document model, `.grapho` format, fill, library model (folders, items, Trash) | third party crates only |
| ffi crate, one per platform (`ffi-android` today, `ffi-windows` and `ffi-apple` planned) | Bindings and marshalling for that platform, nothing else | `grapho-core` |
| Shell | The native app for one platform, layered as below | its own ffi crate, through the engine bridge |

### Shell layering

Every shell has the same three layers and the same feature words. The Android package names
are the example. Future shells mirror them in their own idiom (namespace, module).

| Layer | Android example | What it is | May depend on |
|---|---|---|---|
| engine bridge | `engine` | The platform side of the ffi contract (`Native.kt`) | no other shell layer |
| note and canvas | `note` | Canvas, wet ink renderer, dry cache, viewport, notebook model wrapper | engine bridge |
| ui | `ui` and subpackages | Screens and components | note and canvas |
| entry point | `MainActivity` | Wires the shell together | anything |

Rules that do not bend:

- `grapho-core` depends on no platform crate (no `jni`, no `android`, no `windows` or Apple
  crates), no UI, and holds no global mutable state. `#![forbid(unsafe_code)]` is enforced.
- Core APIs are designed for every ffi consumer: C ABI friendly, with no JNI types and no
  Android assumptions in core signatures.
- An ffi crate holds no engine logic. It converts arguments and results and calls the core.
  Every entry point catches panics and errors at the boundary instead of unwinding into the
  host runtime.
- Each platform shell gets its own ffi crate. Platform code never goes inside `core`.
- `ui` never imports the engine bridge directly, and `note` never imports `ui`. The engine
  bridge imports no other shell layer.
- No circular dependencies between layers, ever.
- The Android package rules are enforced by `python3 scripts/check_boundaries.py`. Run it
  before you push.

## One engine, fast wet ink

The stroke under the pen (wet ink) goes through each platform's fastest drawing path. On
Android that is the front-buffered renderer in `note`. Everything else is shared Rust, so a
note looks identical on every device.

Do not move drawing or geometry logic into Kotlin that the Windows and Apple shells would have
to duplicate. If a second shell would need it, it belongs in `grapho-core`.

## Seams and swappability

Before merging a feature, answer: if we swapped this dependency tomorrow, how many files
change? If the answer is more than the seam plus its registration, the seam is in the wrong
place.

- Icons go through the `GraphoIcon` set in each shell (on Android, `ui/components/Icons.kt`).
- Theme goes through the shell's theme layer (on Android, `ui/theme`).
- Keep pure logic (viewport math, geometry, tool state) in code that imports neither the UI
  toolkit nor the platform framework. These are the parts worth testing and the parts most likely to be
  reused.
- Wrap third party dependencies at one point of contact.

## Persisted data

The `.grapho` format ([docs/file-format.md](../docs/file-format.md)) is a public contract.
Notes outlive app versions, so treat storage as something other people depend on.

- Files written by any released version must keep opening.
- **Persisted enums and ids are append only.** `Tool` values, pattern kinds, paper names and
  similar are on disk in real notebooks. Inserting or renumbering a member breaks every file
  that used the ones after it.
- **Normalize old shapes at load**, in the core, not in the view. List every persisted field
  that reaches the view before changing how one is shown.
- A format change updates `docs/file-format.md` in the same pull request, bumps the format
  version, and ships a migration path. A reader refuses a version higher than it supports
  rather than skipping data it does not understand.
- The library (folders, items, Trash) is device data owned by the core, stored as one versioned
  JSON file ([docs/file-format.md](../docs/file-format.md)). It points at notebooks by file name
  and never parses them to list them.
- **Never restore transient view state as document data.** Selection, an open pen wheel and
  scroll position are not part of a notebook.
- App preferences (on Android, `SharedPreferences`) are separate from document data. Do not put one in the
  other.
- Ids are UUIDs and never reused, so documents edited on two devices can be merged later.
- **The format round trips between platforms.** A notebook saved on Android opens identically
  on Windows and Apple, and the reverse. Sync moves the same bytes everywhere.
- Paths, line endings and float formats are platform independent. Store paths in one
  normalized form, use one line ending, and write floats in a fixed, locale independent
  format, so a file never depends on the device that wrote it.
