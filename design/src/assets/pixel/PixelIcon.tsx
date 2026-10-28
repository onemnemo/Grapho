import type { SVGProps } from 'react';
import { pixelGrids, type PixelIconName } from './pixelGrids';

export interface PixelIconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: PixelIconName;
  /** Size of one cell in px. 2 inline, 3 chrome, 4 and up for features and covers. */
  cell?: number;
}

const pathFor = (name: PixelIconName) =>
  pixelGrids[name]
    .map((row, y) => [...row.matchAll(/#+/g)].map((r) => `M${r.index} ${y}h${r[0].length}v1h-${r[0].length}z`).join(''))
    .join('');

/** A crisp 7 by 7 pixel glyph in the current colour. Never used on tools. */
export function PixelIcon({ name, cell = 3, ...rest }: PixelIconProps) {
  const s = cell * 7;
  return (
    <svg width={s} height={s} viewBox="0 0 7 7" shapeRendering="crispEdges" aria-hidden="true" focusable="false" {...rest}>
      <path d={pathFor(name)} fill="currentColor" />
    </svg>
  );
}
