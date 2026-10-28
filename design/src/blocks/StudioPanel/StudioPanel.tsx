import { useState, type ReactNode } from 'react';
import {
  CARDS, FONTS, SUBJECT_ICONS, TEXT_STYLES, TOOLS, PACKDEF, PACK_NOUN, allPackItems, clipItems, packArt, packItems, photoItems,
  type PackedTool, type PanelItem, type ToolKey,
} from '@/assets/studio';
import { IconButton } from '@/components/IconButton';
import { SearchField } from '@/components/SearchField';
import { SegmentedControl } from '@/components/SegmentedControl';
import { cx } from '@/lib/cx';
import { PackCategory } from '../PackCategory';
import { PackTile } from '../PackTile';
import { StickerTile } from '../StickerTile';
import { PackActions, Note, Sub } from './parts';
import { ActionRow } from '@/components/ActionRow';
import styles from './StudioPanel.module.css';

export type PanelImport = 'image' | 'svg' | 'video' | 'pack';
type Bind = (spec: string) => Record<string, unknown>;

export interface StudioPanelProps {
  tool: ToolKey;
  /** Whether the studio is on a cover or a folder. */
  kind?: 'cover' | 'folder';
  /** Open a pack straight away, for stories and for coming back to the last one. */
  initialPack?: string;
  initialQuery?: string;
  iconStyle?: 'line' | 'soft';
  onIconStyle?: (s: 'line' | 'soft') => void;
  /** Handlers from useDragIn for each tile. Without them a tile adds on click. */
  bind?: Bind;
  onAdd?: (spec: string) => void;
  onImport?: (what: PanelImport) => void;
  /** Opens Settings on Packs. */
  onGetPacks?: () => void;
  className?: string;
}

const isPacked = (t: ToolKey): t is PackedTool => t === 'stickers' || t === 'shapes' || t === 'textures';
const GRID: Record<PackedTool, string> = { stickers: 'g3', shapes: 'g4', textures: 'g2' };

/** The left panel: the tool's name, a search that stays put, packs as tiles, categories inside a pack, and a way to get more packs. */
export function StudioPanel({ tool, initialPack, initialQuery = '', iconStyle: styleProp, onIconStyle, bind, onAdd, onImport, onGetPacks, className }: StudioPanelProps) {
  const [pack, setPack] = useState<Record<string, string | null>>(initialPack ? { [tool]: initialPack } : {});
  const [query, setQuery] = useState<Record<string, string>>(initialQuery ? { [tool]: initialQuery } : {});
  const [open, setOpen] = useState<Record<string, string[]>>({});
  const [iconQ, setIconQ] = useState('');
  const [iconInner, setIconInner] = useState<'line' | 'soft'>('line');
  const iconStyle = styleProp ?? iconInner;
  const name = TOOLS.find((t) => t[0] === tool)?.[1] ?? '';

  const tile = (it: PanelItem) => (
    <StickerTile key={it.spec} art={it.art} name={it.name} size={it.size} caption={it.cell} {...(bind ? bind(it.spec) : { onClick: () => onAdd?.(it.spec) })} />
  );

  let head: ReactNode = <h3>{name}</h3>;
  let pinned: ReactNode = null;
  let body: ReactNode = null;

  if (isPacked(tool)) {
    const cur = pack[tool] ?? null;
    const q = (query[tool] ?? '').trim().toLowerCase();
    const defs = PACKDEF[tool];
    const noun = PACK_NOUN[tool];
    const set = (v: string) => setQuery((s) => ({ ...s, [tool]: v }));
    if (!cur) {
      pinned = <SearchField size="sm" width={208} focusWidth={208} placeholder={`Search ${noun}`} value={query[tool] ?? ''} onChange={set} />;
      if (q) {
        const hits = defs.flatMap((d) => allPackItems(tool, d.id)).filter((it) => it.name.toLowerCase().includes(q));
        body = <><div className={cx(styles.grid, styles[GRID[tool]])}>{hits.map(tile)}</div>{!hits.length && <Note>Nothing matches in your packs.</Note>}</>;
      } else {
        body = (
          <>
            <div className={styles.packs}>
              {defs.map((d) => (
                <PackTile key={d.id} name={d.name} detail={`${allPackItems(tool, d.id).length} ${noun}`} art={packArt(tool, d.id)} onClick={() => { setPack((s) => ({ ...s, [tool]: d.id })); set(''); }} />
              ))}
            </div>
            <PackActions onGet={onGetPacks} onImport={() => onImport?.('pack')} />
          </>
        );
      }
    } else {
      const d = defs.find((x) => x.id === cur) ?? defs[0];
      head = (
        <>
          <IconButton className={styles.back} size="sm" icon="chevron_left" label={`Back to ${name}`} onClick={() => { setPack((s) => ({ ...s, [tool]: null })); set(''); }} />
          <h3>{d.name}</h3>
        </>
      );
      pinned = <SearchField size="sm" width={208} focusWidth={208} placeholder={`Search ${d.name}`} value={query[tool] ?? ''} onChange={set} />;
      const key = `${tool}:${d.id}`;
      const openSet = open[key] ?? [d.tabs[0][0]];
      const toggle = (k: string) => setOpen((s) => ({ ...s, [key]: openSet.includes(k) ? openSet.filter((x) => x !== k) : [...openSet, k] }));
      const sections = d.tabs.map(([k, title]) => {
        const all = packItems(tool, d.id, k);
        const its = all.filter((it) => !q || it.name.toLowerCase().includes(q));
        if (q && !its.length) return null;
        return (
          <PackCategory key={k} title={title} count={its.length} open={q ? true : openSet.includes(k)} onToggle={() => toggle(k)} previews={all.filter((i) => !i.cell).map((i) => i.art)}>
            <div className={cx(styles.grid, styles[GRID[tool]])}>{its.map(tile)}</div>
          </PackCategory>
        );
      });
      body = (
        <>
          <div className={styles.cats}>{sections}</div>
          {sections.every((s) => !s) && <Note>Nothing matches in this pack.</Note>}
          <PackActions onGet={onGetPacks} getLabel="Manage packs" />
        </>
      );
    }
  } else if (tool === 'images') {
    body = (
      <>
        <ActionRow icon="upload" onClick={() => onImport?.('image')}>Add from device</ActionRow>
        <Sub>Photos</Sub>
        <div className={cx(styles.grid, styles.g2)}>{photoItems().map(tile)}</div>
      </>
    );
  } else if (tool === 'text') {
    body = (
      <>
        <div className={styles.list}>
          {TEXT_STYLES.map(([k, label, s]) => (
            <button key={k} type="button" className={styles.textRow} {...(bind ? bind('tx:' + k) : { onClick: () => onAdd?.('tx:' + k) })}>
              <span style={{ font: `${s.weight} ${s.size}px/1 ${FONTS[s.font][0]}`, letterSpacing: s.ls }}>{label}</span>
            </button>
          ))}
        </div>
      </>
    );
  } else if (tool === 'cards') {
    body = <div className={cx(styles.grid, styles.g2)}>{CARDS.map((c) => <StickerTile key={c[0]} name={c[1]} art={{ t: 'card', def: c }} {...(bind ? bind('cd:' + c[0]) : { onClick: () => onAdd?.('cd:' + c[0]) })} />)}</div>;
  } else if (tool === 'icons') {
    const list = SUBJECT_ICONS.filter((n) => n.includes(iconQ.toLowerCase()));
    pinned = <SearchField size="sm" width={208} focusWidth={208} placeholder="Search icons" value={iconQ} onChange={setIconQ} />;
    body = (
      <>
        <SegmentedControl size="xs" fit aria-label="Icon style" className={styles.styleSeg} value={iconStyle} onChange={(v) => { setIconInner(v as 'line' | 'soft'); onIconStyle?.(v as 'line' | 'soft'); }}
          options={[{ value: 'line', label: 'Line' }, { value: 'soft', label: 'Soft' }]} />
        <div className={cx(styles.grid, styles.g5)}>
          {list.map((n) => <StickerTile key={n} name={n} size="sm" art={{ t: 'icon', name: n, colour: 'var(--fg)', soft: iconStyle === 'soft' }} {...(bind ? bind('ic:' + n) : { onClick: () => onAdd?.('ic:' + n) })} />)}
        </div>
        <ActionRow icon="import" className={styles.below} onClick={() => onImport?.('svg')}>Import SVG</ActionRow>
        {!list.length && <Note>No icons match.</Note>}
      </>
    );
  } else {
    body = (
      <>
        <ActionRow icon="upload" onClick={() => onImport?.('video')}>Add from device</ActionRow>
        <Sub>Clips</Sub>
        <div className={cx(styles.grid, styles.g2)}>
          {clipItems().map(tile)}
        </div>
      </>
    );
  }

  return (
    <aside className={cx(styles.panel, className)} aria-label={name}>
      <div className={cx(styles.head, pack[tool] && isPacked(tool) && styles.hasBack)}>{head}</div>
      {pinned && <div className={styles.pinned}>{pinned}</div>}
      <div className={styles.body}>{body}</div>
    </aside>
  );
}
