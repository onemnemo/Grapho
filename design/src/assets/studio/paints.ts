/* Painted fills: four soft radial blooms over a deep base. */
export type PaintKey = 'ember' | 'dusk' | 'gold' | 'sea' | 'rose' | 'lilac';
export const PAINT: Record<PaintKey, { name: string; bg: [string, string, string, string, string] }> = {
  ember: { name: 'Ember', bg: ['#3a1a1c', '#F28A2E', '#D9487A', '#E0402E', '#F6C453'] },
  dusk: { name: 'Dusk', bg: ['#1b1936', '#5C8DF0', '#B9A6F0', '#3E3A6D', '#D9487A'] },
  gold: { name: 'Gold', bg: ['#3a2c14', '#F6C453', '#F28A2E', '#9fc87a', '#f4e2b0'] },
  sea: { name: 'Sea', bg: ['#10302f', '#3FB27A', '#5C8DF0', '#9ED8F2', '#2d6b5a'] },
  rose: { name: 'Rose', bg: ['#3a1428', '#D9487A', '#F4A9C6', '#B9A6F0', '#E0402E'] },
  lilac: { name: 'Lilac', bg: ['#241d44', '#B9A6F0', '#8FB0FF', '#F4A9C6', '#5C8DF0'] },
};
export const PAINTKEYS = Object.keys(PAINT) as PaintKey[];
export const paintCSS = (k: PaintKey) => {
  const [b, a, c, d, e] = PAINT[k].bg;
  return `radial-gradient(60% 50% at 18% 22%, ${a}, transparent 72%), radial-gradient(52% 52% at 84% 28%, ${c}, transparent 72%), radial-gradient(62% 58% at 72% 88%, ${d}, transparent 72%), radial-gradient(42% 42% at 22% 88%, ${e}, transparent 72%), ${b}`;
};
