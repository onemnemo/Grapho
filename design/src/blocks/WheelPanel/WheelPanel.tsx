import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { OverlayLayer } from '@/lib/OverlayLayer';
import { cx } from '@/lib/cx';
import styles from './WheelPanel.module.css';

export interface WheelPanelProps extends HTMLAttributes<HTMLDivElement> {
  /** Width in dp. */
  width: number;
  /** list is the pen list beside the wheel (8 padding); dialog is a centred panel (14 padding); form is a roomier centred panel (22 padding) for the picker and colours. */
  variant?: 'list' | 'dialog' | 'form';
  /** Where the panel grows from, as a CSS transform origin. */
  origin?: string;
  /** leave plays the exit. */
  state?: 'open' | 'leave';
  children: ReactNode;
}

/** The one surface everything that opens from the wheel shares. */
export function WheelPanel({ width, variant = 'list', origin, state = 'open', className, style, children, ...rest }: WheelPanelProps) {
  return (
    <div
      className={cx(styles.panel, styles[variant], className)}
      data-state={state}
      style={{ width, transformOrigin: origin, ...style } as CSSProperties}
      {...rest}
    >
      {children}
    </div>
  );
}

export interface PanelModalProps {
  open?: boolean;
  /** Press on the scrim or Escape. Without it the scrim does not dismiss. */
  onDismiss?: () => void;
  children: ReactNode;
}

const NOOP = () => {};

/** A scrim over the note with its panel centred, inside the nearest positioned parent. */
export function PanelModal({ open = true, onDismiss, children }: PanelModalProps) {
  return (
    <OverlayLayer open={open} onClose={onDismiss ?? NOOP} placement="container" dismissOnScrim={!!onDismiss} dismissOnEscape={!!onDismiss}>
      {children}
    </OverlayLayer>
  );
}
