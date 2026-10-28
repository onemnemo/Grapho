import type { CSSProperties, HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './Guides.module.css';

export interface RulerProps extends HTMLAttributes<HTMLDivElement> {
  /** Centre of the ruler on the page, in px. */
  x: number;
  y: number;
  /** Degrees, clockwise. */
  angle?: number;
  /** Length in cm. The scale is fixed: one cm is 38px. */
  length?: number;
  /** Show the angle while it is being turned. */
  turning?: boolean;
  onClose?: () => void;
}

const CM = 38;

/** The ruler on the page: a pale band with a measured edge. Drag it to move, turn it by the handle, close it with the cross. */
export function Ruler({ x, y, angle = 0, length = 14, turning = false, onClose, className, style, ...rest }: RulerProps) {
  const w = length * CM;
  const ticks = Array.from({ length: length * 10 + 1 }, (_, i) => i);
  return (
    <>
      <div className={cx(styles.ruler, className)} style={{ left: x, top: y, width: w, rotate: `${angle}deg`, ...style } as CSSProperties} role="group" aria-label="Ruler" {...rest}>
        <svg className={styles.ticks} viewBox={`0 0 ${w} 22`} width={w} height="22" aria-hidden>
          {ticks.map((i) => {
            const cm = i % 10 === 0, half = i % 5 === 0;
            return <line key={i} x1={i * (CM / 10)} x2={i * (CM / 10)} y1="0" y2={cm ? 14 : half ? 10 : 6} />;
          })}
          {Array.from({ length }, (_, i) => i + 1).map((n) => (
            <text key={n} x={n * CM} y="27" textAnchor="middle">{n}</text>
          ))}
        </svg>
        <span className={styles.turn} aria-hidden><Icon name="rotate" size={18} /></span>
        {onClose && (
          <button type="button" className={styles.close} aria-label="Close ruler" onClick={onClose}>
            <Icon name="close" size={16} />
          </button>
        )}
      </div>
      {turning && <span className={styles.readout} style={{ left: x, top: y - 62 }}>{Math.round(((angle % 360) + 360) % 360)}°</span>}
    </>
  );
}
