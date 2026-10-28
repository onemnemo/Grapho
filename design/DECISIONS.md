# Design decisions

This log records the design decisions for Grapho. Each entry gives the context, the decision, the
alternatives rejected and the consequences. Entries are not rewritten. A later entry that changes an
earlier one names it. [Earlier decisions](docs/decisions.md) stand unless an entry here replaces them;
where the two disagree, this file wins.

## Scope

- **Context:** The design covers a Storybook design workspace (`design/src`) and an Android prototype
  (`android/`). The product priority is Android tablets with S Pen first, then Windows, iPad and macOS.
- **Decision:** The current design is `design/src` and its decision log. The prototype is the Android
  app, reviewed from code because no device or emulator was available. In scope: Android tablets
  first, then Windows, iPad and macOS, designed for stylus, then touch, mouse and keyboard. Phones are
  not a target, but the layout must work down to a compact width because tablet split screen and
  small desktop windows reach it anyway. The exact minimum size is set with the screens.
- **Rejected:** Treating the Android app as the design (it is a subset and drifts); designing phones as
  a first-class target (no product decision supports it).
- **Consequences:** Every layout gets compact, medium and expanded behaviour. Android findings are
  code-level until verified on a device.

## Relationship to the earlier decision log

- **Context:** [docs/decisions.md](docs/decisions.md) holds the earlier decisions (themes, packs, selection,
  the pen wheel). Several are still sound and a few work against the principles.
- **Decision:** Earlier decisions stand unless an entry here replaces them. Kept as starting
  points: packs versus extensions, a note keeps its own copy of pack content, people can make
  packs, visual weight and selection style, drag to reorder, Ink as the default theme,
  the tinted selection fill. To be revisited in the content system work: the rules that remove every
  badge and count, because removing every badge and count also removes the source, risk and state
  cues that pack and extension lifecycle needs.
- **Rejected:** Starting from a blank page (throws away sound, tested decisions); keeping every past
  decision as law (some conflict with the principles).
- **Consequences:** Each later entry that changes an earlier decision names it.

## One token source, generated into every shell

- **Context:** The Android theme was hand-written beside the design tokens, with a different font
  (Manrope against Geist), radii (8, 12, 16 against 6, 10, 14), a blue accent and no motion or layer
  tokens. The design CSS also bypasses its own tokens for most radii, sizes and spacing.
- **Decision:** `design/tokens/tokens.json` in the W3C Design Tokens format becomes the single
  source. `design/src/tokens/tokens.css` and each shell's theme files are generated from it, never
  edited by hand.
- **Rejected:** Keeping `tokens.css` as the source and porting by hand (the cause of the drift); a
  shared runtime theme engine across shells (heavier than the problem needs).
- **Consequences:** A build step is needed in `design/` and in each shell. The Custom theme's
  derivation rules must be expressible as data, not only as CSS `color-mix`, so every shell computes
  the same result.

## Stubbed flows are undesigned

- **Context:** Many prototype flows fake success: import adds an empty pack, export writes nothing,
  the library settings button does nothing, Get more packs shows a toast.
- **Decision:** A stub that fakes success counts as an undesigned flow. Every such flow gets a real
  design including its failure states before it is called done.
- **Rejected:** Treating stubs as implementation detail (they hid most of the dead ends in the journeys).
- **Consequences:** Each stubbed flow is covered explicitly by the flow specs.

## A library sidebar, persistent on expanded windows

- **Context:** Trash, Starred and nested folders had no route, and the library header overflowed on
  narrow windows.
- **Decision:** Library places (Home, Starred, Folders, Trash) live in a sidebar, persistent at 840
  and up and a drawer below that. Supersedes the library header in [docs/decisions.md](docs/decisions.md)
  only in that the header no longer has to carry navigation.
- **Rejected:** More header buttons (breaks the four-action rule); a bottom navigation bar (wastes
  height on a writing device and does not fit folders).
- **Consequences:** Home keeps its Recent and Folders rows. The drawer matches the Samsung Notes
  model people know.

## Confirm by dialog, not by holding

- **Context:** The earlier rule used a hold-to-confirm button for permanent deletes. It is used
  nowhere, is hard to discover and has no expected keyboard equivalent.
- **Decision:** Reversible actions happen at once with Undo. Irreversible ones use a Dialog with a
  danger button labelled with the verb and count. `HoldButton` is removed.
- **Rejected:** Keeping both (two patterns for one job); typing the name to confirm (too heavy for
  a notes app).
- **Consequences:** Trash makes almost every delete reversible, so the Dialog is rare.

## One side panel replaces centred tool modals

- **Context:** Colour and customise panels opened as centred modals over the page being written on.
  Inspector, Panel, WheelPanel and QuickSheet overlapped in what they do.
- **Decision:** One Side panel surface, non-modal, beside the page on expanded windows and over the
  page edge on medium. It hosts pages, layers, tool settings, the colour editor, insert pickers and
  extension panels. On compact windows it becomes a Sheet.
- **Rejected:** Keeping centred modals for "quick" edits (they block the page they edit).
- **Consequences:** The pen wheel keeps in-wheel editing for size and opacity; anything larger opens
  the Side panel.

## Window size classes drive layout

- **Context:** The library and Studio had no responsive model.
- **Decision:** Compact under 600, medium 600 to 839, expanded 840 and up, measured on the window.
  Minimum window 360 by 480.
- **Rejected:** Device-based layouts (split screen and desktop windows break them).
- **Consequences:** Every screen spec states its three layouts.

## Selected items are tinted and checked, never shrunk and dimmed

- **Context:** The library shrank selected items and dimmed the rest, which reads as disabled and
  contradicts the tinted selection rule.
- **Decision:** One selection grammar (SYSTEM-MAP 4.7) and one look: tinted fill plus a check.
  Supersedes the library selection line in [docs/decisions.md](docs/decisions.md).
- **Rejected:** Keeping a library-only look.
- **Consequences:** Library, pickers in manage mode and Settings content pages behave the same.

## The core owns the shared models; shells render

- **Context:** The system map did not say who owns the library model, the library undo, the
  registries, jobs, search or deep links, so every shell would have invented its own.
- **Decision:** The Rust core owns the library model and Trash, the library action journal (last
  100 actions per device), document undo stacks, the command, settings and content type registries
  as data, the settings store, the job runner, the search index and queries, the deep link
  resolver, and pack and extension install state. Shells own rendering, input and platform
  services. Handwriting recognition is a per-platform provider behind one seam. Detail in
  `system/ownership.md`.
- **Rejected:** Per-shell models synchronised by convention (the cause of the Android drift).
- **Consequences:** Proposed changes to `standards/01-architecture.md` and `docs/file-format.md` are
  listed in `system/ownership.md`. Open question: recogniser parity across platforms.

## Extensions are sandboxed scripts with declarative native UI

- **Context:** The earlier decision said extensions are "sandboxed web code". A web view per extension
  cannot honour the slot rules, looks foreign in every shell and is hard to secure.
- **Decision:** Replaces that part of the earlier decision. Extensions run in a sandboxed interpreter
  owned by the core, with no DOM and no native code. Panels and settings pages are trees of Grapho
  standard controls rendered natively. Blocks render to a vector drawing plus a stored fallback.
  Permissions are enforced at the interpreter's API boundary.
- **Rejected:** Web views (foreign look, unenforceable slots); native plugins (unsafe, per
  platform).
- **Consequences:** Extensions cannot draw arbitrary UI. Open question: Apple App Store policy on
  interpreted extensions.

## A tool dock that never recedes

- **Context:** The earlier map let chrome recede while writing, but Undo must never be hidden, and
  the quick slots sat on the receding bar.
- **Decision:** A new Tool dock surface holds the current tool, five quick slots (pinned never
  dropped), Undo and Redo, on the edge away from the writing hand, and never recedes. The note bar
  recedes while writing and returns on a top edge tap or 1.5 s without pen contact.
- **Rejected:** A note bar that never recedes (takes writing space); Undo only by gesture (not
  discoverable).
- **Consequences:** Handedness decides the dock, side panel and toast side. The S Pen button click
  summons the wheel; holding it is the eraser.

## The side panel on compact is a bottom panel, not a sheet

- **Context:** The side panel was a modal Sheet on compact windows, which goes against principle 1.
- **Decision:** Replaces that part of the side panel decision. On compact the side panel is a
  non-modal bottom panel at half height; the page above stays live and writable.
- **Rejected:** A modal sheet (blocks writing); hiding panels on compact (loses function in split
  screen).
- **Consequences:** Back closes the bottom panel before leaving the place.

## Text size follows the operating system

- **Context:** Appearance had its own text size, which fights OS scaling and 200% zoom.
- **Decision:** No Grapho text size. All type scales with the operating system setting.
- **Rejected:** A second multiplier (double scaling, untestable).
- **Consequences:** Layouts are checked at 200% OS scaling.

## Content system rules

- **Context:** The content system draft raised four points that needed a decision.
- **Decision:** The rules of the content system specification are accepted. The remove Sheet appears
  only when the pack has items in use; otherwise removal is immediate with an Undo toast. Notes that
  embed pack assets move to file version 4; newer apps open every older file as required, older apps
  refuse version 4. Extension rows may carry one quiet "Connects to N sites" line, an exception to the
  rule against redundant subtext. Install consent is a Sheet.
- **Rejected:** Always showing the remove Sheet (a step before an undoable action); keeping document
  assets outside the file (breaks sharing and offline).
- **Consequences:** The core keeps a per-document usage index of asset hashes so "Used in N
  notes" is cheap. `docs/file-format.md` gets a version 4 section with the implementation.

## Content system: signing and embedding

- **Context:** Four items in the content system needed a decision.
- **Decision:** Every pack and extension is signed and its publisher key is pinned on first
  install. Documents embed resolved items (definition and bytes), keyed by hash, validated on open
  like an import. Built-in content is referenced by id and never embedded. A pack on or off switch
  shows no toast. All-rights-reserved assets render in received documents but cannot be extracted
  into a library or a pack.
- **Rejected:** Unsigned packs with name matching (spoofable); embedding built-ins (pushes every
  note to file version 4).
- **Consequences:** Open questions: Apple App Store policy, and whether a pack catalogue exists.
  Extension quotas need measurement on real hardware.

## Contrast pairs are an allow-list

- **Context:** The build tests 1740 declared pairs. An independent recomputation agrees with it except
  for translucent fills composited on `surface/strong` (14 pairs, 4.26 to 4.49).
- **Decision:** `$extensions.grapho.pairs` is an allow-list: a foreground may only sit on a host
  listed for it, and the build fails any listed pair below its threshold. Translucent fills never
  sit on `surface/strong`.
- **Rejected:** Grading every possible pair (most never occur, and it pushes colours to extremes);
  tuning status colours for the `surface/strong` case (the combination does not exist).
- **Consequences:** Component specs name the host of every foreground, and the lint checks
  placements against `tokens/usage-rules.json`.

## Flow gap resolutions

- **Context:** The flow specs left twelve gaps in the map (`flows/COMPONENTS.md`).
- **Decision:** A short height class (under 500) turns the dock horizontal.
  Contextual settings live in the Tool settings tab, never the document menu. The Pages tab uses
  the Selection bar. The wheel centre holds width and Previous tool. `appearance.resetTheme` and
  `bar.show` are registered, and the palette opens from the Search field with `>`. Text
  highlighting is the Highlighter's "Snap to text" option. QuickSheet becomes the New item Sheet.
  The empty library may show "Sign in to restore". The first-run Sheet ignores scrim taps.
  Settings and the sync status sit at the sidebar foot. Extensions are removed like packs.
- **Rejected:** A height-based layout for every surface (only the dock and side panel need it).
- **Consequences:** `SYSTEM-MAP.md` and `system/registries.md` are updated to match.
- **Open:** Key renames, the Studio drawing area on medium, and compact drawings for every flow are
  not yet decided.

## Storybook is the living style guide; components are built in React

- **Context:** A standalone HTML gallery was considered, but the repository already has a React and
  Storybook workspace (`design/src`) with stories, theme and language toolbars and guard rails, documented
  as the source of truth for every shell. A second gallery would drift from it.
- **Decision:** No standalone HTML gallery is kept. The token build generates `design/src/tokens/tokens.css`
  with a legacy alias layer so existing components keep working. Each component spec in
  `design/components/` is built as a React component with a story for every variant and state, and
  integration gates and screens are Screen stories reviewed through the Storybook toolbars (theme,
  density, platform, language).
- **Rejected:** A standalone HTML gallery (duplicates Storybook, drifts); specs only, with no
  rendered components (claims about visual weight cannot be checked).
- **Consequences:** The React component library is the source the shells port from. The legacy
  aliases are removed once every component reads the semantic tokens.

## Ink is never lost to chrome

- **Context:** Chrome could swallow or fake input while writing in three ways: the note bar's top-edge
  return tap ate pen dots at the top of the page, a pen-down after a popover was dropped, and a resting
  palm could press Undo on the dock.
- **Decision:** Pen input passes through light surfaces: a pen-down on the page outside a Popover
  or Menu closes it and the stroke is drawn. The note bar's top-edge return tap counts only for
  finger and mouse. While the pen is near or just used, palm-sized touches are ignored on all
  chrome and scrims, and dock taps must be short. Pushed views go three levels deep in Sheets and
  the Side panel and one in a Popover, each with Back. Extension consent is the extension kind of
  the import review Sheet. An extension that is off leaves no trace in chrome.
- **Rejected:** Dropping the first pen stroke to dismiss (loses ink silently); disabling the dock
  while the pen is near (blocks the off-hand Undo people rely on).
- **Consequences:** SYSTEM-MAP section 3 covers light modality, pushed views and the Back order.
  Palm thresholds come from each platform's touch size reporting and need device testing.

## Product answers: catalogue, extensions on Apple, recognition, depth

- **Context:** Four product questions were open.
- **Decision:**
  - There is an official, free, curated pack catalogue on grapho.one. Every picker's Get more opens
    it filtered by type. It still has offline and unavailable states, and Import from file stays.
  - Extensions ship first on Android and Windows. iPad and macOS ship packs at launch and add
    extensions only after App Store policy on interpreted extensions is confirmed with Apple. The
    design keeps one extension system for every platform; the Apple shells hide the Extensions
    entry until then.
  - Handwriting search uses each platform's built-in recogniser behind the core's recognition seam.
    Results and supported languages may differ between devices; the index stores the text
    candidates, so a document recognised on one device is searchable on another.
  - The remaining component work continues at full depth.
- **Rejected:** File sharing only (makes Get more a dead end for most students); extensions on
  Apple from day one (App Review risk); one shared recognition engine (large effort before launch).
- **Consequences:** The catalogue needs a hosted service and a review process for submitted packs.
  Recognition parity across platforms is a known limitation, not a bug.

## The original UI is the design

- **Context:** An earlier build replaced the workspace UI in place: new theme colours, a rebuilt
  Paper theme, a library sidebar and top bar, a tool dock, a permanent side panel for layers, a new
  selection look and restyled components. That direction was rejected. Grapho is a tablet app in
  the family of GoodNotes, Samsung Notes and Apple Notes: a minimal library with folders and
  notebooks on one home screen and New in the top corner, and a note view that gives the page as
  much room as possible.
- **Decision:** `design/src` is restored to the original UI. Every later change fixes something
  broken, inconsistent or rough inside the original visual language, names the gap it fixes,
  and is shown before and after per component. A layout or structural change needs approval first.
  This replaces these earlier entries:
  - *A library sidebar, persistent on expanded windows*: no library sidebar. At most an optional
    collapsible list behind a small toggle.
  - *One side panel replaces centred tool modals* and *The side panel on compact is a bottom panel*:
    no permanent side panel over the page. Layers, pages and tool settings open on demand and close
    again; they never take writing space by default.
  - *Selected items are tinted and checked*: the original selection look stays until a fix for it is
    agreed.
  - *A tool dock that never recedes*: no tool dock. The original note bar, which hides itself while
    writing (with its setting), and the collapsible side toolbar stay.
  - *Window size classes drive layout*: tablet layouts come first; compact phone layouts are a
    fallback, not the reference.
  - *One token source, generated into every shell*, as applied: `design/tokens/tokens.json` and the
    derived themes stay as a proposal only and do not drive `design/src`. The original themes,
    including Paper, stay.
- **Rejected:** Keeping the new structure with the old colours (the structure itself was the
  problem); keeping the rebuild (it is a different app).
- **Consequences:** Specs under `design/components`, `design/screens`, `design/flows` and
  `SYSTEM-MAP.md` that assume a sidebar, a dock, a permanent side panel or the new themes are void
  where they disagree with this entry. The known gaps in the prototype, and the pack and
  extension model, remain the input for the fixes.
