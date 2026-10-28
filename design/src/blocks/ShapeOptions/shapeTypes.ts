export type ShapeKind = 'star' | 'polygon' | 'box' | 'oval';
export type OutlineStyle = 'solid' | 'dashed' | 'dotted';

export interface ShapeValues {
  kind: ShapeKind;
  /** Star: number of points. */
  points: number;
  /** Star: how deep the notches cut, 10 to 90. */
  sharpness: number;
  /** Polygon: number of sides. */
  sides: number;
  /** Box: corner radius in dp. */
  radius: number;
  outline: OutlineStyle;
  /** Stroke width in dp. */
  width: number;
  fill: boolean;
}

export const defaultShape: ShapeValues = { kind: 'star', points: 5, sharpness: 50, sides: 6, radius: 16, outline: 'solid', width: 4, fill: false };

/** Dash pattern for an outline style at a stroke width. Dotted uses round caps and a near zero dash. */
export const dashArray = (o: OutlineStyle, w: number) => (o === 'dashed' ? `${w * 3} ${w * 2}` : o === 'dotted' ? `0.1 ${w * 2.2}` : undefined);
