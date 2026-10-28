import type { ReactNode } from 'react';
import styles from './Highlight.module.css';

/** Wraps the first match of `query` in text with a soft gold mark. Returns the text when nothing matches. */
export function Highlight({ text, query }: { text: string; query?: string }): ReactNode {
  const q = query?.trim();
  if (!q) return text;
  const i = text.toLowerCase().indexOf(q.toLowerCase());
  if (i < 0) return text;
  return (
    <>
      {text.slice(0, i)}
      <mark className={styles.mark}>{text.slice(i, i + q.length)}</mark>
      {text.slice(i + q.length)}
    </>
  );
}
