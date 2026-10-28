import type { HTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';
import { cx } from '@/lib/cx';
import styles from './FieldRow.module.css';

interface BaseProps {
  label: ReactNode;
  /** A quiet line under the label. */
  hint?: ReactNode;
  /** right puts the control at the end of the row; below gives it the full width under the label. */
  control?: 'right' | 'below';
  /** No hairline above the row: spacing separates the rows instead. */
  plain?: boolean;
  /** The control: a toggle, a segmented control, a slider, a value. */
  children?: ReactNode;
}

export type FieldRowProps =
  | (BaseProps & Omit<HTMLAttributes<HTMLDivElement>, 'children'> & { onClick?: undefined })
  | (BaseProps & Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { onClick: NonNullable<ButtonHTMLAttributes<HTMLButtonElement>['onClick']> });

/** A 52px settings or inspector row: label and hint on the left, one control on the right or underneath. Hairline between stacked rows. */
export function FieldRow({ label, hint, control = 'right', plain = false, children, className, onClick, ...rest }: FieldRowProps) {
  const cls = cx(styles.row, plain && styles.plain, control === 'below' && styles.below, onClick && styles.press, className);
  const inner = (
    <>
      <span className={styles.label}>
        <span>
          {label}
          {hint && <small>{hint}</small>}
        </span>
      </span>
      {children != null && <span className={styles.ctl}>{children}</span>}
    </>
  );
  if (onClick) {
    return (
      <button type="button" className={cls} onClick={onClick} {...(rest as ButtonHTMLAttributes<HTMLButtonElement>)}>
        {inner}
      </button>
    );
  }
  return (
    <div className={cls} {...(rest as HTMLAttributes<HTMLDivElement>)}>
      {inner}
    </div>
  );
}
