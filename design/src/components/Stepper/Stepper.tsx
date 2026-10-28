import type { HTMLAttributes } from 'react';
import { IconButton } from '../IconButton';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Stepper.module.css';

export interface StepperProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  min?: number;
  max?: number;
  step?: number;
  value?: number;
  defaultValue?: number;
  onChange?: (value: number) => void;
  'aria-label': string;
}

/** A pill with minus, the number and plus. For small counts such as star points or polygon sides. */
export function Stepper({ min = 0, max = 99, step = 1, value, defaultValue = min, onChange, className, ...rest }: StepperProps) {
  const [v, setV] = useControllable(value, defaultValue, onChange);
  const to = (n: number) => setV(Math.min(max, Math.max(min, n)));
  return (
    <div role="group" className={cx(styles.stepper, className)} {...rest}>
      <IconButton icon="minus" label="Fewer" size="sm" variant="ghost" iconSize={18} disabled={v <= min} onClick={() => to(v - step)} />
      <output className={styles.value} aria-live="polite">{v}</output>
      <IconButton icon="plus" label="More" size="sm" variant="ghost" iconSize={18} disabled={v >= max} onClick={() => to(v + step)} />
    </div>
  );
}
