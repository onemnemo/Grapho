# Trust: authenticity, consent, blocklist, containment

## 1. Authenticity

Every pack and extension is signed. The file's identity is the triple (kind, id, publisher key).

- **Algorithm**: Ed25519. The signature covers the hash of the canonical manifest, which lists every
  file's hash, so it covers the whole archive. Format in `manifests.md`.
- **Trust on first use.** The first install of (kind, id) pins the publisher key to the id. An
  update must carry the same key.
- **A different key is not an update.** A file with a known id and another key is shown as a separate
  item: status "Same name as one you have, from a different publisher. This is not an update." with
  both publisher fingerprints and authors. It can be installed beside the first as its own record;
  nothing of the first changes; it never inherits grants, holders or the Trash entry of the first.
  The identity key of every table (store, Trash, blocklist, sync) is (kind, id, publisher key).
- **Packs a user makes** are signed automatically with an Ed25519 key generated on first export on
  their device and held in the platform keystore (or the account's keystore when signed in, so it
  follows them to another device). The user never sees a key. Losing the key means later versions
  appear as a different publisher, which the review explains.
- **Unsigned files are refused** (`E_UNSIGNED`: "This file has no signature, so Grapho cannot tell
  who made it."), except a file this device exported itself and now re-imports, recognised by a
  local export ledger of file hashes.
- **Grapho's own packs and extensions** are signed by a key pinned inside the app (the trust list,
  replaced by app updates). Only that key may use the author "Grapho" and the source group
  "From Grapho". An item can never move from signed to unsigned.
- **What the user sees**: the detail shows the publisher fingerprint in groups of four; the review
  says "New publisher", or "Same publisher as your Botanica". Names are shown after normalisation;
  two names equal after confusable folding trigger the same "same name" display with authors.
- **Ids are not editable.** "Save a copy" mints a new id; "Save a new version" keeps the id and
  suggests the stored version plus one. A pack arriving from another publisher is never re-versioned
  by you: exporting items you took from it creates a new pack.

## 2. Consent

Consent is a Sheet, or a view pushed inside the review Sheet. It never appears as a Dialog.

### What it shows

All declared permissions plus those implied by the slots (`extension-api.md`, section 9), each with
"needed by" labels, grouped by tier, high risk first: Outside the app, Your library, This document.
`storage.own` is not shown.

### The generic sentence

The rule has no special cases. Let R be the set of the extension's read-like permissions
(`doc.read`, `selection.read`, `library.read`, `clipboard.read`, `files.pick`, `microphone`,
`camera`), and N its `network` permission. If R and N are both present, consent and the extension
row show one sentence that names every source in R and every host in N:

> It can read {sources} and send information to {hosts}.

Sources use these words: this document, what you select, your library, what you copy, the files you
choose, what the microphone hears, what the camera sees. Hosts: up to three named, then "and 2 more
sites". Example: "It can read this document and your library and send information to cards.example.com."

The sentence is also the extension row's risk line ("Can read your library and connect to 1 site").
It is a statement about capability. Hosts are the user's trust decision, not a claim of safety:
the consent copy says "Only allow sites you trust". The detail shows a log, "Sent to cards.example.com
today: 3.2 MB".

### States

- **Granted** is stored per permission. "Install turned off" grants nothing. Switching an extension
  On while any permission is ungranted opens the consent view first (state Needs permission).
- **Increase** is computed as the difference between the new manifest's (id, argument) tuples,
  including implied ones, and the set granted today, not the previous manifest. A changed host, a
  new host, or a new contribution that implies a permission is an increase. A permission the user
  revoked stays revoked after an update.
- **Staged update**: the old version keeps running until the user answers. The choices are
  **Allow and update**, **Update later** (the update waits; the old version stays On with its blocks
  live) and Cancel (the update is discarded). A decrease is silent and is noted in the changes list.
- **Revoking** a permission in the extension detail turns off the contributions that need it and
  lists them; revoking `doc.read` while a command that needs it runs ends the run.

## 3. Containing damage

- `library.write` cannot permanently delete anything. `trash(id)` moves to Trash. It cannot touch
  packs, extensions, permissions or another extension's storage.
- Before an extension's first write to a document in a session, the core takes a restore point;
  "Restore before Flashcards" is in the document menu until the document is closed.
- Clipboard read only inside a user started command. Camera and microphone show an in-app
  indicator that cannot be hidden, and stop at the end of the call.
- Spoofing: extension UI is tree controls in a framed panel with the name always shown; no password
  field kind; a drawing in a panel has a visible frame, no text ops. A block's frame header shows
  `extName`, stored in the block, in quotes with the short id while the extension is not installed;
  the placeholder never claims it is a Grapho message. "Get extension" opens the catalogue at the
  id with the name and id visible; a fallback image is drawn inside a fixed inset with a caption
  "Picture saved in the note".
- Remote code: the interpreter has no eval and loads only archive files, but a script may download
  data and act on it. The consent copy therefore says "It loads data from {hosts}". The enforceable
  controls are the API boundary, host list, quotas and the strike rule.
- Pack content is data: no code is accepted (see `manifests.md`).

## 4. Blocklist

- **Channel**: a signed list shipped with the app and refreshed whenever the catalogue or an update
  check is reachable. Offline, the last copy is used; a copy older than 30 days shows a quiet line in
  Settings, Extensions ("Safety list last updated 41 days ago").
- **Keys**: (kind, id, publisher key) and content hashes of items.
- **Effects**: the pack or extension becomes Blocked (Off, cannot be turned on, can be Removed).
  Kept copies, library holders and embedded document items whose hash is blocked draw the type's
  missing-asset placeholder. Nothing is deleted from a document. An active blocked theme falls back
  to Ink. A blocked extension's blocks show the fallback frame.

## 5. Crash handling

- Start-up safe mode: two consecutive failed starts, where a failure is a crash or kill (not a slow
  start), counted persistently. Opening the last note automatically counts as part of a start, and a
  document open that crashes counts as a failed start of its own.
- Before each call into an extension, the core writes a marker naming it; on a crash the marker
  attributes the failure. On the second failure, the next start is safe mode with the attributed
  extension flagged "Turned off after an error".
- Session strikes: `extension-api.md`, section 8.
- Safe mode follows `design/themes/custom-themes.md` ("The way back"): default theme, packs and
  extensions off, notes open normally, nothing deleted.
- A pack whose item renders crash the picker: tiles render in an isolated job with a time budget;
  a failing tile shows the placeholder and the pack row says "Some content could not be shown".
