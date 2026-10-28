# ShortcutField

## 1. Purpose, and when not to use

**Purpose.** Record a key chord for a command in Settings, General, Keyboard, and name clashes. A FieldShell row ([field-shell.md](field-shell.md)), per command and per chord.

**When not to use.** Typing text: TextField. Choosing a command: Select or the palette. Pen button or gesture bindings: not planned here; they would need a separate binding field.

## 2. Anatomy

```
 Previous page
 +-------------------------+  [close]  [reset]
 | Ctrl + Shift + [        |
 +-------------------------+
 Press the keys. Escape cancels.
```

Three siblings, none nested: the `chord` button (a box of key caps), `Clear` (IconButton, `close`) and `Reset` (IconButton, `reset`). Then the message row. A command with two default chords shows two rows; "Add another chord" adds a second, up to two.

## 3. Variants and sizes

- `size`: `small` | `default` (small resolves to default at touch density).
- Clear sets the chord to **None**. Reset restores the **Default** from the registry. The two words are shown: the helper under a changed chord reads "Default is Ctrl+K."

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Rest | Key caps on `surface/strong` with `border/control` edge; "None" in `text/muted` when empty |
| Listening | `border-width/emphasis` edge in `border/focus` plus the ring, and the placeholder reads "Press keys" in the caps area; the message row says "Press the keys. Escape cancels." Three cues that are not colour: thicker edge, words, caps placeholder |
| Hover | `surface/control-hover` on the chord button |
| Error (clash) | `alert_circle` and "Ctrl+K is taken by Command palette. Keep it there, or give it to this command." with a Replace text Button; Replacing unbinds the other command and shows a Toast with Undo |
| Error (reserved) | "Alt+Tab is used by the system. Choose another." |
| Read-only | Not used; every command is remappable |
| Disabled | Commands from an extension the user turned off |

## 5. Content rules

- **Clash is per platform and per context**, using the registry (registries.md 1.3): the same chord may serve two commands in different contexts (Ctrl+Z in the library and in a document), and then it is not a clash. A clash is only within one context.
- **Reserved chords** are the registry's rule 6 list only (Alt+F4, Win chords, Cmd+Q, Cmd+H, Cmd+Tab, Alt+Tab, Android system keys). Nothing else is "fixed".
- A chord is modifiers plus one key; single keys (letters, digits, Page Up and Down, Delete, Enter, Backspace) are allowed where the registry allows them for that context.
- **Extension commands** never take a built-in chord: a clash leaves the command unbound and says so on the extension's page. Replace never moves a chord toward an extension command on its own; it only runs when the user is editing that very command and chooses it.
- **Layout safe:** the stored chord is the physical key plus modifiers; it is displayed with the active layout. A chord that needs AltGr is rejected: "AltGr gives a character on this keyboard. Choose another." Registry defaults offer a layout-neutral alternative.
- Key caps never split. In right-to-left text the caps keep their order. Modifier names follow the registry (Ctrl and Cmd as words).
- No chord: "None". 500 commands: the list uses Filter above it.

## 6. Overflow rule

Two chords per command at most.

## 7. Extension slots

Extension commands appear in the same list under the extension's name (SYSTEM-MAP 7, Command). ShortcutField has no extension schema node.

## 8. Pack content

None.

## 9. Input

- Handwriting: **opted out** (FieldShell 9.4).
- Mouse and keyboard: click, or Enter or Space on the chord button, starts listening. While listening the field captures chords; **Escape cancels, Tab leaves (and cancels), Backspace and Delete clear to None**. These four keys are therefore bound only from the fallback below. The OS and screen readers consume some keys (Alt+Tab, Win, Cmd+Q, TalkBack, VoiceOver and Narrator commands); those never arrive, and the helper tells a screen reader user to use the fallback.
- **Fallback for touch, pen and assistive tech.** A Sheet from the row: modifier Checkboxes (Ctrl or Cmd, Shift, Alt or Option), a key Select with Filter, and Done. It also binds Escape, Tab, Backspace and Delete. One Sheet, no stacking.
- Announce: "Shortcut set to Ctrl+Shift+[." Clashes are announced as errors.
- ARIA: the chord is a `button` named "<command> shortcut, <chord>"; while listening it is `textbox` with `aria-live="polite"`. Clear and Reset are separate buttons with Tooltips "Clear" and "Reset to default". Native: key capture on a focusable in Compose, WinUI and SwiftUI.

## 10. Tokens

FieldShell tokens, plus `surface/strong` and `border/control` (key cap), `radius/key`, `type/label`, `border/focus`, `border-width/emphasis`, `size/keycap` (token to add), `size/hit/touch`.

## 11. Do and Don't

- Do name the other command and the context in a clash.
- Do keep Tab as an exit while listening.
- Don't nest Clear and Reset inside the chord button.
- Don't treat a chord in another context as a clash.

## 12. Traces to

Flow 12. New. Commands to register: none (Clear and Reset edit a value; they are not registry commands).
