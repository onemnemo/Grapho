import { useContext, useId, useMemo, useRef, useState, type HTMLAttributes, type KeyboardEvent, type ReactNode } from 'react';
import { cx } from '@/lib/cx';
import { MenuContext, type MenuContextValue } from './MenuContext';
import styles from './Menu.module.css';

export interface MenuProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Called after any item is chosen, so the surrounding Popover can close. */
  onClose?: () => void;
  /** Name for assistive tech. */
  label?: string;
  /** Only set by a submenu so ArrowLeft closes that level. */
  closeSelf?: () => void;
  children: ReactNode;
}

const ITEMS = '[role^="menuitem"]:not([aria-disabled="true"])';

/** A list of choices with arrow key movement. Put it inside a Popover. */
export function Menu({ onClose, label, closeSelf, className, children, onKeyDown, ...rest }: MenuProps) {
  const parent = useContext(MenuContext);
  const [activeSub, setActiveSub] = useState<string | null>(null);
  const ref = useRef<HTMLDivElement>(null);
  const close = onClose ?? parent?.close ?? (() => {});
  const value = useMemo<MenuContextValue>(
    () => ({ close, activeSub, setActiveSub, closeSelf }),
    [onClose, parent, activeSub, closeSelf],
  );

  const handleKey = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    const root = ref.current;
    if (e.defaultPrevented || !root || !root.contains(e.target as Node)) return;
    const items = [...root.querySelectorAll<HTMLElement>(ITEMS)].filter((el) => el.closest('[role="menu"]') === root);
    const at = items.indexOf(document.activeElement as HTMLElement);
    let next: HTMLElement | undefined;
    if (e.key === 'ArrowDown') next = items[(at + 1) % items.length];
    else if (e.key === 'ArrowUp') next = items[(at <= 0 ? items.length : at) - 1];
    else if (e.key === 'Home') next = items[0];
    else if (e.key === 'End') next = items[items.length - 1];
    else if (e.key === 'ArrowLeft' && closeSelf) {
      closeSelf();
      e.preventDefault();
      return;
    } else return;
    e.preventDefault();
    next?.focus();
  };

  return (
    <MenuContext.Provider value={value}>
      <div ref={ref} role="menu" aria-label={label} className={cx(styles.menu, className)} onKeyDown={handleKey} {...rest}>
        {children}
      </div>
    </MenuContext.Provider>
  );
}

export interface MenuSectionProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The small label above the group: Sort by, Import. */
  label?: string;
  children: ReactNode;
}

export function MenuSection({ label, className, children, ...rest }: MenuSectionProps) {
  const id = useId();
  return (
    <div role="group" aria-labelledby={label ? id : undefined} className={cx(styles.section, className)} {...rest}>
      {label && <div id={id} className={styles.label} aria-hidden>{label}</div>}
      {children}
    </div>
  );
}

export function MenuSeparator() {
  return <div role="separator" className={styles.sep} />;
}
