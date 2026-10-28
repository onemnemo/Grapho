# Flows

Surfaces are the names in SYSTEM-MAP section 3. Each flow lists the trigger, steps, states and exits.
Copy is draft voice, short and plain. Stacking rule: one Sheet at a time; further steps are pushed
views inside it with Back, at most three deep. A Dialog is only for a short irreversible decision.
Consent and review are Sheets or views inside one, never Dialogs. The route for every picker host is
the table in `surfaces.md`, section 11.

Shared exits for every Sheet: Cancel, scrim tap (only if nothing changed), Escape. Nothing is
installed, shared or removed until the final primary button.

## 1. Share your own content as a pack (Export as pack)

Rule: exporting never moves anything. Items stay Yours, in their one source.

1. **Settings page, Content, a type** (manage mode) or the **side panel picker in manage mode**. The
   source filter is on Yours. Tap Select (1), Select all (2) or tap items. The Selection bar shows
   Export as pack among its first four actions for packable types (3).
2. The **New pack Sheet** opens with one scrolling view: a summary "14 stickers" (tap to edit the
   selection by type tabs), the **Name** field, "More details" (collapsed), and the primary
   **Share**. Typing the name (4) and Share (5) hands the file to the system share sheet; the
   user picks the app or contact (6). Save to files is the secondary button.
3. First share only: a "Name on shared packs" field appears inline, with "Stay anonymous"; it is
   remembered. More details: description, licence (default "Free to share, credit me"), preview
   (auto, replaceable), and for a pack the user already made, the version. Id is under Advanced,
   read only.
4. Checks as the Sheet opens: items from packs whose licence forbids sharing, or `noExtract`, are
   unchecked with the reason; items made in the cover or template editors carry `components`
   (their recorded parts) which are copied in as resources with their own licence and origin, and a
   part that cannot be shared blocks that item with the reason. Items with licence `unknown`
   (kept from a received note) are blocked. No other detection is attempted.
5. The pack is signed with the device key (made on first use). A record is added under **Made by
   you** in Settings, Packs: id, name, version, item hashes. It is not a source and has no switch.
6. After Share: Toast "Botanica.graphopack ready" with Save to files.
7. Exits: Cancel; no space or cannot write: inline "Could not save. Not enough space." with Try again.

Tap count to share ten stickers you own: **6** (Select, Select all, Export as pack, Name, Share,
chosen app). Re-sharing a made pack: Packs, its row, Share (3 taps).

States: empty selection (primary disabled), 500 items (summary and tabs, virtual list), name already
used by another pack of yours (hint "Botanica 2" suggested), licence "Other" (a text field appears
under More details).

## 2. New pack (mixed content)

1. Settings, Packs, New pack; also Library New menu, Pack.
2. **New pack Sheet**, tabs by type (from the registry) with checkable tiles of Yours. An empty tab
   offers the type's import action ("Add images" for stickers, which creates stickers in Yours and
   ticks them) or its editor (which closes the Sheet and keeps the draft for the session).
3. Name, then **Share** or **Save to files**, as in flow 1. The record goes under Made by you.
4. Cancel with items ticked simply keeps the draft until the app closes; no confirm Dialog.

## 3. Save a new version and Share update

1. Packs, Made by you, row overflow: Save a new version, Save a copy, Share.
2. **Save a new version** re-snapshots the same item hashes from Yours (changed items are the ones
   whose hash differs), keeps the id, suggests the stored version plus one patch, and shows "What
   changes": added, changed, removed items. Primary **Share update**.
3. **Save a copy** mints a new id; it is a new pack signed by your key.
4. A made pack's items remain Yours; deleting the record deletes only the record.

## 4. Share an installed pack

Pack detail has **Share** (system share sheet) and Export (save). A pack with `redistribute: false`
shows both disabled with "This pack cannot be shared". A pack the user did not make is shared as the
original archive, unchanged (same signature).

## 5. Import from a file

1. Entries: picker footer **Import**, Packs page **Import**, Library New menu, Pack, the share target,
   drag in. The chooser filters to `.graphopack` and `.graphoext` and accepts by content.
2. The **import review Sheet** opens in Reading (skeleton, progress, Cancel). The core verifies the
   archive and signature (`manifests.md`, `trust.md`).
3. The review shows: header (preview, name, author, version, licence, size, publisher: "New
   publisher" or "Same publisher as your Botanica"); the status line (lifecycle, Conflicts); source
   ("From a file" or "From the catalogue"); one line "Adds 12 stickers and 1 theme. Nothing changes
   until you use it. Themes never apply on their own."; type tabs with counts and a virtual grid;
   "3 items you already have" for hash-matched items; "Not supported in this version"; theme results
   from the guard; for extensions: what it adds and "Asks for 3 permissions".
4. Primary: Install, for a pack and for an extension (the extension review is the consent view, flow 14).
5. Installing: the Sheet closes and a Progress item runs; then Toast "Botanica installed" with
   **View**. If the import began in a one type picker, the action is "Show in stickers" and opens that
   picker with the new pack's source selected. Newly installed packs come first in the source line
   until one of their items is used.
6. Items whose hash matches an item you already have are **linked**: the picker shows them once under
   the existing item's source; the new pack does not duplicate them. Removing the pack never removes a
   linked item.

Refusals (inline, primary becomes Close; the last line depends on the source):
"This file is not a Grapho pack." "This pack is damaged." "This pack has no signature, so Grapho
cannot tell who made it." "This pack is too large (300 MB, limit 250)." "This pack has something
that is not allowed in a pack: a script." "Needs Grapho 1.5 or newer. You have 1.4." "Grapho stopped
this pack because it was reported as unsafe." "Not enough space." Next step: from a chat or share
"Ask them to send it again"; from the catalogue "Try again later"; from a file "Pick another file".
Extensions on a shell with no runtime: "Extensions are not available on this device."

## 6. Share intent and drag in

- A file shared from a chat app: the app comes forward, the review Sheet opens over what was open. If
  the pen is down, the Sheet is queued and a Toast "Pack ready to review" with Review appears after
  pen-up.
- A friend without Grapho gets only the file; the store link "Get Grapho" at grapho.one is on the
  share sheet text.
- Drag in: a "Drop to install" zone covers the window; a pack file is never placed as an image. Several
  files: one review per file, "1 of 3", Install all, Skip this one; the queue holds at most 5 and the
  activity list shows "5 more waiting" beyond that.

## 7. Browse and Get more

Get more opens the catalogue as a Sheet from a page, or a pushed view inside a Sheet or panel from a
picker (route table). Catalogue list (view 1), pack detail with the review inside it (view 2), the
consent view for an extension (view 3).

1. Header: "Get more stickers", search, tiles with preview, name, author, size, Get or Installed.
2. **Offline**: Banner "You are offline. Showing packs you have already seen." Cached tiles stay;
   Get is disabled for uncached files with "Needs a connection"; Import from file stays.
3. **No catalogue** (open product question): empty state "No pack catalogue yet." with **Import from
   file** and **Open grapho.one** (a link set in configuration, not hard coded). Same empty state with
   Try again on error.
4. Loading skeleton; empty result "No sticker packs yet. Import one from a file."
5. Exits: Back, close, install done (Toast).

## 8. Turn on and off

The row switch in Settings, Packs or Extensions, or in the detail. It acts at once. There is **no
toast**: the switch is its own undo. A picker open beside the change keeps its scroll position and
does not animate the change in (principle 1). Turning an extension off adds one line in its detail
when it has blocks in notes ("Its blocks in notes show their saved picture."). An Off pack stays
listed in Get more as "Installed, turn on". An item in a holder of an Off pack gets the quiet line
"In use" in the pack detail.

## 9. Update

Base case, file only: a newer file arrives (flow 5, status Update) and installs; the dot is shown
only when a catalogue exists. A creator offers updates with Share update (flow 3).

1. Review shows "New version of Botanica" and "What changes": added, changed (with before and after
   thumbnails), removed (with usage: "You use it in the wheel and 2 notes. It stays available in
   Yours."), and for items in holders "3 items in your wheel changed": Keep mine or Use new, defaults per
   lifecycle. A different publisher is not an update (flow 12).
2. Progress item; atomic job; old version works until the swap. The old version is kept 30 days with
   "Revert to 1.2.0" in the detail.
3. Toast "Botanica updated to 1.3.0" with View changes. Failure: nothing changed, Banner on the row.
4. **Use the newer versions** in a note: Sheet listing changed items (before, after, ticked), primary
   **Use newer versions**, one undo step; removed items are not listed.
5. "Skip this version" removes the dot.

## 10. Remove, Undo, Restore

1. Trigger: row overflow, or a Remove row at the bottom of the pack detail (not under Advanced).
2. If nothing uses the pack (no holder, no note): remove at once; Toast "Botanica removed" with Undo.
   If something does: the **Remove sheet** (surfaces.md, section 6): usage lines, "42 unused items",
   Trash line, primary **Remove**. Toast "Botanica removed. 3 items kept in Yours." with Undo.
3. The job is atomic: kept copies are written and holders retargeted, then the pack is marked
   Trashed. Items that are Yours are untouched; linked items stay where they were first.
4. Undo (toast, Undo command, activity list): the pack returns in its old state. Kept copies stay in
   Yours.
5. **Trash** is the one name: the Trash place has a Packs and Extensions filter; rows show name,
   author, days left, Restore, Delete now. Delete now: the case 3 Dialog "Delete Botanica forever?
   Notes that use it keep working." with "Delete forever". Packs does not repeat a separate "Recently
   removed" group; it shows a link "Trash (2)".
6. Extension Delete now: "Delete Flashcards and the 12 decks it saved? Notes keep their saved
   pictures. This cannot be undone."
7. Importing a pack that is in Trash: the review says "You removed this on 24 Oct" with Restore.

## 11. Missing pack asset in a received document

1. A document whose items are all embedded opens with nothing to see.
2. If it uses origins from packs this device lacks, a **line** appears under the note bar, not in the
   page and not moving the canvas, only after the pen lifts: "Uses 6 stickers from Botanica by Ana
   Ruiz." with **Keep in my library** (items become Yours with licence `unknown` and label "Kept from
   Botanica"), **Get the pack** (catalogue or the unavailable state), and dismiss. Dismissal is stored
   per note and pack on this device. Items marked `noExtract` do not offer Keep.
3. A damaged, over cap or blocked item draws the type's placeholder; the line says "1 sticker cannot
   be shown." The note opens either way.
4. Never a blank gap, a blocked note or an error Dialog.

## 12. Different publisher with the same id

The review shows both publishers' names and fingerprints and: "This is not an update. It is from a
different publisher than your Botanica." Primary **Install as a separate pack**. It gets its own
record, switch and Trash entry and none of the first one's grants or holders.

## 13. Missing extension

1. A block whose extension is missing, off, in Trash, blocked, or on a shell without a runtime shows
   its stored fallback in a placeholder frame headed "Made with 'Flashcards'" (the stored name in
   quotes plus the short id), caption "Picture saved in the note", and the stored text.
2. Tap: Menu with Get extension (catalogue at the id, name and id visible), Keep as image (one undo
   step), Turn on when installed but off. On a device with no runtime only Keep as image, and the
   status reads "Not available on this device".
3. Move, resize, copy and delete work; editing does not.
4. When the extension arrives the block is live again with no file change. Newer data: fallback with
   "Needs a newer version of Flashcards" and Update.

## 14. Extension install with permissions

1. Review Sheet (flow 5) plus Source (From Grapho only if the Grapho key signed it), author, what it
   adds ("A side panel, 2 commands, a block"), and "Asks for 3 permissions".
2. The review Sheet is the **consent view**: publisher and signature, what it adds, permissions by
   tier with "needed by" labels, and the generic sentence (`trust.md`). Foot: Cancel, primary
   **Install**, secondary **Install turned off** (grants nothing; state Needs permission).
3. Result: Toast "Flashcards installed" with View. The extension's first panel shows a header "Flashcards,
   installed by you" for sideloaded ones.
4. Turn on later with an ungranted permission opens the consent Sheet.
5. Incompatible, unknown permission, an install whose declared permissions miss one implied by a
   contribution: refusal with the reason.

## 15. Permission increase on update

1. The update is staged; the old version keeps running. The review compares (id, argument) tuples,
   including implied ones, with what is granted now. New ones are marked "New".
2. Consent Sheet, with only the new permissions marked New: Cancel, primary **Update**, secondary **Update later**. Update later leaves "Update
   waiting for your OK" on the row; the extension stays On and its blocks stay live. Cancel discards the
   update.
3. A decrease is silent. A revoked permission stays revoked.

## 16. Strikes, start-up safe mode

1. **Strikes**: at the third abort or crash in a session an extension is turned off. Banner in
   Settings, Extensions and the Library: "Flashcards was turned off after problems." Turn on again,
   Details.
2. **Start-up safe mode**: two consecutive failed starts (crash or kill; opening the last note counts
   as part of the start; a note open that crashes counts), or Settings, Extensions, Start in safe mode,
   or the platform launch option in `custom-themes.md` ("The way back").
3. In safe mode: default theme, packs and extensions off; Banner "Safe mode. Themes, packs and extensions
   are off." with **Turn them back on** (a one by one list of extensions; a start failure turns
   that one off again) and **Restart normally**. Dismiss is allowed any time; for an extension the user
   leaves Off, the choice is stored as Off.
4. Safe mode never edits a document or removes data.

## 17. Theme pack that fails or adjusts

Defined in `design/themes/custom-themes.md`; this folder adds only the pack parts.

1. At review each theme tile shows the guard's outcome: accepted (plain), adjusted ("Adjusted so text
   stays readable", with What changed), or rejected ("Theme unavailable"). A rejected theme is not
   installed; the rest of the pack works. If all themes of a pack fail and it has nothing else, the
   pack is Incompatible.
2. Applying a pack theme uses that file's Keep or Revert banner (10 seconds).
3. A re-check that changes the result applies silently as that file says; an active theme that now fails
   is replaced by the default.
4. The theme command "Reset theme" and Settings, Appearance are always reachable.

## 18. Sync between devices

Per `sync.md`: the pack record appears on the other device ("Waiting to download" until online), on and
off follow the last writer, Remove on one device turns the pack off elsewhere with "Removed on Mia's
tablet" (Remove or Keep), extensions arrive Off in Needs permission. Documents never wait.

## 19. Interaction with the Library between flows

- Opening a note never prompts about packs except the line in flow 11.
- Turning off or removing never closes or reloads an open note.
- Progress items stay in the activity list for 24 hours.
