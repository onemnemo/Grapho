import { useId, useState, type CSSProperties, type HTMLAttributes, type ReactNode } from 'react';
import { OverlayLayer, type OverlayPlacement } from '@/lib/OverlayLayer';
import { CloseButton } from '../CloseButton';
import { cx } from '@/lib/cx';
import styles from './Sheet.module.css';

export type SheetVariant = 'modal' | 'side';

export interface SheetProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  /** One quiet line under the title. */
  subtitle?: ReactNode;
  /** modal is the centred sheet; side slides in from an edge like the library filter sheet. */
  variant?: SheetVariant;
  /** Width of the modal in px. 540 is the studio sheet; the Settings overlay and import sheets go wider. */
  width?: number;
  /** Which edge a side sheet comes from. */
  edge?: 'end' | 'start';
  /** Small controls in the header, before the close button: Import and Select in the presets picker. */
  actions?: ReactNode;
  /** Buttons for the footer. The footer stays put while the body scrolls. */
  footer?: ReactNode;
  /** viewport covers the window; container fills the nearest positioned parent. */
  placement?: OverlayPlacement;
  dismissOnScrim?: boolean;
  /** Removes the body padding and clipping so the content brings its own layout, as the Settings overlay does. */
  flush?: boolean;
  closeLabel?: string;
  children: ReactNode;
}

/** A sheet with a header, a scrolling body and a footer. Escape closes it and focus stays inside. */
export function Sheet({
  open,
  onClose,
  title,
  subtitle,
  variant = 'modal',
  width,
  edge = 'end',
  footer,
  actions,
  placement = 'viewport',
  dismissOnScrim = true,
  flush = false,
  closeLabel = 'Close',
  className,
  style,
  children,
  ...rest
}: SheetProps) {
  const titleId = useId();
  const [scrolled, setScrolled] = useState(false);
  return (
    <OverlayLayer open={open} onClose={onClose} placement={placement} dismissOnScrim={dismissOnScrim}>
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={cx(
          styles.sheet,
          styles[variant],
          variant === 'side' && edge === 'start' && styles.start,
          className,
        )}
        style={width ? ({ '--sheet-w': `${width}px`, ...style } as CSSProperties) : style}
        {...rest}
      >
        <header className={styles.head} data-scrolled={scrolled}>
          <div className={styles.titles}>
            <h2 id={titleId} className={styles.title}>{title}</h2>
            {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          </div>
          {actions}
          <CloseButton onClick={onClose} aria-label={closeLabel} />
        </header>
        <div className={cx(styles.body, flush && styles.flush)} onScroll={(e) => setScrolled(e.currentTarget.scrollTop > 2)}>
          {children}
        </div>
        {footer && <footer className={styles.foot}>{footer}</footer>}
      </section>
    </OverlayLayer>
  );
}
