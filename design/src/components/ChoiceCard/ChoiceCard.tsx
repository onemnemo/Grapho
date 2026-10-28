import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './ChoiceCard.module.css';

export interface ChoiceCardProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'title'> {
  /** The picture of what choosing it does. */
  preview: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  selected?: boolean;
  /** lg shows a tall well behind the preview (Sticker or Image). sm shows a short one (canvas size). */
  size?: 'lg' | 'sm';
}

/** A big card that shows the result of a choice: a preview, a name and one plain line. Selected is a white ring and a check. */
export function ChoiceCard({ preview, title, description, selected = false, size = 'lg', className, type = 'button', ...rest }: ChoiceCardProps) {
  return (
    <button type={type} aria-pressed={selected} className={cx(styles.card, styles[size], className)} {...rest}>
      <span className={styles.preview}>{preview}</span>
      <b className={styles.title}>{title}</b>
      {description && <span className={styles.desc}>{description}</span>}
      <i className={styles.check} aria-hidden="true"><Icon name="check" size={14} strokeWidth={2.4} /></i>
    </button>
  );
}
