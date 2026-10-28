import { useLayoutEffect, useRef, type HTMLAttributes, type KeyboardEvent } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './SegmentedControl.module.css';

export interface SegmentOption {
  value: string;
  label?: string;
  icon?: IconName;
  disabled?: boolean;
}

export interface SegmentedControlProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  options: SegmentOption[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  /** xs is the 26px pill of dense rows such as the studio inspector. */
  size?: 'xs' | 'sm' | 'md';
  /** Fill the width and share it equally between the options. */
  fit?: boolean;
  /** Icons only: compact square options, labels become tooltips and accessible names. */
  iconsOnly?: boolean;
  'aria-label': string;
}

export function SegmentedControl({
  options,
  value,
  defaultValue,
  onChange,
  size = 'md',
  fit = false,
  iconsOnly = false,
  className,
  ...rest
}: SegmentedControlProps) {
  const [current, setCurrent] = useControllable(value, defaultValue ?? options[0]?.value ?? '', onChange);
  const root = useRef<HTMLDivElement>(null);
  const thumb = useRef<HTMLSpanElement>(null);
  const seen = useRef(false);

  // One thumb that slides. It is positioned from the selected option, never remounted.
  useLayoutEffect(() => {
    const place = (animate: boolean) => {
      const t = thumb.current;
      const sel = root.current?.querySelector<HTMLElement>('[aria-checked="true"]');
      if (!t || !sel || !sel.offsetWidth) return;
      if (!animate) t.style.transition = 'none';
      t.style.width = `${sel.offsetWidth}px`;
      t.style.transform = `translateX(${sel.offsetLeft}px)`;
      t.style.opacity = '1';
      if (!animate) {
        void t.offsetWidth;
        t.style.transition = '';
      }
    };
    place(seen.current);
    seen.current = true;
    const ro = new ResizeObserver(() => place(false));
    if (root.current) ro.observe(root.current);
    return () => ro.disconnect();
  }, [current, options, size, iconsOnly, fit]);

  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 } as Record<string, number>)[e.key];
    if (!d) return;
    const enabled = options.filter((o) => !o.disabled);
    const i = enabled.findIndex((o) => o.value === current);
    const next = enabled[(i + d + enabled.length) % enabled.length];
    if (!next) return;
    e.preventDefault();
    setCurrent(next.value);
    root.current?.querySelector<HTMLElement>(`[data-value="${CSS.escape(next.value)}"]`)?.focus();
  };

  return (
    <div
      ref={root}
      role="radiogroup"
      className={cx(styles.seg, size === 'sm' && styles.sm, size === 'xs' && styles.xs, fit && styles.fit, iconsOnly && styles.icons, className)}
      onKeyDown={move}
      {...rest}
    >
      <span ref={thumb} className={styles.thumb} aria-hidden="true" />
      {options.map((o) => {
        const on = o.value === current;
        return (
          <button
            key={o.value}
            type="button"
            role="radio"
            aria-checked={on}
            aria-label={iconsOnly ? o.label : undefined}
            title={iconsOnly ? o.label : undefined}
            tabIndex={on ? 0 : -1}
            disabled={o.disabled}
            data-value={o.value}
            className={styles.opt}
            onClick={() => !on && setCurrent(o.value)}
          >
            {o.icon && <Icon name={o.icon} size={size === 'md' ? 18 : 16} />}
            {!iconsOnly && o.label && <span>{o.label}</span>}
          </button>
        );
      })}
    </div>
  );
}
