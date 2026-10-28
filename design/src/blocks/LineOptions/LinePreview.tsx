import { useId } from 'react';
import { InkLayer } from '../NotePage';
import { dashArray } from '../ShapeOptions';
import { headSize, type LineEnding, type LineValues } from './lineTypes';
import styles from './LinePreview.module.css';

const A: [number, number] = [90, 400];
const B: [number, number] = [410, 250];

const pathFor = (p: LineValues['path']) =>
  p === 'straight'
    ? `M${A[0]} ${A[1]} L${B[0]} ${B[1]}`
    : p === 'curved'
      ? `M${A[0]} ${A[1]} C${A[0] + 60},${A[1] - 190} ${B[0] - 120},${B[1] + 150} ${B[0]},${B[1]}`
      : `M${A[0]} ${A[1]} H${(A[0] + B[0]) / 2} V${B[1]} H${B[0]}`;

/** Arrowheads are sized from the line width (see headSize), so they follow the stroke instead of staying fixed. */
function Markers({ id, width }: { id: string; width: number }) {
  const size = headSize(width);
  const pen = (width * 10) / size;
  const common = { viewBox: '0 0 10 10', markerUnits: 'userSpaceOnUse' as const, orient: 'auto-start-reverse', markerWidth: size, markerHeight: size, refY: 5 };
  return (
    <defs className={styles.defs}>
      <marker id={`${id}-arrow`} refX={8} {...common}><path className={styles.fill} d="M0 0l10 5-10 5z" /></marker>
      <marker id={`${id}-open`} refX={9} {...common}><path className={styles.open} d="M1 1l8 4-8 4" strokeWidth={pen} /></marker>
      <marker id={`${id}-dot`} refX={5} {...common}><circle className={styles.fill} cx="5" cy="5" r="4" /></marker>
      <marker id={`${id}-square`} refX={5} {...common}><rect className={styles.fill} x="1.5" y="1.5" width="7" height="7" /></marker>
      <marker id={`${id}-diamond`} refX={5} {...common}><path className={styles.fill} d="M5 0l5 5-5 5-5-5z" /></marker>
      <marker id={`${id}-bar`} refX={5} {...common}><path className={styles.open} d="M5 0v10" strokeWidth={pen} /></marker>
    </defs>
  );
}

/** The page art for a selected line: a caption, the line with its endings, and its two end handles. */
export function LinePreview({ values: v, caption = 'Draw the flow' }: { values: LineValues; caption?: string }) {
  const id = useId().replace(/:/g, '');
  const ref = (e: LineEnding) => (e === 'none' ? undefined : `url(#${id}-${e})`);
  return (
    <InkLayer width={800} height={500}>
      <Markers id={id} width={v.width} />
      <text x="64" y="68" fontSize="40">{caption}</text>
      <path
        className={styles.line}
        d={pathFor(v.path)}
        strokeWidth={v.width}
        strokeLinecap={v.outline === 'dotted' ? 'round' : 'butt'}
        strokeDasharray={dashArray(v.outline, v.width)}
        markerStart={ref(v.start)}
        markerEnd={ref(v.end)}
      />
      {[A, B].map((p, i) => <circle key={i} className={styles.handle} cx={p[0]} cy={p[1]} r="6" />)}
    </InkLayer>
  );
}
