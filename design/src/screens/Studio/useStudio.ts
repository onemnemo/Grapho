import { useRef, useState } from 'react';
import { buildPresetItems, deep, designArea, DIM, L, tTex, TEX, newLayer, type Design, type Layer, type PresetItem, type TextureKey, type ToolKey } from '@/assets/studio';
import type { InspectorAction } from '@/blocks/Inspector';
import type { StickerResult } from '@/blocks/BackgroundRemover';
import type { Crop } from '@/blocks/CropDialog';
import { useToast } from '@/components/Toast';
import { applyProp, useDragIn, useStudioEditor, useStudioKeys } from '@/lib/studio';
import type { PanelImport } from '@/blocks/StudioPanel';

export type StudioDialog = null | 'unsaved' | 'presets' | 'export' | 'import' | 'picture' | 'crop';

export interface StudioOptions {
  design: Design;
  tool?: ToolKey;
  dialog?: StudioDialog;
  onClose?: () => void;
  onGetPacks?: () => void;
}

/** All the state and actions of the studio screen: one editor, the open tool, dialogs, and the toasts that follow each action. */
export function useStudio({ design, tool: tool0 = 'stickers', dialog: dialog0 = null, onClose, onGetPacks }: StudioOptions) {
  const ed = useStudioEditor(design);
  const toast = useToast();
  const [tool, setTool] = useState<ToolKey>(tool0);
  const [iconStyle, setIconStyle] = useState<'line' | 'soft'>('line');
  const [playing, setPlaying] = useState(false);
  const [safe, setSafe] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [appearId, setAppearId] = useState<string | null>(null);
  const [swapKey, setSwapKey] = useState(0);
  const [over, setOver] = useState(false);
  const [carrying, setCarrying] = useState(false);
  const [yours, setYours] = useState<Design[]>([]);
  const [dialog, setDialog] = useState<StudioDialog>(dialog0);
  const [picture, setPicture] = useState<string | undefined>(undefined);
  const [exporting, setExporting] = useState<PresetItem[]>(() => (dialog0 === 'export' ? buildPresetItems(design.kind).slice(0, 3) : []));
  const canvas = useRef<HTMLDivElement | null>(null);
  const file = useRef<HTMLInputElement | null>(null);
  const d = ed.design;
  const noun = d.kind === 'cover' ? 'Cover' : 'Folder';

  const add = (l: Layer, at?: { x: number; y: number }) => {
    ed.addLayer(l, at);
    setAppearId(l.id);
    window.setTimeout(() => setAppearId(null), 400);
  };

  const bind = useDragIn({
    target: () => canvas.current?.getBoundingClientRect() ?? null,
    onOver: setOver,
    onCarry: setCarrying,
    onTap: (spec) => add(newLayer(spec, d, { iconStyle })),
    onDrop: (spec, x, y) => {
      const r = canvas.current?.getBoundingClientRect();
      if (!r) return;
      const s = r.width / DIM[d.kind].W;
      add(newLayer(spec, d, { iconStyle }), { x: (x - r.left) / s, y: (y - r.top) / s });
    },
  });

  const remove = (id: string) => {
    const restore = ed.deleteLayer(id);
    toast.show('Layer deleted', { undo: restore });
  };

  const handleFile = (f: File) => {
    if (f.type.startsWith('video/')) {
      add(L('video', { src: URL.createObjectURL(f), ...designArea(d), name: f.name.slice(0, 20) }));
      toast.show('Video added. Muted and looping.');
    } else if (/\.graphopack$/i.test(f.name)) setDialog('import');
    else if (f.type.startsWith('image/')) {
      const r = new FileReader();
      r.onload = () => { setPicture(String(r.result)); setDialog('picture'); };
      r.readAsDataURL(f);
    } else toast.show('That file type is not supported');
  };

  const onImport = (what: PanelImport) => {
    if (what === 'pack') { setDialog('import'); return; }
    if (file.current) {
      file.current.accept = what === 'video' ? 'video/mp4,video/webm' : what === 'svg' ? 'image/svg+xml' : 'image/png,image/gif,image/jpeg,image/webp';
      file.current.click();
    }
  };

  const edit = (path: string, value: unknown, final: boolean) => {
    ed.live((x) => applyProp(x, ed.selected, path, value));
    if (final) ed.commit();
  };

  const action = (a: InspectorAction) => {
    const l = ed.selected;
    if (!l) return;
    const A = designArea(d);
    if (a === 'dup') ed.duplicate(l.id);
    else if (a === 'del') remove(l.id);
    else if (a === 'lock') ed.patchLayer(l.id, { lock: !l.lock }, true);
    else if (a === 'fwd') ed.move(l.id, 1);
    else if (a === 'bwd') ed.move(l.id, -1);
    else if (a === 'flipx') ed.patchLayer(l.id, { fx: l.fx ? 0 : 1 }, true);
    else if (a === 'flipy') ed.patchLayer(l.id, { fy: l.fy ? 0 : 1 }, true);
    else if (a === 'ctrx') ed.patchLayer(l.id, { x: A.x }, true);
    else if (a === 'ctry') ed.patchLayer(l.id, { y: A.y }, true);
    else if (a === 'bind') ed.patchLayer(l.id, { bind: 'name' }, true);
    else if (a === 'fill') ed.patchLayer(l.id, { ...A, r: 0 }, true);
    else if (a === 'crop') setDialog('crop');
  };

  const keys = useStudioKeys({ editor: ed, editingId, onEndEdit: () => endEdit(), onDelete: remove });

  const endEdit = () => { setEditingId(null); ed.commit(); };
  const editText = (text: string) => ed.live((x) => { const l = x.layers.find((q) => q.id === editingId); if (l) { l.text = text; delete l.bind; } });

  const material = (key: TextureKey) => add(tTex(key, designArea(d), { name: TEX[key].name, op: TEX[key].op ?? 1 }));

  const usePreset = (src: Design) => {
    ed.replaceWith(src);
    setSwapKey((k) => k + 1);
    toast.show('Preset applied', { undo: ed.undo });
  };

  const savePreset = () => {
    const name = `My ${d.kind} ${yours.length + 1}`;
    setYours((y) => [{ ...deep(d), name }, ...y]);
    toast.show(`Saved as preset: ${name}`);
  };

  const done = () => { ed.markSaved(); toast.show(`${noun} saved`); };
  const close = () => { if (ed.dirty) setDialog('unsaved'); else { toast.show('Closed'); onClose?.(); } };
  const keep = () => setDialog(null);
  const save = () => { ed.markSaved(); setDialog(null); toast.show(`${d.name} saved`); };
  const discard = () => { ed.discard(); setDialog(null); toast.show('Changes discarded'); };

  const addSticker = (r: StickerResult) => {
    const w = d.kind === 'cover' ? 200 : 220;
    add(L('sticker', { src: r.src, w, h: w / r.ratio, cut: r.cut, sh: 1, name: r.removed ? 'Cut-out sticker' : 'Imported sticker' }));
  };
  const addImage = (crop: Crop, aspect: number) => {
    const w = d.kind === 'cover' ? 200 : 220;
    add(L('image', { src: picture, w, h: w / aspect, crop: { ...crop }, rad: 6, sh: 1, name: 'Imported image' }));
  };
  const applyCrop = (crop: Crop, aspect: number) => {
    const l = ed.selected;
    if (l) ed.patchLayer(l.id, { crop: { ...crop }, ...(aspect ? { h: l.w / aspect } : null) }, true);
  };

  return {
    ed, d, tool, setTool, iconStyle, setIconStyle, playing, setPlaying, safe, setSafe, editingId, setEditingId, appearId, swapKey, over, carrying,
    yours, dialog, setDialog, picture, exporting, setExporting, canvas, file, bind, remove, handleFile, onImport, edit, action, keys, endEdit, editText,
    material, usePreset, savePreset, done, close, keep, save, discard, addSticker, addImage, applyCrop,
    onGetPacks,
  };
}
