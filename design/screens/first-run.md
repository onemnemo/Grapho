# Screen: First run

## 1. Purpose

From first launch to the first pen stroke in about a minute, with the two answers that decide the feel
of the pen (writing hand and pen-only) set right and nothing else asked. First run is one Sheet with
steps, opened over the empty library. It is not a Settings page and not a place. It never shows again;
both answers can be changed later in Settings, Pen and tools.

## 2. Flows served

01 first run (main), 02 (the first notebook through NewItemSheet), 03 (the first-use hint on the dock),
14 ("Sign in to restore" link), 13 (a shared file or link that arrives first is held and applied after).

## 3. Steps

| # | Step | Shown when |
|---|---|---|
| 1 | Language | Only if the device language is not supported |
| 2 | Writing hand and pen-only | Always |
| 3 | The empty library | After the Sheet closes (Continue, Skip or Escape) |
| 4 | New notebook | NewItemSheet with defaults that make Create valid |
| 5 | The first note opens with a first-use hint on the dock tool | Once |

Permissions are asked in context, never here: files at Import, the camera at Take photo, a microphone
only when an extension asks. Theme is not asked. Sync stays off until chosen.

## 4. Layout and regions

Regions: Sheet (header with step dots and the title, body, foot with Skip and Continue), the empty
library behind it (inert while the Sheet is open). The Sheet is centred on Medium and Expanded and a
bottom Sheet near full height on Compact. Scrim tap does nothing. Escape is Skip.

### Expanded and Medium, step 2 (Sheet over the empty library)

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

### Compact, step 2

```
+----------------------------------+
| o o   Welcome                    |
| Which hand do you write with?    |
| +------------------------------+ |
| | [right]  Right          (x)  | |
| +------------------------------+ |
| | [left]   Left                | |
| +------------------------------+ |
| Only the pen draws.       [ on ] |
| S Pen found.                     |
|  Skip                [ Continue ]|
+----------------------------------+
```

The ChoiceCards stack on Compact. Continue and Skip stay on one line down to 360 wide, also in German
("Weiter", "Ueberspringen").

### Step 3, the empty library (Expanded, R)

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

The Sidebar is a drawer on Medium and Compact. L: nothing on this step mirrors; the Toast side changes
to the right.

### Step 5, first-use hint (Expanded, R and L)

```
R (dock left):                         L (dock right):
+----+---------------------------+     +---------------------------+----+
|[~] | +------------------------+|     |+------------------------+ |[~] |
|S1  | | Tap to change tools.   ||     || Tap to change tools.   | |S1  |
|S2  | | Click the S Pen button ||     || Click the S Pen button | |S2  |
|Un  | | for the wheel.[Got it] ||     || for the wheel.[Got it] | |Un  |
+----+---------------------------+     +---------------------------+----+
```

On Compact the hint sits over the dock corner strip, on the off-hand side. It is an overlay: it never
moves content, never steals focus, and never returns.

### Short

The Sheet is full height; the foot keeps Continue and Skip; the body scrolls. The ChoiceCards stack.

## 5. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Frame | Sheet (first run recipe) | Step dots in the header; Skip (`text` Button) and Continue (the one `primary` Button) in the foot; scrim tap never dismisses |
| Step 1 | Select with SearchField filter; device languages pinned first, names in their own scripts | |
| Step 2 | ChoiceCard x2 (Right, Left, drawn), Toggle "Only the pen draws. Fingers scroll and zoom." | The ChoiceCards are a radio group |
| Step 3 | EmptyState `first-use` `md` with the mascot, Button `primary` large "New notebook", text Buttons "Import" and "Sign in to restore" | TopBar New becomes `secondary` while the EmptyState primary shows |
| Step 4 | Sheet NewItemSheet kind notebook | Defaults make Create valid |
| Step 5 | Popover (first-use hint) with Got it | Anchored to the dock's current tool, `role=status` |
| Error | Banner `placement` region | Step 3 only |

Pen-only follows the hardware until the user touches the Toggle: a stylus seen sets it on with the line
"S Pen found."; none sets it off with "Fingers will draw." Once the user sets it, their choice wins and
is never overridden.

## 6. States

| State | What the user sees |
|---|---|
| Unsupported device language | Step 1 appears |
| Supported language | Step 1 is skipped; Step 2 is the first |
| Stylus seen or not | The line under the Toggle changes as described; the Toggle follows until touched |
| Skip on any step | Defaults kept: right hand, pen-only on if a stylus has touched the screen, else off; the empty library |
| Interrupted | Answers given are kept; next launch resumes at the unanswered step |
| Loading | Local; Skeleton tiles after 300 ms if the library index is slow |
| Error, storage unreadable | The EmptyState is replaced by a Banner region danger "Grapho could not open its storage. Try again." with Retry |
| Offline | Not applicable; first run needs no network. "Sign in to restore" shows only where sync is available |
| Very large | Not applicable; nothing to list |
| Long German text | Cards and the Toggle label wrap; Continue and Skip stay on one line |
| 200% text | The Sheet is full height; cards stack; body scrolls; the foot keeps Continue and Skip; the Toggle label wraps and the line under it stays |
| Held link or shared file | Applied after the Sheet closes, never during |
| Left-handed | After the Left card is chosen the Sheet itself does not mirror; the dock, hint, panel, scrubber and Toasts do |

## 7. Behaviour on resize and split screen

The Sheet re-lays out live between centred and bottom (crossing 600) and keeps the current step and the
answers. The empty library behind it follows the library classes. Rotating the tablet mid-step keeps the
step. In a half-width split screen the first run is the Compact layout.

## 8. Keyboard and focus order

Focus lands on the first control of the step (the Select, or the Right ChoiceCard). Tab goes through the
cards (a radio group, arrows move, Space selects), the Toggle, then Skip, then Continue. Enter is
Continue unless focus is in a multi-line field; Escape is Skip. Step changes announce "Step 2 of 2". When
the Sheet closes focus goes to "New notebook" in the empty library. A screen reader reads the cards as
"Right, selected" and the Toggle with its state. Back on step 2 returns to step 1 if it was shown; on the
empty library, system Back leaves the app.
