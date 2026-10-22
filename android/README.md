# Grapho for Android

The Android app: a Kotlin and Jetpack Compose shell around `grapho-core`.

## What's in it

- **Notebook list.** Create notebooks (pages) and whiteboards (an infinite
  canvas), rename, delete, import and export `.grapho` files.
- **Note screen**, laid out like Samsung Notes: a thin bar with back, the title,
  undo, redo and a More menu. Pens, colours, eraser and fill live in the pen
  wheel.
- **Pages with backgrounds**: blank, lined, grid or dotted paper in A4, A5 or
  Letter, portrait or landscape.
- **S Pen first.** The pen draws; fingers scroll and pinch to zoom. Holding
  the S Pen button erases. "Draw with finger" is in the More menu.
- Notebooks save automatically to the app's storage as `.grapho` files.

## How ink is drawn

The stroke under the pen is drawn straight into the display's front buffer
(`CanvasFrontBufferedRenderer`), with `MotionEventPredictor` reaching a few
milliseconds ahead. This path measured about 1 ms of input-to-draw time on
a Galaxy Tab. When the pen lifts, the stroke
is stored in the document and redrawn with antialiased outlines from
`grapho-core`.

Ink is stored in points (1/72 inch), so a note looks the same size on every
screen.

## Building locally

Needs Android Studio (or the SDK and JDK 17), Rust, and:

```sh
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk
export ANDROID_NDK_HOME=/path/to/ndk
cd android && ./gradlew installRelease
```

The Gradle build runs `cargo ndk` itself and bundles the library.

## Signing

Builds are signed with `test.keystore`, which is committed on purpose, so
each new APK installs over the last one. It's public, so only use it for
test builds. Store releases will be signed with a private key kept in CI
secrets.
