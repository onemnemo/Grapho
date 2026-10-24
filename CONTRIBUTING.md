# Contributing to Grapho

Thank you for wanting to contribute to Grapho.

Grapho is a free, open-source handwriting notes app founded by **@torstfugl** and maintained by
**@torstfugl** and **@MightyThunder88** under the **@onemnemo** GitHub organization.

Please read this document before opening a pull request.

## Project license

Unless otherwise stated, contributions to Grapho are made under the project license:

- Apache License 2.0

By submitting a contribution, you agree that your contribution may be used, modified,
distributed, and maintained as part of Grapho under the project license.

You retain copyright to your own contributions unless otherwise agreed in writing.

## Project governance

Grapho uses a founder-led governance model.

The project owner and maintainers may accept, reject, modify, revert, or remove contributions.
Contributing does not grant ownership, voting rights, release rights, administrative access,
maintainership, or decision-making authority over the official project.

## No employment or payment promise

Contributions are voluntary.

Contributing to Grapho does not create employment, contractor status, partnership, ownership,
equity, revenue share, or a guarantee of future payment.

## Development setup

Build and test instructions are in the [README](README.md#building) and
[android/README.md](android/README.md). In short, `cargo test` covers the Rust core, and the
Android shell needs the Android SDK, a JDK, and `cargo-ndk`. Other shells document their
setup in their own folder once they exist.

## Before contributing

Please try to:

1. Read the [README](README.md).
2. Read [standards/quick-reference.md](standards/quick-reference.md), and the matching file in [standards/](standards/) if your
   change touches code.
3. Search existing issues and pull requests.
4. Open an issue first for large features, architecture changes, file format changes, or
   major interface changes.
5. Keep pull requests focused and reasonably small.

## Pull request expectations

A good pull request should:

- explain what changed
- explain why the change is useful
- link related issues when relevant
- include screenshots or recordings for visible changes
- include tests for new logic
- keep `.grapho` files written by older versions opening, and update
  [docs/file-format.md](docs/file-format.md) when the format changes
- back any latency or performance claim with a proof of correct output from the same run
- use commit messages in the `type(scope): subject` form described in
  [standards/07-git.md](standards/07-git.md)
- avoid unrelated formatting or cleanup
- keep behaviour and saved data identical across platforms, with platform-specific code in
  the shell
- follow the existing architecture and coding style
- avoid adding large dependencies without discussion

Pull requests are merged with rebase, so each commit should stand on its own.

## Architecture and dependency decisions

Grapho values a clean separation between the shared Rust core and the platform shells, ink
quality, performance, open file formats, and long-term maintainability.

New dependencies should be intentional. A dependency may be rejected if it adds unnecessary
complexity, size, licensing issues, security risk, platform issues, or architectural debt.

## Security issues

Please do not publicly disclose security vulnerabilities before giving the maintainers a
reasonable chance to respond.

Use the contact instructions in [SECURITY.md](SECURITY.md).

## Code of conduct

Participation in Grapho is covered by [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

Be kind, be constructive, and remember that this is a passion project maintained by real
people.
