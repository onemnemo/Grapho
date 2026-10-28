import { useRef, useState, type PointerEvent } from 'react';
import styles from './Ink.module.css';

export interface StrokeLayerProps {
  /** Finished strokes as SVG path data, in px of the layer. */
  strokes: string[];
  /** A stroke began. */
  onStart?: () => void;
  /** A stroke ended; its path is handed over to keep. */
  onCommit?: (path: string) => void;
  disabled?: boolean;
}

/** A layer you can write on with a pen, a finger or a mouse. It only draws; keeping the strokes is up to the screen. */
export function StrokeLayer({ strokes, onStart, onCommit, disabled = false }: StrokeLayerProps) {
  const ref = useRef<SVGSVGElement>(null);
  const [path, setPath] = useState<string | null>(null);
  const at = (e: PointerEvent) => {
    const r = ref.current?.getBoundingClientRect();
    return `${(e.clientX - (r?.left ?? 0)).toFixed(1)} ${(e.clientY - (r?.top ?? 0)).toFixed(1)}`;
  };
  const end = () => {
    if (path) onCommit?.(path);
    setPath(null);
  };
  return (
    <svg
      ref={ref}
      className={styles.strokes}
      aria-hidden="true"
      onPointerDown={(e) => {
        if (disabled) return;
        e.currentTarget.setPointerCapture(e.pointerId);
        setPath(`M${at(e)}`);
        onStart?.();
      }}
      onPointerMove={(e) => path && setPath(`${path} L${at(e)}`)}
      onPointerUp={end}
      onPointerCancel={end}
    >
      {[...strokes, ...(path ? [path] : [])].map((d, i) => <path key={i} className={styles.live} d={d} />)}
    </svg>
  );
}
