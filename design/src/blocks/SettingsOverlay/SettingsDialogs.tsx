import { useRef } from 'react';
import type { PackItem } from '../PackRow';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { NewPackSheet } from '../NewPackSheet';
import { PackBrowseSheet } from '../PackBrowseSheet';
import { PackImportSheet } from '../PackImportSheet';
import { RemovePackDialog } from '../RemovePackDialog';
import type { SettingsState } from './useSettings';

/** The sheets and the question that Settings opens: New pack, import, browse and remove. */
export function SettingsDialogs({ settings: s, placement = 'viewport' }: { settings: SettingsState; placement?: OverlayPlacement }) {
  // keep the last pack so the dialog can leave gracefully
  const last = useRef<PackItem | null>(null);
  if (s.removing) last.current = s.removing;
  return (
    <>
      {last.current && <RemovePackDialog open={!!s.removing} pack={last.current} placement={placement} onConfirm={s.confirmRemove} onCancel={s.cancelRemove} />}
      {s.importOpen && <PackImportSheet open placement={placement} onClose={s.closeImport} onInstall={s.installImported} />}
      {s.browseOpen && <PackBrowseSheet open placement={placement} installed={s.fromCatalogue} initialKind={s.browseKind} onClose={s.closeBrowse} onInstall={s.installCatalogue} />}
      {s.newPack && (
        <NewPackSheet
          open placement={placement} items={s.visibleContent} initialSelected={s.newPack.ids} initialStep={s.newPack.ids.length ? 'details' : 'pick'}
          onClose={s.closeNewPack} onExport={(file) => s.exportedPack(file)}
        />
      )}
    </>
  );
}
