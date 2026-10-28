import { useContext, useId, useRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { Popover } from '../Popover';
import { cx } from '@/lib/cx';
import { MenuContext } from './MenuContext';
import { Menu } from './Menu';
import styles from './Menu.module.css';

export interface MenuItemProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onSelect' | 'value'> {
  icon?: IconName;
  /** A quieter second line under the label. */
  description?: ReactNode;
  /** A trailing value such as the current sort: Newest first. */
  value?: ReactNode;
  /** Marks the current choice with a check on the right. Leave undefined for plain actions. */
  checked?: boolean;
  /** An on or off setting, as Ruler in the note menu: shows a small switch on the right and stays open. Use instead of checked. */
  toggle?: boolean;
  /** Items that open a nested menu. Shows a chevron. */
  submenu?: ReactNode;
  tone?: 'default' | 'danger';
  /** Keep the menu open after choosing, for toggles. */
  keepOpen?: boolean;
  onSelect?: () => void;
  children: ReactNode;
}

export function MenuItem({
  icon,
  description,
  value,
  checked,
  toggle,
  submenu,
  tone = 'default',
  keepOpen = false,
  disabled,
  onSelect,
  onClick,
  onPointerEnter,
  onKeyDown,
  className,
  children,
  ...rest
}: MenuItemProps) {
  const menu = useContext(MenuContext);
  const id = useId();
  const ref = useRef<HTMLButtonElement>(null);
  const hasSub = submenu != null;
  const subOpen = hasSub && menu?.activeSub === id;
  const isSwitch = toggle !== undefined;
  const role = isSwitch ? 'menuitemcheckbox' : checked !== undefined ? 'menuitemradio' : 'menuitem';

  return (
    <>
      <button
        ref={ref}
        type="button"
        role={role}
        aria-checked={isSwitch ? toggle : checked}
        aria-disabled={disabled || undefined}
        aria-haspopup={hasSub ? 'menu' : undefined}
        aria-expanded={hasSub ? subOpen : undefined}
        data-open={subOpen || undefined}
        className={cx(styles.item, isSwitch && styles.switchRow, tone === 'danger' && styles.danger, className)}
        onPointerEnter={(e) => {
          onPointerEnter?.(e);
          menu?.setActiveSub(hasSub && !disabled ? id : null);
        }}
        onKeyDown={(e) => {
          onKeyDown?.(e);
          if (hasSub && !disabled && (e.key === 'ArrowRight' || e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault();
            menu?.setActiveSub(id);
          }
        }}
        onClick={(e) => {
          onClick?.(e);
          if (disabled) return;
          if (hasSub) {
            menu?.setActiveSub(subOpen ? null : id);
            return;
          }
          onSelect?.();
          if (!keepOpen && !isSwitch) menu?.close();
        }}
        {...rest}
      >
        {icon && <span className={styles.icon}><Icon name={icon} size={18} /></span>}
        <span className={styles.text}>
          {children}
          {description && <span className={styles.desc}>{description}</span>}
        </span>
        {value != null && <span className={styles.value}>{value}</span>}
        {checked !== undefined && !isSwitch && <Icon name="check" size={18} className={styles.check} />}
        {isSwitch && <span className={styles.switch} aria-hidden />}
        {hasSub && <Icon name="chevron_right" size={16} className={styles.chevron} />}
      </button>
      {hasSub && (
        <Popover
          open={subOpen}
          onClose={() => menu?.setActiveSub(null)}
          anchorRef={ref}
          placement="right-start"
          offset={4}
        >
          <Menu closeSelf={() => { menu?.setActiveSub(null); ref.current?.focus(); }}>{submenu}</Menu>
        </Popover>
      )}
    </>
  );
}
