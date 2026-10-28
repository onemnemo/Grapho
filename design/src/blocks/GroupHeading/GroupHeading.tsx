import type { HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './GroupHeading.module.css';

export interface GroupHeadingProps extends HTMLAttributes<HTMLDivElement> {
  label: string;
  count?: number;
  /** quiet is the smaller Pinned label above the lettered groups. */
  tone?: 'default' | 'quiet';
}

/** Sticks to the top of the page while its group scrolls past. */
export function GroupHeading({ label, count, tone = 'default', className, ...rest }: GroupHeadingProps) {
  return (
    <div className={cx(styles.group, tone === 'quiet' && styles.quiet, className)} {...rest}>
      <b>{label}</b>{count != null && ` ${count}`}
    </div>
  );
}
