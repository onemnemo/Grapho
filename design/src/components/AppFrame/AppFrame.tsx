import type { HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import styles from './AppFrame.module.css';

export interface AppFrameProps extends HTMLAttributes<HTMLDivElement> {
  /** Round corners and a hairline, for a screen shown inside a page rather than full window. */
  framed?: boolean;
}

/** The surface a full screen sits on. Screens put their layout inside it. */
export function AppFrame({ framed = false, className, ...rest }: AppFrameProps) {
  return <div className={cx(styles.frame, framed && styles.framed, className)} {...rest} />;
}
