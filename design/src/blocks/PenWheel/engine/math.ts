/* Small numeric helpers shared by the wheel engine. */
export const PI = Math.PI;
export const DEG = PI / 180;
export const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
export const mix = (a: number, b: number, t: number) => a + (b - a) * t;
export const smooth = (t: number) => t * t * (3 - 2 * t);
export const normDeg = (d: number) => ((((d + 540) % 360) + 360) % 360) - 180;
export const wrapDeg = (d: number, period: number) => {
  let r = d % period;
  if (r >= period / 2) r -= period;
  if (r < -period / 2) r += period;
  return r;
};
export const polar = (r: number, deg: number) => ({ x: r * Math.cos(deg * DEG), y: r * Math.sin(deg * DEG) });
export const hexOf = (c: string) => c.toUpperCase();

export { hexToRgb, hexToRgba } from '@/assets/wheel/colour';

/** A short haptic tick, only after the person has touched the page. */
export function tick(ms = 8) {
  try {
    if (navigator.userActivation && !navigator.userActivation.hasBeenActive) return;
    navigator.vibrate?.(ms);
  } catch {
    /* no haptics here */
  }
}
