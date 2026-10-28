import { useId, type CSSProperties, type HTMLAttributes, type ReactNode } from 'react';
import { OverlayLayer, type OverlayPlacement } from '@/lib/OverlayLayer';
import { cx } from '@/lib/cx';
import styles from './Dialog.module.css';

export interface DialogProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  /** One or two plain sentences. */
  children?: ReactNode;
  /** The buttons, right aligned. Put the main one last. */
  actions: ReactNode;
  /** alertdialog for questions that must be answered; dialog for the rest. */
  role?: 'dialog' | 'alertdialog';
  placement?: OverlayPlacement;
  /** Clicking the scrim counts as closing. */
  dismissOnScrim?: boolean;
  /** Width in px. 440 by default; the studio's picture dialogs use 520. */
  width?: number;
  /** compact has 24px padding and a tighter title, for dialogs that hold a picture and controls. */
  density?: 'roomy' | 'compact';
}

/** A small centred question: title, a sentence, buttons. */
export function Dialog({
  open,
  onClose,
  title,
  children,
  actions,
  role = 'dialog',
  placement = 'viewport',
  dismissOnScrim = true,
  width,
  density = 'roomy',
  className,
  style,
  ...rest
}: DialogProps) {
  const titleId = useId();
  const bodyId = useId();
  return (
    <OverlayLayer open={open} onClose={onClose} placement={placement} layer="dialog" dismissOnScrim={dismissOnScrim}>
      <div
        role={role}
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={children ? bodyId : undefined}
        tabIndex={-1}
        className={cx(styles.dialog, density === 'compact' && styles.compact, className)}
        style={width ? ({ '--dialog-w': `${width}px`, ...style } as CSSProperties) : style}
        {...rest}
      >
        <h2 id={titleId} className={styles.title}>{title}</h2>
        {children && <div id={bodyId} className={styles.body}>{children}</div>}
        <div className={styles.actions}>{actions}</div>
      </div>
    </OverlayLayer>
  );
}
