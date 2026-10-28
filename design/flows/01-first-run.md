# 01 First run

## Goal

From first launch to the first pen stroke in about a minute, with the two answers that decide the
feel of the pen (writing hand, pen-only) set right, and nothing else asked. Skip keeps the defaults.

Counting rule (all flows): start at the named place, count every tap and every field focus, add 1
for the Note bar reveal when the user is mid-writing, add 1 where a Picker footer collapses into
More on compact. Expanded and compact are given when they differ.

## Entry points

- First launch after install.
- A deep link or shared file arriving first is held and applied after (system/registries.md section 5).
- Settings, Pen and tools changes both answers later. First run never shows again.

## Surface

First run is a **Sheet with steps**, opened over the empty library (not a Settings page, not a
place). Header: step dots and the title; foot: Skip (text button) and Continue (primary). Steps are
views inside the one Sheet. Scrim tap does nothing; Escape is Skip. `Sheet`, `ChoiceCard`,
`Toggle`, `Select`, `Button`, `Popover` (hint).

## Steps

| # | Step | Components |
|---|---|---|
| 1 | Language, only if the device language is unsupported | `Select` with `SearchField` filter; device languages pinned first |
| 2 | Writing hand and pen-only | two `ChoiceCard`s (Right, Left, drawn), a `Toggle` "Only the pen draws. Fingers scroll and zoom." |
| 3 | Sheet closes; empty library | `EmptyState` md with the mascot, `Button` primary "New notebook", text link "Import", text link "Sign in to restore" (see 14) |
| 4 | New notebook | `NewItemSheet` (02); defaults make Create valid |
| 5 | First note opens | a first-use hint `Popover` on the dock's tool icon |

Skip on any step keeps the defaults: right hand, pen-only on if a stylus has touched the screen
(off otherwise). Theme is not asked. Sync stays off until chosen (14). Permissions come in context:
files at Import, camera at Take photo (15), microphone only when an extension asks.

Pen-only detection: while the user has not touched the toggle, it follows the hardware (stylus
seen: on, "S Pen found."; none: off, "Fingers will draw."). Once the user sets it, their choice
wins and is never overridden.

First-use hint (step 5): a hint `Popover` anchored to the dock's tool icon, overlaid with no reflow:
"Tap to change tools. Click the S Pen button to open the wheel anywhere." with Got it. It fades on
Got it or after the first stroke; it never moves content and never returns. Without a stylus
the text is "Tap the pen to change tools."

## Tap counts

| Task | Taps |
| --- | --- |
| Launch to first stroke, defaults | Continue (step 2), New notebook, Create = 3 (4 with a language step; Skip is also 1) |
| Left-handed | +1 (Left card) |
| Name the first notebook | +1 (the field tap raises the keyboard; typing is not a tap) |

Setup cost is paid once: 3 taps, then never.

## States

| State | What the user sees |
|---|---|
| Unsupported device language | Step 1, names in their own scripts |
| Stylus seen / not seen | see pen-only detection above |
| Loading | local; skeleton tiles after 300 ms if the library index is slow |
| Error: storage unreadable | The empty state is replaced by an inline error "Grapho could not open its storage. Try again." with Retry |
| Offline | Not applicable: first run needs no network |
| Very large | Not applicable: nothing to list |
| Interrupted | Answers given are kept; next launch resumes at the unanswered step |
| Compact | Bottom Sheet near full height; cards stack |
| German | "Weiter", "Überspringen"; Continue and Skip stay on one line down to 360 dp |

## Exits

| Exit | Result |
|---|---|
| Continue on the last step | Empty library |
| Skip, Escape | Defaults kept, empty library |
| Back on step 2 | Step 1 if shown |
| Storage failure | Inline error with Retry |
| System Back on the empty library | Leaves the app |

## Wireframes

Expanded, step 2 (Sheet over the empty library):

```
+------------------------------------------------+
| o o   Welcome                                  |
+------------------------------------------------+
|  Which hand do you write with?                 |
|   +--------------+     +--------------+        |
|   |  [right]     |     |  [left]      |        |
|   |  Right  (x)  |     |  Left        |        |
|   +--------------+     +--------------+        |
|  Only the pen draws. Fingers scroll.   [ on ]  |
|  S Pen found.                                  |
+------------------------------------------------+
|  Skip                              [ Continue ]|
+------------------------------------------------+
```

Empty library, Expanded:

```
+-----------+------------------------------------------------+
| Home      | Library                                         |
| Starred   +------------------------------------------------+
| Trash     |                 (mascot)                        |
| [gear]    |        Your first notebook starts here.         |
|           |            [ New notebook ]                     |
|           |       Import     Sign in to restore             |
+-----------+------------------------------------------------+
```

First note, hint over the dock (Expanded, right-handed):

```
+----+-------------------------------------------------------+
|[~] | +-------------------------------+                     |
|S1  | | Tap to change tools. Click    |                     |
|S2  | | the S Pen button for the      |                     |
|Un  | | wheel anywhere.     [ Got it ]|                     |
|Re  | +-------------------------------+                     |
+----+-------------------------------------------------------+
```

Medium: as Expanded; the library sidebar is a drawer. Compact: bottom Sheet; hint over the bottom
dock strip. Left-handed: the dock and its hint mirror to the right.

## Access

Keyboard: Tab through cards, Space selects, Enter is Continue, Escape is Skip. Screen reader: the
cards are a radio group ("Right, selected"); the toggle announces its state; step changes announce
"Step 2 of 2". Focus returns to the empty state's New notebook when the Sheet closes.
