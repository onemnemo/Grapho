# Git, commits, and pull requests

The audience for a commit message is somebody reading this repository on GitHub who has never
seen the author's machine. Everything below follows from that.

## Subject line

The format is mandatory, and `scripts/check_commits.py` enforces it on pull requests:

```
type(scope): subject
```

Subject is lowercase and imperative. A bare `canvas: fix zoom` is wrong; it must be
`fix(canvas): keep the pinch focus point fixed while zooming`.

```
feat(wheel): open the pen wheel under the stylus hover point
fix(format): read pattern kinds written before dotted paper existed
perf(ink): reuse the outline buffer between samples
```

- Types: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
- Scopes: `core`, `format`, `ink`, `fill`, `ffi`, `android`, `windows`, `apple`, `canvas`, `library`, `note`,
  `wheel`, `ui`, `theme`, `design`, `docs`, `ci`, `repo`, `release`. `design` is the design
  workspace in `design/`.
- The subject says what changed for a user, not which function was edited.
- Commits made before this standard used plain sentence subjects. The check applies only to
  new commits.

## Body

A one-line summary of the change, then bullets, one per distinct part.

- Bullets are terse by default: state what changed.
- Add a reason to a bullet **only** when the change is non-obvious: a bug fix, a migration, a
  workaround, or a deliberate divergence. Features skew terse, fixes keep the why.
- Skip the summary line when there is a single bullet or the subject already says it all.
  Do not bullet a single-idea change.
- A small self-evident change gets a one-line body, or none at all.
- The body is a plain changelog of the resulting repository state: what changed, and when
  necessary, the constraint or invariant that explains why the code must be this way.

Write about the code as it stands, not the path that led there.

```
fix(format): read pattern kinds written before dotted paper existed

Older files store the pattern as a bare string. The loader now maps
it to the current shape, so notebooks saved by earlier builds open
with the paper they had.
```

## Hard rules

- **No em dashes or en dashes**, in the subject or the body.
- **No references to local-only material.** No section numbers, milestone identifiers, plan or
  spec filenames, no "per the spec" or "per the plan".
- The message must stand alone for a first-time reader.

## Granularity

Bundle related work into one commit. Do not commit every tiny step.

- A feature lands in roughly two or three commits along its natural seams: one per layer
  (core, ffi, shell) or one per user-visible surface. Not eight.
- A pure-logic module and the code that is its only consumer belong in the same commit.
- A trivial follow-up that refines the commit before it is folded into that parent rather than
  added as its own commit.
- Every commit builds and passes CI on its own, because changes are merged with rebase.

## Pull requests

- Changes land through pull requests into `main`. Never push directly to `main`.
- Pull requests are merged with rebase, not squash, so each commit must stand on its own and
  the history you wrote is the history that stays.
- Do not amend or rewrite commits that have been pushed, and do not rewrite history on
  someone else's branch.
- Same rules as commit bodies: self-contained for a first-time reader, no dashes, no trailers.
- Fill in the pull request template, including the exact checks you ran.
