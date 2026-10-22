# Grapho

A free, open-source handwriting notes app for tablets.

Grapho is in early development. Things will break, and the file format may
still change.

## Features

The Android app is the only one so far. It's mainly tested on Samsung Galaxy
Tabs with the S Pen.

- Notebooks and whiteboards with paper templates
- Pens, eraser, shapes, text boxes and lasso select
- Layers and images
- PDF import and export
- Library with folders

Notes are saved as `.grapho` files. See [docs/file-format.md](docs/file-format.md)
for the format.

## Structure

| Path | Contents |
|---|---|
| `core/` | Rust core with the ink, document model and file format |
| `ffi-android/` | JNI bindings for the Android app |
| `android/` | Android app (Kotlin), see [android/README.md](android/README.md) |
| `docs/` | Specs |

The stroke currently being drawn is rendered natively on each platform for low
latency. Everything else goes through the shared core.

## Roadmap

1. Android app
2. Handwriting to text, checklists, links between notes, folder sync
3. Windows
4. iPad and macOS

## Building

```sh
cargo test                              # Rust core
cd android && ./gradlew assembleDebug   # Android app, needs cargo-ndk
```

CI builds an APK for every pull request (`grapho-android` artifact).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [standards/](standards/).

## License

[Apache 2.0](LICENSE)
