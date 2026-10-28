/* Springs, eases, the one frame loop and the turning dial. Same model as Compose spring():
   a = -k x - c v, c = 2 z sqrt(k), mass 1. */
import { clamp } from './math';

export class Spring {
  x: number;
  v = 0;
  t: number;
  constructor(x = 0, public k = 400, public z = 1) {
    this.x = x;
    this.t = x;
  }
  set(v: number) {
    this.x = v;
    this.t = v;
    this.v = 0;
  }
  to(t: number, k?: number, z?: number, v0?: number) {
    this.t = t;
    if (k != null) this.k = k;
    if (z != null) this.z = z;
    if (v0 != null) this.v = v0;
  }
  step(dt: number) {
    const c = 2 * this.z * Math.sqrt(this.k);
    const n = Math.max(1, Math.ceil(dt / 0.004));
    const h = dt / n;
    for (let i = 0; i < n; i++) {
      const a = -this.k * (this.x - this.t) - c * this.v;
      this.v += a * h;
      this.x += this.v * h;
    }
    if (Math.abs(this.x - this.t) < 0.0005 && Math.abs(this.v) < 0.005) {
      this.x = this.t;
      this.v = 0;
    }
  }
  get done() {
    return this.x === this.t && this.v === 0;
  }
}

/** A 0..1 value that is either sprung or tweened (a tween is used for short fades). */
export class Ease {
  s: Spring;
  private tw: { from: number; to: number; ms: number; e: number } | null = null;
  constructor(v = 0) {
    this.s = new Spring(v);
  }
  get x() {
    return this.s.x;
  }
  set(v: number) {
    this.tw = null;
    this.s.set(v);
  }
  spring(t: number, k = 400, z = 1) {
    this.tw = null;
    this.s.to(t, k, z);
  }
  tween(t: number, ms: number) {
    this.tw = { from: this.s.x, to: t, ms, e: 0 };
    this.s.t = t;
    this.s.v = 0;
  }
  step(dt: number) {
    const w = this.tw;
    if (w) {
      w.e += dt * 1000;
      const p = clamp(w.e / w.ms, 0, 1);
      this.s.x = w.from + (w.to - w.from) * (1 - Math.pow(1 - p, 3));
      if (p >= 1) {
        this.s.x = w.to;
        this.tw = null;
      }
    } else this.s.step(dt);
  }
  get done() {
    return !this.tw && this.s.done;
  }
}

export interface Tickable {
  /** Return false when there is nothing left to animate. */
  tick(dt: number): boolean | void;
}

/** One requestAnimationFrame loop for every wheel on the page. */
export const Loop = {
  items: new Set<Tickable>(),
  raf: 0,
  last: 0,
  add(o: Tickable) {
    this.items.add(o);
    if (!this.raf) {
      this.last = performance.now();
      this.raf = requestAnimationFrame(this.frame);
    }
  },
  remove(o: Tickable) {
    this.items.delete(o);
  },
  frame: (now: number) => {
    const L = Loop;
    const dt = Math.min(0.034, Math.max(0.001, (now - L.last) / 1000));
    L.last = now;
    for (const o of Array.from(L.items)) if (o.tick(dt) === false) L.items.delete(o);
    L.raf = L.items.size ? requestAnimationFrame(L.frame) : 0;
  },
};

interface SpinOptions {
  min?: number;
  max?: number;
  start?: number;
}

/** A dial in degrees: dragged 1:1, released with the finger's speed so it glides and settles on a slot. */
export class Spin {
  min: number;
  max: number;
  offset: number;
  slot: number;
  sp: Spring | null = null;
  onDetent: (() => void) | null = null;
  onChange: (() => void) | null = null;
  _wt?: number;
  constructor(public step: number, o: SpinOptions = {}) {
    this.min = o.min ?? -Infinity;
    this.max = o.max ?? Infinity;
    this.offset = o.start ?? 0;
    this.slot = Math.round(this.offset / step);
  }
  get moving() {
    return !!this.sp;
  }
  turnTo(v: number) {
    this.offset = v;
    const n = Math.round(v / this.step);
    if (n !== this.slot) {
      this.slot = n;
      this.onDetent?.();
    }
    this.onChange?.();
  }
  dragBy(d: number) {
    this.turnTo(clamp(this.offset + d, this.min, this.max));
  }
  grab() {
    this.sp = null;
  }
  glideTo(v: number) {
    v = clamp(v, this.min, this.max);
    this.sp = new Spring(this.offset, 220, 1);
    this.sp.t = v;
    this.onChange?.();
  }
  snapTo(v: number) {
    this.sp = null;
    this.offset = clamp(v, this.min, this.max);
    this.slot = Math.round(this.offset / this.step);
    this.onChange?.();
  }
  release(vel: number) {
    const from = this.offset;
    const step = this.step;
    let target = Math.round((from + vel * 0.3) / step) * step;
    if (vel > 60 && target < from) target = Math.ceil(from / step) * step;
    if (vel < -60 && target > from) target = Math.floor(from / step) * step;
    target = clamp(target, this.min, this.max);
    this.sp = new Spring(from, 200, 1);
    this.sp.v = vel;
    this.sp.t = target;
    this.onChange?.();
  }
  tick(dt: number) {
    if (!this.sp) return false;
    this.sp.step(dt);
    this.turnTo(clamp(this.sp.x, this.min, this.max));
    if (this.sp.done) this.sp = null;
    return true;
  }
}
