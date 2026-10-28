const STEPS: Array<[number, number]> = [[1, 0], [-1, 0], [0, 1], [0, -1], [0.7, 0.7], [-0.7, 0.7], [0.7, -0.7], [-0.7, -0.7]];
/** The white die-cut line round a sticker, drawn as eight offset drop shadows. */
export const outlineFilter = (c: number) => STEPS.map(([a, b]) => `drop-shadow(${(a * c).toFixed(2)}px ${(b * c).toFixed(2)}px 0 #fff)`).join(' ');
