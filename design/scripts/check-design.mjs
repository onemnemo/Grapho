// Design system guard rails. Run with `npm run check`.
// 1. No raw colours outside the token file: CSS and TSX use var(--token).
// 2. Every component and block folder has Name.tsx, Name.stories.tsx and index.ts.
// 3. Shipped code never imports Storybook scaffolding from src/stories-support.
// 4. Component names are unique across components, blocks and screens.
import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { join, relative, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const src = join(root, 'src');
const TOKENS = join(src, 'tokens', 'tokens.css');
// Asset data (icon paths, mascot sprite, cover palettes) is artwork, not chrome.
const RAW_ALLOWED = [join(src, 'assets')];
const RAW_COLOUR = /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(/;

const problems = [];
const walk = (dir) =>
  readdirSync(dir).flatMap((n) => {
    const p = join(dir, n);
    return statSync(p).isDirectory() ? walk(p) : [p];
  });

for (const file of walk(src)) {
  if (file === TOKENS || RAW_ALLOWED.some((d) => file.startsWith(d))) continue;
  if (!/\.(css|tsx?)$/.test(file)) continue;
  readFileSync(file, 'utf8').split('\n').forEach((line, i) => {
    const code = line.replace(/\/\*.*?\*\/|\/\/.*$/g, '');
    if (RAW_COLOUR.test(code)) problems.push(`${relative(root, file)}:${i + 1}: raw colour, use a token from src/tokens/tokens.css`);
  });
}

const SHOWCASES = ['screens/StudioSheets/'];
for (const file of walk(src)) {
  const rel = relative(src, file);
  if (!/\.tsx?$/.test(file) || rel.startsWith('stories-support') || /\.stories\.tsx?$/.test(file) || SHOWCASES.some((s) => rel.startsWith(s))) continue;
  if (/from '@\/stories-support\//.test(readFileSync(file, 'utf8'))) problems.push(`${relative(root, file)}: imports from stories-support, which is for stories only`);
}

const seen = new Map();
for (const layer of ['components', 'blocks', 'screens']) {
  const dir = join(src, layer);
  if (!existsSync(dir)) continue;
  for (const name of readdirSync(dir)) {
    const folder = join(dir, name);
    if (!statSync(folder).isDirectory()) continue;
    if (seen.has(name)) problems.push(`${layer}/${name}: name already used in ${seen.get(name)}`);
    seen.set(name, layer);
    const need = layer === 'screens' ? [`${name}.tsx`, `${name}.stories.tsx`] : [`${name}.tsx`, `${name}.stories.tsx`, 'index.ts'];
    for (const f of need) if (!existsSync(join(folder, f))) problems.push(`${layer}/${name}: missing ${f}`);
  }
}

if (problems.length) {
  console.error(problems.join('\n'));
  console.error(`\n${problems.length} problem(s).`);
  process.exit(1);
}
console.log(`design check ok (${basename(src)}/ clean)`);
