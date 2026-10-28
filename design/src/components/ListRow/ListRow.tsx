import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './ListRow.module.css';

interface BaseProps {
  /** A thumbnail or any small visual. Takes precedence over leadingIcon. */
  leading?: ReactNode;
  /** Shorthand for a 36px icon tile. */
  leadingIcon?: IconName;
  title: ReactNode;
  /** A quieter second line under the title. */
  description?: ReactNode;
  /** A value on the right: a date, a count, a size. */
  meta?: ReactNode;
  /** A chevron, a toggle, a ••• button. */
  trailing?: ReactNode;
  /** Draws the white selection ring. */
  selected?: boolean;
  /** Hairline above every row after the first when stacked. */
  divided?: boolean;
}

export type ListRowProps =
  | (BaseProps & Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'title'> & { onClick: NonNullable<ButtonHTMLAttributes<HTMLButtonElement>['onClick']> })
  | (BaseProps & Omit<HTMLAttributes<HTMLDivElement>, 'title' | 'onClick'> & { onClick?: undefined });

/** A 52px row: leading, title, meta, trailing. A button when it has onClick, otherwise a plain row. */
export function ListRow({ leading, leadingIcon, title, description, meta, trailing, selected, divided, className, onClick, ...rest }: ListRowProps) {
  const cls = cx(styles.row, divided && styles.divided, className);
  const inner = (
    <>
      {(leading || leadingIcon) && (
        <span className={styles.leading}>
          {leading ?? <span className={styles.tile}><Icon name={leadingIcon as IconName} size={18} /></span>}
        </span>
      )}
      <span className={styles.text}>
        <span className={styles.title}>{title}</span>
        {description && <span className={styles.desc}>{description}</span>}
      </span>
      {meta != null && <span className={styles.meta}>{meta}</span>}
      {trailing && <span className={styles.trailing}>{trailing}</span>}
    </>
  );
  if (onClick) {
    return (
      <button type="button" className={cls} aria-pressed={selected || undefined} onClick={onClick} {...(rest as ButtonHTMLAttributes<HTMLButtonElement>)}>
        {inner}
      </button>
    );
  }
  return (
    <div className={cls} aria-current={selected || undefined} {...(rest as HTMLAttributes<HTMLDivElement>)}>
      {inner}
    </div>
  );
}
