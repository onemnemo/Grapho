import { en, type Catalogue, type Key } from './en';

const ACCENTS: Record<string, string> = {
  a: 'á', b: 'ƀ', c: 'ç', d: 'ð', e: 'é', f: 'ƒ', g: 'ğ', h: 'ĥ', i: 'í', j: 'ĵ', k: 'ķ', l: 'ł', m: 'ɱ',
  n: 'ñ', o: 'ö', p: 'þ', q: 'ǫ', r: 'ŕ', s: 'š', t: 'ŧ', u: 'ü', v: 'ṽ', w: 'ŵ', x: 'ẋ', y: 'ý', z: 'ž',
  A: 'Å', B: 'Ɓ', C: 'Ç', D: 'Ð', E: 'É', F: 'Ƒ', G: 'Ĝ', H: 'Ĥ', I: 'Î', J: 'Ĵ', K: 'Ķ', L: 'Ļ', M: 'Ṁ',
  N: 'Ñ', O: 'Ö', P: 'Þ', Q: 'Ǫ', R: 'Ŕ', S: 'Š', T: 'Ŧ', U: 'Û', V: 'Ṽ', W: 'Ŵ', X: 'Ẋ', Y: 'Ý', Z: 'Ž',
};
const VOWELS = /[aeiouyAEIOUY]/;

/** The pseudo-locale: accents every letter and stretches the text by about 35% by doubling vowels, then
 *  further letters, so layouts that only fit English show it at once. {placeholders} are left alone. */
export function pseudo(text: string): string {
  const target = Math.ceil(text.length * 0.35);
  let added = 0;
  let out = '';
  let hold = false;
  for (const ch of text) {
    if (ch === '{') hold = true;
    if (hold) { out += ch; if (ch === '}') hold = false; continue; }
    const a = ACCENTS[ch] ?? ch;
    out += a;
    if (added < target && /[a-zA-Z]/.test(ch) && (VOWELS.test(ch) || (out.length % 3 === 0))) { out += a.toLowerCase(); added++; }
  }
  while (added < target) { out += '·'; added++; }
  return out;
}

export const xx: Catalogue = Object.fromEntries((Object.keys(en) as Key[]).map((k) => [k, pseudo(en[k])]));
