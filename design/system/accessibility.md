# Accessibility: keyboard, focus, Back, screen readers, scaling, motion, targets

Detail spec for principle 6 (every input is first class), covering stacking, tooltips and text size. Shortcut chords are in `registries.md`; layouts in `document.md`. Every
action in Grapho has a keyboard path and an accessible name because it is a command in the registry.

## 1. Keyboard map per place

Chords below are Ctrl on Windows and Android, Cmd on macOS and iPad. Only structure keys are
listed here; command chords are in the registry table.

| Place | Keys |
|---|---|
| Everywhere | Tab and Shift+Tab move focus; Ctrl+K palette; Ctrl+, Settings; Ctrl+/ shortcuts list; F6 and Shift+F6 cycle regions |
| Library grid | Arrows move between items (2D); Home and End first and last; Page Up and Down by a screenful; Enter opens; Space toggles selection and enters selection mode; Shift+Arrows extend a range; Ctrl+A select all; Escape leaves selection; Menu key or Shift+F10 opens the item Menu; typing a letter in Name sort jumps to the item |
| Library sidebar and drawer | Up and Down move; Right expands, Left collapses or moves to parent; Enter opens the place; F2 renames a folder; Delete moves it to Trash; the drawer traps focus while open and Escape closes it |
| Top bar | Left and Right between actions; Enter activates; Ctrl+F focuses search |
| Document page | Arrows scroll when the page has focus; Page Up and Down page; Ctrl+Home and End first and last page; Ctrl+Alt+G Go to page; `T` wheel; letters for tools |
| Note bar | Left and Right between items (roving tabindex style: one tab stop for the bar); Enter activates; Escape returns focus to the page |
| Tool dock | Up and Down (vertical) or Left and Right (compact strip) between tool, slots, Undo, Redo; Enter activates; 1 to 5 select slots anywhere in the document |
| Wheel (keyboard mode) | Arrows between segments and rings; Enter picks; Space picks and closes; Home and End; Escape closes |
| Side panel | Ctrl+Shift+1 to 4 (Cmd+Option+1 to 4 on Apple) open tabs; the tab rail is one tab stop with Left and Right (or Up and Down on a vertical rail); Tab enters the tab content; Escape returns to the page |
| Pages tab | Arrows move between thumbnails; Enter goes to the page; Space toggles selection; Alt+Up and Alt+Down move the selected page; Delete deletes (undoable) |
| Picker (any host) | Tab enters the Filter field; Down moves to items; Arrows in the grid; Enter applies (Pick) or opens details (Manage); Space toggles selection; footer slots are Tab stops in order Get more, Import, New, Manage |
| Menu | Up and Down; Right opens a submenu, Left closes it; Enter runs; type-ahead by first letter; Escape closes and returns focus to the trigger |
| Popover | focus moves into it; Tab stays inside until closed; Escape closes |
| Sheet and Dialog | focus is trapped; first focus goes to the first field or, for a Dialog, to the safe button (Cancel); Enter on the primary action only when focus is not in a multi line field; Escape is Cancel |
| Command palette | focus in the field; Up and Down move; Enter runs; Escape closes; Tab is not used for results |
| Settings | the section list and the page are two regions (F6); Up and Down in the section list; Tab through controls; Ctrl+F filters the settings index |
| Studio | the canvas preview, the controls panel and the Done and Cancel bar are regions; Ctrl+Enter is Done; Escape asks if anything changed, else cancels |
| Toast | not in the Tab order. F6 reaches the toast region; Enter runs its action; Escape dismisses |
| Find bar | Enter or Ctrl+G next, Shift+Enter or Ctrl+Shift+G previous (while the find bar has focus), Escape closes and puts the caret at the current match |

## 2. Focus order

1. Focus order follows reading and layout order for the current locale: top to bottom, start to
   end. In right-to-left locales the order mirrors, except the Tool dock, which keeps its
   handedness position and a fixed order (tool, slots, Undo, Redo).
2. The regions of the document, in order: note bar, page, Tool dock, side panel, toasts. F6 cycles
   them. The dock comes after the page in order even though it sits at an edge, because the page
   is the primary content.
3. When a layer opens, focus moves into it. When it closes, focus returns to the control that
   opened it. If that control no longer exists (for example a deleted item), focus goes to the next
   item in the list, else to the list, else to the place's heading.
4. When a place changes, focus goes to its heading (the top bar title) so a screen reader announces
   the place. Returning to a place restores the scroll position, the selection and the focused item.
5. A focus ring is always visible when the last input was a keyboard, a switch or a screen reader.
   It is at least 2 dp wide, 3:1 against both the control and the background, and is never clipped.
   After pointer input the ring is hidden except on text fields.
6. Disabled controls stay in the focus order when they hold information that disabling hides
   (the reason is announced). Controls that are hidden are not focusable.
7. Chrome that recedes does not steal focus when it returns, and a receded note bar is reachable
   with F6 (it returns when focus enters it).
8. Focus never moves on its own while the pen is writing.
9. Moving items by drag always has an equivalent (Move to, Move up and down); focus stays on the
   moved item.

## 3. Back and Escape order

Back (Android system back, predictive back, the Back control) and Escape close the topmost thing,
in this order:

| Step | What closes |
|---|---|
| 1 | a popover or menu |
| 2 | a dialog |
| 3 | a pushed view inside a sheet (back to the sheet's previous view) |
| 4 | a sheet (if it has changes, the Dialog for discard comes first, per its rules) |
| 5 | the side panel, on compact only (on medium and expanded, the panel stays and the next step runs) |
| 6 | selection mode |
| 7 | the place: leave the document to where it came from, or go up one Library place, or leave Settings |

Additional rules:

- The wheel and the Find bar count as popovers. The command palette is a popover.
- A toast is never reached by Back. On a platform where Escape reaches a focused toast, it
  dismisses that toast.
- Studio: Done saves and returns; Back and Cancel discard, asking once if anything changed. Studio
  is a place, so step 7 applies to it, with the discard ask first.
- "More in Settings" from inside a Sheet asks keep or discard, closes the Sheet, then navigates;
  Back from the opened Settings page returns to the place under the Sheet.
- At the root of the Library Home, Back on Android leaves the app (system behaviour). Escape does
  nothing there.
- Predictive back shows the preview of the surface that would close, using the same order.

## 4. Screen reader roles and labels per surface type

Names come from the same string keys as the visible label. Icon-only controls get the command's
name and shortcut as the accessible name and description. Roles are given as platform neutral
terms; each shell maps them to TalkBack, Narrator or NVDA, and VoiceOver.

| Surface | Role | Label and announcements |
|---|---|---|
| Top bar | toolbar, with a heading for the place name | "Library, toolbar". Actions are buttons. Search is a search field |
| Sidebar | navigation landmark, tree | "Library places". Folders are tree items with level, expanded state and count: "Maths, folder, level 2, collapsed, 12 items" |
| Note bar | toolbar | "Note bar". Title is a button "Title, Maths notes, rename". Page indicator is a button "Page 12 of 300, go to page". Receding is not announced |
| Tool dock | toolbar | "Tool dock". Current tool is a button with state "Pen, black, 2 point, double tap to open tools". Slots are buttons "Quick slot 2, highlighter, yellow". Undo and Redo have disabled state |
| Tool wheel | menu (summoned) | "Tools menu" with groups Tools, Pens, Colours. Items are menu item radios with selected state. Opening announces the group and the current tool |
| Side panel | complementary region with a tab list | "Side panel, Pages tab". Tabs are tabs with selected state and position "2 of 5". Content has its own roles |
| Quick slots | within the dock toolbar | as above; a pinned slot adds "pinned" |
| Popover | dialog (non-modal) | labelled by its title, or by the trigger name. Announced on open |
| Menu | menu, menu items, groups with headings | items with shortcut as description; submenus announce "submenu"; checked items as menu item checkbox or radio |
| Sheet | dialog (modal) | labelled by its title; the content behind is inert and hidden from the reader |
| Dialog | alertdialog | labelled by its title, described by its message; the message is read before the buttons |
| Toast | status live region (polite); a toast with Undo is not interruptive | "Moved 3 items to Maths. Undo, button". An error toast uses an assertive alert |
| Progress item | progressbar with value, live status | "Importing Maths.pdf, 40 percent, Cancel, button". Indeterminate says "in progress". Updates are announced at every 25 percent, not continuously. Completion is a status |
| Banner | region with role status, or alert for errors | labelled by its first sentence; its buttons are in order |
| Tooltip | tooltip, as description of its control | not a separate stop; see the touch rule in section 7 |
| Command palette | combobox with listbox | "Command palette. Type a command". Results announce count: "5 results". Each result: name, group, shortcut, disabled reason |
| Picker | region with a Filter field and a list or grid | "Stickers, 24 items". Items are options or buttons with name and state; source filter is a list of toggle chips announced with count; footer buttons in order |
| Studio | main region with a canvas preview image and a controls region | the preview has a text description updated when controls change; controls are standard form controls; Done and Cancel are the last two buttons |
| Selection bar | toolbar | "3 selected" is a live status; actions are buttons; More is a menu button |
| Settings page | main region with headings and a list of controls | each setting is a labelled control with its help text as description; a setting that depends on another announces why it is disabled |
| Library grid item | list item or grid cell | "Maths notes, notebook, 42 pages, starred, modified yesterday". Selected state is announced. Custom actions: Open, Select, Star, Move to, Delete |
| Page in Pages tab | list item | "Page 12, selected". Custom actions: Move up, Move down, Duplicate, Delete |
| Canvas page (the ink) | image with a text description; ink is not a text area | "Page 12 of 300". Recognised text, when the index has it, is exposed as the description. Objects are reachable through the selection model; Tab inside the page cycles through objects in reading order, each with its kind and bounds in words |
| Extension block | image or group | label is the extension's alt text, or the fallback text; the placeholder names the missing extension |

Rules:

- Every image has alt text or is marked decorative. An extension tree with an image without alt text
  is rejected at install.
- Live regions: at most one polite announcement per second. Writing never triggers announcements.
- Counts and states are part of the name, not only visual: selected, expanded, starred, pinned,
  disabled with reason, "N of M".
- Custom actions on list items mirror the Menu, so a screen reader user gets the same commands as
  a long press user.

## 5. Text scaling

- Text size follows the operating system. Grapho has no separate text size. Sizes are scaled
  with the system font scale (sp on Android, Dynamic Type on Apple, the Windows text scale and
  the display scale).
- The design holds at **200 percent** text and in a half-width split screen (compact, 360 dp wide).
  At 200 percent:
  - Single line labels wrap or truncate with a full label tooltip and accessible name; they do not
    overlap or clip.
  - Rows and buttons grow in height; fixed heights are minimums.
  - Icon-only controls do not scale their icon beyond 1.5 times, but their targets stay at least 44 dp.
  - Segmented controls become stacked lists when their labels do not fit on one line.
  - Menus and the palette scroll; Sheets scroll their content with fixed header and footer.
  - The note bar drops pinned items to More earlier, as at compact. The dock keeps tool, Undo and
    Redo and reduces visible slots.
  - The picker grid reduces columns, never item preview size below its type's minimum.
- A German label (about 40 percent longer than English) must fit every control at 100 percent
  without truncating its meaning.
- Ink, page content and PDF text do not scale with the system text size; they zoom with the page.
- Above 200 percent the layout still scrolls and nothing becomes unreachable, but is not a
  supported design target.

## 6. Reduced motion

Honour the OS setting (`appearance.reduceMotion` defaults to follow system and can be forced on
or off).

| Motion | Normal | Reduced |
|---|---|---|
| Note bar recede and return | 150 ms and 200 ms slide with fade | instant opacity change, 0 ms |
| Sheet, panel, drawer, popover | slide or scale 200 ms | cross fade 100 ms or none for popovers |
| Wheel open | scale from the pen tip 120 ms | instant |
| Toast in and out | slide 200 ms | fade 100 ms |
| Skeleton shimmer | moving shimmer | static tint |
| Progress | bar animates; indeterminate sweeps | determinate bar steps without easing; indeterminate shows a static "working" bar with a text label |
| Mascot | may animate | still image |
| Page turn, zoom to fit | animated | immediate |

Essential motion is not removed: a drag follows the finger and ink follows the pen. Nothing flashes
more than three times in a second. Nothing animates while the pen touches the page, at any setting.

## 7. Target sizes and the tooltip rule

### Targets

- Touch and stylus targets are at least `size/hit/touch` (48 dp on Android, 44 on Apple and
  Windows), including icon-only controls in the note bar, the dock, the Pages tab, the scrubber
  and the panel handle.
- Hit areas are reserved in layout: spacing between adjacent controls guarantees the touch target
  without overlapping hit areas.
- With a fine pointer at compact density, controls may be drawn at the compact height (28) with a
  hit area of at least 24 by 24 (WCAG 2.2, 2.5.8). The touch minimum applies whenever a touch or pen
  was last used.
- Wheel segments are at least `size/hit/touch` across at their narrowest point; a ring with too many segments
  pages (dots) instead of shrinking them.
- The edge guard of 16 dp must not take space out of a target: edge targets are inset by 16 dp on
  the writing-hand side.

### Tooltips on touch and stylus

- A tooltip names an icon-only control and shows its shortcut when it has one.
- **Touch**: a long press on an icon-only control shows its tooltip without activating the control;
  the control activates only on a short tap. The tooltip stays while the finger is down and for 1.5 s
  after release.
- **S Pen hover** shows the tooltip after 500 ms, as a mouse does; touching activates.
- **Mouse and keyboard**: after 500 ms of hover and on focus from the keyboard (not on focus from a
  click), hidden on Escape or when the pointer or focus leaves.
- A tooltip is never the only place a name exists. The accessible name is always set.
- Tooltips do not show over the page while the pen is writing.

## 8. Colour-independent state cues

Colour never carries state alone. Each state has a shape, text, position or pattern cue as well.

| State | Cue besides colour |
|---|---|
| Selected (library item, page, option) | tinted fill plus a check mark in the corner |
| Current tool or colour | a ring plus a small notch or underline on the control; the wheel segment is enlarged |
| Toggle on | a pressed (inset) form and a different icon shape (filled versus outlined), plus the state in the accessible name |
| Disabled | reduced contrast plus, in menus, the reason text; never only grey |
| Focus | 2 dp ring with 3:1 contrast, an offset gap |
| Error | an icon (triangle with mark) and a text sentence; red is a supplement |
| Warning, offline, conflict | a labelled Banner with an icon and text |
| Sync state | a distinct icon for each of off, up to date, syncing, offline, conflict, storage full, plus text on the sync page |
| Starred | a filled star shape versus an outlined one |
| Pinned slot or item | a pin glyph |
| Undo and Redo unavailable | disabled form plus no response, and "nothing to undo" as the reason on focus |
| Hidden items in a source filter | the chip "Hidden" and, in the list, an eye-off icon and the word |
| Colour swatches in the wheel and editor | the colour name is shown (in the picker, the accessible name and a text readout in the editor); the selected one has a ring and check |
| Progress | a bar plus a number or "working" text |
| Drop target | highlight plus a drop line or an outline |
| Receded note bar | its return is by a tap at the top edge; the fact is not a colour state |

Contrast: text 4.5:1, large text and meaningful icons 3:1, against every theme including the
user's themes. A theme that fails is adjusted and a Banner says so ("this theme was adjusted for
contrast"), as the map's Banner rule describes. Ink colours are content and are not subject to the
chrome contrast rule, but the colour editor warns when a colour is very close to the page.

## Open questions

1. Tooltips show on a long press on touch. A long press on a library item already starts a drag or
   selection, so this spec limits the long press tooltip to icon-only controls, which are not
   draggable. Open question: confirm that no icon-only control is draggable.
2. A receded note bar is reachable with F6 and returns on focus. It returns on a top-edge tap or 1.5 s
   without pen contact. Open question: should focus entering it also count as a return?
3. Predictive back previews follow the same order as Back. Open question: should step 5 (side panel on
   compact) be skipped when the panel is at full height? This spec closes it in both cases.
