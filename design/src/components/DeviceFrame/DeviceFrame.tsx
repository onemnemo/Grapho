import type { CSSProperties, HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './DeviceFrame.module.css';

export interface DeviceFrameProps extends HTMLAttributes<HTMLDivElement> {
  width?: number;
  height?: number;
  /** stage is the black behind a note page; screen is the app's page colour with a gutter. */
  tone?: 'stage' | 'screen';
  /** Round it like a tablet or a phone. */
  shape?: 'tablet' | 'phone';
}

/** Storybook and screen helper: a fixed-size window that overlays and the note page are placed inside. */
export function DeviceFrame({ width = 1040, height = 640, tone = 'stage', shape = 'tablet', className, style, children, ...rest }: DeviceFrameProps) {
  return (
    <div
      className={cx(styles.frame, styles[tone], styles[shape], className)}
      style={{ '--fw': `${width}px`, '--fh': `${height}px`, ...style } as CSSProperties}
      {...rest}
    >
      {children}
    </div>
  );
}
