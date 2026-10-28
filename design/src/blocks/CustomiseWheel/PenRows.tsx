import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Checkbox } from '@/components/Checkbox';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { ReorderList } from '@/components/ReorderList';
import { IconButton } from '@/components/IconButton';
import { SegmentedControl } from '@/components/SegmentedControl';
import { TextField } from '@/components/TextField';
import { hexToRgba } from '@/blocks/PenWheel/engine/math';
import { ALPHA_MAX, ALPHA_MIN, HL_RANGE, PEN_RANGE, numberOfWidth, widthOfNumber } from '@/blocks/PenWheel/engine/geometry';
import type { PenPreset, WheelModel } from '@/blocks/PenWheel/engine/model';
import { ColourBar } from '@/blocks/ColourPicker';
import { PenGlyphFor } from '@/blocks/PenList';
import { LineTiles } from '@/blocks/WheelPanel';
import { clamp } from '@/blocks/PenWheel/engine/math';
import { ColourField, SizeDots } from './PenEditorParts';
import styles from './CustomiseWheel.module.css';

const pt1 = (n: number) => (Math.round(n * 10) / 10).toFixed(1);

function Editor({ pen, palette, onChange }: { pen: PenPreset; palette: string[]; onChange: (p: PenPreset) => void }) {
  const hl = pen.kind === 'highlighter';
  const n = numberOfWidth(hl, pen.w);
  return (
    <div className={styles.editor}>
      <TextField variant="inline" label="Pen name" maxLength={24} value={pen.name} onChange={(e) => onChange({ ...pen, name: e.target.value })} />
      <SegmentedControl
        size="sm"
        fit
        aria-label="Kind"
        options={[{ value: 'pen', label: 'Pen' }, { value: 'highlighter', label: 'Highlighter' }]}
        value={pen.kind}
        onChange={(k) => {
          if (k === pen.kind) return;
          const [a, b] = k === 'highlighter' ? HL_RANGE : PEN_RANGE;
          onChange({ ...pen, kind: k as PenPreset['kind'], w: clamp(pen.w, a, b), alpha: k === 'highlighter' ? 115 : 255 });
        }}
      />
      <div className={styles.cap}>{`Size ${n} (${pt1(pen.w)} pt)`}</div>
      <SizeDots value={n} onChange={(v) => onChange({ ...pen, w: widthOfNumber(hl, v) })} />
      <div className={styles.cap}>Line</div>
      <LineTiles value={pen.line} onChange={(line) => onChange({ ...pen, line })} />
      <div className={styles.cap}>{`Opacity ${Math.round((pen.alpha * 100) / ALPHA_MAX)}%`}</div>
      <ColourBar
        kind="opacity"
        label="Opacity"
        value={(pen.alpha - ALPHA_MIN) / (ALPHA_MAX - ALPHA_MIN)}
        gradient={`linear-gradient(90deg,${hexToRgba(pen.color, 0.1)},${pen.color})`}
        onChange={(x) => onChange({ ...pen, alpha: Math.round(ALPHA_MIN + x * (ALPHA_MAX - ALPHA_MIN)) })}
      />
      <div className={styles.cap}>Colour</div>
      <ColourField color={pen.color} palette={palette} onChange={(color) => onChange({ ...pen, color })} />
    </div>
  );
}

function Row({ pen, handle, open, palette, picked, onToggle, onChange, onDelete }: {
  pen: PenPreset; handle: ReactNode; open: boolean; palette: string[];
  /** Set while choosing pens to share: the row ticks instead of opening. */
  picked?: boolean;
  onToggle: () => void; onChange: (p: PenPreset) => void; onDelete: () => void;
}) {
  const choosing = picked !== undefined;
  const [grown, setGrown] = useState(false);
  useEffect(() => {
    if (!open) { setGrown(false); return; }
    let b = 0;
    const a = requestAnimationFrame(() => { b = requestAnimationFrame(() => setGrown(true)); });
    return () => { cancelAnimationFrame(a); cancelAnimationFrame(b); };
  }, [open]);
  return (
    <div className={styles.row} data-open={open || undefined} data-grown={grown || undefined}>
      <div className={styles.rowHead} role="button" tabIndex={0} aria-expanded={choosing ? undefined : open} aria-pressed={choosing ? picked : undefined}
        aria-label={choosing ? pen.name : `Edit ${pen.name}`}
        onClick={onToggle} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onToggle(); } }}>
        <span onClick={(e) => e.stopPropagation()} className={styles.rowGrip}>{handle}</span>
        <PenGlyphFor className={styles.rowGlyph} pen={pen} />
        <b>{pen.name}</b>
        {choosing ? (
          <span className={styles.rowPick}><Checkbox tabIndex={-1} aria-hidden checked={picked} onClick={(e) => e.preventDefault()} /></span>
        ) : (
          <span onClick={(e) => e.stopPropagation()} className={styles.rowTools}>
            <IconButton icon="trash" label={`Delete ${pen.name}`} onClick={onDelete} />
          </span>
        )}
      </div>
      <div className={styles.edWrap}>{open && <Editor pen={pen} palette={palette} onChange={onChange} />}</div>
    </div>
  );
}

/** Stable ids for pens that have none: a pen keeps its id when it is moved or edited. */
function usePenIds() {
  const ids = useRef(new WeakMap<PenPreset, string>());
  const next = useRef(0);
  const idOf = (p: PenPreset) => {
    let id = ids.current.get(p);
    if (!id) { id = `pen${next.current++}`; ids.current.set(p, id); }
    return id;
  };
  const carry = (from: PenPreset, to: PenPreset) => { ids.current.set(to, idOf(from)); };
  return { idOf, carry };
}

export function PenRows({ model, open, onOpen, onUndoable, onShare, onImport }: {
  model: WheelModel; open: string | null; onOpen: (id: string | null) => void; onUndoable?: (text: string, undo: () => void) => void;
  onShare?: (pens: PenPreset[]) => void; onImport?: () => void;
}) {
  const pens = model.pens;
  const { idOf, carry } = usePenIds();
  const [picked, setPicked] = useState<Set<string> | null>(null);
  const [menu, setMenu] = useState(false);
  const more = useRef<HTMLSpanElement>(null);
  const flip = (id: string) => setPicked((p) => { const n = new Set(p); if (n.has(id)) n.delete(id); else n.add(id); return n; });
  const palette = model.rim.filter((c): c is string => !!c);
  return (
    <div>
      <ReorderList<PenPreset>
        items={pens}
        getId={idOf}
        getLabel={(p) => p.name}
        itemClassName={styles.penItem}
        onChange={(next) => model.setPens(next)}
        renderItem={(pen, { handle, index }) => {
          const id = idOf(pen);
          return (
            <Row
              pen={pen}
              handle={handle}
              open={!picked && open === id}
              palette={palette}
              picked={picked ? picked.has(id) : undefined}
              onToggle={() => (picked ? flip(id) : onOpen(open === id ? null : id))}
              onChange={(p) => { carry(pen, p); model.setPens(pens.map((x) => (x === pen ? p : x))); }}
              onDelete={() => {
                model.setPens(pens.filter((x) => x !== pen));
                if (open === id) onOpen(null);
                onUndoable?.('Pen deleted', () => model.setPens(model.pens.slice(0, index).concat(pen, model.pens.slice(index))));
              }}
            />
          );
        }}
      />
      {picked ? (
        <div className={styles.share}>
          <Button variant="text" size="sm" onClick={() => setPicked(null)}>Cancel</Button>
          <Button variant="primary" size="sm" icon="export" disabled={!picked.size}
            onClick={() => { onShare?.(pens.filter((p) => picked.has(idOf(p)))); setPicked(null); }}>
            {picked.size ? `Share ${picked.size} as pack` : 'Share as pack'}
          </Button>
        </div>
      ) : (
        <div className={styles.addRow}>
          <Button block className={styles.add} onClick={() => model.saveCurrentPen()}>
            <Icon name="plus" size={18} />
            Save current pen
          </Button>
          <span ref={more} className={styles.addMore}>
            <IconButton icon="more" label="More" variant="ghost" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)} />
          </span>
          <Popover open={menu} onClose={() => setMenu(false)} anchorRef={more} placement="top-end" kind="menu">
            <Menu label="More" onClose={() => setMenu(false)}>
              <MenuItem icon="export" onSelect={() => { onOpen(null); setPicked(new Set()); }}>Share as pack</MenuItem>
              <MenuItem icon="import" onSelect={() => onImport?.()}>Import pack</MenuItem>
            </Menu>
          </Popover>
        </div>
      )}
    </div>
  );
}
