import type { ButtonHTMLAttributes } from 'react';
import { IconButton } from '@/components/IconButton';
import { cx } from '@/lib/cx';
import styles from './CloseButton.module.css';

/** The bare round close control in sheet headers and banners, pulled in so its glyph lines up with the edge. */
export function CloseButton({ className, 'aria-label': label = 'Close', ...rest }: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'>) {
  return <IconButton icon="close" label={label} variant="ghost" size="sm" iconSize={18} className={cx(styles.close, className)} {...rest} />;
}
