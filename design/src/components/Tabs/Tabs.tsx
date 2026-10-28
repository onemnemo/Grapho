import { useRef, type HTMLAttributes, type KeyboardEvent } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Tabs.module.css';

export interface TabItem {
  value: string;
  label: string;
  icon?: IconName;
  /** Small count after the label. */
  count?: number;
  disabled?: boolean;
}

export interface TabsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  tabs: TabItem[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  /** Share the full width equally (sheets) or hug the labels (inline). */
  layout?: 'fill' | 'hug';
  'aria-label': string;
}

/** Tabs as in the studio sheets: Presets, Colour, Icon, Finish. */
export function Tabs({ tabs, value, defaultValue, onChange, layout = 'fill', className, ...rest }: TabsProps) {
  const [current, setCurrent] = useControllable(value, defaultValue ?? tabs[0]?.value ?? '', onChange);
  const root = useRef<HTMLDivElement>(null);

  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowRight: 1, ArrowLeft: -1 } as Record<string, number>)[e.key];
    if (!d) return;
    const enabled = tabs.filter((t) => !t.disabled);
    const i = enabled.findIndex((t) => t.value === current);
    const next = enabled[(i + d + enabled.length) % enabled.length];
    if (!next) return;
    e.preventDefault();
    setCurrent(next.value);
    root.current?.querySelector<HTMLElement>(`[data-value="${CSS.escape(next.value)}"]`)?.focus();
  };

  return (
    <div ref={root} role="tablist" className={cx(styles.tabs, layout === 'hug' && styles.hug, className)} onKeyDown={move} {...rest}>
      {tabs.map((t) => {
        const on = t.value === current;
        return (
          <button
            key={t.value}
            type="button"
            role="tab"
            aria-selected={on}
            tabIndex={on ? 0 : -1}
            disabled={t.disabled}
            data-value={t.value}
            className={styles.tab}
            onClick={() => setCurrent(t.value)}
          >
            {t.icon && <Icon name={t.icon} size={18} />}
            <span>{t.label}</span>
            {t.count !== undefined && <span className={styles.count}>{t.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
