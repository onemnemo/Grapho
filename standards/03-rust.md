# Rust

Applies to `core/` and every ffi crate (`ffi-android/` today, `ffi-windows/` and `ffi-apple/`
planned). Layering rules are in `01-architecture.md`.

## Toolchain and checks

- Stable toolchain, edition 2021. No nightly features.
- `cargo fmt` and `cargo clippy --all-targets -- -D warnings` are clean before every push.
- `grapho-core` carries `#![forbid(unsafe_code)]`. Anything that needs `unsafe` belongs in an
  ffi crate, kept to the smallest block with a comment naming the invariant it relies on.
- Placeholder macros (unfinished-code stubs and debug print macros) are denied by the
  workspace lints. Do not allow them locally to get around it.

## Errors

- Errors are typed: functions return `Result` with an error enum that says what went wrong,
  not a string. Implement `Display` and `std::error::Error` for it.
- No panics in library code paths reachable from the FFI. Bad input from a file or from
  a shell is an `Err`, not a crash.
- `unwrap` and `expect` are allowed only where an invariant guarantees success, and the
  `expect` message names the invariant. "Cannot fail" is not a message.
- Never swallow an error. Propagate it, or handle it and say why that is safe.

## The FFI boundary

Applies to every ffi crate, whatever the host language.

- Entry points marshal and delegate. No engine logic lives in an ffi crate.
- Catch panics at every boundary. Never unwind across FFI. A failure comes back to the host as
  a result it can show or log, never as a silent default.
- Document the ownership of every pointer and handle on every C ABI function: who allocates,
  who frees, and which call releases it. A handle has one owner and one release call.
- Keep the surface small and boring: primitives, arrays, and serialized data. Do not leak core
  types across the boundary, and keep signatures usable from a plain C ABI.
- Keep platform types out of core signatures. The core is designed for every ffi crate.

## Documentation

- `///` on public items, covering behaviour and edge cases: empty input, a single sample,
  out-of-range values, what is returned on failure.
- Module headers (`//!`) are a few lines on what the module is for, not a history.
- Comments follow `06-comments-and-copy.md`.

## Geometry and units

- Document geometry is in points (1/72 inch), so a note is the same physical size on every
  screen. Convert to pixels at the edge, in the platform shell.
- A value in any other unit carries it in its name (`spacing_mm`, `screen_px`).
- Colours are `0xRRGGBBAA` unsigned integers, as in the file format.

## Hot paths

Stroke building runs once per input sample, and tessellation and outlines run on every
redraw of a page.

- Reuse buffers across calls instead of allocating per sample or per frame.
- Allocate with a known capacity when the size is known.
- Do not log, format strings, or take locks in these paths.
- Measure before optimizing, and prove correct output when you report a number
  (`05-testing-and-verification.md`).

## Structure

- One concept per module, with the public surface re-exported from `lib.rs`.
- Tests live in `#[cfg(test)] mod tests` at the bottom of the module they cover.
- Dependencies are added deliberately. A new crate needs a reason in the pull request, a
  compatible license, and no platform requirement if it is used by `grapho-core`.
