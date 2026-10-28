import type { CSSProperties, HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './PictureWorking.module.css';

export type PictureJob = 'removing' | 'converting';

export interface PictureWorkingProps extends HTMLAttributes<HTMLDivElement> {
  job: PictureJob;
  /** The picture on the page, in px: left, top, width and height. */
  box: { x: number; y: number; width: number; height: number };
  onCancel?: () => void;
}

const COPY: Record<PictureJob, { icon: 'cutout' | 'to_ink'; text: string }> = {
  removing: { icon: 'cutout', text: 'Removing background' },
  converting: { icon: 'to_ink', text: 'Converting to ink' },
};

/**
 * The working state of a picture on the page: a veil, one scan line that sweeps it, and a small pill under it with the job's own icon.
 * The picture itself stays where it is. Cancel is the only control.
 */
export function PictureWorking({ job, box, onCancel, className, style, ...rest }: PictureWorkingProps) {
  const c = COPY[job];
  return (
    <div className={cx(styles.root, className)} data-job={job} style={{ left: box.x, top: box.y, width: box.width, height: box.height, ...style } as CSSProperties} {...rest}>
      <div className={styles.veil} aria-hidden>
        <i className={styles.scan} />
      </div>
      <div role="status" className={styles.pill}>
        <span className={styles.icon}><Icon name={c.icon} size={18} /></span>
        <span>{c.text}</span>
        {onCancel && <button type="button" className={styles.cancel} onClick={onCancel}>Cancel</button>}
      </div>
    </div>
  );
}
