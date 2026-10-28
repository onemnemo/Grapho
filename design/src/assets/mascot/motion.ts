export const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
export const reduceMotion = typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
