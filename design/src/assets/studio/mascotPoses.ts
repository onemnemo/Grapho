import { Mascot, type AnimName } from '../mascot/mascot';

/* The real mascot, captured as small PNGs so it can sit on a cover as a sticker. Never named. */
const POSES: Array<[AnimName, number, number]> = [
  ['wave', 1, 3], ['cheer', 2, 5], ['idle', 0, 0], ['sleep', 0, 2], ['sit', 0, 4], ['write', 0, 6],
  ['scared', 0, 1], ['lookat', 0, 5], ['worry', 0, 3], ['slump', 0, 2], ['balance', 0, 4], ['hang', 0, 1],
];
export interface MascotPose { key: string; src: string; anim: AnimName }

function capture(anim: AnimName, frame: number, seed: number): string {
  try {
    const c = document.createElement('canvas');
    const m = new Mascot(c, seed);
    m.show(anim, frame);
    const u = c.toDataURL();
    m.destroy();
    return u;
  } catch {
    return '';
  }
}

let cache: MascotPose[] | null = null;
export function mascotPack(): MascotPose[] {
  if (typeof document === 'undefined') return [];
  return (cache ??= POSES.map(([anim, f, s]) => ({ key: `m_${anim}`, src: capture(anim, f, s), anim })));
}
