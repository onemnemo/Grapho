import type { CSSProperties, HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './ScrollRow.module.css';

export interface ScrollRowProps extends HTMLAttributes<HTMLDivElement> {
  /** Space between items in px. */
  gap?: number;
}

export function ScrollRow({ gap = 22, className, style, ...rest }: ScrollRowProps) {
  return <div className={cx(styles.row, className)} style={{ '--row-gap': `${gap}px`, ...style } as CSSProperties} {...rest} />;
}
