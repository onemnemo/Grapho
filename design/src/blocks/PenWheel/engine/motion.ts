/* Carry and placement motion (PenWheelCarry.kt WheelMotion). The wheel is lifted and follows the
   finger with no smoothing; on release it glides on from the speed it left with. */
import { tick } from './math';
import { EDGES, POPUP_R, PULL_OUT, clampAlong, clampCircleX, clampCircleY, dockTarget, edgePoint, type Edge, type Placement } from './geometry';
import { Ease, Spring } from './springs';

export interface MotionHost {
  p: Placement;
  W: number;
  H: number;
  setFlag(name: 'sliding' | 'carrying', on: boolean): void;
  commit(p: Placement): void;
  carry(on: boolean): void;
  wake(): void;
  markDirty(): void;
  /** Register an ease so the frame loop steps it. */
  track(...e: Ease[]): void;
}

export interface Pose {
  shape: number;
  lift: number;
  hint?: number;
  centre: { x: number; y: number };
  hintEdge?: Edge;
  slide?: boolean;
}

type Mode = 'rest' | 'slide' | 'free' | 'land';
interface Pt { x: number; y: number }

export class Motion {
  mode: Mode = 'rest';
  held = false;
  active = false;
  hintEdge: Edge | null = null;
  target: Edge | null = null;
  pose: Pose | null = null;
  morph: Ease;
  liftE = new Ease(0);
  settle = new Ease(1);
  hint = new Ease(0);
  private px = new Spring(0, 400, 1);
  private py = new Spring(0, 400, 1);
  private fx = 0; private fy = 0; private sx = 0; private sy = 0; private gx = 0; private gy = 0; private cx = 0; private cy = 0;
  private alongStart = 0;
  private samples: Array<{ t: number; x: number; y: number }> = [];

  constructor(private w: MotionHost) {
    this.morph = new Ease(w.p.docked ? 0 : 1);
    w.track(this.morph, this.liftE, this.settle, this.hint);
  }
  get shape() {
    if (this.pose) return this.pose.shape;
    return this.mode === 'rest' ? (this.w.p.docked ? 0 : 1) : this.morph.x;
  }
  get lift() {
    return this.pose ? this.pose.lift : this.liftE.x;
  }
  get hintFade() {
    return this.pose ? this.pose.hint || 0 : this.hint.x;
  }
  private lenOf(e: Edge) {
    return EDGES[e].vertical ? this.w.H : this.w.W;
  }
  private spot(dockedOn: boolean, on: Edge, at: number, fx: number, fy: number): Pt {
    const { W, H } = this.w;
    return dockedOn ? edgePoint(on, clampAlong(at, this.lenOf(on)), W, H) : { x: clampCircleX(fx, W), y: clampCircleY(fy, H) };
  }
  get centre(): Pt {
    if (this.pose) return this.pose.centre;
    const p = this.w.p;
    switch (this.mode) {
      case 'rest': return this.spot(p.docked, p.edge, p.along, p.floatX, p.floatY);
      case 'slide': return this.spot(true, p.edge, this.slideAlong(), p.floatX, p.floatY);
      case 'free': {
        const k = 1 - this.settle.x;
        return { x: this.fx - this.gx + this.cx * k, y: this.fy - this.gy + this.cy * k };
      }
      default: return { x: this.px.x, y: this.py.x };
    }
  }
  hintCentre(on: Edge) {
    const c = this.centre;
    return this.spot(true, on, EDGES[on].vertical ? c.y : c.x, 0, 0);
  }
  private slideAlong() {
    const p = this.w.p;
    const moved = EDGES[p.edge].vertical ? this.fy - this.sy : this.fx - this.sx;
    return clampAlong(this.alongStart + moved, this.lenOf(p.edge));
  }
  begin(f: Pt) {
    const w = this.w;
    const p = w.p;
    if (this.held) return false;
    const c = this.centre;
    const sliding = this.mode === 'rest' && p.docked;
    this.target = null;
    this.hint.set(0);
    this.fx = this.sx = f.x; this.fy = this.sy = f.y;
    this.gx = f.x - c.x; this.gy = f.y - c.y;
    this.cx = this.cy = 0;
    this.samples = [{ t: performance.now(), x: f.x, y: f.y }];
    this.held = true;
    this.active = true;
    if (sliding) {
      this.alongStart = clampAlong(p.along, this.lenOf(p.edge));
      this.morph.set(0); this.liftE.set(0);
      this.mode = 'slide';
      w.setFlag('sliding', true);
    } else {
      if (this.mode === 'rest') { this.morph.set(1); this.liftE.set(0); }
      this.settle.set(1);
      this.mode = 'free';
      this.morph.spring(1, 400, 1);
      this.liftE.spring(1, 420, 0.8);
    }
    w.setFlag('carrying', true);
    w.carry(true);
    w.wake();
    tick(14);
    return true;
  }
  move(f: Pt) {
    if (!this.held) return;
    this.fx = f.x; this.fy = f.y;
    const now = performance.now();
    this.samples.push({ t: now, x: f.x, y: f.y });
    while (this.samples.length > 2 && now - this.samples[0].t > 90) this.samples.shift();
    const p = this.w.p;
    if (this.mode === 'slide') {
      const e = p.edge;
      const into = e === 'left' ? f.x - this.sx : e === 'right' ? this.sx - f.x : e === 'top' ? f.y - this.sy : this.sy - f.y;
      if (into >= PULL_OUT) this.pullOut();
    } else if (this.mode === 'free') this.aim(true);
    this.w.markDirty();
    this.w.wake();
  }
  private velocity(): Pt {
    const s = this.samples;
    if (s.length < 2) return { x: 0, y: 0 };
    const a = s[0];
    const b = s[s.length - 1];
    const dt = (b.t - a.t) / 1000;
    if (dt < 0.012 || performance.now() - b.t > 80) return { x: 0, y: 0 };
    return { x: (b.x - a.x) / dt, y: (b.y - a.y) / dt };
  }
  end() {
    if (!this.held) return;
    this.held = false;
    const w = this.w;
    if (this.mode === 'slide') {
      this.commit({ docked: true, edge: w.p.edge, along: this.slideAlong(), floatX: w.p.floatX, floatY: w.p.floatY });
      this.mode = 'rest';
      this.active = false;
      w.setFlag('sliding', false);
      w.setFlag('carrying', false);
      w.carry(false);
    } else if (this.mode === 'free') this.land(this.centre, this.velocity());
    w.markDirty();
    w.wake();
  }
  reset() {
    this.held = false;
    this.target = null;
    this.mode = 'rest';
    this.morph.set(this.w.p.docked ? 0 : 1);
    this.liftE.set(0);
    this.hint.set(0);
    this.active = false;
    this.w.setFlag('sliding', false);
    this.w.setFlag('carrying', false);
  }
  private land(c: Pt, v: Pt) {
    const w = this.w;
    const p = w.p;
    const dockOn = this.target;
    let toEdge: boolean;
    let at: Pt;
    if (dockOn) {
      const along = clampAlong(EDGES[dockOn].vertical ? c.y : c.x, this.lenOf(dockOn));
      this.commit({ docked: true, edge: dockOn, along, floatX: p.floatX, floatY: p.floatY });
      at = this.spot(true, dockOn, along, 0, 0);
      toEdge = true;
    } else {
      const fx = clampCircleX(c.x, w.W);
      const fy = clampCircleY(c.y, w.H);
      this.commit({ docked: false, edge: p.edge, along: p.along, floatX: fx, floatY: fy });
      at = { x: fx, y: fy };
      toEdge = false;
    }
    this.target = null;
    this.mode = 'land';
    const k = toEdge ? 420 : 360;
    const z = toEdge ? 1 : 0.82;
    this.px.x = c.x; this.py.x = c.y; this.px.v = v.x; this.py.v = v.y;
    this.px.to(at.x, k, z);
    this.py.to(at.y, k, z);
    this.morph.spring(toEdge ? 0 : 1, 400, 1);
    this.liftE.spring(0, 420, 1);
    this.hint.tween(0, 140);
  }
  private pullOut() {
    const c = this.centre;
    let gx = this.fx - c.x;
    let gy = this.fy - c.y;
    const cap = 0.5 * POPUP_R;
    const len = Math.hypot(gx, gy);
    if (len > cap) { gx *= cap / len; gy *= cap / len; }
    this.gx = gx; this.gy = gy;
    this.cx = c.x - (this.fx - gx);
    this.cy = c.y - (this.fy - gy);
    this.settle.set(0);
    this.settle.spring(1, 400, 1);
    this.morph.spring(1, 400, 1);
    this.liftE.spring(1, 420, 0.8);
    this.mode = 'free';
    this.w.setFlag('sliding', false);
    tick(12);
    this.aim(false);
  }
  private aim(doTick: boolean) {
    const c = this.centre;
    const near = dockTarget(c.x, c.y, this.w.W, this.w.H);
    if (near === this.target) return;
    this.target = near;
    if (near) {
      this.hintEdge = near;
      this.hint.set(0);
      this.hint.tween(1, 140);
    } else this.hint.tween(0, 140);
    if (doTick) tick(8);
  }
  private commit(p: Placement) {
    this.w.commit(p);
  }
  tick(dt: number) {
    if (this.mode === 'land') {
      this.px.step(dt);
      this.py.step(dt);
      if (this.px.done && this.py.done && this.morph.done && this.liftE.done) {
        this.mode = 'rest';
        this.morph.set(this.w.p.docked ? 0 : 1);
        this.liftE.set(0);
        this.active = false;
        this.w.setFlag('carrying', false);
        this.w.carry(false);
      }
    }
    return this.mode !== 'rest' || !this.morph.done || !this.liftE.done || !this.hint.done || !this.settle.done;
  }
}
