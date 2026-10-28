import { useRef, useState, type CSSProperties, type HTMLAttributes, type PointerEvent } from 'react';
import { Button } from '@/components/Button';
import { clamp } from '@/assets/studio';
import { cx } from '@/lib/cx';
import styles from './CropMode.module.css';

export interface CropRect { l: number; t: number; r: number; b: number }
export const FULL_CROP: CropRect = { l: 0, t: 0, r: 1, b: 1 };

type Handle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'move';
const HANDLES: Handle[] = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'];
const MIN = 0.12;

export interface CropFrameProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The picture on the page, in px: left, top, width and height. */
  box: { x: number; y: number; width: number; height: number };
  /** The kept part, as fractions of the picture. */
  value?: CropRect;
  onChange?: (rect: CropRect) => void;
}

/** The picture while it is being cropped: everything outside the frame is dimmed, the frame has a thirds grid and eight handles. Drag a handle to size it, drag inside to move it. */
export function CropFrame({ box, value, onChange, className, style, ...rest }: CropFrameProps) {
  const [own, setOwn] = useState<CropRect>(FULL_CROP);
  const rect = value ?? own;
  const set = (r: CropRect) => { setOwn(r); onChange?.(r); };
  const drag = useRef<{ h: Handle; x: number; y: number; r: CropRect } | null>(null);

  const down = (h: Handle) => (e: PointerEvent<HTMLElement>) => {
    e.stopPropagation();
    drag.current = { h, x: e.clientX, y: e.clientY, r: rect };
    e.currentTarget.setPointerCapture(e.pointerId);
  };
  const move = (e: PointerEvent<HTMLElement>) => {
    const d = drag.current;
    if (!d) return;
    const host = e.currentTarget.closest<HTMLElement>('[data-crop]');
    const bb = host?.getBoundingClientRect();
    if (!bb) return;
    const dx = (e.clientX - d.x) / bb.width, dy = (e.clientY - d.y) / bb.height;
    let { l, t, r, b } = d.r;
    if (d.h === 'move') {
      const w = r - l, h = b - t;
      l = clamp(l + dx, 0, 1 - w); t = clamp(t + dy, 0, 1 - h); r = l + w; b = t + h;
    } else {
      if (d.h.includes('w')) l = clamp(l + dx, 0, r - MIN);
      if (d.h.includes('e')) r = clamp(r + dx, l + MIN, 1);
      if (d.h.includes('n')) t = clamp(t + dy, 0, b - MIN);
      if (d.h.includes('s')) b = clamp(b + dy, t + MIN, 1);
    }
    set({ l, t, r, b });
  };
  const up = () => { drag.current = null; };

  const frame = { left: `${rect.l * 100}%`, top: `${rect.t * 100}%`, width: `${(rect.r - rect.l) * 100}%`, height: `${(rect.b - rect.t) * 100}%` };
  return (
    <div data-crop className={cx(styles.root, className)} style={{ left: box.x, top: box.y, width: box.width, height: box.height, ...style } as CSSProperties} {...rest}>
      <i className={styles.dim} style={{ left: 0, top: 0, right: 0, height: frame.top }} />
      <i className={styles.dim} style={{ left: 0, bottom: 0, right: 0, top: `${rect.b * 100}%` }} />
      <i className={styles.dim} style={{ left: 0, top: frame.top, width: frame.left, height: frame.height }} />
      <i className={styles.dim} style={{ right: 0, top: frame.top, left: `${rect.r * 100}%`, height: frame.height }} />
      <div className={styles.frame} style={frame} onPointerDown={down('move')} onPointerMove={move} onPointerUp={up} onPointerCancel={up}>
        <i className={styles.grid} aria-hidden />
        {HANDLES.map((h) => (
          <button key={h} type="button" tabIndex={-1} aria-label={`Crop handle ${h}`} className={styles.handle} data-h={h} onPointerDown={down(h)} onPointerMove={move} onPointerUp={up} onPointerCancel={up} />
        ))}
      </div>
    </div>
  );
}

export interface CropBarProps extends HTMLAttributes<HTMLDivElement> {
  /** Where it sits in the frame, in px: x is the centre, y the top edge. */
  x: number;
  y: number;
  onReset?: () => void;
  onCancel?: () => void;
  onDone?: () => void;
}

/** Reset and Cancel are quiet text; Done is the one solid button. */
export function CropBar({ x, y, onReset, onCancel, onDone, className, style, ...rest }: CropBarProps) {
  return (
    <div role="toolbar" aria-label="Crop" className={cx(styles.bar, className)} style={{ left: x, top: y, ...style } as CSSProperties} {...rest}>
      <Button variant="text" size="sm" onClick={onReset}>Reset</Button>
      <Button variant="text" size="sm" onClick={onCancel}>Cancel</Button>
      <Button variant="primary" size="sm" onClick={onDone}>Done</Button>
    </div>
  );
}
