import { useLayoutEffect, useRef, type CSSProperties, type KeyboardEvent } from 'react';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import { Swatch, type SwatchKind, type SwatchShape, type SwatchSize } from './Swatch';
import styles from './SwatchRow.module.css';

export interface SwatchOption {
  value: string;
  /** CSS colour. Omit for the none and add kinds. */
  color?: string;
  label: string;
  kind?: SwatchKind;
}

export interface SwatchRowProps {
  swatches: SwatchOption[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  /** Called when the add swatch is pressed. */
  onAdd?: () => void;
  size?: SwatchSize;
  shape?: SwatchShape;
  check?: boolean;
  /** Gap between swatches in px. */
  gap?: number;
  className?: string;
  'aria-label': string;
}

/** A group of swatches with ONE ring that glides to the chosen one, so nothing remounts or flickers. */
export function SwatchRow({
  swatches,
  value,
  defaultValue,
  onChange,
  onAdd,
  size = 'md',
  shape = 'round',
  check = false,
  gap = 12,
  className,
  ...rest
}: SwatchRowProps) {
  const [current, setCurrent] = useControllable(value, defaultValue ?? '', onChange);
  const root = useRef<HTMLDivElement>(null);
  const ring = useRef<HTMLSpanElement>(null);
  const seen = useRef(false);

  useLayoutEffect(() => {
    const place = (animate: boolean) => {
      const r = ring.current;
      const sel = root.current?.querySelector<HTMLElement>('[aria-checked="true"]');
      if (!r) return;
      if (!sel) {
        r.dataset.hidden = '';
        return;
      }
      delete r.dataset.hidden;
      if (!animate) r.style.transition = 'none';
      r.style.width = `${sel.offsetWidth + 10}px`;
      r.style.height = `${sel.offsetHeight + 10}px`;
      r.style.transform = `translate(${sel.offsetLeft - 5}px, ${sel.offsetTop - 5}px)`;
      if (!animate) {
        void r.offsetWidth;
        r.style.transition = '';
      }
    };
    place(seen.current);
    seen.current = true;
    const ro = new ResizeObserver(() => place(false));
    if (root.current) ro.observe(root.current);
    return () => ro.disconnect();
  }, [current, swatches, size, shape, gap]);

  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 } as Record<string, number>)[e.key];
    if (!d) return;
    const pick = swatches.filter((s) => s.kind !== 'add');
    const i = pick.findIndex((s) => s.value === current);
    const next = pick[(i + d + pick.length) % pick.length];
    if (!next) return;
    e.preventDefault();
    setCurrent(next.value);
    root.current?.querySelector<HTMLElement>(`[data-value="${CSS.escape(next.value)}"]`)?.focus();
  };

  const firstPick = swatches.find((s) => s.kind !== 'add')?.value;
  const focusable = swatches.some((s) => s.value === current) ? current : firstPick;

  return (
    <div
      ref={root}
      role="radiogroup"
      className={cx(styles.row, shape === 'square' && styles.square, className)}
      style={{ gap } as CSSProperties}
      onKeyDown={move}
      {...rest}
    >
      <span ref={ring} className={styles.ring} data-hidden="" aria-hidden="true" />
      {swatches.map((s) =>
        s.kind === 'add' ? (
          <Swatch key={s.value} kind="add" label={s.label} size={size} shape={shape} ownRing={false} role="button" aria-checked={undefined} onClick={onAdd} />
        ) : (
          <Swatch
            key={s.value}
            data-value={s.value}
            kind={s.kind}
            color={s.color}
            label={s.label}
            size={size}
            shape={shape}
            check={check}
            ownRing={false}
            selected={s.value === current}
            tabIndex={s.value === focusable ? 0 : -1}
            onClick={() => setCurrent(s.value)}
          />
        ),
      )}
    </div>
  );
}
