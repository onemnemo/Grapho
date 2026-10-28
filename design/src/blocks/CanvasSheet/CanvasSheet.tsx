import { useMemo, useState } from 'react';
import { Icon } from '@/assets/icons';
import { CanvasPreview } from '@/assets/covers/CanvasPreview';
import {
  CV_COLS, CV_FIXED, CV_GAPS, CV_NEW, CV_PRESETS, CV_TPL, cvDims, cvMeta,
  type CanvasGap, type CanvasPaper, type CanvasPattern, type CanvasSettings,
} from '@/assets/studio';
import { ChoiceCard } from '@/components/ChoiceCard';
import { Chip } from '@/components/Chip';
import { PresetTile } from '@/components/PresetTile';
import { SegmentedControl } from '@/components/SegmentedControl';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { ColourPane } from '../DesignSheet';
import { LocationField, NameField, QuickSheet } from '../QuickSheet';
import styles from './CanvasSheet.module.css';

export interface CanvasSheetProps {
  open: boolean;
  onClose: () => void;
  mode: 'new' | 'edit';
  /** What the sheet starts from. New starts on the Dot grid preset. */
  initial?: CanvasSettings;
  initialTab?: 'presets' | 'colour' | 'pattern' | 'size';
  onCreate?: (canvas: CanvasSettings) => void;
  onDelete?: () => void;
  /** Where it can go: the library and its folders. */
  locations?: string[];
  /** Where a new one goes, such as the folder you are in. */
  defaultWhere?: string;
  placement?: OverlayPlacement;
}

const TABS = [{ value: 'presets', label: 'Presets' }, { value: 'colour', label: 'Background' }, { value: 'pattern', label: 'Pattern' }, { value: 'size', label: 'Size' }];
const COLOURS = CV_COLS.flatMap(([group, list]) => list.map((hex) => ({ hex, label: `${group} ${hex}` })));
const ORIENT = [{ value: 'land', label: 'Landscape' }, { value: 'port', label: 'Portrait' }];
const GAPS = CV_GAPS.map(([value, label]) => ({ value, label }));

/** New canvas and Edit canvas: one sheet. A canvas is a background, a pattern and a size, drawn live. */
export function CanvasSheet({ open, onClose, mode, initial, initialTab = 'presets', onCreate, onDelete, locations, defaultWhere, placement }: CanvasSheetProps) {
  const [c, setC] = useState<CanvasSettings>(initial ?? { ...CV_NEW, where: defaultWhere });
  const [sel, setSel] = useState(() => (initial ? CV_PRESETS.findIndex((p) => p[1].toLowerCase() === initial.bg.toLowerCase() && p[2] === initial.tpl) : 1));
  const [custom, setCustom] = useState<string | null>(null);
  const [tab, setTab] = useState<string>(initialTab);
  const [bump, setBump] = useState(0);

  const patch = (p: Partial<CanvasSettings>, keepPreset = false) => { setC((x) => ({ ...x, ...p })); if (!keepPreset) setSel(-1); setBump((b) => b + 1); };
  const meta = cvMeta(c);
  const [pw, ph] = useMemo(() => cvDims(c, 210), [c]);
  const blank = c.tpl === 'blank';

  const panes: Record<string, React.ReactNode> = {
    presets: (
      <>
        <div className={styles.grid}>
          {CV_PRESETS.map(([name, bg, tpl], i) => (
            <PresetTile key={name} shape="canvas" label={name} selected={i === sel}
              onClick={() => { setC((x) => ({ ...x, bg, tpl })); setSel(i); setCustom(null); setBump((b) => b + 1); }}
              preview={<CanvasPreview settings={{ bg, tpl, gap: 'M', size: 'inf' }} width={96} height={68} plain flat />} />
          ))}
        </div>
      </>
    ),
    colour: <ColourPane colours={COLOURS} columns={7} rows={2} value={c.bg} custom={custom} onPick={(bg) => { setCustom(null); patch({ bg }); }} onCustom={(bg) => { setCustom(bg); patch({ bg }); }} />,
    pattern: (
      <>
        <div className={styles.pats}>
          {CV_TPL.map(([k, n]) => (
            <PresetTile key={k} variant="swatch" bare slotHeight={68} label={n} selected={k === c.tpl} onClick={() => patch({ tpl: k as CanvasPattern })}
              preview={<CanvasPreview settings={{ bg: c.bg, tpl: k, gap: c.gap, size: 'inf' }} width={84} height={62} plain flat />} />
          ))}
        </div>
        <div className={styles.label}>Spacing</div>
        <div className={blank ? styles.off : undefined}>
          <SegmentedControl fit aria-label="Spacing" options={GAPS} value={c.gap} onChange={(gap) => patch({ gap: gap as CanvasGap })} />
        </div>
      </>
    ),
    size: (
      <>
        <div className={styles.sizes}>
          <ChoiceCard size="sm" title="Infinite" selected={c.size === 'inf'} onClick={() => patch({ size: 'inf' }, true)}
            preview={<span className={styles.inf}><b><Icon name="infinity" size={20} strokeWidth={2.2} /></b></span>} />
          <ChoiceCard size="sm" title="Fixed" selected={c.size === 'fixed'} onClick={() => patch({ size: 'fixed' }, true)}
            preview={<span className={styles.fix} />} />
        </div>
        {c.size === 'fixed' ? (
          <div className={styles.fixed}>
            <div className={styles.label}>Size</div>
            <div className={styles.chips}>
              {Object.entries(CV_FIXED).map(([k, [n]]) => (
                <Chip key={k} selected={k === c.fixed} onChange={() => patch({ fixed: k as CanvasPaper }, true)}>{n}</Chip>
              ))}
            </div>
            <div className={styles.orient}>
              <SegmentedControl fit aria-label="Orientation" options={ORIENT} value={c.orient} onChange={(orient) => patch({ orient: orient as 'land' | 'port' }, true)} />
            </div>
          </div>
        ) : null}
      </>
    ),
  };

  return (
    <QuickSheet
      open={open} onClose={onClose} kind="canvas" mode={mode} placement={placement} tabs={TABS} tab={tab} onTab={setTab} panes={panes[tab]}
      title={`${mode === 'edit' ? 'Edit' : 'New'} canvas`} createLabel={mode === 'edit' ? 'Save' : 'Create canvas'}
      onCreate={() => onCreate?.({ ...c, meta })} onDelete={onDelete}
      preview={<div key={bump} className={styles.bump}><CanvasPreview settings={c} width={pw} height={ph} /></div>}
      fields={
        <>
          <NameField value={c.name} onChange={(name) => setC((x) => ({ ...x, name }))} />
          <LocationField value={c.where ?? 'Library'} options={locations} onChange={(where) => setC((x) => ({ ...x, where }))} />
        </>
      }
    />
  );
}
