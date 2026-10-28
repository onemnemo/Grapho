import type { CSSProperties } from 'react';
import styles from './ThumbArt.module.css';

export type DocKind = 'plain' | 'chart' | 'highlight';

export interface DocArtProps { kind?: DocKind }

const at = (top: number, width = 76): CSSProperties => ({ top: `${top}%`, right: `${100 - 12 - width}%` });

/** First page of a PDF: title bar, text lines, and a chart or a highlighted passage. */
export function DocArt({ kind = 'plain' }: DocArtProps) {
  return (
    <div className={styles.doc}>
      <i className={`${styles.line} ${styles.bar}`} style={at(12, 38)} />
      <i className={styles.line} style={at(24)} />
      <i className={styles.line} style={at(30, 62)} />
      <i className={styles.line} style={at(36)} />
      {kind === 'chart' && (
        <svg className={styles.chart} viewBox="0 0 100 60" aria-hidden="true">
          <path d="M8 52 L40 10 L68 42" stroke="#5C8DF0" strokeWidth="3" fill="none" />
          <path d="M0 56H100" stroke="#23242A" strokeWidth="2" />
        </svg>
      )}
      {kind === 'highlight' && (
        <>
          <i className={`${styles.line} ${styles.hl}`} style={at(46, 52)} />
          <i className={styles.line} style={at(56)} />
          <i className={styles.line} style={at(64, 60)} />
        </>
      )}
    </div>
  );
}

/** A canvas thumbnail: dot grid with a few strokes and shapes. */
export function CanvasArt() {
  return (
    <div className={styles.canvas}>
      <svg viewBox="0 0 120 160" fill="none" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
        <path d="M16 50 Q26 36 38 50" stroke="#F28A2E" />
        <circle cx="82" cy="46" r="13" stroke="#D9487A" />
        <path d="M18 90 L44 74 L66 92 L100 72" stroke="#5C8DF0" />
        <path d="M20 118h26M20 130h20" stroke="#EDECE8" />
        <rect x="76" y="108" width="24" height="24" rx="5" stroke="#F6C453" />
      </svg>
    </div>
  );
}
