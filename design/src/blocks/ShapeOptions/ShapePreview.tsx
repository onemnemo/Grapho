import { InkLayer, SelectionOutline } from '../NotePage';
import { dashArray, type ShapeValues } from './shapeTypes';
import styles from './ShapePreview.module.css';

const fmt = (n: number) => n.toFixed(1);

function starPoints(cx: number, cy: number, R: number, n: number, sharp: number) {
  const out: string[] = [];
  for (let i = 0; i < n * 2; i++) {
    const a = -Math.PI / 2 + (i * Math.PI) / n;
    const r = i % 2 ? R * (1 - (0.25 + (sharp / 100) * 0.6)) : R;
    out.push(`${fmt(cx + r * Math.cos(a))},${fmt(cy + r * Math.sin(a))}`);
  }
  return out.join(' ');
}
const polyPoints = (cx: number, cy: number, R: number, n: number) =>
  [...Array(n)].map((_, i) => {
    const a = -Math.PI / 2 + (i * 2 * Math.PI) / n;
    return `${fmt(cx + R * Math.cos(a))},${fmt(cy + R * Math.sin(a))}`;
  }).join(' ');

/** The page art for a selected shape: a caption in handwriting, the shape, its selection box. */
export function ShapePreview({ values: v, caption = 'Mark the key steps' }: { values: ShapeValues; caption?: string }) {
  const cx = 200, cy = 290, R = 100;
  const common = {
    className: `${styles.shape} ${v.fill ? styles.filled : styles.open}`,
    strokeWidth: v.width,
    strokeDasharray: dashArray(v.outline, v.width),
  };
  const size = R * 2 + 16;
  return (
    <InkLayer width={800} height={500}>
      <text x="64" y="68" fontSize="40">{caption}</text>
      {v.kind === 'star' && <polygon points={starPoints(cx, cy, R, v.points, v.sharpness)} {...common} />}
      {v.kind === 'polygon' && <polygon points={polyPoints(cx, cy, R, v.sides)} {...common} />}
      {v.kind === 'box' && <rect x={cx - R} y={cy - R * 0.72} width={R * 2} height={R * 1.44} rx={v.radius} {...common} />}
      {v.kind === 'oval' && <ellipse cx={cx} cy={cy} rx={R} ry={R * 0.72} {...common} />}
      <SelectionOutline x={cx - R - 8} y={cy - R - 8} width={size} height={size} fill={false} />
    </InkLayer>
  );
}
