import { useRef, useState, type CSSProperties, type HTMLAttributes, type KeyboardEvent, type PointerEvent } from 'react';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Slider.module.css';

export interface SliderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  min?: number;
  max?: number;
  step?: number;
  value?: number;
  defaultValue?: number;
  onChange?: (value: number) => void;
  /** The drag or key press is finished: a good moment to save an undo step. */
  onChangeEnd?: (value: number) => void;
  /** Shown after the number in the bubble, e.g. "%" or "px". */
  unit?: string;
  /** A white bubble with the value above the knob while dragging. */
  bubble?: boolean;
  disabled?: boolean;
  /** sm is the compact 24 high slider of dense rows such as the studio inspector. */
  size?: 'md' | 'sm';
  'aria-label': string;
}

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

export function Slider({
  min = 0,
  max = 100,
  step = 1,
  value,
  defaultValue = 50,
  onChange,
  onChangeEnd,
  unit = '',
  bubble = false,
  disabled = false,
  size = 'md',
  className,
  ...rest
}: SliderProps) {
  const [v, setV] = useControllable(value, defaultValue, onChange);
  const [drag, setDrag] = useState(false);
  const track = useRef<HTMLDivElement>(null);
  const pct = ((v - min) / (max - min)) * 100;

  const commit = (n: number) => setV(clamp(Math.round(n / step) * step, min, max));
  const at = (e: PointerEvent) => {
    const r = track.current?.getBoundingClientRect();
    if (!r) return;
    commit(min + clamp((e.clientX - r.left) / r.width, 0, 1) * (max - min));
  };
  const key = (e: KeyboardEvent) => {
    const big = Math.max(step, (max - min) / 10);
    const d = ({ ArrowRight: step, ArrowUp: step, ArrowLeft: -step, ArrowDown: -step, PageUp: big, PageDown: -big } as Record<string, number>)[e.key];
    if (e.key === 'Home') commit(min);
    else if (e.key === 'End') commit(max);
    else if (d) commit(v + d);
    else return;
    e.preventDefault();
  };

  return (
    <div
      role="slider"
      tabIndex={disabled ? -1 : 0}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={v}
      aria-valuetext={`${v}${unit}`}
      aria-disabled={disabled || undefined}
      className={cx(styles.slider, size === 'sm' && styles.sm, drag && styles.drag, disabled && styles.disabled, className)}
      style={{ '--pct': `${pct}%` } as CSSProperties}
      onPointerDown={(e) => {
        if (disabled) return;
        e.currentTarget.setPointerCapture(e.pointerId);
        setDrag(true);
        at(e);
      }}
      onPointerMove={(e) => drag && at(e)}
      onPointerUp={() => { if (drag) onChangeEnd?.(v); setDrag(false); }}
      onPointerCancel={() => setDrag(false)}
      onKeyDown={key}
      onKeyUp={(e) => { if (e.key.startsWith('Arrow') || e.key.startsWith('Page') || e.key === 'Home' || e.key === 'End') onChangeEnd?.(v); }}
      {...rest}
    >
      <div ref={track} className={styles.track}>
        <div className={styles.fill} />
      </div>
      <div className={styles.knob}>
        {bubble && <span className={styles.bubble}>{v}{unit}</span>}
      </div>
    </div>
  );
}
