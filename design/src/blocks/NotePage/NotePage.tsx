import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { cx } from '@/lib/cx';
import styles from './NotePage.module.css';

export type NotePattern = 'blank' | 'lined' | 'grid' | 'dotted';
export type NotePaper = 'white' | 'cream' | 'slate' | 'dark' | 'black';

export interface NotePageProps extends HTMLAttributes<HTMLDivElement> {
  /** The page background: lines, grid or dots on the paper colour. Defaults to the plain page. */
  pattern?: NotePattern;
  paper?: NotePaper;
  /** Space the page gives up on each edge, in px. A full line bar takes 40 from the page; a floating bar takes none. */
  inset?: { top?: number; right?: number; bottom?: number; left?: number };
  children?: ReactNode;
}

/** The paper in a note frame. It fills the frame and eases out of the way of a bar that reserves space. */
export function NotePage({ inset, pattern, paper, className, style, children, ...rest }: NotePageProps) {
  return (
    <div
      className={cx(styles.page, className)}
      data-pattern={pattern}
      data-tone={paper === 'slate' || paper === 'dark' || paper === 'black' ? 'dark' : undefined}
      style={{ ...(paper ? { '--paper': `var(--page-${paper})` } : null), '--it': `${inset?.top ?? 0}px`, '--ir': `${inset?.right ?? 0}px`, '--ib': `${inset?.bottom ?? 0}px`, '--il': `${inset?.left ?? 0}px`, ...style } as CSSProperties}
      {...rest}
    >
      {children}
    </div>
  );
}
