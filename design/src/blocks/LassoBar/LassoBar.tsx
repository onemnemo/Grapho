import { useLayoutEffect, useRef, useState, type CSSProperties, type HTMLAttributes, type MouseEvent } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { IconButton } from '@/components/IconButton';
import { Tooltip } from '@/components/Tooltip';
import { cx } from '@/lib/cx';
import styles from './LassoBar.module.css';

export interface LassoAction {
  id: string;
  icon: IconName;
  label: string;
  /** Delete: red, and set apart from the rest by space. */
  danger?: boolean;
  /** The option this button opened is showing, as Shape does while its panel is open. */
  active?: boolean;
  /** Show the label beside the icon. For the few actions an icon alone would not explain. */
  text?: boolean;
}
export type LassoItem = LassoAction | 'separator';

/** Ink, shapes and text: Colour and Thickness, Copy and Duplicate, then More. Cut, order, layer and flip are in More. Delete sits apart. */
export const lassoActions: LassoItem[] = [
  { id: 'colour', icon: 'palette', label: 'Colour' },
  { id: 'thickness', icon: 'sliders', label: 'Thickness' },
  { id: 'copy', icon: 'copy', label: 'Copy' },
  { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' },
  { id: 'more', icon: 'more', label: 'More' },
  'separator',
  { id: 'delete', icon: 'trash', label: 'Delete', danger: true },
];

/** One picture: the three picture actions carry words, the rest are icons. */
export const lassoImageActions: LassoItem[] = [
  { id: 'crop', icon: 'crop', label: 'Crop', text: true },
  { id: 'removeBackground', icon: 'cutout', label: 'Remove background', text: true },
  { id: 'convertToInk', icon: 'to_ink', label: 'Convert to ink', text: true },
  { id: 'copy', icon: 'copy', label: 'Copy' },
  { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' },
  { id: 'more', icon: 'more', label: 'More' },
  'separator',
  { id: 'delete', icon: 'trash', label: 'Delete', danger: true },
];

export interface LassoBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  items?: LassoItem[];
  /** Where it sits in the frame, in px: x is the centre, y is the top edge. */
  x: number;
  y: number;
  tipSide?: 'top' | 'bottom';
  onAction?: (id: string, event: MouseEvent<HTMLButtonElement>) => void;
}

/** The quiet floating bar that rides on a selection. No emphasis: every action weighs the same, and Delete is only set apart. */
export function LassoBar({ items = lassoActions, x, y, tipSide = 'top', onAction, className, style, ...rest }: LassoBarProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [left, setLeft] = useState(x);
  // Keep the whole bar inside its frame: a selection near an edge pulls it in.
  useLayoutEffect(() => {
    const bar = ref.current;
    const host = bar?.offsetParent as HTMLElement | null;
    if (!bar || !host) { setLeft(x); return; }
    const half = bar.offsetWidth / 2 + 8;
    setLeft(Math.min(Math.max(x, half), Math.max(half, host.clientWidth - half)));
  }, [x, items]);
  return (
    <div
      ref={ref}
      role="toolbar"
      aria-label="Selection actions"
      className={cx(styles.bar, className)}
      style={{ left, top: y, ...style } as CSSProperties}
      {...rest}
    >
      {items.map((it, i) => {
        if (it === 'separator') return <i key={`s${i}`} className={styles.gap} aria-hidden />;
        if (it.text) {
          return (
            <button key={it.id} type="button" className={cx(styles.act, it.danger && styles.danger)} data-on={it.active || undefined} aria-pressed={it.active || undefined} onClick={(e) => onAction?.(it.id, e)}>
              <Icon name={it.icon} size={18} />
              {it.label}
            </button>
          );
        }
        return (
          <Tooltip key={it.id} content={it.label} side={tipSide}>
            <IconButton
              icon={it.icon}
              label={it.label}
              size="sm"
              iconSize={18}
              active={it.active}
              className={it.danger ? styles.danger : undefined}
              onClick={(e) => onAction?.(it.id, e)}
            />
          </Tooltip>
        );
      })}
    </div>
  );
}
