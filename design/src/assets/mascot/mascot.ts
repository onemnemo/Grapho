import { clamp, reduceMotion } from './motion';
import { forget, visibility } from './visibility';

/**
 * The Grapho pixel mascot: a 13 x 14 sprite with a shimmering rainbow body.
 *
 * The sprite is built from parts (legs, arms, body, face) every frame, so poses stay consistent and the
 * animations are just lists of poses with durations. The Assets/Mascot story shows every animation.
 *
 * Layout (sprite pixels): the body is 9 x 8 at x 2..10, y 3..10, with the little notch at the top left. Three rows of
 * headroom above it (arms up, a carried block) and three rows of legs below. Feet sit on row `FOOT_Y` (13) in every
 * grounded frame, so ground alignment is exact: put the canvas bottom on the ground and you are done.
 * Canvas aspect is 13 : 14. Draw it at a whole multiple (5x = 65 x 70 px, 8x = 104 x 112 px).
 *
 * Usage
 *   const m = new Mascot(canvas, seed);        // resizes the canvas to SPRITE_W x SPRITE_H itself
 *   m.play('walk');                             // loops by default for looping anims (idle, walk, wave ...)
 *   m.play('land', { onDone: () => ... });      // one-shots hold their last frame, then run `after` if the anim has one
 *   m.play('walk', { speed: 1.5 });             // speed multiplier; calling play() with the running looping anim does not restart it
 *   m.expression = 'happy' | null;             // override the eyes for every frame (null = whatever the animation says)
 *   m.face(-1);                                 // mirror the sprite itself (no CSS flip needed); m.facing is 1 or -1
 *   m.look = -1 | 0 | 1;                        // pupils in sprite space (1 = the way the sprite faces, 0 = at the viewer)
 *   m.onFrame = (frame, i) => ...;              // fires on entering every frame; frame.adv is the walking step in sprite px
 *   m.hand();                                   // {x, y} of the hands for this frame (carry / push / write), sprite px
 *   m.destroy();
 * Timing runs on one shared requestAnimationFrame ticker, only for mascots on screen (`m.visible()`), and does not run
 * at all with reduced motion (the first frame, or the last frame of a one-shot, is drawn once).
 *
 * Legacy API (still works, mapped onto the new frames): `legs` ('stand' | 'a' | 'b' | 'kick' | 'tuck'), `wave`, `sleep`,
 * `blink`, `blush`, `look`, `flipped` (the canvas is mirrored by CSS, so pupils are swapped), `walking`, `draw()`,
 * `WALK`, `walker()`, `rainbow()`, `MASCOT_PIXELS`.
 */
export const SPRITE_W = 13;
export const SPRITE_H = 14;
/** The row the feet stand on (the last row). */
export const FOOT_Y = SPRITE_H - 1;

export type Eyes = 'normal' | 'half' | 'blink' | 'closed' | 'happy' | 'surprised' | 'effort' | 'down';
export type Mouth = 'none' | 'smile' | 'o' | 'open';
type ArmName =
  | 'none' | 'down' | 'short' | 'back' | 'out' | 'waveA' | 'waveB' | 'up' | 'upHigh' | 'hold' | 'fwd' | 'limp' | 'limp2' | 'grip' | 'wr1' | 'wr2';

interface Pose {
  /** Body shift in x, and top / bottom edge deltas (negative = up). `wd` widens the body by that many px per side. */
  bx: number;
  ty: number;
  by: number;
  wd: number;
  eyes: Eyes;
  /** Pupils in sprite space; undefined follows `Mascot.look`. */
  look?: -1 | 0 | 1;
  mouth: Mouth;
  l: ArmName;
  r: ArmName;
  legs: string[];
  /** Shift the legs sideways (hanging sway). */
  lx: number;
  blush: boolean;
  sweat: boolean;
}
export interface Frame extends Pose {
  ms: number;
  /** How far a walker travels, in sprite pixels, when this frame starts. */
  adv: number;
}

/** Leg rows, 13 wide and bottom aligned. '' rows are empty. The body is drawn over them. */
const STAND = ['...XX...XX...', '...XX...XX...', '...XX...XX...'];
const SPREAD = ['...XX...XX...', '...XX...XX...', '.XX.......XX.'];
const PASS_R = ['...XX...XX...', '...XX....XX..', '...XX........'];
const PASS_L = ['...XX...XX...', '..XX.....XX..', '........XX...'];
const STRIDE = ['...XX...XX...', '..XX.....XX..', '.XX.......XX.'];
const WIDE = ['...XX...XX...', '..XX.....XX..', '..XX.....XX..'];
const lifted = (legs: string[], n: number) => [...legs, ...Array<string>(n).fill('')];
const DANGLE = ['...XX...XX...', '...XX...XX...', '...XX...XX...', '...XX...XX...', ''];
const SIT = ['', '', '..XX...XXXX..'];

/** Arm pixels as [outward, y from body top]. The left arm goes out to the left, the right arm to the right. */
const ARMS: Record<Exclude<ArmName, 'fwd'>, [number, number][]> = {
  none: [],
  down: [[0, 5], [0, 6]],
  short: [[0, 5]],
  back: [[0, 6]],
  out: [[0, 4], [1, 4]],
  waveA: [[0, 3], [1, 2]],
  waveB: [[0, 2], [0, 1]],
  up: [[0, 2], [1, 1]],
  upHigh: [[0, 1], [1, 0]],
  hold: [[0, 1], [0, 0], [0, -1]],
  limp: [[0, 4], [0, 5], [0, 6]],
  limp2: [[0, 4], [1, 5], [1, 6]],
  grip: [[0, 3], [0, 4]],
  wr1: [[0, 6], [1, 6]],
  wr2: [[0, 6], [1, 7]],
};

const BASE: Pose = { bx: 0, ty: 0, by: 0, wd: 0, eyes: 'normal', mouth: 'none', l: 'down', r: 'down', legs: STAND, lx: 0, blush: false, sweat: false };
const F = (ms: number, p: Partial<Pose> = {}, adv = 0): Frame => ({ ...BASE, ...p, ms, adv });

export type AnimName =
  | 'idle' | 'walk' | 'run' | 'wave' | 'hang' | 'fall' | 'land' | 'jump' | 'push' | 'strain' | 'sit' | 'ride'
  | 'scared' | 'cheer' | 'carry' | 'hold' | 'sleep' | 'write' | 'lookat' | 'worry' | 'slump' | 'balance';

export interface Anim {
  frames: Frame[];
  loop: boolean;
  /** For one-shots: the animation to switch to when it ends (otherwise the last frame is held). */
  after?: AnimName;
  note: string;
}

/** One walk cycle: contact (body low, feet apart), passing, contact, passing. Each frame moves one sprite pixel. */
const walkCycle = (ms: number, extra: Partial<Pose>, adv = 1): Frame[] => [
  F(ms, { ty: 1, by: 1, legs: SPREAD, l: 'short', r: 'down', ...extra }, adv),
  F(ms, { legs: PASS_R, l: 'down', r: 'short', ...extra }, adv),
  F(ms, { ty: 1, by: 1, legs: SPREAD, l: 'down', r: 'short', ...extra }, adv),
  F(ms, { legs: PASS_L, l: 'short', r: 'down', ...extra }, adv),
];

/**
 * Run cycle. Two half steps of three frames: CONTACT (lead foot lands in front, body leans in), DOWN (weight over the
 * planted foot, body at its lowest, the other leg swings through), FLIGHT (both feet off the ground, body at its
 * highest, trailing leg kicked back). The body travels `adv` sprite px per frame, and the planted foot is drawn `adv`
 * px further back in every grounded frame (CONTACT to DOWN: columns 10-11 to 8-9), so it stays put on the ground.
 * The arms pump against the legs: right arm forward while the right leg is back. Flight lasts longer than the other
 * frames (a small hang time), and `adv` follows ms so the speed stays even.
 */
const RUN_CONTACT = ['...XX....XX..', '..XX......XX.', '..........XX.'];
const RUN_DOWN = ['....XX..XX...', '....XX..XX...', '........XX...'];
const RUN_FLIGHT = ['....XX...XX..', '...XX.....XX.', '..XX......XX.', ''];
const runFrame = (ms: number, p: Partial<Pose>): Frame => F(ms, { bx: 1, mouth: 'smile', look: 1, ...p }, ms / 35);
const RUN: Frame[] = [
  runFrame(70, { ty: 0, by: 0, legs: RUN_CONTACT, r: 'out', l: 'back' }),
  runFrame(70, { ty: 1, by: 1, legs: RUN_DOWN, r: 'short', l: 'short' }),
  runFrame(90, { ty: -1, by: -1, legs: RUN_FLIGHT, r: 'back', l: 'short', eyes: 'happy' }),
  runFrame(70, { ty: 0, by: 0, legs: RUN_CONTACT, r: 'back', l: 'short' }),
  runFrame(70, { ty: 1, by: 1, legs: RUN_DOWN, r: 'short', l: 'short' }),
  runFrame(90, { ty: -1, by: -1, legs: RUN_FLIGHT, r: 'out', l: 'back', eyes: 'happy' }),
];

export const ANIMS: Record<AnimName, Anim> = {
  idle: { loop: true, note: 'breathing; blinks and follows the cursor on top', frames: [F(1500), F(750, { ty: 1 })] },
  walk: { loop: true, note: 'waddle, one sprite pixel per frame', frames: walkCycle(115, {}) },
  run: {
    loop: true,
    note: 'six frames, a real run: contact, down, flight, twice with the arms swapped. The planted foot slides back exactly as fast as the body travels, so feet never skate',
    frames: RUN,
  },
  wave: {
    loop: true,
    note: 'one hand waves, smiling',
    frames: [
      F(190, { r: 'waveA', mouth: 'smile' }),
      F(190, { r: 'waveB', mouth: 'smile', ty: 1 }),
      F(190, { r: 'waveA', mouth: 'smile' }),
      F(190, { r: 'waveB', mouth: 'smile', ty: 1 }),
      F(190, { r: 'waveA', mouth: 'smile' }),
      F(260, { r: 'waveB', mouth: 'smile', ty: 1 }),
    ],
  },
  hang: {
    loop: true,
    note: 'held from the top: limp, legs hanging straight, a slow sleepy blink',
    frames: [
      F(1800, { ty: -3, by: -2, eyes: 'half', legs: DANGLE, l: 'limp', r: 'limp' }),
      F(160, { ty: -3, by: -2, eyes: 'blink', legs: DANGLE, l: 'limp', r: 'limp' }),
    ],
  },
  fall: {
    loop: true,
    note: 'cute flail: arms up, legs paddling',
    frames: [
      F(120, { ty: -1, legs: STRIDE, l: 'up', r: 'upHigh', eyes: 'surprised', mouth: 'open' }),
      F(120, { ty: -1, legs: ['...XX...XX...', '...XX...XX...', '....X...X....'], l: 'upHigh', r: 'up', eyes: 'surprised', mouth: 'open' }),
    ],
  },
  jump: {
    loop: false,
    after: 'fall',
    note: 'crouch and hop off, then falls',
    frames: [F(90, { ty: 2, by: 1, legs: STAND, l: 'down', r: 'down', eyes: 'closed' }), F(110, { ty: -1, by: -1, legs: lifted(STAND, 1), l: 'up', r: 'up', eyes: 'surprised', mouth: 'o' })],
  },
  land: {
    loop: false,
    after: 'idle',
    note: 'squash, rebound, settle (one-shot, ends on the idle pose)',
    frames: [
      F(110, { ty: 3, by: 1, wd: 1, eyes: 'effort', l: 'out', r: 'out' }),
      F(90, { ty: 2, by: 1, wd: 1, eyes: 'closed', l: 'out', r: 'out' }),
      F(90, { ty: -1, eyes: 'normal', l: 'up', r: 'up', mouth: 'smile' }),
      F(110, { ty: 1, l: 'short', r: 'short' }),
      F(140),
    ],
  },
  push: {
    loop: true,
    note: 'leaning into something, arm forward, feet stepping; hand at ANCHORS.push',
    frames: [
      F(160, { bx: 1, ty: 1, by: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1 }, 1),
      F(160, { bx: 1, legs: PASS_R, r: 'fwd', l: 'back', look: 1 }, 1),
      F(160, { bx: 1, ty: 1, by: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1 }, 1),
      F(160, { bx: 1, legs: PASS_L, r: 'fwd', l: 'back', look: 1 }, 1),
    ],
  },
  strain: {
    loop: true,
    note: 'stuck pushing: feet planted, shaking, squinting, sweating',
    frames: [
      F(70, { bx: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1, eyes: 'effort' }),
      F(70, { bx: 0, ty: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1, eyes: 'effort', sweat: true }),
      F(70, { bx: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1, eyes: 'effort', sweat: true }),
      F(70, { bx: 0, ty: 1, legs: SPREAD, r: 'fwd', l: 'back', look: 1, eyes: 'effort' }),
    ],
  },
  sit: { loop: true, note: 'sitting, legs out in front; seat at ANCHORS.sit', frames: [F(1600, { ty: 3, by: 2, legs: SIT }), F(800, { ty: 4, by: 2, legs: SIT })] },
  ride: {
    loop: true,
    note: 'sitting and holding on (for a cart), bouncing along',
    frames: [
      F(170, { ty: 3, by: 2, legs: SIT, l: 'grip', r: 'grip', look: 1, mouth: 'smile' }),
      F(170, { ty: 4, by: 2, legs: SIT, l: 'grip', r: 'grip', look: 1, mouth: 'smile' }),
      F(170, { ty: 3, by: 2, legs: SIT, l: 'grip', r: 'grip', look: 1, mouth: 'smile' }),
      F(170, { ty: 3, by: 2, bx: 0, legs: SIT, l: 'grip', r: 'grip', look: 1, mouth: 'smile', eyes: 'happy' }),
    ],
  },
  scared: {
    loop: true,
    note: 'a moment like a rail breaking: wide eyes, trembling, sweat',
    frames: [
      F(60, { l: 'up', r: 'up', eyes: 'surprised', mouth: 'open', sweat: true, legs: WIDE }),
      F(60, { bx: 1, l: 'up', r: 'up', eyes: 'surprised', mouth: 'open', legs: WIDE, lx: 1 }),
      F(60, { l: 'up', r: 'up', eyes: 'surprised', mouth: 'open', sweat: true, legs: WIDE }),
      F(60, { bx: -1, l: 'up', r: 'up', eyes: 'surprised', mouth: 'open', legs: WIDE, lx: -1 }),
    ],
  },
  cheer: {
    loop: true,
    note: 'little jump with both arms up, then a squat to go again',
    frames: [
      F(130, { ty: 2, by: 1, eyes: 'happy', mouth: 'smile', l: 'down', r: 'down' }),
      F(80, { ty: -1, by: -1, legs: lifted(STAND, 1), eyes: 'happy', mouth: 'smile', l: 'up', r: 'up' }),
      F(120, { ty: -2, by: -2, legs: lifted(WIDE, 2), eyes: 'happy', mouth: 'smile', l: 'upHigh', r: 'upHigh' }),
      F(120, { ty: -2, by: -2, legs: lifted(WIDE, 2), eyes: 'happy', mouth: 'smile', l: 'up', r: 'up' }),
      F(80, { ty: -1, by: -1, legs: lifted(STAND, 1), eyes: 'happy', mouth: 'smile', l: 'up', r: 'up' }),
      F(100, { ty: 3, by: 1, wd: 1, eyes: 'happy', mouth: 'smile', l: 'out', r: 'out' }),
      F(170, { ty: 1, eyes: 'happy', mouth: 'smile', l: 'short', r: 'short' }),
    ],
  },
  carry: {
    loop: true,
    note: 'walking with a block held overhead; draw it at ANCHORS.carry (or m.hand())',
    frames: walkCycle(125, { l: 'hold', r: 'hold', look: 1 }),
  },
  hold: {
    loop: true,
    note: 'standing still with a block held overhead (carry, without the walking)',
    frames: [F(900, { l: 'hold', r: 'hold', look: 1 }), F(500, { l: 'hold', r: 'hold', look: 1, ty: 1 })],
  },
  sleep: {
    loop: true,
    note: 'eyes shut, slow breathing',
    frames: [F(1900, { ty: 1, eyes: 'closed', legs: STAND }), F(1500, { ty: 2, eyes: 'closed', legs: STAND })],
  },
  write: {
    loop: true,
    note: 'sitting, looking down, hand scribbling (seamless)',
    frames: [
      F(170, { ty: 3, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr1' }),
      F(170, { ty: 3, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr2' }),
      F(170, { ty: 3, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr1' }),
      F(170, { ty: 3, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr2' }),
      F(170, { ty: 3, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr1' }),
      F(240, { ty: 4, by: 2, legs: SIT, eyes: 'down', look: 1, r: 'wr2' }),
    ],
  },
  lookat: {
    loop: true,
    note: 'turns to the viewer with a small smile',
    frames: [F(1400, { look: 0, mouth: 'smile' }), F(700, { look: 0, mouth: 'smile', ty: 1 })],
  },
  worry: {
    loop: true,
    note: 'standing still, watching something go wrong: wide eyes, small o, the odd blink',
    frames: [F(1300, { look: 0, eyes: 'surprised', mouth: 'o' }), F(140, { look: 0, eyes: 'blink', mouth: 'o' }), F(900, { look: 0, eyes: 'surprised', mouth: 'o', ty: 1 })],
  },
  slump: {
    loop: true,
    note: 'sitting, deflated: eyes down, a slow heavy breath',
    frames: [F(1500, { ty: 4, by: 2, legs: SIT, eyes: 'down' }), F(900, { ty: 5, by: 3, legs: SIT, eyes: 'down' }), F(160, { ty: 5, by: 3, legs: SIT, eyes: 'closed' })],
  },
  balance: {
    loop: true,
    note: 'carried on something that moves: arms out, wobbling, surprised',
    frames: [
      F(240, { legs: WIDE, l: 'out', r: 'up', eyes: 'surprised', mouth: 'o' }),
      F(240, { legs: WIDE, l: 'up', r: 'out', eyes: 'surprised', mouth: 'o', ty: 1 }),
    ],
  },
};
export const ANIM_NAMES = Object.keys(ANIMS) as AnimName[];

/** Where props attach, in sprite pixels. For the exact point of the current frame use `Mascot.hand()`. */
export const ANCHORS = {
  /** Bottom centre of a block held overhead. */
  carry: { x: 6.5, y: 2 },
  /** Where the pushing hand ends (the right edge of the sprite). */
  push: { x: 13, y: 8.5 },
  /** The point a hanging mascot is held by (top of the head). */
  hang: { x: 6.5, y: 1 },
  /** The seat (sit, ride, write): the underside of the body. */
  sit: { x: 6.5, y: 13 },
  /** The pen hand while writing. */
  write: { x: 12.5, y: 12 },
};

const STOPS = ['#f6c453', '#f28a2e', '#e0402e', '#d9487a', '#a46be0', '#5aa2f0', '#9ed8f2'];
const hex = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));

/** Sample the rainbow at t (wraps), with optional brightness jitter. */
export function rainbow(t: number, jitter = 0) {
  t = ((t % 1) + 1) % 1;
  const f = t * (STOPS.length - 1);
  const i = Math.floor(f);
  const k = f - i;
  const a = hex(STOPS[i]);
  const b = hex(STOPS[Math.min(i + 1, STOPS.length - 1)]);
  return `rgb(${a.map((v, n) => clamp(Math.round(v + (b[n] - v) * k + jitter), 0, 255)).join(',')})`;
}

const INK = '#0b0b0b';
const WHITE = '#ffffff';
const PINK = '#ff8fb8';
const SWEAT = '#9ed8f2';

type Cell = { ch: string; cx: number; cy: number };

function compose(p: Pose, look: number): Cell[][] {
  const g: Cell[][] = Array.from({ length: SPRITE_H }, () => Array.from({ length: SPRITE_W }, () => ({ ch: '.', cx: 0, cy: 0 })));
  const set = (x: number, y: number, ch: string, cx = x, cy = y) => {
    if (x < 0 || y < 0 || x >= SPRITE_W || y >= SPRITE_H) return;
    g[y][x] = { ch, cx, cy };
  };
  const x0 = 2 - p.wd + p.bx;
  const x1 = 10 + p.wd + p.bx;
  const top = 3 + p.ty;
  const bot = 10 + p.by;
  const ox = p.bx;
  const oy = 3 + p.ty - 3; // colour follows the body, so bobbing does not flicker the rainbow
  const ly = SPRITE_H - p.legs.length;
  p.legs.forEach((row, r) => [...row].forEach((c, x) => c === 'X' && set(x + p.lx, ly + r, 'X')));
  const arm = (side: -1 | 1, name: ArmName) => {
    const px: [number, number][] = [];
    if (name === 'fwd') {
      // reaches a fixed column so the hand stays put while the body shakes
      for (let x = x1 + 1; x <= 12; x++) px.push([x - x1 - 1, 5]);
    } else px.push(...ARMS[name]);
    px.forEach(([o, dy]) => set(side < 0 ? x0 - 1 - o : x1 + 1 + o, top + dy, 'A', (side < 0 ? x0 - 1 - o : x1 + 1 + o) - ox, top + dy - oy));
  };
  arm(-1, p.l);
  arm(1, p.r);
  for (let y = top; y <= bot; y++) for (let x = x0; x <= x1; x++) if (!(y === top && x === x0 + 1)) set(x, y, 'X', x - ox, y - oy);
  const lx = x0 + 1;
  const rx = x1 - 2;
  const e0 = top + 3;
  const cx = (x0 + x1) / 2;
  const pupilCol = (c0: number, left: boolean) => (look > 0 ? c0 + 1 : look < 0 ? c0 : left ? c0 + 1 : c0);
  const eye = (c0: number, left: boolean) => {
    const pc = pupilCol(c0, left);
    switch (p.eyes) {
      case 'normal':
      case 'half':
        for (let y = e0 + (p.eyes === 'half' ? 1 : 0); y <= e0 + 1; y++) {
          set(c0, y, c0 === pc ? 'K' : 'W');
          set(c0 + 1, y, c0 + 1 === pc ? 'K' : 'W');
        }
        break;
      case 'blink':
      case 'closed':
        set(c0, e0 + 1, 'K');
        set(c0 + 1, e0 + 1, 'K');
        break;
      case 'down':
        set(c0, e0, 'W');
        set(c0 + 1, e0, 'W');
        set(c0, e0 + 1, 'K');
        set(c0 + 1, e0 + 1, 'K');
        break;
      case 'happy': {
        const m = left ? c0 : c0 + 1;
        set(m, e0, 'K');
        set(m - 1, e0 + 1, 'K');
        set(m + 1, e0 + 1, 'K');
        break;
      }
      case 'surprised':
        for (let y = e0 - 1; y <= e0 + 1; y++) {
          set(c0, y, c0 === pc && y === e0 ? 'K' : 'W');
          set(c0 + 1, y, c0 + 1 === pc && y === e0 ? 'K' : 'W');
        }
        break;
      case 'effort':
        if (left) {
          set(c0, e0 - 1, 'K');
          set(c0 + 1, e0, 'K');
          set(c0, e0 + 1, 'K');
        } else {
          set(c0 + 1, e0 - 1, 'K');
          set(c0, e0, 'K');
          set(c0 + 1, e0 + 1, 'K');
        }
        break;
    }
  };
  eye(lx, true);
  eye(rx, false);
  const my = top + 6;
  if (p.mouth === 'smile') {
    set(cx - 1, my, 'K');
    set(cx + 1, my, 'K');
    set(cx, my + 1, 'K');
  } else if (p.mouth === 'o') set(cx, my, 'K');
  else if (p.mouth === 'open') {
    set(cx - 1, my, 'K');
    set(cx, my, 'K');
    set(cx + 1, my, 'K');
    set(cx, my + 1, 'K');
  }
  if (p.blush) {
    set(lx, my - 1, 'P');
    set(lx + 1, my - 1, 'P');
    set(rx, my - 1, 'P');
    set(rx + 1, my - 1, 'P');
  }
  if (p.sweat) set(x1 - 1, top - 1, 'S');
  return g;
}

/** The standing sprite as text rows (X body, E / W pupil / white), used by the Doodles notebook cover. */
export const MASCOT_PIXELS: string[] = compose({ ...BASE }, 1).map((row) => row.map((c) => (c.ch === 'K' ? 'E' : c.ch === 'W' ? 'W' : c.ch === 'X' || c.ch === 'A' ? 'X' : '.')).join(''));

const LEGACY_LEGS = {
  stand: BASE,
  a: { ...BASE, legs: PASS_R },
  b: { ...BASE, legs: PASS_L },
  /** Feet off the ground, kicking: held, or riding the footer skyline. */
  kick: { ...BASE, ty: -1, by: -1, legs: ['...XX...XX...', '..XX.....XX..', '.X.........X.', ''] },
  tuck: { ...BASE, ty: -1, by: -1, legs: ['...XX...XX...', '...XX...XX...', '', ''] },
};
export type LegFrame = keyof typeof LEGACY_LEGS;
/** Each frame moves the walker exactly one pixel, so the feet never slide. */
export const WALK: LegFrame[] = ['a', 'stand', 'b', 'stand'];

const all = new Set<Mascot>();

export interface PlayOptions {
  loop?: boolean;
  onDone?: () => void;
  speed?: number;
}

export class Mascot {
  /** Pupils in sprite space: 1 = the way the sprite faces, -1 = the other way, 0 = at the viewer. Follows the cursor. */
  look: -1 | 0 | 1 = 1;
  /** Legacy: pink cheeks. */
  blush = false;
  /** Legacy: the canvas is mirrored with CSS, so the cursor look is inverted. Use `face()` for new code. */
  flipped = false;
  /** Legacy: while walking, the eyes ignore the cursor. */
  walking = false;
  shift: number;
  facing: 1 | -1 = 1;
  anim: AnimName = 'idle';
  onFrame?: (frame: Frame, index: number) => void;
  readonly visible: () => boolean;
  readonly frame = { i: 0, t: 0 };
  private readonly ctx: CanvasRenderingContext2D;
  private readonly jitter: number[];
  private loop = true;
  private speed = 1;
  private done = false;
  private onDone?: () => void;
  private override: Eyes | null = null;
  private forceBlink = false;
  private blinkAt = 0;
  private blinkT = -1;
  private fixed: Partial<Pose> | null = null;
  private _legs: LegFrame = 'stand';
  private _wave = false;
  private _sleep = false;
  private drawnShift = -1;
  dirty = true;

  constructor(readonly cv: HTMLCanvasElement, seed: number) {
    cv.width = SPRITE_W;
    cv.height = SPRITE_H;
    this.ctx = cv.getContext('2d')!;
    let s = seed;
    this.jitter = [...Array(SPRITE_W * SPRITE_H)].map(() => ((s = (s * 16807) % 2147483647) / 2147483647 - 0.5) * 26);
    this.shift = (seed % 7) / 10;
    this.blinkAt = 1200 + ((seed * 977) % 2400);
    this.visible = visibility(cv);
    all.add(this);
    startTicker();
    this.enter(0);
    this.draw();
  }

  /** The animation definition being played. */
  get def() {
    return ANIMS[this.anim];
  }

  play(name: AnimName, opts: PlayOptions = {}) {
    const def = ANIMS[name];
    const loop = opts.loop ?? def.loop;
    this.fixed = null;
    if (this.anim === name && !this.done && this.loop && loop && !opts.onDone) {
      this.speed = opts.speed ?? this.speed;
      this.draw();
      return this;
    }
    this.anim = name;
    this.loop = loop;
    this.speed = opts.speed ?? 1;
    this.onDone = opts.onDone;
    this.done = false;
    this.frame.i = 0;
    this.frame.t = 0;
    this.enter(0);
    if (reduceMotion) {
      // no ticking: show the end state of a one-shot, the first frame of a loop, and report done
      if (!loop) {
        this.frame.i = def.frames.length - 1;
        this.done = true;
        const cb = this.onDone;
        this.onDone = undefined;
        if (cb) setTimeout(cb, 0);
      }
    }
    this.draw();
    return this;
  }

  /** Freeze on one frame of an animation (for the lab's filmstrips and for stills). */
  show(name: AnimName, index = 0) {
    this.fixed = null;
    this.anim = name;
    this.done = true;
    this.frame.i = clamp(index, 0, ANIMS[name].frames.length - 1);
    this.frame.t = 0;
    this.draw();
    return this;
  }

  /** Override the eyes for every frame (null = whatever the animation says). */
  get expression(): Eyes | null {
    return this.override;
  }
  set expression(e: Eyes | null) {
    if (e === this.override) return;
    this.override = e;
    this.draw();
  }

  /** Mirror the sprite itself. 1 faces right, -1 faces left. */
  face(dir: 1 | -1) {
    if (dir === this.facing) return this;
    this.facing = dir;
    this.draw();
    return this;
  }

  get frameNow(): Frame {
    return this.def.frames[Math.min(this.frame.i, this.def.frames.length - 1)];
  }

  /** The hand point of the current frame in sprite px: where a carried block's bottom centre or the pushed object's face goes. */
  hand() {
    const p = this.frameNow;
    const cx = 6.5 + p.bx;
    const top = 3 + p.ty;
    let pt = { x: cx, y: top - 1 };
    if (p.r === 'fwd') pt = { x: 13, y: top + 5.5 };
    else if (p.r === 'wr1' || p.r === 'wr2') pt = { x: 12.5, y: top + 6.5 };
    else if (p.l === 'limp' || p.l === 'limp2') pt = { x: cx, y: top };
    return this.facing < 0 ? { x: SPRITE_W - pt.x, y: pt.y } : pt;
  }

  private enter(i: number) {
    this.frame.i = i;
    this.onFrame?.(this.def.frames[i], i);
  }

  /** Called by the shared ticker for visible mascots. */
  step(dt: number) {
    // slow shimmer: a full rainbow cycle in about 30 s
    this.shift = (this.shift + dt * 0.000033) % 1;
    const q = Math.floor(this.shift * 220);
    if (q !== this.drawnShift) this.dirty = true;
    const eyesOpen = this.override === null && this.frameNow.eyes === 'normal' && !this.fixed;
    if (this.blinkT >= 0) {
      this.blinkT += dt;
      if (this.blinkT > 170) {
        this.blinkT = -1;
        this.blinkAt = 1800 + Math.random() * 3200;
      }
      this.dirty = true;
    } else {
      this.blinkAt -= dt;
      if (this.blinkAt <= 0 && eyesOpen) {
        this.blinkT = 0;
        this.dirty = true;
      } else if (this.blinkAt <= 0) this.blinkAt = 400;
    }
    if (!this.fixed && !this.done) {
      const frames = this.def.frames;
      this.frame.t += dt * this.speed;
      let guard = 8;
      while (this.frame.t >= frames[this.frame.i].ms && guard--) {
        this.frame.t -= frames[this.frame.i].ms;
        if (this.frame.i + 1 < frames.length) this.enter(this.frame.i + 1);
        else if (this.loop) this.enter(0);
        else {
          this.done = true;
          this.frame.t = 0;
          const after = this.def.after;
          const cb = this.onDone;
          this.onDone = undefined;
          if (after) this.play(after);
          cb?.();
          this.dirty = true;
          break;
        }
        this.dirty = true;
      }
    }
    if (this.dirty) this.draw();
  }

  draw() {
    this.dirty = false;
    const { ctx } = this;
    this.drawnShift = Math.floor(this.shift * 220);
    ctx.clearRect(0, 0, SPRITE_W, SPRITE_H);
    const base = this.fixed ? { ...this.frameNow, ...this.fixed } : this.frameNow;
    const pose: Pose = { ...base };
    if (this.override) pose.eyes = this.override;
    else if (this.forceBlink || (this.blinkT >= 0 && pose.eyes === 'normal')) pose.eyes = this.blinkT >= 0 && this.blinkT < 55 ? 'half' : 'blink';
    if (this.blush) pose.blush = true;
    const look = pose.look ?? this.look;
    const g = compose(pose, look);
    const flip = this.facing < 0;
    for (let y = 0; y < SPRITE_H; y++) {
      for (let x = 0; x < SPRITE_W; x++) {
        const c = g[y][flip ? SPRITE_W - 1 - x : x];
        if (c.ch === '.') continue;
        let col: string;
        if (c.ch === 'X' || c.ch === 'A') {
          const cx = clamp(flip ? SPRITE_W - 1 - c.cx : c.cx, 0, SPRITE_W - 1);
          const cy = clamp(c.cy, 0, SPRITE_H - 1);
          col = rainbow((cx * 0.55 + cy) / 18 + this.shift, this.jitter[cy * SPRITE_W + cx] - (c.ch === 'A' ? 30 : 0));
        } else col = c.ch === 'K' ? INK : c.ch === 'W' ? WHITE : c.ch === 'P' ? PINK : SWEAT;
        ctx.fillStyle = col;
        ctx.fillRect(x, y, 1, 1);
      }
    }
  }

  /** Legacy: set the pupils from the screen side the cursor is on (-1 left, 1 right). */
  setLook(d: number) {
    const v = (d * this.facing * (this.flipped ? -1 : 1) > 0 ? 1 : -1) as 1 | -1;
    if (v !== this.look) {
      this.look = v;
      this.draw();
    }
  }

  get legs() {
    return this._legs;
  }
  set legs(v: LegFrame) {
    if (v === this._legs) return;
    this._legs = v;
    this.syncLegacy();
  }
  get wave() {
    return this._wave;
  }
  set wave(v: boolean) {
    if (v === this._wave) return;
    this._wave = v;
    this.syncLegacy();
  }
  get sleep() {
    return this._sleep;
  }
  set sleep(v: boolean) {
    if (v === this._sleep) return;
    this._sleep = v;
    this.syncLegacy();
  }
  get blink() {
    return this.forceBlink;
  }
  set blink(v: boolean) {
    this.forceBlink = v;
  }

  private syncLegacy() {
    if (this._legs !== 'stand') {
      // a fixed leg frame (walk step, kick, tuck): the legs and arms come from the old flags, the rest from idle
      this.anim = 'idle';
      this.done = false;
      this.fixed = { ...LEGACY_LEGS[this._legs], l: 'down', r: this._wave ? 'up' : 'down', eyes: this._sleep ? 'closed' : 'normal' };
      this.draw();
    } else if (this._wave) this.play('wave');
    else if (this._sleep) this.play('sleep');
    else this.play('idle');
  }

  destroy() {
    all.delete(this);
    forget(this.cv);
  }
}

let raf = 0;
let last = 0;
let pointerBound = false;

function tick(now: number) {
  const dt = Math.min(50, now - last || 16);
  last = now;
  for (const m of all) if (m.visible()) m.step(dt);
  raf = all.size ? requestAnimationFrame(tick) : 0;
}

function startTicker() {
  if (!pointerBound) {
    pointerBound = true;
    window.addEventListener(
      'pointermove',
      (e) =>
        all.forEach((m) => {
          if (m.walking || !m.visible()) return;
          const r = m.cv.getBoundingClientRect();
          m.setLook(e.clientX < r.left + r.width * 0.35 ? -1 : 1);
        }),
      { passive: true },
    );
  }
  if (reduceMotion || raf) return;
  last = performance.now();
  raf = requestAnimationFrame(tick);
}

export interface WalkerOptions {
  seed: number;
  /** Lane bounds as fractions of the lane width. */
  lo?: number;
  hi?: number;
  startAt?: number;
  /** Optional ground height (px) under the span [left, left + width], used to ride the overscroll skyline. */
  lift?: (left: number, width: number) => number;
}

type Phase = 'idle' | 'walk' | 'carried' | 'air' | 'landing' | 'perched';

/**
 * A mascot that strolls back and forth inside `lane`: idles a while, sometimes waves or looks at you, turns, walks
 * a stretch, stops. With `lift` it is carried up when the ground rises (balancing, surprised), hops off and falls with
 * gravity when the ground drops away, lands (squash, rebound), idles a beat and strolls on.
 * `place()` is called every frame the ground moves.
 */
export function walker(cv: HTMLCanvasElement, lane: HTMLElement, { seed, lo = 0, hi = 1, startAt = 0.5, lift }: WalkerOptions) {
  const m = new Mascot(cv, seed);
  let x: number | null = null;
  let y = 0;
  let vy = 0;
  let peak = 0;
  let dir: 1 | -1 = 1;
  let phase: Phase = 'idle';
  let until = performance.now() + 900;
  let stopping = false;
  let justTurned = false;
  let fallRaf = 0;
  m.look = 1;
  m.walking = true;

  const px = () => cv.clientWidth / SPRITE_W || 5;
  const bounds = () => [lane.clientWidth * lo, lane.clientWidth * hi - cv.clientWidth] as const;
  const ground = () => (lift && x !== null ? lift(x, cv.clientWidth) : 0);
  const paint = () => {
    if (x === null) return;
    const u = px();
    cv.style.left = `${Math.round(x / u) * u}px`;
    cv.style.bottom = `${Math.round(y / u) * u}px`;
  };
  const ensureX = () => {
    if (x !== null) return true;
    if (!lane.clientWidth) return false;
    const [min, max] = bounds();
    x = min + (max - min) * startAt;
    paint();
    return true;
  };
  const idleFor = (ms: number) => {
    phase = y > 0.5 ? 'perched' : 'idle';
    stopping = false;
    until = performance.now() + ms;
    m.play('idle');
  };

  m.onFrame = (f, i) => {
    if (import.meta.env.DEV) cv.dataset.state = `${phase}:${m.anim}:${i}`;
    if (phase !== 'walk' || x === null) return;
    const [min, max] = bounds();
    x = clamp(x + dir * f.adv * px(), min, max);
    paint();
    if (x <= min || x >= max) {
      // end of the lane: stop on a passing frame and turn round
      dir = (x <= min ? 1 : -1) as 1 | -1;
      m.face(dir);
      justTurned = true;
      idleFor(700 + Math.random() * 900);
    } else if (stopping && (i === 1 || i === 3)) idleFor(1400 + Math.random() * 2600);
  };

  const decide = () => {
    const r = Math.random();
    if (r < 0.1 && !justTurned) {
      phase = 'idle';
      until = performance.now() + 1500;
      m.play('wave');
    } else if (r < 0.2 && !justTurned) {
      phase = 'idle';
      until = performance.now() + 1600;
      m.play('lookat');
    } else {
      if (!justTurned && Math.random() < 0.4) {
        dir = (-dir) as 1 | -1;
        m.face(dir);
        justTurned = true;
        idleFor(350);
        return;
      }
      justTurned = false;
      phase = 'walk';
      stopping = false;
      until = performance.now() + 1500 + Math.random() * 3200;
      m.play('walk');
    }
  };

  const timer = reduceMotion
    ? 0
    : window.setInterval(() => {
        if (!m.visible() || !ensureX()) return;
        const now = performance.now();
        if (phase === 'idle' && now >= until) decide();
        else if (phase === 'walk' && now >= until) stopping = true;
      }, 60);
  if (reduceMotion) requestAnimationFrame(ensureX);

  const startFall = (hop: number) => {
    cancelAnimationFrame(fallRaf);
    phase = 'air';
    vy = hop;
    m.play(hop > 0 ? 'jump' : 'fall');
    let prev = performance.now();
    const step = (now: number) => {
      const dt = Math.min(0.033, (now - prev) / 1000);
      prev = now;
      const g = ground();
      vy -= 1800 * dt;
      y = Math.max(g, y + vy * dt);
      peak = Math.max(peak, y);
      paint();
      if (y <= g + 0.01 && vy <= 0) {
        y = g;
        paint();
        land(g);
        return;
      }
      if (vy < 0 && m.anim === 'jump') m.play('fall');
      fallRaf = requestAnimationFrame(step);
    };
    fallRaf = requestAnimationFrame(step);
  };

  const land = (g: number) => {
    fallRaf = 0;
    vy = 0;
    const dropped = peak - y;
    peak = y;
    phase = g > 0.5 ? 'perched' : 'landing';
    until = Infinity;
    const finish = () => {
      if (phase === 'landing' || phase === 'perched') idleFor(g > 0.5 ? 400 : 1200 + Math.random() * 600);
    };
    if (dropped > 10) m.play('land', { onDone: finish });
    else {
      m.play('idle');
      finish();
    }
    if (g > 0.5) phase = 'perched';
  };

  /** Called every frame the ground moves. Rising ground carries it; falling ground leaves it to drop on its own. */
  const place = () => {
    if (!ensureX() || x === null) return;
    const g = ground();
    if (phase === 'air') {
      // the ground came back up under it
      if (g >= y) {
        cancelAnimationFrame(fallRaf);
        fallRaf = 0;
        vy = 0;
        y = g;
        phase = 'carried';
        m.play('balance');
        paint();
      }
      return;
    }
    if (g > y + 0.5) {
      if (phase !== 'carried') {
        phase = 'carried';
        stopping = false;
        peak = y;
        m.play('balance');
      }
      y = g;
      peak = Math.max(peak, y);
      paint();
    } else if (g < y - 0.5 && y > 0.5) {
      startFall(phase === 'carried' ? 240 : 0);
    }
  };

  return {
    mascot: m,
    place,
    stop() {
      clearInterval(timer);
      cancelAnimationFrame(fallRaf);
      m.onFrame = undefined;
      m.destroy();
    },
  };
}
