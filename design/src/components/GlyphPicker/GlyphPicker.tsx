import { useRef, type HTMLAttributes, type KeyboardEvent, type ReactNode } from 'react';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './GlyphPicker.module.css';

export interface GlyphOption {
  value: string;
  /** Spoken name and hover title. */
  label: string;
  /** A small drawing or an Icon, drawn in the current colour. */
  glyph: ReactNode;
}

export interface GlyphPickerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  options: GlyphOption[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  'aria-label': string;
}

/** A row of small drawings with one selected: path, outline, width. Pick with a tap or the arrow keys. */
export function GlyphPicker({ options, value, defaultValue, onChange, className, ...rest }: GlyphPickerProps) {
  const [current, setCurrent] = useControllable(value, defaultValue ?? options[0]?.value ?? '', onChange);
  const root = useRef<HTMLDivElement>(null);
  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 } as Record<string, number>)[e.key];
    if (!d) return;
    const at = options.findIndex((o) => o.value === current);
    const next = options[(at + d + options.length) % options.length];
    e.preventDefault();
    setCurrent(next.value);
    root.current?.querySelector<HTMLElement>(`[data-value="${CSS.escape(next.value)}"]`)?.focus();
  };
  return (
    <div ref={root} role="radiogroup" className={cx(styles.glyphs, className)} onKeyDown={move} {...rest}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={o.value === current}
          aria-label={o.label}
          data-value={o.value}
          tabIndex={o.value === current ? 0 : -1}
          title={o.label}
          className={styles.opt}
          onClick={() => setCurrent(o.value)}
        >
          {o.glyph}
        </button>
      ))}
    </div>
  );
}
