import { useState } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Dialog } from '@/components/Dialog';
import { OptionTile, OptionTileGroup } from '@/components/OptionTile';
import { SegmentedControl } from '@/components/SegmentedControl';
import { TextField } from '@/components/TextField';
import { Toggle } from '@/components/Toggle';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './ExportDialog.module.css';

export type ExportFormat = 'pdf' | 'images' | 'notebook';
export type ExportPages = 'all' | 'current' | 'range';

export interface ExportChoice { format: ExportFormat; pages: ExportPages; range: string; backgrounds: boolean }

export interface ExportDialogProps {
  open: boolean;
  onClose: () => void;
  /** Pages in the note, for checking the range. */
  pageCount?: number;
  /** An infinite canvas exports as one page, so the page choice is replaced by a plain value. */
  infinite?: boolean;
  /** The export is running: the button shows the work and nothing can change. */
  exporting?: boolean;
  initial?: Partial<ExportChoice>;
  onExport?: (choice: ExportChoice) => void;
  placement?: OverlayPlacement;
}

const FORMATS: Array<{ value: ExportFormat; label: string; icon: 'pdf' | 'image' | 'notebook' }> = [
  { value: 'pdf', label: 'PDF', icon: 'pdf' },
  { value: 'images', label: 'Images', icon: 'image' },
  { value: 'notebook', label: 'Notebook', icon: 'notebook' },
];

/** Parses "1-3, 5" into page numbers. Null when it is not a list of pages inside the note. */
export function parseRange(text: string, count: number): number[] | null {
  const out: number[] = [];
  for (const part of text.split(',').map((p) => p.trim()).filter(Boolean)) {
    const m = /^(\d+)(?:\s*-\s*(\d+))?$/.exec(part);
    if (!m) return null;
    const a = Number(m[1]), b = m[2] ? Number(m[2]) : a;
    if (a < 1 || b < a || b > count) return null;
    for (let n = a; n <= b; n += 1) out.push(n);
  }
  return out.length ? out : null;
}

/** Export a note: the format as three tiles, which pages, and whether the page backgrounds come along. Export is the one solid button. */
export function ExportDialog({ open, onClose, pageCount = 12, infinite = false, exporting = false, initial, onExport, placement = 'viewport' }: ExportDialogProps) {
  const [c, setC] = useState<ExportChoice>({ format: 'pdf', pages: 'all', range: '', backgrounds: true, ...initial });
  const set = (p: Partial<ExportChoice>) => setC((v) => ({ ...v, ...p }));
  const notebook = c.format === 'notebook';
  const rangeBad = !notebook && !infinite && c.pages === 'range' && (c.range.trim() === '' || !parseRange(c.range, pageCount));
  const rangeError = rangeBad && c.range.trim() !== '' ? `Use page numbers from 1 to ${pageCount}, like 1-3, 5.` : undefined;

  return (
    <Dialog
      open={open}
      onClose={exporting ? () => {} : onClose}
      placement={placement}
      width={460}
      density="compact"
      title="Export"
      actions={
        <>
          <Button variant="text" onClick={onClose} disabled={exporting}>Cancel</Button>
          <Button variant="primary" disabled={rangeBad || exporting} onClick={() => onExport?.(c)}>{exporting ? 'Exporting' : 'Export'}</Button>
        </>
      }
    >
      <div className={styles.body} data-busy={exporting || undefined}>
        <OptionTileGroup columns={3} aria-label="Format">
          {FORMATS.map((f) => (
            <OptionTile key={f.value} layout="tile" label={f.label} selected={c.format === f.value} disabled={exporting} onClick={() => set({ format: f.value })} visual={<span className={styles.glyph}><Icon name={f.icon} size={24} /></span>} />
          ))}
        </OptionTileGroup>
        {!notebook && (
          <>
            <div className={styles.group}>
              <span className={styles.label}>Pages</span>
              {infinite ? (
                <span className={styles.single}>The whole canvas, as one page</span>
              ) : (
                <SegmentedControl
                  size="sm"
                  fit
                  aria-label="Pages"
                  value={c.pages}
                  onChange={(v) => set({ pages: v as ExportPages })}
                  options={[{ value: 'all', label: 'All pages', disabled: exporting }, { value: 'current', label: 'Current page', disabled: exporting }, { value: 'range', label: 'Range', disabled: exporting }]}
                />
              )}
              {!infinite && c.pages === 'range' && (
                <TextField variant="inline" size="md" label="Page range" placeholder="1-3, 5" value={c.range} disabled={exporting} onChange={(e) => set({ range: e.target.value })} />
              )}
              {rangeError && <span role="alert" className={styles.error}>{rangeError}</span>}
            </div>
            <Toggle label="Include page backgrounds" checked={c.backgrounds} disabled={exporting} onChange={(backgrounds) => set({ backgrounds })} />
          </>
        )}
      </div>
    </Dialog>
  );
}
