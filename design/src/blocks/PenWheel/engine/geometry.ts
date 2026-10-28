/* Wheel geometry and value mappings, in dp. Mirrors the Android app (PenWheel*.kt).
   The layout is drawn for a wheel docked on the left; the other edges turn and mirror it. */
import { clamp, normDeg, polar, wrapDeg } from './math';

export const DISC_R = 170;
export const INNER_R = 116;
export const CORE_R = 58;
export const RIM_R = 143;
export const DOCK_TOP = 36;
export const POPUP_R = 150;
export const DISC_CY = DOCK_TOP + DISC_R;
export const RIM_STEP = 17;
export const TOOL_STEP = 38;
export const TOOL_REACH = 100;
export const GAP = 16;
export const SIZE_FROM = -86;
export const SIZE_TO = -18;
export const OP_FROM = 20;
export const OP_TO = 84;
export const PULL_OUT = 72;
export const DOCK_GAP = 110;
export const CARRY_SCALE = 0.04;
export const DOCKED_SCALE = DISC_R / POPUP_R;
export const TAB_W = 28;
export const TAB_H = 72;
export const GRIP_W = 30;
export const PIN_W = 32;
export const HANDLE_H = 30;
export const HANDLE_W = GRIP_W + PIN_W + 1;
export const HANDLE_GAP = 2;
export const GRIP_SLOP = 4;
export const TOUCH_SLOP = 8;
export const HOLD_MS = 380;
export const ALPHA_MIN = 26;
export const ALPHA_MAX = 255;
export const DOT_NUMBERS = [1, 15, 29, 44, 58, 72, 87, 100];
export const ERASER_STEPS = [4, 7, 12, 20, 35, 60];
export const ERASER_FROM = -86;
export const ERASER_TO = -18;
export const WHOLE_AT = 40;
export const PART_AT = 74;

export type Edge = 'left' | 'top' | 'right' | 'bottom';

export const EDGES: Record<Edge, { face: number; mirror: 1 | -1; vertical: boolean }> = {
  left: { face: 0, mirror: 1, vertical: true },
  top: { face: 90, mirror: -1, vertical: false },
  right: { face: 180, mirror: -1, vertical: true },
  bottom: { face: -90, mirror: 1, vertical: false },
};
export const toScreen = (edge: Edge, deg: number) => normDeg(EDGES[edge].face + EDGES[edge].mirror * deg);
export const fromScreen = (edge: Edge, deg: number) => normDeg(EDGES[edge].mirror * (deg - EDGES[edge].face));

export interface Geo {
  full: boolean;
  r: number;
  k: number;
  core: number;
  toolR: number;
  rimR: number;
  swatchR: number;
  inner: number;
  sizeBtn: number;
  rainbow: number;
}
export function makeGeo(full: boolean): Geo {
  const r = full ? POPUP_R : DISC_R;
  const k = r / DISC_R;
  return {
    full,
    r,
    k,
    core: CORE_R * k,
    toolR: 87 * k,
    rimR: RIM_R * k,
    swatchR: 13 * k,
    inner: INNER_R * k,
    sizeBtn: full ? -90 : -80,
    rainbow: full ? 90 : 80,
  };
}

export const stepOf = (full: boolean, n: number) => (full && n > 0 ? 360 / n : RIM_STEP);
export const swatchFade = (g: Geo, a: number, step: number) => {
  if (g.full) {
    const d = Math.min(Math.abs(normDeg(a + 90)), Math.abs(normDeg(a - 90)));
    return clamp((d - GAP) / step, 0, 1);
  }
  return clamp((4 * RIM_STEP - Math.abs(a)) / RIM_STEP, 0, 1);
};
export const toolAngle = (full: boolean, i: number, off: number, n: number) => (full ? wrapDeg(i * (360 / n) + off, 360) : i * TOOL_STEP + off);
export const toolFade = (full: boolean, a: number) => (full ? 1 : clamp((TOOL_REACH - Math.abs(a)) / 20, 0, 1));

export interface Placement {
  docked: boolean;
  edge: Edge;
  /** Position of the disc centre along its edge. */
  along: number;
  floatX: number;
  floatY: number;
}
export const clampAlong = (a: number, len: number) => (len >= DISC_CY + DISC_R ? clamp(a, DISC_CY, len - DISC_R) : len / 2);
export const clampCircleX = (x: number, W: number) => clamp(x, Math.min(POPUP_R, W / 2), Math.max(POPUP_R, W - POPUP_R));
export const clampCircleY = (y: number, H: number) => clamp(y, Math.min(POPUP_R + DOCK_TOP, H / 2), Math.max(POPUP_R + DOCK_TOP, H - POPUP_R));
const gapTo = (e: Edge, x: number, y: number, W: number, H: number) => (e === 'left' ? x : e === 'top' ? y : e === 'right' ? W - x : H - y);
export function dockTarget(x: number, y: number, W: number, H: number): Edge | null {
  let best: Edge | null = null;
  let bd = 1e9;
  for (const e of ['left', 'top', 'right', 'bottom'] as Edge[]) {
    const d = gapTo(e, x, y, W, H);
    if (d < bd) {
      bd = d;
      best = e;
    }
  }
  return bd < DOCK_GAP ? best : null;
}
export const edgePoint = (e: Edge, along: number, W: number, H: number) =>
  e === 'left' ? { x: 0, y: along } : e === 'right' ? { x: W, y: along } : e === 'top' ? { x: along, y: 0 } : { x: along, y: H };
export function handleTopLeft(radius: number, edge: Edge | null) {
  const u = polar(1, edge ? toScreen(edge, -45) : -45);
  const reach = (HANDLE_W / 2) * Math.abs(u.x) + (HANDLE_H / 2) * Math.abs(u.y);
  const d = radius + HANDLE_GAP + reach;
  return { x: u.x * d - HANDLE_W / 2, y: u.y * d - HANDLE_H / 2 };
}

export const PEN_RANGE: [number, number] = [0.3, 12];
export const HL_RANGE: [number, number] = [4, 30];
const rangeOf = (hl: boolean) => (hl ? HL_RANGE : PEN_RANGE);
export const numberOfWidth = (hl: boolean, pt: number) => {
  const [a, b] = rangeOf(hl);
  return clamp(Math.round(1 + (Math.log(clamp(pt, a, b) / a) / Math.log(b / a)) * 99), 1, 100);
};
export const widthOfNumber = (hl: boolean, n: number) => {
  const [a, b] = rangeOf(hl);
  return a * Math.pow(b / a, clamp((n - 1) / 99, 0, 1));
};
export const dotRadius = (n: number) => 2 + 7 * clamp((n - 1) / 99, 0, 1);
export const sizeAngle = (n: number) => SIZE_FROM + ((n - 1) / 99) * (SIZE_TO - SIZE_FROM);
export const sizeNumberAt = (deg: number) => 1 + clamp((deg - SIZE_FROM) / (SIZE_TO - SIZE_FROM), 0, 1) * 99;
export const alphaAt = (deg: number) => clamp(Math.round((100 - 90 * clamp((deg - OP_FROM) / (OP_TO - OP_FROM), 0, 1)) * 2.55), ALPHA_MIN, ALPHA_MAX);
export const angleOfAlpha = (a: number) => OP_FROM + (1 - clamp((a / 2.55 - 10) / 90, 0, 1)) * (OP_TO - OP_FROM);
export const eraserAngle = (i: number) => ERASER_FROM + ((ERASER_TO - ERASER_FROM) * i) / (ERASER_STEPS.length - 1);
export const eraserRadius = (i: number) => dotRadius(1 + (99 * i) / (ERASER_STEPS.length - 1));
export const eraserPtAt = (deg: number) => 4 * Math.pow(60 / 4, clamp((deg - ERASER_FROM) / (ERASER_TO - ERASER_FROM), 0, 1));
export const nearestEraserStep = (pt: number) => {
  let b = 0;
  let bd = 1e9;
  ERASER_STEPS.forEach((s, i) => {
    const d = Math.abs(Math.log(pt / s));
    if (d < bd) {
      bd = d;
      b = i;
    }
  });
  return b;
};
export const nearestDot = (n: number) => DOT_NUMBERS.reduce((b, d) => (Math.abs(d - n) < Math.abs(b - n) ? d : b), 1);
