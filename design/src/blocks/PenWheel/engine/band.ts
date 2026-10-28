/* The opacity band: checker, then the ink fading from 100 to 10 percent along the arc, round ends.
   Drawn once per ink colour on a canvas; colours of the chrome are read from the wheel tokens. */
import { inkAt } from '@/assets/wheel/colour';
import { DEG, PI, clamp, mix, polar } from './math';
import { OP_FROM, OP_TO } from './geometry';

export interface BandColours {
  checkerA: string;
  checkerB: string;
  edge: string;
}

export function bandColours(el: Element): BandColours {
  const cs = getComputedStyle(el);
  const read = (n: string) => cs.getPropertyValue(n).trim();
  return { checkerA: read('--wh-checker-dark'), checkerB: read('--wh-checker-light'), edge: read('--wh-band-edge') };
}

export function drawBand(cv: HTMLCanvasElement, dp: number, rimR: number, ink: string, col: BandColours) {
  const dpr = 2;
  const ctx = cv.getContext('2d');
  if (!ctx) return;
  const c = dp / 2;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, dp, dp);
  const outer = rimR + 13;
  const innerR = rimR - 13;
  const a0 = OP_FROM * DEG;
  const a1 = OP_TO * DEG;
  ctx.save();
  ctx.beginPath();
  ctx.arc(c, c, outer, a0, a1, false);
  const e = polar(rimR, OP_TO);
  const s = polar(rimR, OP_FROM);
  ctx.arc(c + e.x, c + e.y, 13, a1, a1 + PI, false);
  ctx.arc(c, c, innerR, a1, a0, true);
  ctx.arc(c + s.x, c + s.y, 13, a0 + PI, a0 + 2 * PI, false);
  ctx.closePath();
  ctx.clip();
  ctx.fillStyle = col.checkerA;
  ctx.fillRect(0, 0, dp, dp);
  ctx.fillStyle = col.checkerB;
  const q = 7;
  for (let y = 0, r = 0; y < dp; y += q, r++) for (let x = 0, k = 0; x < dp; x += q, k++) if ((r + k) % 2 === 0) ctx.fillRect(x, y, q, q);
  /* the ink runs through the round ends too, so the colour fills the whole shape */
  const steps = 110;
  const cap = 14 / rimR;
  const b0 = a0 - cap;
  const b1 = a1 + cap;
  for (let i = 0; i < steps; i++) {
    const a = mix(b0, b1, i / steps);
    const da = (b1 - b0) / steps + 0.012;
    const t = clamp((a - a0) / (a1 - a0), 0, 1);
    ctx.fillStyle = inkAt(ink, 1 - 0.9 * t);
    ctx.beginPath();
    ctx.arc(c, c, outer + 2, a, a + da);
    ctx.arc(c, c, innerR - 2, a + da, a, true);
    ctx.closePath();
    ctx.fill();
  }
  ctx.restore();
  ctx.lineWidth = 1;
  ctx.strokeStyle = col.edge;
  ctx.beginPath();
  ctx.arc(c, c, outer - 0.5, a0, a1);
  ctx.arc(c + e.x, c + e.y, 12.5, a1, a1 + PI);
  ctx.arc(c, c, innerR + 0.5, a1, a0, true);
  ctx.arc(c + s.x, c + s.y, 12.5, a0 + PI, a0 + 2 * PI);
  ctx.closePath();
  ctx.stroke();
}
