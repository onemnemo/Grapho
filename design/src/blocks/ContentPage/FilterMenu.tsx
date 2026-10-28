import { useRef, useState, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { Menu } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import styles from './FilterMenu.module.css';

export interface FilterMenuProps {
  /** The quiet word: Source, Sort. */
  label: string;
  /** What is chosen now. */
  value: string;
  boundary?: 'viewport' | 'container';
  /** The menu items. Call close after a choice. */
  children: (close: () => void) => ReactNode;
}

/** A small button that reads "Sort  Name" and opens a menu of single choices. */
export function FilterMenu({ label, value, boundary = 'viewport', children }: FilterMenuProps) {
  const ref = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);
  return (
    <>
      <button ref={ref} type="button" className={styles.btn} aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
        <span>{label}</span>
        <b>{value}</b>
        <Icon name="chevron_down" size={14} />
      </button>
      <Popover open={open} onClose={close} anchorRef={ref} placement="bottom-start" kind="menu" boundary={boundary}>
        <Menu onClose={close} label={label} className={styles.menu}>{children(close)}</Menu>
      </Popover>
    </>
  );
}
