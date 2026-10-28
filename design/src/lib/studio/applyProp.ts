import { FONTS, PAPER_COLOURS, shade, type Design, type FontKey, type Layer } from '@/assets/studio';

const setPath = (o: Record<string, unknown>, path: string, v: unknown) => {
  const ks = path.split('.');
  let t = o;
  ks.slice(0, -1).forEach((k) => { t = t[k] as Record<string, unknown>; });
  t[ks[ks.length - 1]] = v;
};

/**
 * Write one inspector value into a design draft. A path with a dot, such as `base.c1` or `folder.name`, belongs to the design;
 * any other path is a property of the selected layer.
 */
export function applyProp(d: Design, layer: Layer | null, path: string, v: unknown): void {
  if (path === 'base.grad') {
    if (v) { d.base.c2 = shade(d.base.c1, d.kind === 'cover' ? -40 : -30); d.base.ang = 160; } else d.base.c2 = null;
    return;
  }
  if (path === 'folder.tabc' && d.folder) { d.folder.tab = v ? shade(d.folder.back, 30) : null; return; }
  if (path === 'folder.papersN' && d.folder) {
    const cur = d.folder.papers || [];
    d.folder.papers = Array.from({ length: v as number }, (_, i) => cur[i] || PAPER_COLOURS[i]);
    return;
  }
  if (/^(base|cover|folder)\./.test(path)) { setPath(d as unknown as Record<string, unknown>, path, v); return; }
  if (!layer) return;
  if (path === 'text') { layer.text = v as string; delete layer.bind; return; }
  if (path === 'font') {
    layer.font = v as FontKey;
    const w = FONTS[v as FontKey][2];
    if (!w.includes(layer.weight || 0)) layer.weight = w[0];
    return;
  }
  (layer as unknown as Record<string, unknown>)[path] = v;
}
