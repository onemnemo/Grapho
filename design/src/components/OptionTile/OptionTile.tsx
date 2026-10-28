import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';
import { cx } from '@/lib/cx';
import styles from './OptionTile.module.css';

export interface OptionTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** The picture: a small drawing of the result, or an icon tile. */
  visual: ReactNode;
  label: ReactNode;
  hint?: ReactNode;
  selected?: boolean;
  /** card: picture over text, left aligned. tile: small, centred label, in a grid of three or four. row: icon at the side. */
  layout?: 'card' | 'tile' | 'row';
}

/** One choice shown as a picture with a name. A radio: put several in an OptionTileGroup. */
export function OptionTile({ visual, label, hint, selected = false, layout = 'card', className, type = 'button', ...rest }: OptionTileProps) {
  return (
    <button
      type={type}
      role="radio"
      aria-checked={selected}
      className={cx(styles.opt, styles[layout], className)}
      {...rest}
    >
      {visual}
      <span className={styles.text}>
        <b>{label}</b>
        {hint && <small>{hint}</small>}
      </span>
    </button>
  );
}

/** A small icon on a square plate, the visual for a row tile. Holds a soft white fill when the tile is selected. */
export function OptionIcon({ children }: { children: ReactNode }) {
  return <span className={styles.ico}>{children}</span>;
}

export interface OptionTileGroupProps extends HTMLAttributes<HTMLDivElement> {
  /** 1 is a list of row tiles. */
  columns?: 1 | 2 | 3 | 4;
  'aria-label': string;
}

export function OptionTileGroup({ columns = 2, className, children, ...rest }: OptionTileGroupProps) {
  return (
    <div role="radiogroup" className={cx(styles.group, styles[`c${columns}`], className)} {...rest}>
      {children}
    </div>
  );
}
