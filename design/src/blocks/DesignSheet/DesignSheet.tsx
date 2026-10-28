import { useMemo, useState } from 'react';
import {
  COVER_COLS, COVER_TEX, FINISHES, HUES, QUICK_COVERS, QUICK_FOLDERS, SHADES, DesignArt, TileArtView, coverFrom, folderFrom, texArt,
  type Design, type TileArt,
} from '@/assets/studio';
import { buildSampleContent, sampleSources, type ContentItem, type ContentSource } from '@/assets/content';
import { PresetTile } from '@/components/PresetTile';
import { SegmentedControl } from '@/components/SegmentedControl';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { cx } from '@/lib/cx';
import { ContentArt } from '../ContentPage';
import { ColourPane } from './ColourPane';
import { IconPane } from './IconPane';
import { LocationField, NameField, QuickSheet } from '../QuickSheet';
import styles from './DesignSheet.module.css';

export interface DesignChoice {
  name: string;
  hex: string;
  /** Finish of a folder, or texture of a notebook cover. null is plain. */
  finish: string | null;
  icon: string | null;
  where?: string;
  /** The quiet line under the name in the library. */
  meta?: string;
  /** Notebooks: page size and the template new pages start from. */
  pageSize?: string;
  template?: string;
}

export interface DesignSheetProps {
  kind: 'folder' | 'cover';
  open: boolean;
  onClose: () => void;
  mode: 'new' | 'edit';
  /** What the sheet starts from. New starts on the first preset. */
  initial?: DesignChoice;
  initialTab?: string;
  /** The name a new item starts with. */
  defaultName?: string;
  onCreate?: (choice: DesignChoice, design: Design) => void;
  onDelete?: () => void;
  /** Open this look in the studio. */
  onCustomise?: (design: Design) => void;
  /** Where it can go: the library and its folders. */
  locations?: string[];
  /** Where a new one goes, such as the folder you are in. */
  defaultWhere?: string;
  placement?: OverlayPlacement;
}

const PRESETS = { folder: QUICK_FOLDERS, cover: QUICK_COVERS };
const TABS = {
  folder: [{ value: 'presets', label: 'Presets' }, { value: 'colour', label: 'Colour' }, { value: 'icon', label: 'Icon' }, { value: 'finish', label: 'Finish' }],
  cover: [{ value: 'presets', label: 'Presets' }, { value: 'colour', label: 'Colour' }, { value: 'finish', label: 'Texture' }, { value: 'icon', label: 'Icon' }, { value: 'paper', label: 'Paper' }],
};
const PAGE_SIZES = ['A4', 'A5', 'Letter'].map((value) => ({ value, label: value }));

/** Page templates by source: built in first, then packs, then the user's own. */
const templateGroups = (items: ContentItem[], sources: ContentSource[]) => {
  const order = [...sources.filter((x) => x.kind === 'built'), ...sources.filter((x) => x.kind === 'pack' && x.enabled), ...sources.filter((x) => x.kind === 'own')];
  return order.map((src) => [src, items.filter((i) => i.type === 'templates' && i.source === src.id)] as const).filter(([, list]) => list.length);
};
let templates: ReturnType<typeof templateGroups> | null = null;
const sampleTemplates = () => (templates ??= templateGroups(buildSampleContent(), sampleSources));

/** The colours of the Colour tab, in reading order. */
const colourList = (kind: 'folder' | 'cover') =>
  kind === 'folder'
    ? SHADES.flatMap(([sn, fn]) => HUES.map(([hn, h]) => ({ hex: fn(h), label: `${hn} ${sn}` })))
    : COVER_COLS.map((hex) => ({ hex, label: hex }));

export const make = (kind: 'folder' | 'cover', c: DesignChoice, name = c.name): Design => {
  const nm = name || (kind === 'folder' ? 'Untitled folder' : 'Untitled');
  const d = kind === 'folder'
    ? folderFrom(c.hex, { finish: c.finish, icon: c.icon, name: nm })
    : coverFrom(c.hex, { tex: c.finish, name: nm, icon: c.icon, cover: { spine: true } });
  d.meta = c.meta ?? (kind === 'folder' ? 'Empty' : 'Notebook · 24 pages');
  return d;
};

/** The sheet behind both the Folder and the Notebook sheet: presets, colour, finish or texture, and icon, with a live preview. */
export function DesignSheet({ kind, open, onClose, mode, initial, initialTab = 'presets', defaultName, onCreate, onDelete, onCustomise, locations, defaultWhere, placement }: DesignSheetProps) {
  const presets = PRESETS[kind];
  const first: DesignChoice = { name: defaultName ?? (kind === 'folder' ? 'Semester 2' : 'Chemistry'), hex: presets[0][1], finish: presets[0][2], icon: null, where: defaultWhere };
  const [c, setC] = useState<DesignChoice>(() => ({ pageSize: 'A5', template: 'Ruled', ...(initial ?? first) }));
  const [sel, setSel] = useState(() => (initial ? presets.findIndex((p) => p[1].toLowerCase() === initial.hex.toLowerCase() && (p[2] || null) === (initial.finish || null)) : 0));
  const [custom, setCustom] = useState<string | null>(null);
  const [tab, setTab] = useState(initialTab);
  const [bump, setBump] = useState(0);
  const isF = kind === 'folder';

  const patch = (p: Partial<DesignChoice>, keepPreset = false) => { setC((x) => ({ ...x, ...p })); if (!keepPreset) setSel(-1); setBump((b) => b + 1); };
  const design = useMemo(() => make(kind, c), [kind, c]);
  const nameOf = c.name;

  const tileArt = (k: string | null): TileArt => (!k || k === 'plain' ? { t: 'swatch', base: c.hex } : texArt(k as never, c.hex));
  const finishes = isF ? FINISHES : COVER_TEX;
  const finishOn = c.finish || 'plain';

  const panes: Record<string, React.ReactNode> = {
    presets: (
      <div className={styles.grid}>
        {presets.map((p, i) => (
          <PresetTile key={p[0]} shape={kind} label={p[0]} selected={i === sel}
            onClick={() => { setC((x) => ({ ...x, hex: p[1], finish: p[2] })); setSel(i); setCustom(null); setBump((b) => b + 1); }}
            preview={<div className={isF ? styles.fo : styles.cv}><DesignArt design={make(kind, { ...c, hex: p[1], finish: p[2], icon: null }, nameOf)} /></div>} />
        ))}
      </div>
    ),
    colour: <ColourPane colours={colourList(kind)} columns={isF ? 9 : 7} rows={isF ? 3 : 2} value={c.hex} custom={custom} onPick={(hex) => { setCustom(null); patch({ hex }); }} onCustom={(hex) => { setCustom(hex); patch({ hex }); }} />,
    icon: <IconPane value={c.icon} onChange={(icon) => patch({ icon }, true)} />,
    paper: (
      <div className={styles.paper}>
        <div className={styles.field}><span>Page size</span><SegmentedControl fit aria-label="Page size" options={PAGE_SIZES} value={c.pageSize ?? 'A5'} onChange={(pageSize) => setC((x) => ({ ...x, pageSize }))} /></div>
        {sampleTemplates().map(([src, list]) => (
          <section key={src.id} className={styles.tplGroup} aria-label={src.name}>
            <h4>{src.name}</h4>
            <div className={styles.tpl}>
              {list.map((t) => (
                <PresetTile key={t.id} variant="swatch" slotHeight={92} label={t.name} selected={t.name === c.template}
                  onClick={() => setC((x) => ({ ...x, template: t.name }))}
                  preview={<span className={styles.page}><ContentArt item={t} /></span>} />
              ))}
            </div>
          </section>
        ))}
      </div>
    ),
    finish: (
      <div className={cx(styles.fin, styles.fin3)}>
        {finishes.map(([k, n]) => (
          <PresetTile key={k} variant="swatch" slotHeight={84} label={n} selected={k === finishOn} onClick={() => patch({ finish: k === 'plain' ? null : k })}
            preview={<TileArtView art={tileArt(k)} />} />
        ))}
      </div>
    ),
  };

  return (
    <QuickSheet
      open={open} onClose={onClose} kind={kind} mode={mode} placement={placement} tabs={TABS[kind]} tab={tab} onTab={setTab} panes={panes[tab]}
      title={`${mode === 'edit' ? 'Edit' : 'New'} ${isF ? 'folder' : 'notebook'}`}
      createLabel={mode === 'edit' ? 'Save' : isF ? 'Create folder' : 'Create notebook'}
      onCreate={() => onCreate?.(c, make(kind, c))} onDelete={onDelete} onCustomise={onCustomise ? () => onCustomise(make(kind, c)) : undefined}
      preview={<div key={bump} className={cx(isF ? styles.pvF : styles.pvC, styles.bump)}><DesignArt design={design} /></div>}
      fields={
        <>
          <NameField value={c.name} onChange={(name) => setC((x) => ({ ...x, name }))} />
          <LocationField value={c.where ?? 'Library'} options={locations} onChange={(where) => setC((x) => ({ ...x, where }))} />
          {!isF && <p className={styles.paperLine}>{`${c.pageSize ?? 'A5'}, ${c.template ?? 'Ruled'}`}</p>}
        </>
      }
    />
  );
}
