import { useEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from 'react';
import { Icon } from '@/assets/icons';
import { PenGlyph } from '@/assets/wheel/PenGlyph';
import { Button } from '@/components/Button';
import { numberOfWidth } from '@/blocks/PenWheel/engine/geometry';
import { hexToRgba, clamp, tick } from '@/blocks/PenWheel/engine/math';
import type { Line, PenPreset, WheelModel } from '@/blocks/PenWheel/engine/model';
import { useWheelModel } from '@/blocks/PenWheel/useWheelModel';
import { LineTiles, WheelPanel } from '@/blocks/WheelPanel';
import { cx } from '@/lib/cx';
import styles from './PenList.module.css';

export interface PenListProps {
  model: WheelModel;
  /** Open the customise panel on its Pens tab. */
  onEdit?: () => void;
  /** A pen was chosen; the list normally closes. */
  onPicked?: (pen: PenPreset) => void;
  /** A pen was deleted from the list. Show an undo for it. */
  onDeleted?: (pen: PenPreset, index: number) => void;
  /** Height cap in dp. */
  maxHeight?: number;
  /** Which edge the wheel sits on, so the list grows from the right side. */
  edge?: 'left' | 'right' | 'top' | 'bottom' | 'free';
  state?: 'open' | 'leave';
  className?: string;
}

const ORIGIN = { left: '0 50%', free: '0 50%', right: '100% 50%', top: '50% 0', bottom: '50% 100%' } as const;

/** A sample stroke in the pen's own ink, on paper. */
export function Squiggle({ color, alpha, width, line }: { color: string; alpha: number; width: number; line: Line }) {
  const sw = clamp(width * 1.5, 1.5, 8);
  const pad = sw / 2 + 5;
  const W = 150;
  const H = 22;
  let d = '';
  for (let i = 0; i <= 32; i++) {
    const t = i / 32;
    const x = pad + t * (W - 2 * pad);
    const y = H / 2 + Math.sin(t * 4 * Math.PI) * (((H - sw) / 2) * 0.7);
    d += (i ? 'L' : 'M') + x.toFixed(1) + ' ' + y.toFixed(1);
  }
  const dash = line === 'dashed' ? `${(3 * sw).toFixed(1)} ${(4 * sw).toFixed(1)}` : line === 'dotted' ? `0.01 ${(2.5 * sw).toFixed(1)}` : undefined;
  return (
    <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" width="100%" height="22" aria-hidden="true">
      <path d={d} fill="none" stroke={color} strokeOpacity={(alpha / 255).toFixed(2)} strokeWidth={sw.toFixed(1)} strokeLinecap="round" strokeLinejoin="round" strokeDasharray={dash} />
    </svg>
  );
}

/** The pen drawn in its own ink and size, as in the lists. */
export function PenGlyphFor({ pen, className }: { pen: Pick<PenPreset, 'kind' | 'color' | 'alpha' | 'w'>; className?: string }) {
  const t = clamp((numberOfWidth(pen.kind === 'highlighter', pen.w) - 1) / 99, 0, 1);
  return (
    <span className={cx(styles.glyph, className)} style={{ '--ink': hexToRgba(pen.color, pen.alpha / 255) } as CSSProperties}>
      <PenGlyph kind={pen.kind} size={t} />
    </span>
  );
}

function Card({ pen, index, current, deleting, onPick, onHold, onDelete }: {
  pen: PenPreset; index: number; current: boolean; deleting: boolean; onPick: () => void; onHold: () => void; onDelete: () => void;
}) {
  const timer = useRef(0);
  const held = useRef(false);
  const cancel = () => window.clearTimeout(timer.current);
  const key = (e: KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onPick();
    }
  };
  return (
    <div
      className={styles.card}
      role="button"
      tabIndex={0}
      aria-label={pen.name}
      aria-current={current || undefined}
      data-deleting={deleting || undefined}
      style={{ '--n': Math.min(index, 8) } as CSSProperties}
      onKeyDown={key}
      onPointerDown={() => {
        held.current = false;
        timer.current = window.setTimeout(() => { held.current = true; onHold(); tick(12); }, 520);
      }}
      onPointerUp={cancel}
      onPointerLeave={cancel}
      onPointerCancel={cancel}
      onClick={(e) => {
        if ((e.target as Element).closest('[data-delete]')) { onDelete(); return; }
        if (held.current) { held.current = false; return; }
        if (deleting) { onHold(); return; }
        onPick();
      }}
    >
      <PenGlyphFor pen={pen} />
      <div className={styles.body}>
        <b>{pen.name}</b>
        <div className={styles.strip}><Squiggle color={pen.color} alpha={pen.alpha} width={pen.w} line={pen.line} /></div>
      </div>
      <i className={styles.check}><Icon name="check" size={14} /></i>
      <Button className={styles.del} data-delete variant="danger" size="sm" tabIndex={deleting ? 0 : -1}>Delete</Button>
    </div>
  );
}

/** Your pens, beside the wheel. Tap one to use it; hold one to delete it. */
export function PenList({ model, onEdit, onPicked, onDeleted, maxHeight, edge = 'left', state = 'open', className }: PenListProps) {
  useWheelModel(model);
  const [deleting, setDeleting] = useState<number | null>(null);
  const list = useRef<HTMLDivElement>(null);
  const count = useRef(model.pens.length);
  useEffect(() => {
    if (model.pens.length > count.current && list.current) list.current.scrollTop = list.current.scrollHeight;
    count.current = model.pens.length;
  }, [model.pens.length]);
  const cur = model.currentPen();
  return (
    <WheelPanel width={260} state={state} origin={ORIGIN[edge]} className={cx(styles.panel, className)} style={maxHeight ? { maxHeight } : undefined} aria-label="Pens">
      <div className={styles.head}>
        <b>Pens</b>
        <Button size="sm" onClick={onEdit}>Edit</Button>
      </div>
      <LineTiles inPanel value={model.setting.line} onChange={(l) => model.applyLine(l)} />
      <div ref={list} className={styles.cards}>
        {model.pens.map((pen, i) => (
          <Card
            key={i}
            pen={pen}
            index={i}
            current={cur === i}
            deleting={deleting === i}
            onHold={() => setDeleting((d) => (d === i ? null : i))}
            onPick={() => { model.applyPreset(pen); onPicked?.(pen); }}
            onDelete={() => {
              setDeleting(null);
              model.setPens(model.pens.filter((_, k) => k !== i));
              onDeleted?.(pen, i);
            }}
          />
        ))}
      </div>
      <Button block onClick={() => model.saveCurrentPen()}>
        <Icon name="plus" size={18} />
        Save current pen
      </Button>
    </WheelPanel>
  );
}
