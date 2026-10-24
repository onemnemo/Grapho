# Platform shells

Applies to every native shell: `android/` today, and `windows/` and `apple/` once they exist.
Layering rules are in `01-architecture.md`. The first part below holds for every shell. The
platform sections after it add the specifics of one toolchain.

## Rules for every shell

### What belongs in a shell

A shell is thin. It owns only:

- input capture (pen, touch, mouse, keyboard)
- the wet ink adapter
- platform UI
- file pickers and storage access
- platform integration (sharing, windowing, lifecycle, permissions)

Everything that affects how a note looks or what is saved lives in `grapho-core`, so a note is
identical on every device. If a second shell would have to reimplement it, it belongs in the
core, not in a shell.

### Layering

Every shell mirrors the same three layers, with the same feature words, so a contributor who
knows one shell can find things in another:

| Layer | Role | May depend on |
|---|---|---|
| engine bridge (`engine`) | The platform side of the ffi contract: loads the native library and wraps its calls | no other shell layer |
| note and canvas (`note`) | Canvas, wet ink adapter, dry cache, viewport, notebook model wrapper | engine bridge |
| ui (`ui`) | Screens and components | note and canvas |

The entry point wires the shell together. `ui` never reaches the engine bridge directly, and
`note` never imports `ui`. Use the platform's own idiom for the unit (package, namespace,
module), but keep the names. The Android package names are the reference; see
`01-architecture.md` and `02-naming-and-structure.md`.

### The wet ink adapter

Each shell implements the same contract:

1. The stroke under the pen is drawn through the platform's fastest path.
2. When the stroke ends, the shell hands the finished stroke to the core.
3. The shell then draws the core's output, so the dry result is the same on every platform.

The fast path is a latency device only. It never decides what the stroke finally looks like,
and it never writes document data.

### Theme and design tokens

There is one design, shared by all shells.

- Colour, spacing, shape and type values are defined once and mirrored per platform in that
  platform's theme layer. A value changes in the shared definition first, then in each mirror.
- UI chrome never uses a raw colour, number or font in a component. It reads a theme token.
- Ink colour palettes are document data, not chrome. Define each palette once and have every
  consumer read it.
- Icons go through one `GraphoIcon` set per shell. Do not import an icon library directly
  from a screen.
- Shared pieces (buttons, dialogs, menus, text fields) live in one components location per
  shell. Reuse one before writing another.

### Interface design

- Samsung Notes is the baseline layout on every platform. Follow its layout and habits
  closely, then fix what annoys people about it.
- Respect each platform's conventions where they apply, for example the pen and title bar on
  Windows, or hover and squeeze on Apple Pencil. A platform convention changes how a feature
  is reached, never what the feature does.
- Keep chrome calm. Avoid generic decoration: status chips, floating pill toolbars, and busy
  top bars. Tools stay out of the way until needed.
- The pen draws and fingers scroll and zoom.

### Strings

User-facing strings live in the platform's resource system, never in code, and use the same
keys across platforms where the system allows. Some existing strings are hard-coded and are
being migrated: a new or touched screen uses resources, and you move the literals in the code
you edit. Copy rules are in `06-comments-and-copy.md`.

### Threading and performance

- Never block the render or UI thread with I/O or heavy work. Loading, saving, importing and
  exporting run off it.
- Per-frame and per-event code does not allocate needlessly. Reuse objects or take an output
  object instead of creating new ones per event.
- Geometry stays in document points. Convert to pixels only where the view draws.

### Errors

- Never swallow an error. Log it with context (what was being done, which notebook), or
  surface a notice to the user, or both. An empty catch block is a bug.
- A failure from the native layer is a value to handle, not something to ignore.

### Lifecycle

- Release native handles you own, in the matching lifecycle callback or dispose point. A
  handle has one owner.
- Do not hold a window, activity or view in something that outlives it.
- Transient state (selection, an open wheel, scroll) lives in view state and is not written
  to the notebook. App preferences live in the platform's preference store, apart from
  document data.

### Feature parity

A feature is designed for every shell, even when it is built on one first. Do not add a field
to the data or the file format for one shell only, and do not let a shell's behaviour diverge
from the others. If a platform cannot support something, the design says how the others
degrade and the data still round trips.

## Android

Applies to `android/`.

### Stack and checks

- Kotlin and Jetpack Compose. `minSdk` is 29, because front-buffered rendering needs
  Android 10. Do not add code that quietly needs a newer level without a version check.
- ktlint with the Android Studio style, configured in `.editorconfig`. Run it with
  `scripts/ktlint.sh` before you push. Do not disable a rule inline to get past it.
- Android lint runs in CI (`lintRelease`). Fix findings instead of baselining them.

### Layers

Packages under `com.onemnemo.grapho`: `engine` (`Native.kt`, the JNI contract), `note`, and
`ui` with one subpackage per feature. `python3 scripts/check_boundaries.py` enforces the
dependency rules.

### Wet ink

Wet ink uses front-buffered rendering in `note`. When the stroke ends it is handed to the core
and the core's output is drawn.

### Theme, tokens and icons

- UI chrome colours come from `ui/theme` (`Grapho.colors`, generated into `GraphoTokens.kt`).
  Components never write a raw `Color(0x...)` literal.
- Spacing, radius, size and type come from `GraphoSpace`, `Grapho.metrics`, `GraphoShapes` and
  `Grapho.type`, not from numbers in a component.
- Icons go through the `GraphoIcon` set in `ui/components/Icons.kt`.
- Shared pieces live in `ui/components`.

### Strings

User-facing strings belong in `res/values/strings.xml`.

### Threading and performance

Reuse `Path`, `Paint` and `RectF` objects, or take an output object, instead of creating new
ones per event. The canvas never blocks the render thread.

### Errors

Log an exception with context, or surface a notice, or both. An empty `catch` is a bug.

### Lifecycle

Release native handles in the matching lifecycle callback or `onDispose`. Do not hold an
`Activity` or a view in something that outlives it. App preferences live in
`SharedPreferences`.

### Input

Input changes are checked on a real S Pen (`05-testing-and-verification.md`).

## Windows (planned)

These rules apply once the Windows shell exists.

- C# and WinUI 3, in `windows/`. Nullable reference types are enabled and warnings are errors.
- .NET analyzers are on, and `dotnet format` is the formatter and linter. Run it before you
  push. Do not suppress a diagnostic inline to get past it.
- Wet ink uses `InkPresenter` with custom drying: the platform draws the stroke under the pen,
  and on dry the finished stroke goes to the core and the core's output is drawn.
- `ffi-windows` exposes a C ABI. The shell consumes it through P/Invoke or CsWinRT, wrapped in
  an engine bridge namespace so nothing else touches native calls.
- The shell uses the same layering as every other shell: engine bridge, note and canvas, ui.
- Strings live in `.resw` resources with the same keys as the other shells where possible.

## Apple (planned)

These rules apply once the Apple shells exist.

- Swift in `apple/`, with SwiftUI where it fits and UIKit or AppKit where input or rendering
  needs it. `swift-format` and SwiftLint are the formatter and linter.
- Wet ink is drawn with Metal. Apple Pencil input (pressure, tilt, hover, squeeze) is captured
  in the shell and handed to the core as the same input samples every shell sends.
- `ffi-apple` exposes a C ABI, wrapped by a Swift module that is the only code calling it.
- iPad and macOS share one code base. Put shared code in a common module, and keep per-OS
  code to windowing, menus and input differences.
- Strings live in string catalogs with the same keys as the other shells where possible.
- The shell uses the same layering as every other shell: engine bridge, note and canvas, ui.
