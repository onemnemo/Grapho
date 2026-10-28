// Loads tokens.json and answers questions about it: flat leaves, alias resolution, theme variants,
// resolved colour maps. Shared by the CSS build, the Kotlin build and the theme derivation tests.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseHex } from './contrast.mjs';

export const here = dirname(fileURLToPath(import.meta.url));
export const ALIAS = /\{([^{}]+)\}/g;
export const tierOf = (p) => p.split('.')[0];
export const ext = (t, key) => t.$extensions?.grapho?.[key];

export const load = (file = join(here, 'tokens.json')) => {
  const doc = JSON.parse(readFileSync(file, 'utf8'));
  const leaves = new Map();
  const walk = (node, path) => {
    for (const [k, v] of Object.entries(node)) {
      if (k.startsWith('$')) continue;
      const p = path ? `${path}.${k}` : k;
      if (v && typeof v === 'object' && '$value' in v) leaves.set(p, v);
      else if (v && typeof v === 'object') walk(v, p);
    }
  };
  walk(doc, '');
  const errors = [];
  const resolve = (p, seen = []) => {
    const t = leaves.get(p);
    if (!t) { errors.push(`alias target not found: ${p}`); return null; }
    if (seen.includes(p)) { errors.push(`alias cycle: ${[...seen, p].join(' > ')}`); return null; }
    const m = typeof t.$value === 'string' && t.$value.match(/^\{([^{}]+)\}$/);
    return m ? resolve(m[1], [...seen, p]) : t;
  };
  // theme variants
  const variants = [];
  for (const [fam, famNode] of Object.entries(doc.themes)) {
    if (fam.startsWith('$')) continue;
    for (const [mode, node] of Object.entries(famNode)) {
      if (mode.startsWith('$')) continue;
      const g = node.$extensions?.grapho ?? {};
      const v = { id: g.id ?? `${fam}-${mode}`, family: fam, scheme: g.colorScheme ?? mode, isDefault: !!g.default, overrides: new Map() };
      const collect = (n, path) => {
        for (const [k, x] of Object.entries(n)) {
          if (k.startsWith('$')) continue;
          const pp = path ? `${path}.${k}` : k;
          if (x && typeof x === 'object' && '$value' in x) v.overrides.set(`semantic.${pp}`, x);
          else if (x && typeof x === 'object') collect(x, pp);
        }
      };
      collect(node, '');
      variants.push(v);
    }
  }
  const themed = new Set(variants.flatMap((v) => [...v.overrides.keys()]));
  const colourOf = (v, sp) => {
    let tok = v.overrides.get(sp) ?? leaves.get(sp);
    if (!tok) return null;
    if (typeof tok.$value === 'string' && /^\{.+\}$/.test(tok.$value)) tok = resolve(tok.$value.slice(1, -1));
    return parseHex(tok.$value);
  };
  // resolved colour map of one variant: "surface.base" -> {r,g,b,a}, every semantic colour token
  const mapOf = (v) => {
    const m = {};
    for (const [p, t] of leaves) if (tierOf(p) === 'semantic' && t.$type === 'color') m[p.slice(9)] = colourOf(v, p);
    for (const [p, tk] of leaves) {
      if (tierOf(p) !== 'component' || tk.$type !== 'color') continue;
      const al = typeof tk.$value === 'string' && tk.$value.match(/^\{semantic\.(.+)\}$/);
      m[p.slice(10)] = al ? m[al[1]] : colourOf(v, p);
    }
    return m;
  };
  return { doc, leaves, errors, resolve, variants, themed, colourOf, mapOf, defaultVariant: variants.find((x) => x.isDefault), spec: doc.$extensions?.grapho?.pairs, forcedColors: doc.$extensions?.grapho?.forcedColors ?? {} };
};

// Structural checks that do not need colour maths.
export const validate = (M) => {
  const { leaves, errors, variants, themed, defaultVariant } = M;
  const DASH = new RegExp('[' + String.fromCharCode(0x2013, 0x2014) + ']');
  const HEX = /#[0-9a-fA-F]{3,8}\b/;
  const scan = (n, path) => {
    if (typeof n === 'string') { if (DASH.test(n)) errors.push(`dash character in ${path}`); return; }
    if (Array.isArray(n)) return n.forEach((x, i) => scan(x, `${path}[${i}]`));
    if (n && typeof n === 'object') for (const [k, v] of Object.entries(n)) scan(v, `${path}.${k}`);
  };
  scan(M.doc, '');
  for (const [p, t] of leaves) {
    const s = JSON.stringify(t.$value);
    if (!t.$type) errors.push(`missing $type: ${p}`);
    for (const m of s.matchAll(ALIAS)) if (!leaves.has(m[1])) errors.push(`${p}: alias target not found: ${m[1]}`);
    if (tierOf(p) === 'primitive' && s.includes('{')) errors.push(`primitive must not alias: ${p}`);
    if (tierOf(p) !== 'primitive') {
      if (HEX.test(s)) errors.push(`raw hex outside the primitive tier: ${p}`);
      if (t.$type === 'dimension' && !s.includes('{')) errors.push(`raw dimension outside the primitive tier: ${p}`);
      if (t.$type === 'duration' && !s.includes('{')) errors.push(`raw duration outside the primitive tier: ${p}`);
    }
  }
  if (!defaultVariant) errors.push('no default theme variant');
  for (const v of variants) {
    if (v.isDefault) continue;
    for (const p of themed) if (!v.overrides.has(p)) errors.push(`theme ${v.id} does not override ${p.slice(9)}`);
    for (const [p, o] of v.overrides) if (leaves.get(p) && leaves.get(p).$type !== o.$type) errors.push(`theme ${v.id}: type mismatch for ${p}`);
  }
  for (const [p] of themed.entries()) if (!leaves.has(p)) errors.push(`theme overrides unknown token ${p}`);
};
