import { useRef, useState, type HTMLAttributes, type PointerEvent } from 'react';
import { cx } from '@/lib/cx';
import styles from './AlphabetRail.module.css';

const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

export interface AlphabetRailProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  /** Letters that have something under them. The others are dimmed and skipped. */
  available: string[];
  /** The letter of the group at the top of the page. */
  active?: string;
  onSelect: (letter: string) => void;
}

/** Jump between groups by letter. A big bubble names the letter while you slide along it. */
export function AlphabetRail({ available, active, onSelect, className, ...rest }: AlphabetRailProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [drag, setDrag] = useState<{ letter: string; y: number } | null>(null);
  const has = new Set(available);

  const pick = (e: PointerEvent<HTMLDivElement>) => {
    const box = ref.current?.getBoundingClientRect();
    if (!box) return;
    const idx = Math.min(25, Math.max(0, Math.floor(((e.clientY - box.top) / box.height) * 26)));
    let best = LETTERS[idx];
    if (!has.has(best)) {
      for (let d = 1; d < 26; d++) {
        const c = [LETTERS[idx - d], LETTERS[idx + d]].find((l) => l && has.has(l));
        if (c) { best = c; break; }
      }
    }
    setDrag({ letter: best, y: e.clientY - box.top });
    if (has.has(best)) onSelect(best);
  };

  return (
    <div
      ref={ref}
      className={cx(styles.rail, className)}
      role="group"
      aria-label="Jump to letter"
      onPointerDown={(e) => { e.currentTarget.setPointerCapture(e.pointerId); pick(e); }}
      onPointerMove={(e) => { if (drag) pick(e); }}
      onPointerUp={() => setDrag(null)}
      onPointerCancel={() => setDrag(null)}
      {...rest}
    >
      {LETTERS.map((l) => (
        <span key={l} className={styles.letter} data-has={has.has(l)} data-active={(drag?.letter ?? active) === l}>{l}</span>
      ))}
      <div className={styles.bubble} data-on={!!drag} style={drag ? ({ '--y': `${drag.y}px` } as React.CSSProperties) : undefined}>{drag?.letter}</div>
    </div>
  );
}
