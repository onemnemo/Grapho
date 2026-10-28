import { useState } from 'react';
import { DesignArt, type PresetItem } from '@/assets/studio';
import { Button } from '@/components/Button';
import { Checkbox } from '@/components/Checkbox';
import { Sheet } from '@/components/Sheet';
import { TextField } from '@/components/TextField';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './PackExportSheet.module.css';

export interface PackExportSheetProps {
  open: boolean;
  onClose: () => void;
  /** The looks that go into the pack. */
  items: PresetItem[];
  /** Called with the file name, such as "My presets.graphopack". */
  onExport?: (file: string, options: { includeUsed: boolean; addName: boolean }) => void;
  placement?: OverlayPlacement;
}

/** Export the chosen presets as one .graphopack file: a name, and two choices about what goes in. */
export function PackExportSheet({ open, onClose, items, onExport, placement = 'viewport' }: PackExportSheetProps) {
  const [name, setName] = useState('My presets');
  const [includeUsed, setIncludeUsed] = useState(true);
  const [addName, setAddName] = useState(false);
  const file = `${name.trim() || 'My presets'}.graphopack`;
  const n = items.length;
  return (
    <Sheet
      open={open} onClose={onClose} title="Export as a pack" width={540} placement={placement}
      footer={
        <>
          <Button variant="text" onClick={onClose}>Cancel</Button>
          <Button variant="primary" icon="download" disabled={!n} onClick={() => onExport?.(file, { includeUsed, addName })}>Export .graphopack</Button>
        </>
      }
    >
      <div className={styles.summary}>
        <div className={styles.thumbs}>
          {items.slice(0, 5).map((it) => (
            <i key={it.id} className={styles.thumb}><div style={{ width: it.design.kind === 'cover' ? 38 : 64 }}><DesignArt design={it.design} /></div></i>
          ))}
          {n > 5 && <i className={styles.more}>+{n - 5}</i>}
        </div>
        <p className={styles.count}><b>{n} preset{n === 1 ? '' : 's'}</b> selected</p>
      </div>
      <TextField className={styles.name} variant="stacked" label="Pack name" value={name} maxLength={40} spellCheck={false} onChange={(e) => setName(e.target.value)} />
      <div className={styles.opts}>
        <div className={styles.opt}>
          <Checkbox checked={includeUsed} onChange={setIncludeUsed} label="Include what they use" hint="Stickers, icons and textures they use" />
        </div>
        <div className={styles.opt}>
          <Checkbox checked={addName} onChange={setAddName} label="Add my name" />
        </div>
      </div>
      <p className={styles.foot}>Saves as <b>{file}</b>. Page templates are exported the same way, from the template picker.</p>
    </Sheet>
  );
}
