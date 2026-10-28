// Fails if src/lib/deriveTheme.ts (the runtime port) and themes/derive-theme.mjs (the reference) disagree on
// any vector in themes/vectors.json, or if either differs from the expected output stored there.
// Runs on Node 22.18 or newer, which loads .ts files that use only erasable types.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { deriveTheme as port } from '../src/lib/deriveTheme.ts';
import { deriveTheme as reference } from '../themes/derive-theme.mjs';
import { THEME_DATA } from '../src/tokens/themeData.ts';
import { parseHex } from '../tokens/contrast.mjs';

const parse = (m) => Object.fromEntries(Object.entries(m).map(([k, v]) => [k, parseHex(v)]));
const refCtx = { builtins: { 'ink-dark': parse(THEME_DATA.builtins['ink-dark']), 'ink-light': parse(THEME_DATA.builtins['ink-light']) }, spec: THEME_DATA.spec, themable: THEME_DATA.themable };
const vectors = JSON.parse(readFileSync(fileURLToPath(new URL('../themes/vectors.json', import.meta.url)), 'utf8')).vectors;
const problems = [];
for (const v of vectors) {
  const a = JSON.stringify(port(v.input, THEME_DATA)), b = JSON.stringify(reference(v.input, refCtx)), want = JSON.stringify(v.expect);
  if (a !== b) problems.push(`${v.name}: deriveTheme.ts and derive-theme.mjs differ`);
  if (b !== want) problems.push(`${v.name}: reference differs from vectors.json`);
}
if (problems.length) { console.error(problems.join('\n')); process.exit(1); }
console.log(`derive check ok (${vectors.length} vectors identical in deriveTheme.ts and derive-theme.mjs)`);
