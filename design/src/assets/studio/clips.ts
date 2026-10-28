/* Video clips are looping CSS scenes. The poster is a paused frame at a chosen time. */
export type ClipKey = 'aurora' | 'embers' | 'rain' | 'waves' | 'clouds' | 'swirl';
export const CLIPS: Record<ClipKey, { name: string; len: number; bg: string }> = {
  aurora: { name: 'Aurora', len: 8, bg: '#0c1226' },
  embers: { name: 'Embers', len: 6, bg: '#1c0d12' },
  rain: { name: 'Rain', len: 4, bg: '#0f1a2b' },
  waves: { name: 'Waves', len: 6, bg: '#0f3b45' },
  clouds: { name: 'Clouds', len: 10, bg: '#5A8CF0' },
  swirl: { name: 'Swirl', len: 8, bg: '#241d44' },
};
export const CLIPKEYS = Object.keys(CLIPS) as ClipKey[];
