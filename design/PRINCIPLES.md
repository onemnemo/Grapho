# Design principles

These are the tie-breakers. When two good options conflict, the higher principle wins. Each one
comes from problems in the current prototype, not from taste. They extend `docs/branding.md`, which still
holds for voice, colour, shape and the mascot.

## 1. The page belongs to the pen

Writing is the product. Everything else gets out of the way while the pen is down and comes back in
one move when it is needed.

- **We do:** chrome that recedes while writing, tool and colour switches in one gesture from where
  the hand already is, panels that sit beside the page and keep it live, edge and palm guards
  designed per side.
- **We don't:** modal panels over the page for routine changes, fixed strips that eat writing space,
  anything that moves, blinks or reflows while the pen is touching the page.

## 2. Nothing the user made is ever lost

Mistakes, removed packs, missing extensions and other devices are normal. Every one of them has a
designed, recoverable outcome.

- **We do:** undo first and confirm only for what cannot be undone, a Trash behind every delete, notes
  that carry a copy of every pack asset they use, a visible placeholder for content whose extension is
  gone, undo that survives the next notification.
- **We don't:** permanent deletes from one tap, a toast that silently cancels another toast's Undo,
  references that break when a pack is turned off, removed or updated.

## 3. One way to do each job

A person who has used one picker, one sheet or one delete has learned them all.

- **We do:** one picker and manager pattern for every content type, one surface type per job, one
  confirm rule, one place each setting lives with the global index pointing at it.
- **We don't:** a new picker per content type, a sheet where the system says popover, two names for
  the same thing ("Documents" and "Library", "Whiteboard" and "Canvas").

## 4. Built to be added to

Grapho will gain pack types, tools, commands and extensions it does not have today. They arrive
through registries and slots, not new bespoke screens.

- **We do:** content types, tools, commands and extension contributions defined as registry entries
  that existing surfaces read; a written growth and overflow rule for every surface that will grow.
- **We don't:** closed lists of types or tools baked into a component, a toolbar that only works with
  today's item count, a feature that needs a new surface type to exist.

## 5. Content has the colour, chrome stays calm

Covers, ink, stickers and themes are the rich part. The frame around them is quiet so they can be.

- **We do:** grey and white chrome from semantic tokens, pickers that show the real thing large
  enough to choose by eye, one primary action per surface, counts folded into the control they
  describe.
- **We don't:** accent colour in controls, status chips and badges as decoration, six equal-weight
  buttons, a settings form where a visual picker would show the thing itself.

## 6. Every input is first class

Stylus first does not mean stylus only. Fingers, a mouse, a keyboard and a screen reader reach every
action.

- **We do:** 44 point targets on touch, a command registry that gives every action a keyboard path
  and a name, focus that is always visible, hover affordances with a touch and stylus equivalent,
  layouts that hold at 200% text and in a half-width split screen.
- **We don't:** pointer-only engines, actions that exist only on hover or long press, layouts tuned for
  one window size.

## 7. One design, every shell

The design system is platform-neutral data. Each shell renders it natively, with platform
conventions where they help the user, and never drifts on its own.

- **We do:** tokens and component contracts defined once and generated into each shell, the same
  vocabulary on every platform, platform differences written down as decisions.
- **We don't:** hand-copied palettes, a different font or accent on one platform, a shell that adds
  screens the design has never seen.

## 8. Designed for the real state, not the demo

A screen is not designed until its awkward states are.

- **We do:** empty, first run, loading, error, offline and very large states for every view (0, 1 and
  500 items, a 300 page PDF, a German label), long operations with progress and cancel.
- **We don't:** a toast standing in for a screen, a prototype that fakes success, a picker drawn only
  with nine perfect items.
