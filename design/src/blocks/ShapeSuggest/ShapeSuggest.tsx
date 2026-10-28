import { useEffect, useRef, type CSSProperties, type HTMLAttributes, type ReactNode } from 'react';
import { IconButton } from '@/components/IconButton';
import { cx } from '@/lib/cx';
import styles from './ShapeSuggest.module.css';

export interface ShapeSuggestion {
  value: string;
  label: string;
  glyph?: ReactNode;
}

export interface ShapeSuggestProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  /** The closest match, shown solid. */
  best: ShapeSuggestion;
  /** Other close matches, beside it. */
  others?: ShapeSuggestion[];
  /** Where it sits under the stroke, in px: x is the centre, y the top edge. */
  x: number;
  y: number;
  /** How long the countdown runs. Hovering pauses it. */
  durationMs?: number;
  onChoose: (value: string) => void;
  /** The cross: keep the drawing as it is. */
  onDismiss: () => void;
  /** The countdown ran out. */
  onExpire?: () => void;
}

/** "Did you mean": a small chip under a rough stroke offering the clean shape. It counts down and keeps your drawing until you accept. */
export function ShapeSuggest({ best, others = [], x, y, durationMs = 5000, onChoose, onDismiss, onExpire, className, style, ...rest }: ShapeSuggestProps) {
  const clock = useRef({ left: durationMs, started: 0, id: 0 });
  const expire = useRef(onExpire);
  expire.current = onExpire;

  const start = () => {
    window.clearTimeout(clock.current.id);
    clock.current.started = Date.now();
    clock.current.id = window.setTimeout(() => expire.current?.(), clock.current.left);
  };
  const pause = () => {
    window.clearTimeout(clock.current.id);
    clock.current.left = Math.max(400, clock.current.left - (Date.now() - clock.current.started));
  };
  useEffect(() => {
    clock.current.left = durationMs;
    start();
    return () => window.clearTimeout(clock.current.id);
  }, [durationMs]);

  return (
    <div
      role="group"
      aria-label="Shape suggestion"
      className={cx(styles.chip, className)}
      style={{ left: x, top: y, '--suggest-ms': `${durationMs}ms`, ...style } as CSSProperties}
      onPointerEnter={pause}
      onPointerLeave={start}
      {...rest}
    >
      <span className={styles.q}>Did you mean</span>
      {[best, ...others].map((s, i) => (
        <button key={s.value} type="button" className={cx(styles.opt, i === 0 && styles.best)} onClick={() => onChoose(s.value)}>
          {s.glyph}
          {s.label}
        </button>
      ))}
      <IconButton icon="close" label="Keep my drawing" size="sm" variant="ghost" iconSize={18} onClick={onDismiss} />
      <i className={styles.time} />
    </div>
  );
}
