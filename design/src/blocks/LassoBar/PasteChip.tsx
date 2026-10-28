import type { CSSProperties, HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './PasteChip.module.css';

export interface PasteChipProps extends HTMLAttributes<HTMLButtonElement> {
  /** Where it sits in the frame, in px: x is the centre, y the top edge. The tapped point. */
  x: number;
  y: number;
  onPaste?: () => void;
}

/** After a tap on empty page with something copied: one small chip that pastes at that point. */
export function PasteChip({ x, y, onPaste, className, style, ...rest }: PasteChipProps) {
  return (
    <button type="button" className={cx(styles.chip, className)} style={{ left: x, top: y, ...style } as CSSProperties} onClick={onPaste} {...rest}>
      <Icon name="paste" size={18} />
      Paste
    </button>
  );
}
