import type { RefObject } from 'react';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover, type Placement } from '@/components/Popover';
import { SegmentedControl } from '@/components/SegmentedControl';
import { Slider } from '@/components/Slider';
import styles from './SortMenu.module.css';

export type ItemSortKey = 'opened' | 'edited' | 'created' | 'name' | 'size' | 'custom';
export type FolderSortKey = 'name' | 'items' | 'edited';
export type SortKey = ItemSortKey | FolderSortKey;
export type SortDir = 'asc' | 'desc';
export type GroupBy = 'none' | 'date' | 'type' | 'letter';
export type ViewMode = 'grid' | 'list';

export const SORT_LABELS: Record<SortKey, string> = {
  opened: 'Last opened', edited: 'Last edited', created: 'Date created', name: 'Name', size: 'Size', custom: 'Custom order', items: 'Most items',
};

const ITEM_KEYS: ItemSortKey[] = ['opened', 'edited', 'created', 'name', 'size', 'custom'];
const FOLDER_KEYS: FolderSortKey[] = ['name', 'items', 'edited'];

/** The words for the two directions follow the key: A to Z, Smallest first, Oldest first. */
export function directionLabels(key: SortKey): [string, string] {
  if (key === 'name') return ['A to Z', 'Z to A'];
  if (key === 'size') return ['Smallest first', 'Largest first'];
  if (key === 'items') return ['Fewest first', 'Most first'];
  return ['Oldest first', 'Newest first'];
}

export interface SortMenuProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  /** items shows every sort key, group by and cover size; folders is the short version. */
  scope?: 'items' | 'folders';
  sortKey: SortKey;
  sortDir: SortDir;
  group?: GroupBy;
  /** Cover size, 0 to 100. */
  size?: number;
  view?: ViewMode;
  onSortKey: (key: SortKey) => void;
  onSortDir: (dir: SortDir) => void;
  onGroup?: (group: GroupBy) => void;
  onSize?: (size: number) => void;
  placement?: Placement;
}

/** One column, three levels: Sort by, Order (words follow the key), Group by. Cover size last. */
export function SortMenu({
  open, onClose, anchorRef, scope = 'items', sortKey, sortDir, group = 'none', size = 50, view = 'grid',
  onSortKey, onSortDir, onGroup, onSize, placement = 'bottom-end',
}: SortMenuProps) {
  const keys = scope === 'folders' ? FOLDER_KEYS : ITEM_KEYS;
  const [asc, desc] = directionLabels(sortKey);
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} className={styles.pop} aria-label="Sort">
      <Menu>
        <div className={`${styles.label} ${styles.first}`} aria-hidden>Sort by</div>
        {keys.map((k) => (
          <MenuItem key={k} checked={sortKey === k} keepOpen onSelect={() => onSortKey(k)}>{SORT_LABELS[k]}</MenuItem>
        ))}
      </Menu>
      <div className={styles.label}>Order</div>
      <div className={styles.pad}>
        <SegmentedControl
          aria-label="Order"
          fit
          value={sortDir}
          onChange={(v) => onSortDir(v as SortDir)}
          options={[{ value: 'asc', label: asc }, { value: 'desc', label: desc }]}
        />
      </div>
      {scope === 'items' && (
        <>
          <div className={styles.label}>Group by</div>
          <div className={styles.pad}>
            <SegmentedControl
              aria-label="Group by"
              fit
              value={group}
              onChange={(v) => onGroup?.(v as GroupBy)}
              options={[{ value: 'none', label: 'None' }, { value: 'date', label: 'Date' }, { value: 'type', label: 'Type' }, { value: 'letter', label: 'A to Z' }]}
            />
          </div>
          <div className={styles.label}>Cover size</div>
          <div className={styles.size} data-off={view === 'list'}>
            <Slider aria-label="Cover size" value={size} onChange={(v) => onSize?.(v)} disabled={view === 'list'} />
          </div>
        </>
      )}
    </Popover>
  );
}
