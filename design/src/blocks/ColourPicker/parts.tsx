/* The picker's controls: the saturation and brightness square, and the 32dp bars for hue and opacity. */
import { useEffect, useRef, useState, type CSSProperties, type PointerEvent } from 'react';
import { clamp } from '@/blocks/PenWheel/engine/math';
import { hsvToHex, paintSquare } from '@/assets/wheel/colour';
import styles from './ColourPicker.module.css';

/** Pointer capture and normalised position for a draggable surface. */
export function useDrag(onMove: (x: number, y: number, down: boolean) => void) {
  const [drag, setDrag] = useState(false);
  const at = (e: PointerEvent<HTMLElement>, down: boolean) => {
    const b = e.currentTarget.getBoundingClientRect();
    onMove(clamp((e.clientX - b.left) / b.width, 0, 1), clamp((e.clientY - b.top) / b.height, 0, 1), down);
  };
  return {
    drag,
    props: {
      onPointerDown: (e: PointerEvent<HTMLElement>) => {
        if (e.button > 0) return;
        e.preventDefault();
        e.currentTarget.setPointerCapture(e.pointerId);
        setDrag(true);
        at(e, true);
      },
      onPointerMove: (e: PointerEvent<HTMLElement>) => drag && at(e, false),
      onPointerUp: () => setDrag(false),
      onPointerCancel: () => setDrag(false),
    },
  };
}

export interface SquareProps {
  width: number;
  height: number;
  hue: number;
  sat: number;
  val: number;
  onChange: (sat: number, val: number) => void;
}

export function SVSquare({ width, height, hue, sat, val, onChange }: SquareProps) {
  const cv = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    if (cv.current) paintSquare(cv.current, width, height, hue);
  }, [hue, width, height]);
  const { drag, props } = useDrag((x, y) => onChange(x, 1 - y));
  return (
    <div className={styles.square} data-drag={drag || undefined} style={{ width, height }} {...props} role="presentation">
      <canvas ref={cv} width={width * 2} height={height * 2} style={{ width, height }} />
      <i className={styles.squareThumb} style={{ transform: `translate3d(${sat * width}px,${(1 - val) * height}px,0)`, '--c': hsvToHex(hue, sat, val) } as CSSProperties} />
    </div>
  );
}

export interface BarProps {
  kind: 'hue' | 'opacity';
  /** 0 to 1 along the bar. */
  value: number;
  onChange: (value: number) => void;
  /** Opacity bar: the gradient from clear to the ink, as a CSS background. */
  gradient?: string;
  label: string;
}

export function ColourBar({ kind, value, onChange, gradient, label }: BarProps) {
  const { drag, props } = useDrag((x) => onChange(x));
  return (
    <div
      className={`${styles.bar} ${kind === 'hue' ? styles.hue : styles.opacity}`}
      data-drag={drag || undefined}
      style={{ '--f': value, '--g': gradient ?? 'none' } as CSSProperties}
      role="slider"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(value * 100)}
      tabIndex={0}
      onKeyDown={(e) => {
        const d = e.key === 'ArrowRight' || e.key === 'ArrowUp' ? 0.02 : e.key === 'ArrowLeft' || e.key === 'ArrowDown' ? -0.02 : 0;
        if (d) { e.preventDefault(); onChange(clamp(value + d, 0, 1)); }
      }}
      {...props}
    >
      <i className={styles.barThumb} />
    </div>
  );
}
