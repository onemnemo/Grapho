import { useMemo, useState } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Checkbox } from '@/components/Checkbox';
import { RemovableChip } from '@/components/Chip';
import { SearchField } from '@/components/SearchField';
import { SegmentedControl } from '@/components/SegmentedControl';
import { Sheet } from '@/components/Sheet';
import { Toggle } from '@/components/Toggle';
import { Highlight } from '@/components/Highlight';
import { RollingNumber } from '@/components/RollingNumber';
import type { CoverTileKind } from '../CoverTile';
import styles from './FilterSheet.module.css';

export type EditedRange = 'any' | 'week' | 'month' | 'year';

export interface FilterState {
  types: CoverTileKind[];
  edited: EditedRange;
  starred: boolean;
  tags: string[];
}

export const NO_FILTERS: FilterState = { types: [], edited: 'any', starred: false, tags: [] };

export function filtersActive(f: FilterState): boolean {
  return f.types.length > 0 || f.edited !== 'any' || f.starred || f.tags.length > 0;
}

export interface TagCount { name: string; count: number }

export interface FilterSheetProps {
  open: boolean;
  onClose: () => void;
  value: FilterState;
  onChange: (next: FilterState) => void;
  /** How many items of each type are in scope. */
  typeCounts: Record<CoverTileKind, number>;
  tags: TagCount[];
  /** How many items match the filters right now. */
  resultCount: number;
  placement?: 'viewport' | 'container';
}

const TYPES: Array<{ kind: CoverTileKind; label: string; icon: IconName }> = [
  { kind: 'notebook', label: 'Notebooks', icon: 'notebook' }, { kind: 'pdf', label: 'PDFs', icon: 'pdf' }, { kind: 'canvas', label: 'Canvases', icon: 'canvas' },
];
const EDITED = [
  { value: 'any', label: 'Any time' }, { value: 'week', label: 'Week' }, { value: 'month', label: 'Month' }, { value: 'year', label: 'Year' },
];

const toggle = <T,>(list: T[], v: T) => (list.includes(v) ? list.filter((x) => x !== v) : [...list, v]);

/** The library filter, a side sheet so the library stays visible and updates as you tap. */
export function FilterSheet({ open, onClose, value, onChange, typeCounts, tags, resultCount, placement = 'viewport' }: FilterSheetProps) {
  const [tagQuery, setTagQuery] = useState('');
  const shown = useMemo(() => {
    const q = tagQuery.trim().toLowerCase();
    return tags.filter((t) => !q || t.name.toLowerCase().includes(q)).sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [tags, tagQuery]);
  const empty = resultCount === 0;
  return (
    <Sheet
      open={open}
      onClose={onClose}
      variant="side"
      title="Filters"
      placement={placement}
      footer={
        <>
          <Button variant="text" onClick={() => { onChange(NO_FILTERS); setTagQuery(''); }}>Reset</Button>
          <Button variant="primary" size="lg" disabled={empty} onClick={onClose}>
            {empty ? 'No items match' : (
              <span className={styles.show}><span>Show</span><RollingNumber value={resultCount} /><span>{resultCount === 1 ? 'result' : 'results'}</span></span>
            )}
          </Button>
        </>
      }
    >
      <div className={styles.group}>
        <span className={styles.label}>Type</span>
        <div className={styles.types}>
          {TYPES.map((t) => (
            <button key={t.kind} type="button" className={styles.type} aria-pressed={value.types.includes(t.kind)} onClick={() => onChange({ ...value, types: toggle(value.types, t.kind) })}>
              <Icon name={t.icon} size={24} />
              <b>{t.label}</b>
              <span>{typeCounts[t.kind]}</span>
            </button>
          ))}
        </div>
      </div>
      <div className={styles.group}>
        <span className={styles.label}>Edited</span>
        <SegmentedControl aria-label="Edited" fit options={EDITED} value={value.edited} onChange={(v) => onChange({ ...value, edited: v as EditedRange })} />
      </div>
      <div className={styles.switch}>
        Starred only
        <Toggle aria-label="Only starred" checked={value.starred} onChange={(starred) => onChange({ ...value, starred })} />
      </div>
      <div className={styles.tags}>
        <span className={styles.label}>Tags</span>
        <div className={styles.chosen}>
          {value.tags.map((t) => (
            <RemovableChip key={t} removeLabel={`Remove ${t}`} onRemove={() => onChange({ ...value, tags: value.tags.filter((x) => x !== t) })}>{t}</RemovableChip>
          ))}
        </div>
        <SearchField className={styles.search} size="md" width={344} focusWidth={344} placeholder={`Search ${tags.length} tags`} value={tagQuery} onChange={setTagQuery} />
        <div className={styles.list}>
          {shown.length === 0 && <div className={styles.none}>No tag called "{tagQuery}"</div>}
          {shown.map((t) => (
            <label key={t.name} className={styles.row}>
              <Checkbox aria-label={t.name} checked={value.tags.includes(t.name)} onChange={() => onChange({ ...value, tags: toggle(value.tags, t.name) })} />
              <span className={styles.name}><Highlight text={t.name} query={tagQuery} /></span>
              <span className={styles.num}>{t.count}</span>
            </label>
          ))}
        </div>
      </div>
    </Sheet>
  );
}
