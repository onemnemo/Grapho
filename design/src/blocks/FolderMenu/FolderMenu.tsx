import { useRef, useState, type RefObject } from 'react';
import { Icon } from '@/assets/icons';
import { FolderArt, type FolderColour } from '@/assets/covers';
import { Menu, MenuItem, MenuSection, MenuSeparator } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import { cx } from '@/lib/cx';
import styles from './FolderMenu.module.css';

export type FolderAction = 'open' | 'edit' | 'pin' | 'star' | 'move' | 'duplicate' | 'select' | 'delete';

/** What the menu belongs to. Folders pin; notebooks, PDFs and canvases star and duplicate. */
export type MenuTarget = 'folder' | 'notebook' | 'pdf' | 'canvas';

const NOUN: Record<MenuTarget, string> = { folder: 'folder', notebook: 'notebook', pdf: 'PDF', canvas: 'canvas' };

export interface FolderTarget {
  id: string;
  name: string;
  colour: FolderColour;
  /** Name of the folder it sits in, shown on the right. */
  parent?: string;
}

export interface FolderMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  kind?: MenuTarget;
  pinned?: boolean;
  starred?: boolean;
  onAction: (action: FolderAction) => void;
  /** When given, Move to opens a list of these folders. */
  targets?: FolderTarget[];
  onMove?: (folderId: string) => void;
  placement?: Placement;
}

function MoveItems({ targets, onMove }: { targets: FolderTarget[]; onMove: (folderId: string) => void }) {
  return (
    <MenuSection label="Move to">
      {targets.map((t) => (
        <MenuItem key={t.id} value={t.parent} onSelect={() => onMove(t.id)}>
          <span className={styles.target}>
            <span className={styles.art} aria-hidden="true"><FolderArt colour={t.colour} /></span>
            {t.name}
          </span>
        </MenuItem>
      ))}
    </MenuSection>
  );
}

export interface MoveMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  targets: FolderTarget[];
  onMove: (folderId: string) => void;
  placement?: Placement;
}

/** A list of folders to move the selection into. */
export function MoveMenu({ open, onClose, anchorRef, targets, onMove, placement = 'top-start' }: MoveMenuProps) {
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} aria-label="Move to">
      <Menu onClose={onClose} label="Move to">
        <MoveItems targets={targets} onMove={onMove} />
      </Menu>
    </Popover>
  );
}

/** The ••• menu of anything in the library: Open, Edit, Pin or Star, Move to, Duplicate, Select, and Delete at the end. */
export function FolderMenu({ open, onClose, anchorRef, kind = 'folder', pinned = false, starred = false, onAction, targets, onMove, placement = 'bottom-end' }: FolderMenuProps) {
  const folder = kind === 'folder';
  const submenu = targets && onMove ? (
    <Menu label="Move to">
      <MoveItems targets={targets} onMove={onMove} />
    </Menu>
  ) : undefined;
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} aria-label={`${NOUN[kind]} menu`}>
      <Menu onClose={onClose}>
        <MenuItem icon={folder ? 'folder_open' : 'read_mode'} onSelect={() => onAction('open')}>Open</MenuItem>
        <MenuItem icon={kind === 'folder' || kind === 'notebook' ? 'edit' : 'rename'} onSelect={() => onAction('edit')}>{kind === 'folder' || kind === 'notebook' ? `Edit ${NOUN[kind]}` : 'Rename'}</MenuItem>
        {folder
          ? <MenuItem icon="pin" onSelect={() => onAction('pin')}>{pinned ? 'Unpin' : 'Pin to top'}</MenuItem>
          : <MenuItem icon="star" onSelect={() => onAction('star')}>{starred ? 'Unstar' : 'Star'}</MenuItem>}
        <MenuItem icon="folder_move" submenu={submenu} onSelect={() => onAction('move')}>Move to</MenuItem>
        {!folder && <MenuItem icon="duplicate" onSelect={() => onAction('duplicate')}>Duplicate</MenuItem>}
        <MenuItem icon="select" onSelect={() => onAction('select')}>Select</MenuItem>
        <MenuSeparator />
        <MenuItem icon="trash" tone="danger" onSelect={() => onAction('delete')}>{`Delete ${NOUN[kind]}`}</MenuItem>
      </Menu>
    </Popover>
  );
}

export interface FolderMoreButtonProps extends Omit<FolderMenuProps, 'open' | 'onClose' | 'anchorRef'> {
  /** media sits over folder artwork; row sits in a list row. */
  variant?: 'media' | 'row';
  small?: boolean;
  /** Keep the button showing, for stories. */
  show?: boolean;
  /** Start with the menu open, for stories. */
  defaultOpen?: boolean;
  className?: string;
}

/** The ••• button with its menu. It shows on hover and always on touch. */
export function FolderMoreButton({ variant = 'media', small = false, show = false, defaultOpen = false, className, ...menu }: FolderMoreButtonProps) {
  const [open, setOpen] = useState(defaultOpen);
  const ref = useRef<HTMLButtonElement>(null);
  return (
    <>
      <button
        ref={ref}
        type="button"
        className={cx(styles.more, variant === 'row' && styles.row, small && styles.small, className)}
        aria-label={`${NOUN[menu.kind ?? 'folder'][0].toUpperCase()}${NOUN[menu.kind ?? 'folder'].slice(1)} options`}
        aria-haspopup="menu"
        aria-expanded={open}
        data-show={show || undefined}
        onClick={(e) => { e.stopPropagation(); setOpen((v) => !v); }}
        onPointerDown={(e) => e.stopPropagation()}
      >
        <Icon name="more" size={18} />
      </button>
      {/* the menu is portalled, but React still bubbles its clicks to the tile, which would open it */}
      <span onClick={(e) => e.stopPropagation()} onPointerDown={(e) => e.stopPropagation()}>
        <FolderMenu {...menu} open={open} onClose={() => setOpen(false)} anchorRef={ref} />
      </span>
    </>
  );
}
