import { useRef, type HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './RollingNumber.module.css';

export interface RollingNumberProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  value: number;
}

/** A number that rolls up or down when it changes: the digit leaves one way and the new one arrives. */
export function RollingNumber({ value, className, ...rest }: RollingNumberProps) {
  const prev = useRef(value);
  const up = value >= prev.current;
  const old = prev.current;
  prev.current = value;
  return (
    <span className={cx(styles.roll, className)} {...rest}>
      {old !== value && <span key={`o${old}`} className={up ? styles.outUp : styles.outDn} aria-hidden="true">{old}</span>}
      <span key={`n${value}`} className={old !== value ? (up ? styles.inUp : styles.inDn) : undefined}>{value}</span>
    </span>
  );
}
