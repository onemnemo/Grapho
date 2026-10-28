import type { RefObject } from 'react';
import { Menu, MenuItem, MenuSection, MenuSeparator } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import styles from './SelectionMenu.module.css';

export type SelectionKind = 'ink' | 'image';

export type SelectionMenuAction =
  | 'paste' | 'cut' | 'replace' | 'front' | 'back' | 'lock' | 'unlock' | 'flipH' | 'flipV' | 'text' | 'asImage' | 'exportSelection' | 'delete'
  | `layer:${string}` | 'newLayer';

export interface SelectionLayer { id: string; name: string }

export interface SelectionMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
  boundary?: 'viewport' | 'container';
  /** What is selected. A picture adds Replace and drops the ink-only items. */
  kind?: SelectionKind;
  locked?: boolean;
  /** Something is on the clipboard. */
  canPaste?: boolean;
  /** The layers of the note, top first, and the one the selection is on. */
  layers?: SelectionLayer[];
  layer?: string;
  onAction?: (action: SelectionMenuAction) => void;
}

const DEFAULT_LAYERS: SelectionLayer[] = [{ id: 'l2', name: 'Layer 2' }, { id: 'l1', name: 'Layer 1' }];

/**
 * The More menu behind the floating selection bar and the note's selection bar. Group by what it does and let space
 * carry the groups: arrange, then the kind-specific items, then Delete alone under one hairline.
 */
export function SelectionMenu({ open, onClose, anchorRef, placement = 'bottom', boundary = 'container', kind = 'ink', locked = false, canPaste = false, layers = DEFAULT_LAYERS, layer = 'l1', onAction }: SelectionMenuProps) {
  const go = (a: SelectionMenuAction) => () => onAction?.(a);
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} boundary={boundary}>
      <Menu onClose={onClose} label="Selection">
        <MenuSection className={styles.group}>
          {canPaste && <MenuItem icon="paste" onSelect={go('paste')}>Paste</MenuItem>}
          <MenuItem icon="cut" onSelect={go('cut')}>Cut</MenuItem>
          {kind === 'image' && <MenuItem icon="replace" onSelect={go('replace')}>Replace image</MenuItem>}
        </MenuSection>
        <MenuSection className={styles.group}>
          <MenuItem
            icon="bring_front"
            submenu={
              <>
                <MenuItem icon="bring_front" onSelect={go('front')}>Bring to front</MenuItem>
                <MenuItem icon="send_back" onSelect={go('back')}>Send to back</MenuItem>
              </>
            }
          >
            Order
          </MenuItem>
          <MenuItem
            icon="layers"
            submenu={
              <>
                {layers.map((l) => (
                  <MenuItem key={l.id} checked={l.id === layer} onSelect={go(`layer:${l.id}`)}>{l.name}</MenuItem>
                ))}
                <MenuItem icon="plus" onSelect={go('newLayer')}>New layer</MenuItem>
              </>
            }
          >
            Move to layer
          </MenuItem>
          <MenuItem
            icon="flip_h"
            submenu={
              <>
                <MenuItem icon="flip_h" onSelect={go('flipH')}>Flip horizontally</MenuItem>
                <MenuItem icon="flip_v" onSelect={go('flipV')}>Flip vertically</MenuItem>
              </>
            }
          >
            Flip
          </MenuItem>
          <MenuItem icon={locked ? 'unlock' : 'lock'} onSelect={go(locked ? 'unlock' : 'lock')}>{locked ? 'Unlock' : 'Lock'}</MenuItem>
        </MenuSection>
        {kind === 'ink' && (
          <MenuSection className={styles.group}>
            <MenuItem icon="text" onSelect={go('text')}>Convert to text</MenuItem>
            <MenuItem icon="image" onSelect={go('asImage')}>Copy as image</MenuItem>
            <MenuItem icon="export" onSelect={go('exportSelection')}>Export selection</MenuItem>
          </MenuSection>
        )}
        <MenuSeparator />
        <MenuItem icon="trash" tone="danger" onSelect={go('delete')}>Delete</MenuItem>
      </Menu>
    </Popover>
  );
}
