# Naming and folder structure

One set of conventions for the whole project. A contributor who has read one module should be
able to predict the shape of the next one.

## Where things go

```
core/                  grapho-core: one module per concept, document/ for the model and format
ffi-android/           JNI bindings
ffi-windows/           C ABI bindings for the Windows shell (planned)
ffi-apple/             C ABI bindings for the Apple shells (planned)
android/               Android shell
  app/src/main/java/com/onemnemo/grapho/
    engine/            Native.kt, the JNI contract
    note/              canvas, wet ink, dry cache, viewport, notebook wrapper
    ui/                GraphoApp.kt, plus one package per feature
      components/      shared Compose components and Icons.kt
      theme/           GraphoTokens.kt (generated), GraphoText.kt, GraphoShapes.kt, Appearance.kt, Theme.kt
      library/         the notebook list
      note/            the note screen, note bar, pen wheel
windows/               Windows shell (planned), same layers: engine, note, ui
apple/                 iPad and macOS shell (planned), same layers: engine, note, ui
docs/                  specifications, starting with the file format
standards/             this document set
scripts/               repository checks
```

Rules:

- A package per feature in Kotlin, a module per concept in Rust.
- One concept per Rust module. One public composable per Kotlin file where practical, and a
  file per main class.
- Prefer a new file over a growing one. Roughly 400 lines is the point at which a file that
  your change is growing should be split instead.
- Feature words are consistent across layers and across every platform shell. A feature called "notebook" is `notebook` in
  core, every ffi crate and every shell, `Notebook` as a type, and `note` or `library` as the commit
  scope that fits. Do not rename a concept as it crosses a boundary.

## Naming table

| Kind | Convention | Example |
|---|---|---|
| Rust module, function, variable | `snake_case` | `stabilizer`, `tessellate_step` |
| Rust type, trait, enum variant | `PascalCase` | `StrokeBuilder`, `InputSample` |
| Rust constant, static | `SCREAMING_SNAKE_CASE` | `SCREEN_TOLERANCE` |
| Rust tests | `#[cfg(test)] mod tests` at the bottom of the module | |
| Kotlin class, object, interface | `PascalCase`, file named for the main class | `Viewport.kt` |
| Kotlin function, property | `camelCase` | `zoomedAt` |
| `@Composable` function | `PascalCase` | `PenWheel` |
| Kotlin constant | `SCREAMING_SNAKE_CASE` | `PAGE_GAP` |
| Kotlin package | lowercase, one per feature | `ui.library` |
| Kotlin test | `<Name>Test.kt` next to what it covers, in the matching test package | `ViewportTest.kt` |
| C# type, method, property, namespace | `PascalCase`; fields and locals `camelCase` | `StrokeSession`, `LoadAsync` |
| C# interface | `PascalCase` with an `I` prefix | `INotebookStore` |
| C# async method | `Async` suffix | `SaveNotebookAsync` |
| C# constant | `PascalCase` | `PageGap` |
| Swift type, protocol | `UpperCamelCase` | `NotebookStore` |
| Swift function, property, variable | `lowerCamelCase`, following the Swift API Design Guidelines | `zoomed(at:)` |
| Swift constant, enum case | `lowerCamelCase` | `pageGap` |

Class names are singular. Names are descriptive.

## Avoid

- Non-standard abbreviations. Common ones such as `ctx` are fine; `mgr`, `hdlr`, `svc` are not.
- Hungarian notation (`strName`, `intCount`).
- God objects and god composables. If a class fetches data, decides rules, and draws, it is
  three things wearing one name.
- Duplicated menu or action lists. Export one item builder and let both call sites use it.
- A folder or file whose name says nothing (`utils`, `helpers`, `misc`) collecting unrelated
  code. Name the concern.
- Units in the data that are not obvious from the name. Document geometry is in points, so say
  so when a value is anything else (`spacing_mm`, `screenPx`).
