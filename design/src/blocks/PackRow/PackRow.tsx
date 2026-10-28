import { useRef, useState, type HTMLAttributes } from 'react';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem, MenuSeparator } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { Toggle } from '@/components/Toggle';
import { cx } from '@/lib/cx';
import { PackCover } from './PackCover';
import type { PackItem } from './packTypes';
import styles from './PackRow.module.css';

export type PackRowAction = 'open' | 'export' | 'remove';

export interface PackRowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onToggle'> {
  pack: PackItem;
  /** What it holds, in the quiet line: "48 items". */
  detail?: string;
  /** Smaller cover, for a phone. */
  compact?: boolean;
  boundary?: 'viewport' | 'container';
  onToggle?: (enabled: boolean) => void;
  onAction?: (action: PackRowAction) => void;
}

/** A pack as a source: cover, name, what it holds, an on and off switch and a ••• menu. Opening it shows its contents by type. */
export function PackRow({ pack, detail, compact = false, boundary = 'viewport', onToggle, onAction, className, ...rest }: PackRowProps) {
  const moreRef = useRef<HTMLButtonElement>(null);
  const [menu, setMenu] = useState(false);
  return (
    <div className={cx(styles.row, !pack.enabled && styles.off, className)} data-pack={pack.id} {...rest}>
      <button type="button" className={styles.main} onClick={() => onAction?.('open')}>
        <PackCover className={styles.art} art={pack.art} size={compact ? 44 : 52} cell={3} />
        <span className={styles.text}>
          <b>{pack.name}</b>
          {detail && <small>{detail}</small>}
        </span>
      </button>
      <span className={styles.acts}>
        <Toggle variant="marked" checked={pack.enabled} onChange={onToggle} aria-label={`${pack.name} on`} />
        <span ref={moreRef} className={styles.more}>
          <IconButton icon="more" label={`More for ${pack.name}`} size="sm" variant="ghost" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)} />
        </span>
      </span>
      <Popover open={menu} onClose={() => setMenu(false)} anchorRef={moreRef} placement="bottom-end" kind="menu" boundary={boundary}>
        <Menu onClose={() => setMenu(false)} label={`${pack.name} menu`}>
          <MenuItem icon="grid" onSelect={() => onAction?.('open')}>Contents</MenuItem>
          <MenuItem icon="export" onSelect={() => onAction?.('export')}>Export .graphopack</MenuItem>
          <MenuSeparator />
          <MenuItem icon="trash" tone="danger" onSelect={() => onAction?.('remove')}>Remove</MenuItem>
        </Menu>
      </Popover>
    </div>
  );
}
