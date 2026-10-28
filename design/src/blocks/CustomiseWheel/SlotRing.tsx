import styles from './CustomiseWheel.module.css';

const R = 9;
const GAP = 0.34;

const arc = (i: number, n: number) => {
  const a0 = (i / n) * Math.PI * 2 - Math.PI / 2 + GAP / 2;
  const a1 = ((i + 1) / n) * Math.PI * 2 - Math.PI / 2 - GAP / 2;
  const p = (a: number) => `${(12 + R * Math.cos(a)).toFixed(2)} ${(12 + R * Math.sin(a)).toFixed(2)}`;
  return `M${p(a0)}A${R} ${R} 0 0 1 ${p(a1)}`;
};

/** A small ring with one arc per slot on the wheel. The filled arcs are the tools that are on, so the count is read by looking, not by a sentence. */
export function SlotRing({ filled, total }: { filled: number; total: number }) {
  return (
    <svg className={styles.ring} width="24" height="24" viewBox="0 0 24 24" role="img" aria-label={`${filled} of ${total} slots used`}>
      {Array.from({ length: total }, (_, i) => <path key={i} d={arc(i, total)} className={styles.slot} data-on={i < filled || undefined} />)}
    </svg>
  );
}
