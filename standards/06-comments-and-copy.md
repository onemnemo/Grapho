# Comments and user-facing copy

## Comments

Comment the **why**, and only when the why is not obvious from the code. A file with no
comments is a fine outcome. Most code should not need one.

Good:

```rust
// The stabiliser lags the pen by design, so the first sample is passed through unchanged.
if self.count == 0 { return sample; }
```

```kotlin
/** Returns the page index under the point, or null in the gap between pages. */
```

Bad, and why:

```kotlin
// We check this first because otherwise the loop below would crash on an empty list.
// (narrating your own reasoning; the code already says this)

// Added error handling here for the beta.
// (changelog comment; git already records when and why)

// Toolbar with a rounded background
// (restates the obvious; the code already says this)
```

Rules:

- No narration of your own reasoning. Not "first we normalise, then we map", not "this is
  tricky because".
- No changelog comments. Not "changed from X", not "previously this used Y".
- **A comment stands on its own.** No milestone identifiers, no section numbers, no plan or
  spec filenames, no issue ids. Name the thing instead: "the front buffer", "the load path",
  "the page cache".
- No to-do, fix-me or hack markers. `scripts/check_repo.py` rejects them. Raise the follow-up
  as an issue instead.
- Keep it short. A file header is a few lines, not an essay.
- If a comment only says what the next line says, delete it.
- Sound like an engineer explaining a constraint to the next engineer.
- `///` on public Rust items and doc comments on public shell members (KDoc, XML docs, Swift
  markup), covering behaviour plus edge cases, is wanted.

## No em dashes or en dashes. Anywhere a person can read.

This is a house voice rule, not a formatting preference, and it is absolute.
`scripts/check_repo.py` enforces it.

It covers:

- Code comments and doc blocks.
- Commit messages, PR titles, and PR bodies.
- Every user-facing string: UI copy, error messages, content descriptions, the README, release
  notes and changelog entries.

Rewrite the sentence with a comma, parentheses, or a new sentence. Do not substitute a hyphen
or a double hyphen as a disguise; that just looks broken.

## User-facing copy

- Strings live in the platform's resource system (on Android, `res/values/strings.xml`), not
  in code. See `04-platform-shells.md`.
- Write copy the way the product should sound: plain, direct, no filler.
- Say what happened and what the person can do about it. "Could not save. Free up storage and
  try again." beats "An error occurred."
