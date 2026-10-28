// Runs themes/vectors.json through the reference derivation and compares exact output.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deriveTheme } from './derive-theme.mjs';

const file = join(dirname(fileURLToPath(import.meta.url)), 'vectors.json');
const INPUTS = [
  ['harbour-light', { rules: 1, id: 'harbour', inputs: { background: '#EAF0F4', surface: '#D5E0E8', accent: '#1F6F8B', page: '#FFFFFF' } }],
  ['neon-magenta-background', { rules: 1, id: 'neon-magenta', inputs: { background: '#FF00FF' } }],
  ['neon-green-background-neon-accent', { rules: 1, id: 'neon-green', inputs: { background: '#00FF00', accent: '#FF00FF' } }],
  ['mid-grey-808080', { rules: 1, id: 'mid-grey', inputs: { background: '#808080' } }],
  ['mid-grey-777777-forced-dark', { rules: 1, id: 'mid-grey-dark', scheme: 'dark', inputs: { background: '#777777' } }],
  ['pure-black', { rules: 1, id: 'black', inputs: { background: '#000000' } }],
  ['pure-white', { rules: 1, id: 'white', inputs: { background: '#FFFFFF' } }],
  ['low-contrast-surface-on-black', { rules: 1, id: 'low-contrast', inputs: { background: '#08080A', surface: '#E0E0E0' } }],
  ['saturated-blue-background', { rules: 1, id: 'blue', inputs: { background: '#1E40AF' } }],
  ['accent-equals-surface', { rules: 1, id: 'accent-surface', inputs: { background: '#1B1B20', surface: '#2B2B33', accent: '#2B2B33' } }],
  ['page-too-close-to-ink', { rules: 1, id: 'page-ink', inputs: { background: '#F4F1EA', page: '#8A8D96' } }],
  ['overrides-guarded', { rules: 1, id: 'overrides', inputs: { background: '#14181F' }, overrides: { 'hover.fill': '#F5F5F4', 'scrim.default': '#00000000', 'surface.raised': '#FF2D95', 'unknown.token': '#123456' } }],
  ['yellow-background-pale-accent', { rules: 1, id: 'yellow', inputs: { background: '#FFFF00', accent: '#FFFF99' } }],
  ['navy-with-bright-accent', { rules: 1, id: 'navy', inputs: { background: '#0B1F3A', accent: '#4DA3FF' } }],
  ['rules-too-new', { rules: 2, id: 'future', inputs: { background: '#FFFFFF' } }],
  ['invalid-colour', { rules: 1, id: 'bad', inputs: { background: '#12345' } }],
];

export const verifyVectors = async (M, matrix, { write }) => {
  const ctx = { builtins: { 'ink-dark': matrix['ink-dark'].map, 'ink-light': matrix['ink-light'].map }, spec: M.spec, themable: [...M.themed].map((p) => p.slice(9)) };
  const vectors = INPUTS.map(([name, input]) => ({ name, input, expect: deriveTheme(input, ctx) }));
  const text = JSON.stringify({ $description: 'Golden vectors for the custom theme rules, version 1. Generated from derive-theme.mjs with --write-vectors and reviewed by hand; every shell must reproduce expect exactly.', rules: 1, vectors }, null, 1) + '\n';
  if (write) { writeFileSync(file, text); return []; }
  if (!existsSync(file)) return ['themes/vectors.json is missing. Run build-tokens.mjs --write-vectors'];
  return readFileSync(file, 'utf8') === text ? [] : ['themes/vectors.json does not match the reference derivation. Review the change, then run build-tokens.mjs --write-vectors'];
};
