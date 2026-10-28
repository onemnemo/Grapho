import type { ButtonHTMLAttributes, CSSProperties, Ref } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './Swatch.module.css';

export type SwatchSize = 'sm' | 'md' | 'lg';
export type SwatchShape = 'round' | 'square';
export type SwatchKind = 'colour' | 'none' | 'add' | 'custom';

export interface SwatchProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'color'> {
  /** Any CSS colour or token, e.g. "var(--pink)". Chosen at runtime by the user, so it is set as a style. */
  color?: string;
  /** Accessible name: the colour's name. */
  label: string;
  selected?: boolean;
  size?: SwatchSize;
  /** round for pens and text, square for cover and folder colours. */
  shape?: SwatchShape;
  /** none shows a checker with a slash (no fill); add shows a plus (add a custom colour);
   *  custom is the rainbow ring that opens the colour picker, or shows a colour chosen there when `color` is set. */
  kind?: SwatchKind;
  /** A tick on the swatch as well as the ring. */
  check?: boolean;
  /** Draw the ring on the swatch itself. SwatchRow turns this off and slides one shared ring instead. */
  ownRing?: boolean;
  ref?: Ref<HTMLButtonElement>;
}

export function Swatch({
  color,
  label,
  selected = false,
  size = 'md',
  shape = 'round',
  kind = 'colour',
  check = false,
  ownRing = true,
  className,
  style,
  type = 'button',
  ref,
  ...rest
}: SwatchProps) {
  return (
    <button
      ref={ref}
      type={type}
      role="radio"
      aria-checked={selected}
      aria-label={label}
      title={label}
      data-ring={ownRing ? 'own' : 'shared'}
      className={cx(styles.swatch, styles[size], shape === 'square' && styles.square, kind !== 'colour' && styles[kind], className)}
      style={{ ...(color ? { '--c': color } : null), ...style } as CSSProperties}
      {...rest}
    >
      {kind === 'add' && <Icon name="plus" size={16} />}
      {kind === 'none' && <span className={styles.slash} aria-hidden="true" />}
      {check && kind === 'colour' && <Icon name="check" size={size === 'sm' ? 14 : 16} strokeWidth={2.6} className={styles.tick} />}
    </button>
  );
}
