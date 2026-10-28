/* A mock note for the wheel to sit on: dotted paper, a page of handwriting, and a canvas you can draw on. */
import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { SampleNote } from '@/assets/wheel/SampleNote';
import { IconButton } from '@/components/IconButton';
import type { WheelModel } from '@/blocks/PenWheel/engine/model';
import { Ink } from './ink';
import styles from './WheelNoteStage.module.css';

export interface WheelNoteStageProps {
  width?: number;
  height?: number;
  /** Draw with the wheel's live pen. Needs the wheel's model. */
  model?: WheelModel;
  /** Pressing the page while drawing. Return true to swallow the press (the wheel is being carried). */
  onInkDown?: () => boolean | void;
  /** Show the page of handwriting. */
  written?: boolean;
  /** Show the note's top bar with undo and redo. */
  bar?: boolean;
  children?: ReactNode;
}

export function WheelNoteStage({ width = 760, height = 520, model, onInkDown, written = true, bar = false, children }: WheelNoteStageProps) {
  const stage = useRef<HTMLDivElement>(null);
  const base = useRef<HTMLCanvasElement>(null);
  const live = useRef<HTMLCanvasElement>(null);
  const hit = useRef<HTMLDivElement>(null);
  const cursor = useRef<HTMLDivElement>(null);
  const ink = useRef<Ink | null>(null);
  const [, bump] = useState(0);
  const downRef = useRef(onInkDown);
  downRef.current = onInkDown;

  useEffect(() => {
    if (!model || !stage.current || !base.current || !live.current || !hit.current || !cursor.current) return;
    const i = new Ink({ base: base.current, live: live.current, hit: hit.current, cursor: cursor.current, stage: stage.current }, model, width, height);
    i.onDown = () => downRef.current?.();
    i.onStrokes = () => bump((n) => n + 1);
    ink.current = i;
    return () => { i.dispose(); ink.current = null; };
  }, [model, width, height]);

  return (
    <div ref={stage} className={styles.stage} style={{ width, height } as CSSProperties}>
      {written && <SampleNote width={width} height={height} style={{ opacity: 0.55 }} />}
      {model && (
        <>
          <canvas ref={base} className={styles.ink} width={width * 2} height={height * 2} style={{ width, height }} />
          <canvas ref={live} className={styles.ink} width={width * 2} height={height * 2} style={{ width, height }} />
          <div ref={cursor} className={styles.cursor} />
          <div ref={hit} className={styles.hit} />
        </>
      )}
      {bar && (
        <div className={styles.bar}>
          <span className={styles.title}>Adding two vectors<small>Vectors</small></span>
          <IconButton icon="undo" label="Undo" disabled={!ink.current?.canUndo} onClick={() => ink.current?.undo()} />
          <IconButton icon="redo" label="Redo" disabled={!ink.current?.canRedo} onClick={() => ink.current?.redo()} />
        </div>
      )}
      {children}
    </div>
  );
}
