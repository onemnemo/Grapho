# Sync

Sync is in scope. Documents never wait for packs or extensions. SYSTEM-MAP 4.5 owns the Sync
setting, the offline queue and the conflict state "both versions kept"; this file says what travels.

## What syncs, per piece of state

| State | Scope | Rule |
|---|---|---|
| Documents, embedded items | account | Merge by union of content hashes (`documents.md`); never wait on packs |
| Installed packs and extensions (kind, id, publisher, version) | account | The record syncs at once; the archive downloads on the other device when online. Row state "Waiting to download" until then; its items are not in pickers yet |
| On or off | account | Last writer wins, by hybrid logical clock |
| Order (drag handle) | account | Last writer wins |
| Hidden items | account | Set keyed (packId, assetId) and by hash; union, with Unhide as a removal record |
| Remove | per device | Removing on device A puts the pack in A's Trash. Other devices turn it off and show "Removed on Mia's tablet" with Remove and Keep. Nothing is deleted elsewhere |
| Trash dates | device | Local |
| Library holders (wheel slots, quick slots, saved looks, defaults, active theme, folder styles) | account | Each holder is self-contained like a document item: it carries the item definition and blob hash. It works on a device that lacks the pack; the blob downloads like any asset |
| Kept copies | account | Ids derive from (packId, assetId, item hash), so two devices that both remove a pack make the same kept copy |
| Made packs (record) | account | The items sync as Yours |
| Permission grants | device | A grant is never inherited by sync. An extension that arrives on a new device is Off, in state Needs permission, until the user consents there |
| Extension storage | device | Local only; the extension's data inside documents (block `data`) syncs with the document |
| Safe mode, strikes, crash counters | device | Local |
| Blocklist | device cache | Fetched per device |

## Conflicts

- **On and off** from two devices: last writer wins. No prompt.
- **Remove on one device, update on another**: the update stays applied; the removed device keeps its
  Trash; the other device sees "Removed on ..." as above.
- **Same pack installed at two versions**: the higher version wins; the lower device downloads it.
- **Two kept copies of one item**: identical ids, so one record.
- **Item changed in a block on two devices**: both blocks kept (`documents.md`).

## Order of arrival

A note can arrive before the pack it uses. It renders from its embedded items. The pack, when it
arrives, becomes a source in pickers and the in-note line "Uses 6 stickers from Botanica" is resolved
by origin: if the pack is installed, the line is not shown.
