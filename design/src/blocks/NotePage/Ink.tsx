import type { ReactNode, SVGProps } from 'react';
import { cx } from '@/lib/cx';
import styles from './Ink.module.css';

export interface InkLayerProps extends Omit<SVGProps<SVGSVGElement>, 'viewBox'> {
  width?: number;
  height?: number;
  children: ReactNode;
}

/** A drawing surface the size of the page. Children are SVG: text in Caveat, paths in the ink colour. */
export function InkLayer({ width = 800, height = 460, className, children, ...rest }: InkLayerProps) {
  return (
    <svg className={cx(styles.ink, className)} viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="xMinYMid slice" aria-hidden="true" {...rest}>
      {children}
    </svg>
  );
}

/** The sample page: a biology note with a title, a formula, three steps and a drawing. */
export function SampleInk() {
  return (
    <InkLayer>
      <text x="64" y="120" fontSize="46">Cell respiration</text>
      <path className={styles.stroke} d="M64 132c60 8 150 6 236-2" strokeWidth="3" />
      <text x="64" y="186" fontSize="30">glucose + 6 O₂  →  6 CO₂ + ATP</text>
      <text x="64" y="240" fontSize="28">1  glycolysis, in the cytoplasm</text>
      <text x="64" y="282" fontSize="28">2  Krebs cycle, matrix</text>
      <text x="64" y="324" fontSize="28">3  electron chain, inner membrane</text>
      <ellipse className={styles.stroke} cx="560" cy="300" rx="92" ry="48" strokeWidth="3" />
      <path className={styles.stroke} d="M490 298c10-24 20 24 30 0s20 24 30 0 20 24 30 0 20 24 30 0" strokeWidth="3" />
      <text x="500" y="384" fontSize="26">mitochondrion</text>
    </InkLayer>
  );
}

const HANDLES = (x: number, y: number, w: number, h: number) =>
  [[x, y], [x + w / 2, y], [x + w, y], [x + w, y + h / 2], [x + w, y + h], [x + w / 2, y + h], [x, y + h], [x, y + h / 2]];

/** A dashed selection rectangle with its eight handles, drawn over the page. */
export function SelectionOutline({ x, y, width, height, fill = true }: { x: number; y: number; width: number; height: number; fill?: boolean }) {
  return (
    <>
      <rect className={cx(styles.selRect, !fill && styles.noFill)} x={x} y={y} width={width} height={height} rx="4" />
      {HANDLES(x, y, width, height).map(([cx0, cy0], i) => (
        <circle key={i} className={styles.selHandle} cx={cx0} cy={cy0} r="5.5" />
      ))}
    </>
  );
}

/** A hand drawn stroke. It can draw itself in, and fade out when the clean shape replaces it. */
export function SketchStroke({ d, animate = false, faded = false }: { d: string; animate?: boolean; faded?: boolean }) {
  return <path className={cx(styles.sketch, animate && styles.draw, faded && styles.faded)} d={d} pathLength={1} />;
}

/** The clean shape a suggestion was accepted as. It fades in over the stroke it replaces. */
export function CrispShape({ children }: { children: ReactNode }) {
  return <g className={styles.crisp}>{children}</g>;
}
