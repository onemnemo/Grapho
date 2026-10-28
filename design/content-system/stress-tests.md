# Stress tests

Each case is walked against the design. Result: Holds, Holds with a rule added, or Gap.

## 1. Chains of dependencies

**A cover that uses a sticker from another pack.** The cover editor records the parts it used in
`components`. When a user shares the cover, the exporter copies the sticker into that user's pack as a resource
with its own licence and origin. If that sticker's licence is `all-rights-reserved` or the item is
`noExtract`, that cover is blocked with the reason. Installing her pack without the other one works:
the resource is inside. Using the cover embeds the cover item and its resource. Removing the other
pack changes nothing. Parts of items not made in the editors are not detected (an imported flat image has
no record); the licence stays what the image's source says. **Holds** for editor-made content.

**A template that uses a font.** The font is a resource of the same pack, with the font's own embedding
flag checked from the file and a per item licence. A font that forbids embedding is refused at share
time and the template falls back to the default face, saying so. The template and font are embedded
with a note that used them; a text run using the font counts as a use. **Holds**. Open point: the
font licence text does not travel inside a note (Gap 3).

**A template that wants an extension block.** Forbidden inside templates; the manifest may say
`requiresExtensions` for information only. **Holds**.

**Nesting.** `uses` deeper than 4 or a cycle is refused at import and share (`E_NESTING`). **Holds**.

## 2. A theme pack with unreadable colours

The guard in `design/themes/custom-themes.md` decides, not this folder. Neon cannot be made readable:
rejected, shown "Theme unavailable"; Spring is accepted; Dusk is adjusted. The pack installs without
Neon. Applying a pack theme uses the Keep or Revert banner. A later rules change applies silently as
that file says. The way back (Appearance, `theme.reset`, safe mode) is in that file. **Holds**; the
content system adds nothing that could contradict it.

## 3. A 500 sticker pack, and 5000 items

Import review is a virtual grid with counts; hashing, thumbnails and indexing run in the background,
thumbnails lazily on first view. Install is an atomic job with progress and Cancel. Only used stickers
enter a note. Filter searches an incremental per pack index; under All it searches every installed
source's index. Targets for engineering to confirm on the reference tablet: install of 5000 items
under 60 seconds, memory under 150 MB. Duplicate pictures under different ids are refused
("These items are the same picture"). **Holds**.

## 4. A malicious extension

| Attack | Defence |
|---|---|
| Takes over an installed extension's id | Needs the publisher key; a different key is a separate item with a warning, never an update |
| Reads documents and uploads | Consent names every readable source and host in one sentence; hosts exact, https, no redirects off host, no IP literals; a data log; revocable |
| Hidden read through a slot | Slots imply permissions (matrix); an export format means `doc.read`; the install is refused if the manifest omits an implied permission |
| Little at install, more later | Update compares tuples with what is granted; staged; old version runs until consent |
| Fake Grapho UI or consent | Declarative tree only, framed with the name, no password field, drawings framed and text free; no overlay or Banner |
| Hijacks the block fallback | The fallback is made by the core from `render`, shown inside an inset captioned "Picture saved in the note", name from the block in quotes with the id |
| Corrupts or deletes content | Writes are one undo step and a restore point; no permanent delete; Trash only |
| Floods UI or palette | 100 declarations, slot caps, name prefix, reserved verbs |
| Locks the app or janks ink | No code on pen-down; budgets in ms; 3 strikes turn it off; crash marker attributes start-up failures |
| Pack as a carrier | No code, sanitised SVG subset, JSON without expressions, validators on every ingestion including received notes |
| Known bad | Signed blocklist by id and key and hash, usable offline, applies to kept copies and embedded items (placeholder) |
| Remote code | Not claimed: no eval, archive files only, hosts limited; data from hosts is disclosed in consent |

**Holds**, with the stated limit that a trusted host can be abused by its owner. Gap 5 (below) is the
deepest remaining risk.

## 5. A user who removes everything

Built in is a pack that cannot be removed. Removing others sends them to Trash after making kept copies
for holders; Yours is never touched. Pickers show Built in plus Yours. Hiding built-in stickers is allowed
(they have no default); the last visible item of a type that needs a default cannot be hidden, disabled
with "At least one theme must stay". Hidden lists everything hidden with Unhide. Emptying Trash asks for
confirmation in a Dialog "Delete 6 packs forever? Notes keep working." No note changes at any step. **Holds**.

## 6. Two machines, different packs, one note, both ways

Machine A has Botanica 1.0 and Moon phases; B has Botanica 1.2.

- **A to B**: the note embeds the 1.0 fern and the crescent as items with `def`. B renders them exactly.
  Line: "Uses 1 sticker from Moon phases by Author." with Keep (licence `unknown`) and Get. B's 1.2 fern
  has a different hash: nothing changes unless "Use the newer versions".
- **B to A**: B adds the 1.2 fern and a 1.2 only sticker. The note now holds two Botanica versions, by
  hash. A renders both without the sticker's pack. Line: "Uses 3 stickers from Botanica. 2 are newer than
  the pack you have." with Keep and Update pack.
- **Both edit offline, then sync**: `items.json` and `blobs/` merge by union of hashes; no key collides;
  nothing is deleted by a merge; compaction waits for a clean base. A block edited on both is kept twice.
- **An older app** refuses version 4 (accepted, with the one line before the first write); built-in only
  notes stay version 3.
- **An asset hostile in a received note** passes the same validators or draws the placeholder.
**Holds**.

## 7. An update removes an asset

Botanica 1.3 drops Eclipse and changes the leaf pen. Review: "1 removed: you use it in the wheel, a
saved look and 2 notes", and "1 item in your wheel changed": Keep mine (default for pens) or Use new.
Holders get a kept copy "Kept from Botanica" for Eclipse; the pen stays or moves by choice; notes keep the
embedded copies. An item not yet used anywhere that was dropped is recoverable through "Revert to 1.2.0"
(30 days). Rolling back with Replace with older version restores the item; the kept copy stays, labelled
by origin. **Holds**.

## 8. Two packs with the same name

Different ids: both install, shown with authors. Same id, different key: not an update; shown as a
separate item with both fingerprints. A pack with the same id in Trash: Restore. Names are normalised
(no bidi controls) and "Grapho" as author is reserved to the Grapho key. Kept copies are labelled by name,
adding the author only when two kept names clash. **Holds**.

## 9. Further cases

- **Share a pack you made, then edit an item, share again**: the item hash changes; "Save a new version"
  shows it as changed; "Save a copy" is a new pack; an id is never editable. **Holds**.
- **Remove a pack made from your own items**: the items are Yours and stay; only the record is deleted.
- **Import a pack that contains your own item**: the hash matches; the item is linked, shown once under
  Yours; removing that pack leaves it.
- **Crash during Remove**: the journal resumes or rolls back; a startup scan fixes dangling holders.

## Gaps

| # | Gap | Owner and fix |
|---|---|---|
| 1 | The usage index needs building at first launch; counts say "At least N" meanwhile | Engineering |
| 2 | Whether a catalogue exists and who moderates it; the signing key policy for "From Grapho" (the key pinned in the app, rotation by app update) | Product decision |
| 3 | A note carries only claimed licence name and author, not licence text (CC BY wants a link) | Product decision, with legal review: add short text to `claimed` |
| 4 | Reference engine and performance numbers (budgets, memory, install time) must be measured on the reference tablet | Engineering |
| 5 | A trusted host can be abused by its owner; consent can only disclose, not prevent | Accepted; hosts are the user's trust decision |
| 6 | The "noExtract" rule for all-rights-reserved assets | Product decision |
| 7 | App Store policy on interpreted extensions on Apple platforms | Product decision, with legal review |
