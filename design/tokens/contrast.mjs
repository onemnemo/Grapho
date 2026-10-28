// Colour maths and the contrast matrix engine. Dependency free, shared by build-tokens.mjs and
// themes/derive-theme.mjs so the build, the vectors and every custom theme use one set of rules.

export const clamp = (x, a, b) => Math.min(b, Math.max(a, x));
// Half up rounding on a non negative value. Every 8 bit rounding in the system goes through this.
export const roundHalfUp = (x) => Math.floor(x + 0.5);
export const round6 = (x) => Math.round(x * 1e6) / 1e6;

export const parseHex = (h) => {
  const s = h.replace('#', '');
  const n = (i) => parseInt(s.slice(i, i + 2), 16);
  return { r: n(0), g: n(2), b: n(4), a: s.length >= 8 ? n(6) / 255 : 1 };
};
const h2 = (n) => clamp(roundHalfUp(n), 0, 255).toString(16).padStart(2, '0').toUpperCase();
export const toHex = (c) => '#' + h2(c.r) + h2(c.g) + h2(c.b) + (c.a < 1 ? h2(c.a * 255) : '');
export const over = (f, b) => ({ r: f.r * f.a + b.r * (1 - f.a), g: f.g * f.a + b.g * (1 - f.a), b: f.b * f.a + b.b * (1 - f.a), a: 1 });
const lin = (c) => { const x = c / 255; return x <= 0.04045 ? x / 12.92 : ((x + 0.055) / 1.055) ** 2.4; };
const gam = (x) => 255 * (x <= 0.0031308 ? 12.92 * x : 1.055 * x ** (1 / 2.4) - 0.055);
export const lum = (c) => 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b);
export const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };

// OKLab, Ottosson's published matrices. Results are rounded to 6 decimals after every conversion so
// cbrt and pow differences between runtimes cannot change an 8 bit result except at exact ties.
export const toOklab = (c) => {
  const r = lin(c.r), g = lin(c.g), b = lin(c.b);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return { L: round6(0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s), a: round6(1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s), b: round6(0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s) };
};
const fromOklabLinear = ({ L, a, b }) => {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3, m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3, s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s, -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s, -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s];
};
export const toLch = (c) => { const o = toOklab(c); const C = round6(Math.hypot(o.a, o.b)); let h = (Math.atan2(o.b, o.a) * 180) / Math.PI; if (h < 0) h += 360; return { L: o.L, C, h: round6(h) }; };
// OKLCH to an 8 bit colour: keep L and h, bisect C 24 times, keep the in gamut end, then round.
export const fromLch = (L, C, h) => {
  L = clamp(L, 0, 1);
  const hr = (h * Math.PI) / 180;
  const at = (cc) => fromOklabLinear({ L, a: cc * Math.cos(hr), b: cc * Math.sin(hr) });
  const inGamut = (v) => v.every((x) => x >= -0.0001 && x <= 1.0001);
  let c = C;
  if (!inGamut(at(C))) { let lo = 0, hi = C; for (let i = 0; i < 24; i++) { const mid = (lo + hi) / 2; if (inGamut(at(mid))) lo = mid; else hi = mid; } c = lo; }
  const v = at(c).map((x) => gam(clamp(x, 0, 1)));
  return { r: clamp(roundHalfUp(v[0]), 0, 255), g: clamp(roundHalfUp(v[1]), 0, 255), b: clamp(roundHalfUp(v[2]), 0, 255), a: 1 };
};
export const deltaE = (x, y) => { const p = toOklab(x), q = toOklab(y); return Math.hypot(p.L - q.L, p.a - q.a, p.b - q.b); };

export const REQ = { text: 4.5, large: 3, ui: 3 };
const asList = (v, groups) => (Array.isArray(v) ? v : v.startsWith('$') ? groups[v.slice(1)] : [v]).flatMap((x) => (typeof x === 'string' && x.startsWith('$') ? groups[x.slice(1)] : [x]));

// get(token) returns {r,g,b,a}. Returns one result per concrete combination.
export const evalMatrix = (get, spec) => {
  const out = [];
  for (const p of spec.pairs) {
    const fgs = asList(p.fg, spec.groups), bgs = asList(p.bg, spec.groups), hosts = p.over ? asList(p.over, spec.groups) : [null];
    const except = new Set(p.except ?? []);
    const needs = p.req in REQ ? REQ[p.req] : p.req === 'exempt' ? 0 : Number(p.req);
    for (const bg of bgs) for (const host of hosts) {
      const group = [];
      for (const fg of fgs) {
        if (except.has(fg) || except.has(`${fg}|${bg}`)) continue;
        const base = host ? get(host) : null;
        const b = host ? over(get(bg), base) : get(bg);
        const f = over(get(fg), b);
        group.push({ id: p.id, fg, bg, host, ratio: ratio(f, b), need: needs, exempt: p.req === 'exempt', reason: p.reason, any: !!p.any });
      }
      if (p.any && group.length) { const best = Math.max(...group.map((g) => g.ratio)); group.forEach((g) => { g.eff = best; g.ok = best >= g.need; }); } else group.forEach((g) => { g.eff = g.ratio; g.ok = g.exempt || g.ratio >= g.need; });
      out.push(...group);
    }
  }
  return out;
};
export const failures = (res) => res.filter((r) => !r.ok);
