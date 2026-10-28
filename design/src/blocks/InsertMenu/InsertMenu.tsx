import type { RefObject } from 'react';
import { Menu, MenuItem, MenuSection } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import styles from './InsertMenu.module.css';

export type InsertAction = 'photo' | 'takePhoto' | 'pasteImage' | 'pdfPages' | 'blankPage';

export interface InsertMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
  boundary?: 'viewport' | 'container';
  /** Paste image shows only when the clipboard holds a picture. */
  canPasteImage?: boolean;
  /** An infinite canvas has no pages, so the Pages group is left out. */
  infinite?: boolean;
  onAction?: (action: InsertAction) => void;
}

/** Insert: pictures first, pages second. Each row is an action with its own icon. */
export function InsertMenu({ open, onClose, anchorRef, placement = 'bottom-end', boundary = 'container', canPasteImage = true, infinite = false, onAction }: InsertMenuProps) {
  const go = (a: InsertAction) => () => onAction?.(a);
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} boundary={boundary}>
      <Menu onClose={onClose} label="Insert">
        <MenuSection label="Image">
          <MenuItem icon="image" onSelect={go('photo')}>Photo</MenuItem>
          <MenuItem icon="camera" onSelect={go('takePhoto')}>Take photo</MenuItem>
          {canPasteImage && <MenuItem icon="paste" onSelect={go('pasteImage')}>Paste image</MenuItem>}
        </MenuSection>
        {!infinite && (
          <MenuSection label="Pages" className={styles.pages}>
            <MenuItem icon="add_page" onSelect={go('blankPage')}>Add blank page</MenuItem>
            <MenuItem icon="pdf" onSelect={go('pdfPages')}>Insert PDF pages</MenuItem>
          </MenuSection>
        )}
      </Menu>
    </Popover>
  );
}
