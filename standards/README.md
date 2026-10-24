# Grapho standards

The source of truth for how this project is built. Read the file that covers what you are
about to touch. [quick-reference.md](quick-reference.md) is the short version.

| File | Covers |
|---|---|
| [00-principles.md](00-principles.md) | Engineering philosophy: build for the future, no temporary solutions, modularity, efficiency, build vs depend, consistency, evidence |
| [01-architecture.md](01-architecture.md) | Layer map and dependency rules, the core, ffi and shell split, seams, the `.grapho` format as a contract |
| [02-naming-and-structure.md](02-naming-and-structure.md) | Folder layout, the naming table for Rust, Kotlin, C# and Swift, file size, what to avoid |
| [03-rust.md](03-rust.md) | The core and ffi crates: toolchain, errors, panics, docs, geometry, hot paths |
| [04-platform-shells.md](04-platform-shells.md) | Rules for every native shell, then Android, Windows (planned) and Apple (planned) specifics |
| [05-testing-and-verification.md](05-testing-and-verification.md) | What gets a test, how to run things, device checks, latency and performance claims |
| [06-comments-and-copy.md](06-comments-and-copy.md) | Comment style, the no-dash rule, user-facing copy |
| [07-git.md](07-git.md) | Commit format, body shape, granularity, pull requests |

If a rule here conflicts with a principle in `00-principles.md`, the principle wins and the
rule needs fixing. Say so rather than working around it.
