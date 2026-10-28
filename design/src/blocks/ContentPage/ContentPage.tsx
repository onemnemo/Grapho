import { useMemo, useRef, useState } from 'react';
import { contentType, type ContentItem, type ContentSource, type ContentType } from '@/assets/content';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem, MenuSection, MenuSeparator } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { RollingNumber } from '@/components/RollingNumber';
import { SearchField } from '@/components/SearchField';
import { SegmentedControl } from '@/components/SegmentedControl';
import { cx } from '@/lib/cx';
import { PackCategory } from '../PackCategory';
import { ContentTile } from './ContentTile';
import { FilterMenu } from './FilterMenu';
import { countFrom, SORT_OPTIONS, viewItems, type ContentSort, type SourceFilter } from './contentView';
import styles from './ContentPage.module.css';

export interface ContentPageProps {
  type: ContentType;
  items: ContentItem[];
  sources: ContentSource[];
  phone?: boolean;
  boundary?: 'viewport' | 'container';
  initialView?: 'grid' | 'list';
  initialSource?: SourceFilter;
  initialSort?: ContentSort;
  /** Start in select mode with these ids picked, for stories. */
  initialSelected?: string[];
  /** Export as pack was pressed with these ids. */
  onExportAsPack?: (ids: string[]) => void;
  /** Bring in a pack file; what it holds for this type lands on this page. */
  onImport?: () => void;
  /** Open Discover on this type. */
  onDiscover?: (type: ContentType) => void;
  className?: string;
}

const sourceName = (id: string, sources: ContentSource[]) => (id === 'all' ? 'All' : sources.find((s) => s.id === id)?.name ?? 'All');

/** One page per type: a grid or list with a search, a Source filter, Sort, and Select mode that ends in Export as pack. Import and Discover wait in the ••• menu. */
export function ContentPage({ type, items, sources, phone = false, boundary = 'viewport', initialView = 'grid', initialSource = 'all', initialSort = 'recent', initialSelected, onExportAsPack, onImport, onDiscover, className }: ContentPageProps) {
  const [view, setView] = useState(initialView);
  const [source, setSource] = useState<SourceFilter>(initialSource);
  const [sort, setSort] = useState<ContentSort>(initialSort);
  const [query, setQuery] = useState('');
  const [selecting, setSelecting] = useState(!!initialSelected);
  const [menu, setMenu] = useState(false);
  const more = useRef<HTMLSpanElement>(null);
  const [picked, setPicked] = useState<Set<string>>(new Set(initialSelected));
  const [closed, setClosed] = useState<Set<string>>(new Set());
  const info = contentType(type);
  const shown = useMemo(() => viewItems(items, sources, { type, source, sort, query }), [items, sources, type, source, sort, query]);
  const packs = sources.filter((s) => s.kind === 'pack');
  const names = useMemo(() => new Map(sources.map((s) => [s.id, s.name])), [sources]);

  // under All, items sit in one group per source so you can see where each came from
  const groups = source === 'all' ? sources.map((s) => [s, shown.filter((i) => i.source === s.id)] as const).filter(([, l]) => l.length) : null;
  const flip = (id: string) => setClosed((c) => { const n = new Set(c); if (n.has(id)) n.delete(id); else n.add(id); return n; });
  const tiles = (list: ContentItem[]) => (
    <div className={styles.items} data-view={view} data-shape={info.shape} role="list">
      {list.map((it) => (
        <ContentTile
          key={it.id} role="listitem" item={it} view={view} sourceName={groups ? undefined : names.get(it.source)}
          selectable={selecting} selected={picked.has(it.id)} onClick={selecting ? () => toggle(it.id) : undefined}
        />
      ))}
    </div>
  );

  const toggle = (id: string) => setPicked((p) => { const n = new Set(p); if (n.has(id)) n.delete(id); else n.add(id); return n; });
  const stop = () => { setSelecting(false); setPicked(new Set()); };
  const allPicked = shown.length > 0 && shown.every((i) => picked.has(i.id));

  return (
    <div className={cx(styles.page, phone && styles.phone, className)}>
      <div className={styles.bar}>
        <SearchField size="sm" width={phone ? 140 : 168} placeholder="Search" aria-label={`Search ${info.label.toLowerCase()}`} value={query} onChange={setQuery} />
        <FilterMenu label="Source" value={sourceName(source, sources)} boundary={boundary}>
          {(close) => (
            <>
              {(['all', 'built', 'own'] as const).map((id) => (
                <MenuItem key={id} checked={source === id} value={countFrom(items, type, id)} onSelect={() => { setSource(id); close(); }}>{id === 'all' ? 'All' : sourceName(id, sources)}</MenuItem>
              ))}
              <MenuSeparator />
              <MenuSection label="Packs">
                {packs.map((s) => (
                  <MenuItem key={s.id} checked={source === s.id} value={s.enabled ? countFrom(items, type, s.id) : 'Off'} disabled={!s.enabled} onSelect={() => { setSource(s.id); close(); }}>{s.name}</MenuItem>
                ))}
              </MenuSection>
            </>
          )}
        </FilterMenu>
        <FilterMenu label="Sort" value={SORT_OPTIONS.find(([k]) => k === sort)?.[1] ?? ''} boundary={boundary}>
          {(close) => SORT_OPTIONS.map(([k, label]) => <MenuItem key={k} checked={sort === k} onSelect={() => { setSort(k); close(); }}>{label}</MenuItem>)}
        </FilterMenu>
        <span className={styles.gap} />
        <SegmentedControl
          aria-label="View" iconsOnly size="sm" value={view} onChange={(v) => setView(v as 'grid' | 'list')}
          options={[{ value: 'grid', label: 'Grid view', icon: 'grid' }, { value: 'list', label: 'List view', icon: 'list' }]}
        />
        <Button variant="text" size="sm" onClick={() => (selecting ? stop() : setSelecting(true))}>{selecting ? 'Cancel' : 'Select'}</Button>
        <span ref={more} className={styles.more}>
          <IconButton icon="more" label="More" size="sm" variant="ghost" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)} />
        </span>
        <Popover open={menu} onClose={() => setMenu(false)} anchorRef={more} placement="bottom-end" kind="menu" boundary={boundary}>
          <Menu label="More" onClose={() => setMenu(false)}>
            <MenuItem icon="import" onSelect={() => onImport?.()}>Import pack</MenuItem>
            <MenuItem icon="grid" onSelect={() => onDiscover?.(type)}>Discover more</MenuItem>
          </Menu>
        </Popover>
      </div>

      {shown.length && groups ? (
        <div className={styles.groups}>
          {groups.map(([s, list]) => (
            <PackCategory key={s.id} variant="plain" icon={s.kind === 'pack' ? 'pack' : undefined} title={s.name} count={list.length} open={!closed.has(s.id)} onToggle={() => flip(s.id)}>
              {tiles(list)}
            </PackCategory>
          ))}
        </div>
      ) : shown.length ? (
        tiles(shown)
      ) : (
        <p className={styles.none}>Nothing matches.</p>
      )}

      {selecting && (
        <div className={styles.dock}>
          <div className={styles.sel} role="toolbar" aria-label="Selection">
            <span className={styles.count}><b><RollingNumber value={picked.size} /></b> selected</span>
            <Button variant="text" size="sm" onClick={() => setPicked(allPicked ? new Set() : new Set(shown.map((i) => i.id)))}>{allPicked ? 'Clear' : 'Select all'}</Button>
            <Button variant="primary" size="md" icon="export" disabled={!picked.size} onClick={() => onExportAsPack?.([...picked])}>Export as pack</Button>
          </div>
        </div>
      )}
    </div>
  );
}
