import type { CSSProperties, HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './Guides.module.css';

export interface ProtractorProps extends HTMLAttributes<HTMLDivElement> {
  /** Centre of the baseline on the page, in px. */
  x: number;
  y: number;
  /** Degrees the whole protractor is turned, clockwise. */
  angle?: number;
  /** The angle being read, 0 to 180. Shows a line from the centre and a readout at its end. */
  reading?: number;
  onClose?: () => void;
}

const R = 140;
const PAD = 22;
const polar = (deg: number, r: number) => [R + PAD + Math.cos((deg * Math.PI) / 180) * r, R + PAD - Math.sin((deg * Math.PI) / 180) * r];

/** The protractor: a half disc with a tick every 5 degrees and a number every 30. Place two fingers or the pen on an edge to read an angle. */
export function Protractor({ x, y, angle = 0, reading, onClose, className, style, ...rest }: ProtractorProps) {
  const w = (R + PAD) * 2;
  const h = R + PAD + 20;
  const [ax, ay] = polar(0, R), [bx, by] = polar(180, R);
  const ticks = Array.from({ length: 37 }, (_, i) => i * 5);
  const [rx, ry] = reading == null ? [0, 0] : polar(reading, R + 4);
  const [lx, ly] = reading == null ? [0, 0] : polar(reading, R + 26);
  return (
    <div className={cx(styles.protractor, className)} style={{ left: x, top: y, width: w, height: h, transformOrigin: `${R + PAD}px ${R + PAD}px`, rotate: `${angle}deg`, translate: `-${R + PAD}px -${R + PAD}px`, ...style } as CSSProperties} role="group" aria-label="Protractor" {...rest}>
      <svg className={styles.disc} viewBox={`0 0 ${w} ${h}`} width={w} height={h} aria-hidden>
        <path className={styles.face} d={`M${bx} ${by}A${R} ${R} 0 0 1 ${ax} ${ay}L${bx} ${by}Z`} />
        {ticks.map((d) => {
          const big = d % 30 === 0, mid = d % 10 === 0;
          const [x1, y1] = polar(d, R), [x2, y2] = polar(d, R - (big ? 16 : mid ? 11 : 7));
          return <line key={d} className={styles.tick} x1={x1} y1={y1} x2={x2} y2={y2} />;
        })}
        {[0, 30, 60, 90, 120, 150, 180].map((d) => {
          const [tx, ty] = polar(d, R - 30);
          return <text key={d} className={styles.num} x={tx} y={ty + 3.5} textAnchor="middle">{d}</text>;
        })}
        <path className={styles.cross} d={`M${R + PAD - 7} ${R + PAD}h14M${R + PAD} ${R + PAD - 7}v14`} />
        {reading != null && (
          <>
            <line className={styles.read} x1={R + PAD} y1={R + PAD} x2={rx} y2={ry} />
            <text className={styles.value} x={lx} y={ly + 4} textAnchor="middle">{Math.round(reading)}°</text>
          </>
        )}
      </svg>
      <span className={styles.turnP} aria-hidden><Icon name="rotate" size={18} /></span>
      {onClose && (
        <button type="button" className={styles.closeP} aria-label="Close protractor" onClick={onClose}>
          <Icon name="close" size={16} />
        </button>
      )}
    </div>
  );
}
