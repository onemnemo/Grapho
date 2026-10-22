# Grapho

A free, open-source handwriting notes app for tablets. It keeps the familiar
Samsung Notes / GoodNotes workflow and fixes what people complain about:
subscriptions, lock-in, fragile sync, no infinite canvas, weak PDF handling,
and Android and Windows being treated as second-class.

Grapho is early. This repository holds the Android app and the shared Rust
core: notebooks and whiteboards with paper templates, pens and an eraser,
shapes, text boxes, layers, lasso selection, images, PDF import and export,
and a library of notebooks, all saved as open `.grapho` files.

## Principles

- **Your files, an open format.** No account. Notes stay readable without Grapho.
- **Local-first sync you control.** Sync through a folder (Syncthing, Drive, OneDrive, WebDAV).
- **Infinite or paged, per note**, with panning in every direction.
- **PDF done properly.** Keep outlines on import, export full-quality vector PDFs.
- **Calm interface.** Tools stay out of the way until you need them.
- **No paywall, no forced AI.**

## Architecture

```
 Android shell        Windows shell        iPad / macOS shell
 Kotlin               C# + WinUI 3         Swift
 └ wet-ink adapter    └ wet-ink adapter    └ wet-ink adapter
   (front buffer)       (InkPresenter)       (Metal)
          \                 |                 /
           ──────────  grapho-core (Rust)  ──────────
           stroke modelling · stabiliser · tessellation
           document model · file format · PDF · sync
```

The stroke being drawn ("wet ink") goes through each platform's fastest
drawing path. Everything else is shared Rust, so a note looks identical on
every device.

## Roadmap

| Phase | Scope |
|---|---|
| P0 | Ink latency prototype on Android (done) |
| P1 | Android MVP (in progress) that matches Samsung Notes: infinite and paged canvas, layers, unlimited pens, lasso, tabs, PDF import and full-quality export, phone layout |
| P2 | Handwriting-to-text in place, checklists and tables, links between notes, sync, templates |
| P3 | Windows |
| P4 | iPad and macOS |
| P5 | Beyond notes: DOCX/EPUB, web clipping, video, Linux |

## Repository layout

| Path | What it is |
|---|---|
| `core/` | `grapho-core`, the platform-independent engine (Rust): ink, the document model and the `.grapho` file format |
| `docs/` | Specifications, starting with the [file format](docs/file-format.md) |
| `ffi-android/` | JNI bindings that the Android app loads (`ffi-windows/` and `ffi-apple/` follow with their shells) |
| `android/` | The Android app. See [android/README.md](android/README.md) |

## Building

```sh
cargo test                     # Rust core
cd android && ./gradlew assembleDebug   # Android app (needs cargo-ndk, see android/README.md)
```

CI builds an installable APK for every pull request. Download it from the
`grapho-android` artifact on the workflow run.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
[standards/quick-reference.md](standards/quick-reference.md) is the short version, and [standards/](standards/) holds the
full development standard.

## License

[Apache License 2.0](LICENSE)
