import { useRef, useState, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { Sheet } from '@/components/Sheet';
import { Tabs, type TabItem } from '@/components/Tabs';
import { TextField } from '@/components/TextField';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './QuickSheet.module.css';

export interface QuickSheetProps {
  open: boolean;
  onClose: () => void;
  title: string;
  /** Sets the spacing of the preview well and the fields beside it. */
  kind: 'folder' | 'cover' | 'canvas';
  preview: ReactNode;
  /** The fields next to the preview: name, location and anything else. */
  fields: ReactNode;
  tabs: TabItem[];
  tab: string;
  onTab: (tab: string) => void;
  /** The content of the open tab. */
  panes: ReactNode;
  mode: 'new' | 'edit';
  createLabel: string;
  onCreate: () => void;
  onDelete?: () => void;
  onCustomise?: () => void;
  placement?: OverlayPlacement;
}

/** The frame shared by the Folder, Notebook and Canvas sheets: a preview with fields, tabs, and the actions. New and Edit are the same sheet. */
export function QuickSheet({ open, onClose, title, kind, preview, fields, tabs, tab, onTab, panes, mode, createLabel, onCreate, onDelete, onCustomise, placement = 'viewport' }: QuickSheetProps) {
  return (
    <Sheet
      open={open} onClose={onClose} title={title} width={540} placement={placement}
      footer={
        <>
          {mode === 'edit' && <Button variant="danger" icon="trash" onClick={onDelete}>Delete</Button>}
          {onCustomise && <Button icon="sliders" onClick={onCustomise}>Customise</Button>}
          <span className={styles.spacer} />
          <Button variant="text" onClick={onClose}>Cancel</Button>
          <Button variant="primary" icon={mode === 'edit' ? 'check' : 'plus'} onClick={onCreate}>{createLabel}</Button>
        </>
      }
    >
      <div className={styles.top} data-kind={kind}>
        <div className={styles.preview}>{preview}</div>
        <div className={styles.fields}>{fields}</div>
      </div>
      <Tabs className={styles.tabs} aria-label={`${title} options`} tabs={tabs} value={tab} onChange={onTab} />
      <div className={styles.panes}>
        <div key={tab} className={styles.pane} role="tabpanel">{panes}</div>
      </div>
    </Sheet>
  );
}

/** The Name field of a sheet. */
export function NameField({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return <TextField variant="stacked" label="Name" value={value} maxLength={40} spellCheck={false} onChange={(e) => onChange(e.target.value)} />;
}

/** Where a new item goes, for specimens: the library and its top folders. */
export const SAMPLE_LOCATIONS = ['Library', 'Semester 1', 'Personal', 'Art class', 'Readings', 'Projects'];

/** The Location field: reads where the item goes and opens a menu of folders. */
export function LocationField({ value = 'Library', options = SAMPLE_LOCATIONS, onChange }: { value?: string; options?: string[]; onChange?: (v: string) => void }) {
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLDivElement>(null);
  return (
    <>
      <div ref={anchor} className={styles.location}>
        <TextField
          variant="stacked" label="Location" icon="folder" value={value} readOnly
          aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((v) => !v)}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') { e.preventDefault(); setOpen(true); } }}
          trailing={<Icon name="chevron_down" size={16} />}
        />
      </div>
      <Popover open={open} onClose={() => setOpen(false)} anchorRef={anchor} placement="bottom-start" kind="menu">
        <Menu label="Location" onClose={() => setOpen(false)}>
          {options.map((o) => (
            <MenuItem key={o} icon={o === 'Library' ? 'home' : 'folder'} checked={o === value} onSelect={() => onChange?.(o)}>{o}</MenuItem>
          ))}
        </Menu>
      </Popover>
    </>
  );
}
