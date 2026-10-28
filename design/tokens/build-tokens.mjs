#!/usr/bin/env node
// Builds tokens.css from tokens.json. No dependencies.
//
//   node design/tokens/build-tokens.mjs                 write tokens.css and the generated reports
//   node design/tokens/build-tokens.mjs --check         write nothing; fail if any generated file is stale
//   node design/tokens/build-tokens.mjs --write-vectors regenerate themes/vectors.json from the reference derivation
//
// Every run validates tokens.json (aliases, raw values only in the primitive tier, no dash
// characters, every variant complete), checks the full contrast matrix for every theme variant,
// checks that variants are distinct, and verifies the custom theme test vectors.

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { load, validate, tierOf, ext, here, ALIAS } from './tokens-model.mjs';
import { failures, toHex } from './contrast.mjs';
import { legacyLayer } from './legacy.mjs';
import { runMatrix, usageRules, distances, matrixMarkdown, MIN_DISTANCE } from './report.mjs';
import { verifyVectors } from '../themes/verify-vectors.mjs';

const args = new Set(process.argv.slice(2));
const M = load();
const { leaves, errors, variants, themed, defaultVariant } = M;
const fail = (m) => errors.push(m);
validate(M);

const cssName = (p) => { const parts = p.split('.'); const tier = parts.shift(); return '--' + (tier === 'primitive' ? 'p-' : '') + parts.join('-'); };
const alias = (s) => s.replace(ALIAS, (_, p) => (leaves.has(p) ? `var(${cssName(p)})` : p));
const fontFamily = (v) => v.map((f) => (/\s/.test(f) ? `"${f}"` : f)).join(', ');
const val = (v, type) => {
  if (typeof v === 'string') return alias(v);
  switch (type) {
    case 'fontFamily': return fontFamily(v);
    case 'cubicBezier': return `cubic-bezier(${v.join(', ')})`;
    case 'shadow': return v.map((s) => `${val(s.offsetX)} ${val(s.offsetY)} ${val(s.blur)} ${val(s.spread)} ${val(s.color)}`).join(', ');
    case 'gradient': return `linear-gradient(90deg, ${v.map((g) => `${val(g.color)} ${Math.round(g.position * 100)}%`).join(', ')})`;
    default: return String(v);
  }
};
const emit = (p, t) => {
  const name = cssName(p);
  if (t.$type === 'typography') {
    const v = t.$value;
    return [`${name}: ${val(v.fontWeight)} ${val(v.fontSize)}/${val(v.lineHeight)} ${val(v.fontFamily)};`, `${name}-tracking: ${val(v.letterSpacing)};`];
  }
  let value = val(t.$value, t.$type);
  if (t.$type === 'duration' && value === '0ms') value = '0.01ms';
  if (t.$type === 'duration' && /var\(--p-duration-instant\)/.test(value)) return [`${name}: 0.01ms;`];
  return [`${name}: ${value};`];
};

let matrix = null;
if (!errors.length) {
  matrix = runMatrix(M);
  for (const v of variants) for (const f of failures(matrix[v.id].results)) fail(`contrast ${v.id}: ${f.fg} on ${f.bg}${f.host ? ' over ' + f.host : ''} is ${f.ratio.toFixed(2)}:1, needs ${f.need}:1`);
  for (const d of distances(M, matrix)) {
    if (d.base < MIN_DISTANCE) fail(`variants ${d.a} and ${d.b} are too alike on surface/base (${d.base.toFixed(3)} < ${MIN_DISTANCE})`);
    if (d.control < MIN_DISTANCE) fail(`variants ${d.a} and ${d.b} are too alike on surface/control (${d.control.toFixed(3)} < ${MIN_DISTANCE})`);
  }
  for (const m of await verifyVectors(M, matrix, { write: args.has('--write-vectors') })) fail(m);
}
if (errors.length) { console.error(`tokens: ${errors.length} problem(s)\n  ` + errors.slice(0, 60).join('\n  ') + (errors.length > 60 ? `\n  ... and ${errors.length - 60} more` : '')); process.exit(1); }

const lines = [];
const block = (selector, decls, indent = '') => {
  if (!decls.length) return;
  lines.push(`${indent}${selector} {`);
  for (const d of decls) lines.push(`${indent}  ${d}`);
  lines.push(`${indent}}`);
};
const declsFor = (paths) => paths.flatMap((p) => emit(p, leaves.get(p)));
const swap = (paths, key) => paths.flatMap((p) => { const t = leaves.get(p); return emit(p, { ...t, $value: ext(t, key) }); });
const overrideDecls = (v) => [`color-scheme: ${v.scheme};`, ...[...v.overrides].flatMap(([sp, t]) => emit(sp, t))];

const platformTokens = [...leaves].filter(([p, t]) => tierOf(p) === 'semantic' && typeof ext(t, 'platform')?.android === 'string');
const androidDecls = platformTokens.flatMap(([p, t]) => emit(p, { ...t, $value: ext(t, 'platform').android }));
const prims = [], rootSem = [], derived = [], themedDefaults = [], compact = [], reduced = [], more = [];
for (const [p, t] of leaves) {
  const tier = tierOf(p);
  if (tier === 'primitive') { prims.push(p); continue; }
  if (ext(t, 'contrastMore') !== undefined) more.push(p);
  if (themed.has(p)) { themedDefaults.push(p); continue; }
  const refsNonPrim = [...JSON.stringify(t.$value).matchAll(ALIAS)].some((m) => tierOf(m[1]) !== 'primitive');
  if (refsNonPrim) { derived.push(p); continue; }
  rootSem.push(p);
  if (ext(t, 'compact') !== undefined) compact.push(p);
  if (ext(t, 'reducedMotion') !== undefined) reduced.push(p);
}
const touchDecls = declsFor(compact), compactDecls = swap(compact, 'compact'), reducedDecls = swap(reduced, 'reducedMotion');
const moreDecls = more.flatMap((p) => { const t = leaves.get(p); return [`${cssName(p)}: ${alias(ext(t, 'contrastMore'))};`]; });
const forced = Object.entries(M.forcedColors).map(([k, c]) => `${cssName('semantic.' + k)}: ${c};`);

lines.push('/* Generated by design/tokens/build-tokens.mjs from tokens.json. Do not edit by hand.');
lines.push('   Tiers: --p-* primitives (never read by components), semantic names (--surface-base, --text-default),');
lines.push('   component names (--focus-ring-width). Themes are [data-theme="<family>-<mode>"] blocks that override');
lines.push('   semantic names only. Density is app state, not a media query: the app sets data-density="touch" or');
lines.push('   "compact" on the root (see tokens/README.md). Touch is the default. */', '');
lines.push('/* ---- tier 1: primitives, and semantic tokens no theme changes (touch density) ---- */');
block(':root', [...declsFor(prims), ...declsFor(rootSem)]);
lines.push('', '/* ---- default theme: Ink dark ---- */');
const ALIASES = { 'ink-dark': ['ink', 'system'], 'graphite-dark': ['graphite'], 'paper-light': ['paper'] };
const sel = (id) => [`[data-theme="${id}"]`, ...(ALIASES[id] ?? []).map((a) => `[data-theme="${a}"]`)].join(', ');
block(`:root, ${sel(defaultVariant.id)}`, [`color-scheme: ${defaultVariant.scheme};`, ...declsFor(themedDefaults)]);
lines.push('', '/* ---- density: set by the app ---- */');
block('[data-density="touch"]', touchDecls);
block('[data-density="compact"]', compactDecls);
lines.push('', '/* ---- platform: Android uses 48 for the touch hit target (Material) ---- */');
block(':root[data-platform="android"]:not([data-density="compact"])', androidDecls);
block('[data-platform="android"][data-density="touch"]', androidDecls);
lines.push('', '/* ---- other theme variants: semantic overrides only ---- */');
for (const v of variants) if (!v.isDefault) block(sel(v.id), overrideDecls(v));
const inkLight = variants.find((v) => v.id === 'ink-light');
lines.push('', '/* No theme chosen: follow the system. */', '@media (prefers-color-scheme: light) {');
block(':root:not([data-theme]), [data-theme="system"]', overrideDecls(inkLight), '  ');
lines.push('}', '', '/* ---- reduced motion: no movement, end states still apply ---- */', '@media (prefers-reduced-motion: reduce) {');
block(':root:not([data-motion="full"])', reducedDecls, '  ');
lines.push('}');
block('[data-motion="reduced"]', reducedDecls);
lines.push('', '/* ---- more contrast: edges and secondary text step up ---- */', '@media (prefers-contrast: more) {');
block(':root:not([data-contrast="normal"])', moreDecls, '  ');
lines.push('}');
block('[data-contrast="more"]', moreDecls);
lines.push('', '/* ---- forced colours (Windows contrast themes): map to system colours ---- */', '@media (forced-colors: active) {');
block(':root', forced, '  ');
lines.push('}', '', '/* ---- derived: tokens that read semantic colours, declared on every themed element ---- */');
block(':root, [data-theme]', declsFor(derived));
lines.push('');
const css = lines.join('\n');

const cssNames = new Set([...css.matchAll(/(--[a-zA-Z0-9-]+)\s*:/g)].map((m) => m[1]));
const legacy = legacyLayer(cssNames);
for (const e of legacy.errors) fail(e);
const srcCss = css + `
/* ---- legacy aliases: every variable the old tokens.css defined, in terms of the tokens above ----
   Declared on every themed element so a theme preview inside the page gets its own values. The design
   workspace still reads these names; new components read the names above. ${legacy.aliased} names are
   aliases and ${legacy.kept} keep their old value. */
:root, [data-theme] {
${legacy.lines.map((l) => '  ' + l).join('\n')}
}
`;
{ // every var() in the generated css must resolve
  const defined = new Set([...srcCss.matchAll(/(--[a-zA-Z0-9-]+)\s*:/g)].map((m) => m[1]));
  for (const m of srcCss.matchAll(/var\((--[a-zA-Z0-9-]+)/g)) if (!defined.has(m[1])) fail(`src tokens.css uses undefined ${m[1]}`);
}
const hexMap = (id) => Object.fromEntries(Object.entries(matrix[id].map).sort(([a], [b]) => (a < b ? -1 : 1)).map(([k, c]) => [k, toHex(c)]));
const themeData = `// Generated by design/tokens/build-tokens.mjs from tokens.json. Do not edit by hand.
// The data deriveTheme.ts needs at runtime: the two base variants, the contrast pair list, the themable tokens.
export const THEME_DATA = ${JSON.stringify({ builtins: { 'ink-dark': hexMap('ink-dark'), 'ink-light': hexMap('ink-light') }, spec: M.spec, themable: [...themed].map((p) => p.slice(9)).sort() }, null, 1)};
`;
if (errors.length) { console.error(`tokens: ${errors.length} problem(s)\n  ` + errors.join('\n  ')); process.exit(1); }


const rules = usageRules(M);
const outputs = [
  [join(here, 'tokens.css'), css],
  [join(here, '..', 'src', 'tokens', 'tokens.css'), srcCss],
  [join(here, '..', 'src', 'tokens', 'themeData.ts'), themeData],
  [join(here, 'usage-rules.json'), JSON.stringify({ $description: 'Generated. The pairs in tokens.json are an allow-list: each foreground may sit only on its allowed hosts; forbidden lists the rest. Read by scripts/check-design.mjs and component specs.', allowed: rules.allowed, forbidden: rules.forbidden }, null, 2) + '\n'],
  [join(here, '..', 'themes', 'contrast-matrix.md'), matrixMarkdown(M, matrix, rules, distances(M, matrix))],
];
let stale = 0;
for (const [file, text] of outputs) {
  if (args.has('--check')) { if (!existsSync(file) || readFileSync(file, 'utf8') !== text) { console.error(`${file} is out of date. Run: node design/tokens/build-tokens.mjs`); stale++; } }
  else writeFileSync(file, text);
}
if (stale) process.exit(1);
const graded = variants.reduce((n, v) => n + matrix[v.id].results.filter((r) => !r.exempt).length, 0);
console.log(`${args.has('--check') ? 'up to date' : 'wrote tokens.css, usage-rules.json, themes/contrast-matrix.md'}: ${leaves.size} tokens, ${variants.length} theme variants, ${graded} graded contrast pairs passed, distances >= ${MIN_DISTANCE}, vectors verified, ${legacy.aliased} legacy aliases + ${legacy.kept} kept`);
