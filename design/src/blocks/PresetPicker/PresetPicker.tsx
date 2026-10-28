import { useMemo, useRef, useState } from 'react';
import {
  DesignArt, PRESET_SORTS, PRESET_SOURCES, PRESET_TYPES, buildPresetItems, filterPresets,
  type Design, type DesignKind, type PresetItem, type PresetSort, type PresetSource, type PresetType,
} from '@/assets/studio';
import { Button } from '@/components/Button';
import { Chip } from '@/components/Chip';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { PresetTile } from '@/components/PresetTile';
import { SearchField } from '@/components/SearchField';
import { Icon } from '@/assets/icons';
import { Sheet } from '@/components/Sheet';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './PresetPicker.module.css';

export interface PresetPickerProps {
  open: boolean;
  onClose: () => void;
  /** Which kind of look is on show: folders or covers. */
  kind: DesignKind;
  /** Looks the person saved from the studio. They come first, under Yours. */
  yours?: Design[];
  /** Choose one and press Use this. It replaces the layers; the name stays. */
  onUse?: (design: Design) => void;
  /** In select mode: the chosen looks, to be saved as a pack. */
  onExport?: (items: PresetItem[]) => void;
  onImport?: () => void;
  placement?: OverlayPlacement;
  /** Start in select mode, or with a filter set. */
  initialSelecting?: boolean;
  initialQuery?: string;
  initialSource?: PresetSource | 'all';
  initialType?: PresetType | 'all';
}

const WIDTH = { folder: 150, cover: 104 };

/** Presets and library: every look to start from, with search, sort, two filter rows, and a select mode that exports a pack. */
export function PresetPicker({ open, onClose, kind, yours, onUse, onExport, onImport, placement = 'viewport', initialSelecting = false, initialQuery = '', initialSource = 'all', initialType = 'all' }: PresetPickerProps) {
  const all = useMemo(() => buildPresetItems(kind, yours), [kind, yours]);
  const [q, setQ] = useState(initialQuery);
  const [src, setSrc] = useState<PresetSource | 'all'>(initialSource);
  const [type, setType] = useState<PresetType | 'all'>(initialType);
  const [sort, setSort] = useState<PresetSort>('new');
  const [selecting, setSelecting] = useState(initialSelecting);
  const [picks, setPicks] = useState<Set<string>>(new Set());
  const [one, setOne] = useState<string | null>(null);
  const [menu, setMenu] = useState(false);
  const sortRef = useRef<HTMLButtonElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const shown = useMemo(() => filterPresets(all, { q, src, type, sort }), [all, q, src, type, sort]);
  const count = (s: PresetSource | 'all') => (s === 'all' ? all.length : all.filter((i) => i.src === s).length);
  const toTop = () => { if (scrollRef.current) scrollRef.current.scrollTop = 0; };
  const clear = () => { setQ(''); setSrc('all'); setType('all'); toTop(); };
  const toggleSelect = () => { setSelecting((s) => !s); setPicks(new Set()); setOne(null); };
  const tap = (it: PresetItem) => {
    if (selecting) setPicks((p) => { const n = new Set(p); if (n.has(it.id)) n.delete(it.id); else n.add(it.id); return n; });
    else setOne(it.id);
  };
  const chosen = all.filter((i) => picks.has(i.id));
  const use = () => { const it = all.find((i) => i.id === one); if (it) { onUse?.(it.design); onClose(); } };

  return (
    <Sheet
      open={open} onClose={onClose} width={1000} placement={placement}
      title="Presets and library"
      actions={
        <>
          <Button size="sm" onClick={onImport}>Import</Button>
          <Button size="sm" variant={selecting ? 'primary' : 'secondary'} onClick={toggleSelect}>{selecting ? 'Done' : 'Select'}</Button>
        </>
      }
      footer={
        <>
          <span className={styles.note}>{selecting && picks.size > 0 ? `${picks.size} selected` : ''}</span>
          <Button variant="text" onClick={onClose}>Cancel</Button>
          {selecting
            ? <Button variant="primary" icon="download" disabled={!picks.size} onClick={() => onExport?.(chosen)}>Export .graphopack</Button>
            : <Button variant="primary" disabled={one == null} onClick={use}>Use this</Button>}
        </>
      }
    >
      <div className={styles.tools}>
        <div className={styles.search}>
          <SearchField width={999} value={q} onChange={(v) => { setQ(v); toTop(); }} placeholder="Search presets and packs" aria-label="Search presets" />
        </div>
        <div className={styles.sortWrap}>
          <button ref={sortRef} type="button" className={styles.sort} aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu((m) => !m)}>
            <span>Sort</span><b>{PRESET_SORTS.find((s) => s[0] === sort)?.[1]}</b><Icon name="chevron_down" size={14} />
          </button>
          <Popover open={menu} onClose={() => setMenu(false)} anchorRef={sortRef} placement="bottom-end" aria-label="Sort">
            <Menu>
              {PRESET_SORTS.map(([k, n]) => <MenuItem key={k} checked={sort === k} onSelect={() => { setSort(k); toTop(); }}>{n}</MenuItem>)}
            </Menu>
          </Popover>
        </div>
      </div>
      <div className={styles.chips}>
        <div className={styles.chipRow} role="group" aria-label="Source"><span>Source</span>
          {PRESET_SOURCES.map(([k, n]) => <Chip key={k} size="sm" selected={src === k} count={count(k)} onChange={() => { setSrc(k); toTop(); }}>{n}</Chip>)}
        </div>
        <div className={styles.chipRow} role="group" aria-label="Type"><span>Type</span>
          {PRESET_TYPES.map((k) => <Chip key={k} size="sm" selected={type === k} onChange={() => { setType(k); toTop(); }}>{k === 'all' ? 'All' : k}</Chip>)}
        </div>
      </div>
      <div ref={scrollRef} className={styles.scroll}>
        {shown.length ? (
          <div className={styles.grid}>
            {shown.map((it) => (
              <PresetTile
                key={it.id} variant="picker" shape={kind} label={it.name}
                selecting={selecting} selected={selecting ? picks.has(it.id) : one === it.id} onClick={() => tap(it)}
                preview={<div className={styles.art} style={{ width: WIDTH[kind] }}><DesignArt design={it.design} /></div>}
              />
            ))}
          </div>
        ) : (
          <div className={styles.empty}>
            <b>Nothing here</b>
            <Button size="sm" onClick={clear}>Clear filters</Button>
          </div>
        )}
      </div>
    </Sheet>
  );
}
