import type { RefObject } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { Menu, MenuItem, MenuSection, MenuSeparator } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import styles from './NewMenu.module.css';

export type NewKind = 'notebook' | 'canvas' | 'folder' | 'pdf' | 'image' | 'pack';

export interface NewMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  onChoose: (kind: NewKind) => void;
  placement?: Placement;
}

const CREATE: Array<[NewKind, IconName, string]> = [['notebook', 'notebook', 'Notebook'], ['canvas', 'canvas', 'Canvas'], ['folder', 'folder', 'Folder']];
const IMPORT: Array<[NewKind, IconName, string]> = [['pdf', 'pdf', 'PDF'], ['image', 'image', 'Image'], ['pack', 'pack', 'Pack']];

/** What you can make, then what you can bring in. Both groups share one row shape so icons and labels line up; import is quieter by its plate, not by smaller text. */
export function NewMenu({ open, onClose, anchorRef, onChoose, placement = 'bottom-end' }: NewMenuProps) {
  const row = (quiet: boolean) => ([kind, icon, label]: [NewKind, IconName, string]) => (
    <MenuItem key={kind} className={styles.row} onSelect={() => onChoose(kind)}>
      <span className={styles.line}><span className={quiet ? styles.plateQuiet : styles.plate}><Icon name={icon} size={18} /></span>{label}</span>
    </MenuItem>
  );
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} aria-label="New">
      <Menu onClose={onClose}>
        {CREATE.map(row(false))}
        <MenuSeparator />
        <MenuSection label="Import">{IMPORT.map(row(true))}</MenuSection>
      </Menu>
    </Popover>
  );
}
