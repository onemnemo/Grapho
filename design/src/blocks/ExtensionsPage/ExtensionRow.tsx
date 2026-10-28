import type { HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { Toggle } from '@/components/Toggle';
import { cx } from '@/lib/cx';
import type { ExtensionItem } from './extensionTypes';
import styles from './ExtensionRow.module.css';

export interface ExtensionRowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onToggle'> {
  extension: Pick<ExtensionItem, 'icon' | 'name' | 'description' | 'enabled'>;
  onOpen?: () => void;
  onToggle?: (enabled: boolean) => void;
}

/** An extension: a rounded plate with its glyph (packs have gradient covers), name, one line and an on switch. Opening it shows what it can access. */
export function ExtensionRow({ extension: e, onOpen, onToggle, className, ...rest }: ExtensionRowProps) {
  return (
    <div className={cx(styles.row, !e.enabled && styles.off, className)} {...rest}>
      <button type="button" className={styles.main} onClick={onOpen}>
        <span className={styles.plate}><Icon name={e.icon} size={24} /></span>
        <span className={styles.text}>
          <b>{e.name}</b>
          <small>{e.description}</small>
        </span>
      </button>
      <Toggle variant="marked" checked={e.enabled} onChange={onToggle} aria-label={`${e.name} on`} className={styles.toggle} />
    </div>
  );
}
