import type { TextareaHTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './TextArea.module.css';

export interface TextAreaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange'> {
  value: string;
  onChange: (value: string) => void;
  'aria-label': string;
}

/** A small multi-line field on s2, as in the studio inspector's text box. */
export function TextArea({ value, onChange, rows = 2, className, ...rest }: TextAreaProps) {
  return <textarea className={cx(styles.area, className)} rows={rows} value={value} onChange={(e) => onChange(e.target.value)} {...rest} />;
}
