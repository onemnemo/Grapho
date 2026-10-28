// Reference implementation of the custom theme rules, version 1. See themes/custom-themes.md.
// Dependency free. Every shell implements the same steps and must reproduce themes/vectors.json.
//
//   deriveTheme(pack, ctx) -> { outcome, adjustments, reason?, tokens }
//
// ctx = { builtins: { 'ink-dark': map, 'ink-light': map }, spec, themable: [tokens] }
// where map is { 'surface.base': {r,g,b,a}, ... } for every semantic colour token.
import { parseHex, toHex, toLch, toOklab, fromLch, ratio, over, evalMatrix, round6 } from '../tokens/contrast.mjs';

export const RULES_VERSION = 1;
export const CHROMA_MAX = 0.04; // OKLCH chroma ceiling for every chrome surface
const B_STEPS = 15, B_STEP = 0.01; // background moves
const T_STEPS = 15, T_STEP = 0.02; // secondary text fits
const A_STEPS = 30, A_STEP = 0.01; // accent moves
const S_STEPS = 60, S_STEP = 0.01; // status colour moves
const HEX6 = /^#[0-9A-Fa-f]{6}$/, HEX68 = /^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$/;
const TAGS = { red: 25, orange: 55, yellow: 95, green: 145, teal: 195, blue: 250, violet: 295, pink: 350 };
const STATUS = { danger: [29, 0.17], warning: [85, 0.13], success: [155, 0.13], info: [255, 0.14] };

const lchOf = (c) => toLch(c);
const mix = (a, b, t) => { // OKLab mix of two opaque colours, result rounded to 8 bit
  const p = toOklab(a), q = toOklab(b);
  const L = p.L + (q.L - p.L) * t, x = p.a + (q.a - p.a) * t, y = p.b + (q.b - p.b) * t;
  let h = (Math.atan2(y, x) * 180) / Math.PI; if (h < 0) h += 360;
  return fromLch(round6(L), round6(Math.hypot(x, y)), round6(h));
};
const withAlpha = (c, pct) => ({ r: c.r, g: c.g, b: c.b, a: Math.round(pct * 2.55) / 255 });

// Component tokens alias semantic ones; recompute them whenever the semantic values move.
const sync = (m) => { m['switch.track-off'] = m['surface.strong']; m['switch.thumb-off'] = m['text.muted']; m['switch.track-on'] = m['border.control-hover']; m['switch.thumb-on'] = m['surface.base']; m['state.readonly.border'] = m['border.control']; m['media.check-bg'] = m['selection.indicator']; m['media.check-fg'] = m['text.on-action']; m['media.halo'] = m['surface.base']; m['media.cover-neutral'] = m['surface.strong']; m['media.cover-neutral-fg'] = m['text.muted']; return m; };

export function deriveTheme(pack, ctx) {
  const adj = [];
  const reject = (reason) => ({ outcome: 'rejected', reason, adjustments: adj, tokens: null });
  if (!pack || typeof pack !== 'object') return reject('not-a-theme');
  if (!Number.isInteger(pack.rules) || pack.rules > RULES_VERSION || pack.rules < 1) return reject('rules-too-new');
  const inp = pack.inputs ?? {};
  for (const k of ['background', 'surface', 'accent', 'page']) if (inp[k] !== undefined && !HEX6.test(inp[k])) return reject(`invalid-colour:${k}`);
  if (!inp.background) return reject('invalid-colour:background');
  const ov = pack.overrides ?? {};
  const spec = ctx.spec, themable = new Set(ctx.themable);
  const LP = ctx.builtins['ink-dark']['text.default'], DP = ctx.builtins['ink-light']['text.default'];

  // 1. scheme
  const B0 = parseHex(inp.background);
  const dark = pack.scheme === 'dark' ? true : pack.scheme === 'light' ? false : ratio(B0, LP) >= ratio(B0, DP);
  const d = dark ? 1 : -1, base = ctx.builtins[dark ? 'ink-dark' : 'ink-light'];
  const pole = dark ? LP : DP;

  // the working state
  const st = { shift: 0, surface: inp.surface ? parseHex(inp.surface) : null, tMuted: dark ? 0.3 : 0.26, tSubtle: dark ? 0.42 : 0.36, accent: inp.accent ? parseHex(inp.accent) : null, accShift: 0, status: {}, page: inp.page ? parseHex(inp.page) : null };
  const bl = lchOf(B0);
  if (bl.C > CHROMA_MAX) adj.push('background-toned-down');
  const bC = Math.min(bl.C, CHROMA_MAX);

  const assemble = () => {
    const m = { ...base };
    const L0 = Math.min(1, Math.max(0, bl.L - d * st.shift * B_STEP)); // moving "away from the pole" lightens a light scheme and darkens a dark one
    const sm = (L) => fromLch(L, bC, bl.h);
    const b = sm(L0);
    m['surface.base'] = b;
    const baseL = lchOf(b).L;
    m['surface.raised'] = sm(baseL + d * 0.03);
    let control;
    if (st.surface) { const sl = lchOf(st.surface); control = fromLch(sl.L, Math.min(sl.C, CHROMA_MAX), sl.h); } else control = sm(baseL + d * 0.06);
    const cl = lchOf(control).L;
    m['surface.control'] = control;
    m['surface.control-hover'] = sm(cl + d * 0.02);
    m['surface.strong'] = sm(cl + d * 0.05);
    m['surface.overlay'] = sm(dark ? baseL + 0.08 : Math.min(1, baseL + 0.05));
    m['surface.sunken'] = dark ? sm(Math.max(0, baseL - 0.08)) : m['surface.strong'];
    m['border.focus-gap'] = b;
    // text
    m['text.default'] = pole;
    m['text.muted'] = mix(pole, b, st.tMuted);
    m['text.subtle'] = mix(pole, b, Math.max(st.tSubtle, st.tMuted));
    m['state.readonly.fg'] = m['text.muted'];
    m['action.secondary.bg'] = control; m['action.secondary.fg'] = pole; m['action.secondary.bg-hover'] = m['surface.strong'];
    const sl2 = lchOf(m['surface.strong']).L;
    m['action.secondary.bg-pressed'] = [0.04, 0.02, 0].map((o) => sm(sl2 + d * o)).find((c) => ratio(pole, c) >= 4.5) ?? m['surface.strong'];
    for (const k of ['border.focus', 'selection.indicator', 'state.drag.target-line', 'state.drag.insertion', 'action.quiet.fg']) m[k] = pole;
    // accent
    if (st.accent && st.accShift >= 0) {
      const al = lchOf(st.accent), L = al.L + d * A_STEP * st.accShift;
      const A = fromLch(L, al.C, al.h);
      const on = ratio(LP, A) >= ratio(DP, A) ? LP : DP;
      m['action.primary.bg'] = A; m['text.on-action'] = on; m['action.primary.fg'] = on;
      const lA = lchOf(A);
      const hov = fromLch(lA.L + d * 0.04, lA.C, lA.h), prs = fromLch(lA.L - d * 0.04, lA.C, lA.h);
      m['action.primary.bg-hover'] = ratio(on, hov) >= 4.5 ? hov : A;
      m['action.primary.bg-pressed'] = ratio(on, prs) >= 4.5 ? prs : A;
    }
    // status
    for (const [s, [h, C]] of Object.entries(STATUS)) {
      const start = dark ? 0.78 : 0.45, L = start + d * S_STEP * (st.status[s] ?? 0);
      const fg = fromLch(L, C, h);
      m[`state.${s}.fg`] = fg;
      m[`state.${s}.bg`] = withAlpha(fg, dark ? 12 : 8); m[`state.${s}.bg-hover`] = withAlpha(fg, dark ? 16 : 12); m[`state.${s}.line`] = withAlpha(fg, dark ? 22 : 30);
      if (s === 'danger') m['state.danger.bg-pressed'] = withAlpha(fg, dark ? 18 : 12);
    }
    for (const [n, h] of Object.entries(TAGS)) {
      const fg = fromLch((dark ? 0.8 : 0.42) + d * S_STEP * (st.status[`tag.${n}`] ?? 0), dark ? 0.12 : 0.13, h);
      m[`tag.${n}.fg`] = fg; m[`tag.${n}.bg`] = withAlpha(fg, dark ? 16 : 10);
    }
    m['border.error'] = m['state.danger.fg']; m['border.warning'] = m['state.warning.fg'];
    let ss = 0, solid = fromLch(0.52, 0.19, 29);
    while (ratio(m['state.danger.on-solid'], solid) < 4.5 && ss++ < 40) solid = fromLch(0.52 - ss * 0.01, 0.19, 29);
    m['state.danger.solid'] = solid;
    // page
    if (st.page) m['canvas.page'] = st.page;
    const page = m['canvas.page'];
    const pen = base['canvas.ink-default'], penDark = base['content.ink-on-dark'];
    m['canvas.ink-default'] = ratio(pen, page) >= ratio(penDark, page) ? pen : penDark;
    m['canvas.placeholder-text'] = m['canvas.ink-default']; m['canvas.placeholder-muted'] = mix(m['canvas.ink-default'], page, 0.2); m['canvas.placeholder-bg'] = withAlpha(m['canvas.ink-default'], 6); m['canvas.placeholder-edge'] = withAlpha(m['canvas.ink-default'], 62);
    m['canvas.marquee'] = [base['canvas.marquee'], LP, DP].reduce((a, c) => (ratio(c, page) > ratio(a, page) ? c : a));
    return sync(m);
  };
  const bad = (m, fgs) => evalMatrix((k) => m[k], spec).filter((r) => !r.ok && fgs.includes(r.fg));

  // 2. text readability: pole first (drop the surface input, then move the background), then fit mid steps
    st.tMuted = 0; st.tSubtle = 0; // test the pole alone
  for (;;) {
    if (!bad(assemble(), ['text.default']).length) break;
    if (st.surface) { st.surface = null; adj.push('surface-matched'); continue; }
    if (st.shift >= B_STEPS) return reject('unreadable');
    st.shift++;
  }
  if (st.shift) adj.push(dark ? 'background-darker' : 'background-lighter');
  const t0 = { muted: dark ? 0.3 : 0.26, subtle: dark ? 0.42 : 0.36 };
  for (const role of ['muted', 'subtle']) {
    const key = role === 'muted' ? 'tMuted' : 'tSubtle';
    let i = 0;
    for (; i <= T_STEPS; i++) {
      st[key] = Math.max(0, round6(t0[role] - T_STEP * i));
      if (role === 'subtle') st.tSubtle = Math.max(st.tSubtle, st.tMuted);
      if (!bad(assemble(), role === 'muted' ? ['text.muted', 'state.readonly.fg'] : ['text.subtle']).length) break;
    }
    if (st[key] === 0 && t0[role] > 0) adj.push(`${role}-text-strengthened`);
  }
  // 3. border.control and its hover: smallest tint opacity from 40% in steps of 5 that reaches 3:1
  const tint = (dark ? ctx.builtins['ink-dark'] : ctx.builtins['ink-light'])['border.control'];
  // 4. accent
  if (st.accent) {
    let ok = false;
    for (st.accShift = 0; st.accShift <= A_STEPS; st.accShift++) {
      if (!bad(assemble(), ['action.primary.bg', 'text.on-action']).length) { ok = true; break; }
    }
    if (!ok) { st.accent = null; adj.push('accent-dropped'); } else if (st.accShift) adj.push(dark ? 'accent-lighter' : 'accent-darker');
  }
  // 5. status colours
  for (const s of Object.keys(STATUS)) {
    for (st.status[s] = 0; st.status[s] <= S_STEPS; st.status[s]++) {
      if (!bad(assemble(), [`state.${s}.fg`]).length) break;
    }
    if (st.status[s] > S_STEPS) return reject('unreadable');
  }
  for (const n of Object.keys(TAGS)) {
    for (st.status[`tag.${n}`] = 0; st.status[`tag.${n}`] <= S_STEPS; st.status[`tag.${n}`]++) if (!bad(assemble(), [`tag.${n}.fg`]).length) break;
    if (st.status[`tag.${n}`] > S_STEPS) return reject('unreadable');
  }
  // 6. page
  let m = assemble();
  if (st.page && ratio(m['canvas.ink-default'], st.page) < 7) { st.page = null; adj.push('page-replaced'); m = assemble(); }
  // borders need the final surfaces
  const chrome = ['surface.base', 'surface.raised', 'surface.control', 'surface.control-hover', 'surface.strong', 'surface.overlay'];
  let pct = 40;
  for (; pct < 100; pct += 5) { const c = withAlpha(tint, pct); if (chrome.every((k) => ratio(over(c, m[k]), m[k]) >= 3)) break; }
  m['border.control'] = withAlpha(tint, pct); m['border.control-hover'] = withAlpha(tint, Math.min(100, pct + 25));
  sync(m);
  // 7. overrides, guarded like inputs
  for (const key of Object.keys(ov).sort()) {
    if (!themable.has(key) || !HEX68.test(ov[key])) { adj.push(`override-ignored:${key}`); continue; }
    let c = parseHex(ov[key]);
    if (key.startsWith('surface.') && c.a === 1) { const l = lchOf(c); if (l.C > CHROMA_MAX) { c = fromLch(l.L, CHROMA_MAX, l.h); adj.push(`override-toned-down:${key}`); } }
    const cand = sync({ ...m, [key]: c });
    if (evalMatrix((k) => cand[k], spec).some((r) => !r.ok)) { adj.push(`override-dropped:${key}`); continue; }
    m = cand;
  }
  // 8. final verification
  if (evalMatrix((k) => m[k], spec).some((r) => !r.ok)) return reject('verification-failed');
  const tokens = {};
  for (const k of [...themable].sort()) tokens[k] = toHex(m[k]);
  return { outcome: adj.length ? 'adjusted' : 'accepted', adjustments: adj, tokens };
}
