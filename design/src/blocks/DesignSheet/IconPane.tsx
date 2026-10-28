import { useState } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { SUBJECT_ICONS } from '@/assets/studio';
import { SearchField } from '@/components/SearchField';
import { cx } from '@/lib/cx';
import styles from './panes.module.css';

export interface IconPaneProps {
  /** The chosen icon, or null for none. */
  value: string | null;
  onChange: (icon: string | null) => void;
  onImport?: () => void;
}

/** Search, then a grid of subject icons. The first tile is no icon. */
export function IconPane({ value, onChange, onImport }: IconPaneProps) {
  const [q, setQ] = useState('');
  const query = q.trim().toLowerCase();
  const list = SUBJECT_ICONS.filter((n) => !query || n.includes(query));
  return (
    <>
      <SearchField size="sm" width={999} focusWidth={999} placeholder="Search icons" value={q} onChange={setQ} className={styles.search} />
      <div className={styles.icons} role="radiogroup" aria-label="Icon">
        {!query && <IconTile name="slash" label="No icon" on={value === null} onClick={() => onChange(null)} />}
        {list.map((n) => <IconTile key={n} name={n} label={n} on={value === n} onClick={() => onChange(n)} />)}
        <IconTile name="upload" label="Import an icon" add onClick={onImport} />
      </div>
    </>
  );
}

function IconTile({ name, label, on = false, add = false, onClick }: { name: IconName; label: string; on?: boolean; add?: boolean; onClick?: () => void }) {
  return (
    <button type="button" role={add ? 'button' : 'radio'} aria-checked={add ? undefined : on} aria-label={label} title={label} className={cx(styles.ico, add && styles.add)} onClick={onClick}>
      <Icon name={name} size={20} />
    </button>
  );
}
