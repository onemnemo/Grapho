import type { RefObject } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { Menu, MenuItem, MenuSection, MenuSeparator } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import styles from './OverflowMenu.module.css';

export type OverflowAction =
  | 'search' | 'share' | 'export' | 'pageBackground' | 'rename' | 'ruler' | 'protractor' | 'drawWithFinger'
  | 'customiseWheel' | 'readMode' | 'fullScreen' | 'settings';

export interface OverflowMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
  /** viewport floats over the window; container stays inside a framed screen. */
  boundary?: 'viewport' | 'container';
  /** The current page background, shown as the value of its row: Lined. */
  pageBackground?: string;
  drawWithFinger?: boolean;
  rulerOn?: boolean;
  protractorOn?: boolean;
  onAction?: (action: OverflowAction) => void;
}

const QUICK: Array<{ id: OverflowAction; icon: IconName; label: string }> = [
  { id: 'search', icon: 'search', label: 'Search' },
  { id: 'share', icon: 'share', label: 'Share' },
  { id: 'export', icon: 'export', label: 'Export' },
];

/**
 * The note's ••• menu. Three quick actions on top, then named groups: this note, tools, view. Insert lives on the bar's image button
 * only. Ruler, Protractor and Draw with finger are switches and show one. Settings is the single row at the bottom.
 */
export function OverflowMenu({ open, onClose, anchorRef, placement = 'bottom-end', boundary = 'viewport', pageBackground = 'Lined', drawWithFinger = false, rulerOn = false, protractorOn = false, onAction }: OverflowMenuProps) {
  const pick = (a: OverflowAction) => () => onAction?.(a);
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} boundary={boundary} kind="menu" aria-label="Note menu">
      <Menu onClose={onClose} label="Note menu">
        <div className={styles.quick}>
          {QUICK.map((q) => (
            <button key={q.id} type="button" role="menuitem" className={styles.q} onClick={() => { pick(q.id)(); onClose(); }}>
              <Icon name={q.icon} size={20} />
              <span>{q.label}</span>
            </button>
          ))}
        </div>
        <MenuSection className={styles.group} label="This note">
          <MenuItem icon="page" value={pageBackground} onSelect={pick('pageBackground')}>Page background</MenuItem>
          <MenuItem icon="rename" onSelect={pick('rename')}>Rename note</MenuItem>
        </MenuSection>
        <MenuSection className={styles.group} label="Tools">
          <MenuItem icon="ruler" toggle={rulerOn} onSelect={pick('ruler')}>Ruler</MenuItem>
          <MenuItem icon="protractor" toggle={protractorOn} onSelect={pick('protractor')}>Protractor</MenuItem>
          <MenuItem icon="finger" toggle={drawWithFinger} onSelect={pick('drawWithFinger')}>Draw with finger</MenuItem>
          <MenuItem icon="wheel" onSelect={pick('customiseWheel')}>Customise wheel</MenuItem>
        </MenuSection>
        <MenuSection className={styles.group} label="View">
          <MenuItem icon="read_mode" onSelect={pick('readMode')}>Read mode</MenuItem>
          <MenuItem icon="fullscreen" onSelect={pick('fullScreen')}>Full screen</MenuItem>
        </MenuSection>
        <MenuSeparator />
        <MenuItem icon="settings" onSelect={pick('settings')}>Settings</MenuItem>
      </Menu>
    </Popover>
  );
}
