import { useEffect, useRef, type KeyboardEvent } from 'react';
import type { StudioEditor } from './useStudioEditor';

export interface StudioKeysOptions {
  editor: StudioEditor;
  /** Id of the text layer being edited in place. */
  editingId: string | null;
  onEndEdit: () => void;
  /** Delete goes through the screen, so it can offer Undo. */
  onDelete: (id: string) => void;
}

/**
 * The studio's keyboard: Cmd or Ctrl with Z and Shift Z or Y for undo and redo, Delete removes the layer, Cmd D duplicates, Escape clears the selection,
 * arrow keys nudge by 1 and by 10 with Shift. Keys typed in a field are left alone.
 */
export function useStudioKeys({ editor: ed, editingId, onEndEdit, onDelete }: StudioKeysOptions) {
  const timer = useRef<number>(undefined);
  useEffect(() => () => window.clearTimeout(timer.current), []);
  return (e: KeyboardEvent<HTMLElement>) => {
    const t = e.target as HTMLElement;
    if (['INPUT', 'TEXTAREA', 'SELECT'].includes(t.tagName) || t.isContentEditable) {
      if (e.key === 'Escape' && editingId) onEndEdit();
      return;
    }
    const m = e.metaKey || e.ctrlKey;
    const k = e.key.toLowerCase();
    if (m && k === 'z') { e.preventDefault(); if (e.shiftKey) ed.redo(); else ed.undo(); return; }
    if (m && k === 'y') { e.preventDefault(); ed.redo(); return; }
    const l = ed.selected;
    if (!l) return;
    if ((e.key === 'Delete' || e.key === 'Backspace') && !l.lock) { e.preventDefault(); onDelete(l.id); }
    else if (m && k === 'd') { e.preventDefault(); ed.duplicate(l.id); }
    else if (e.key === 'Escape') ed.select(null);
    else if (e.key.startsWith('Arrow') && !l.lock) {
      e.preventDefault();
      const s = e.shiftKey ? 10 : 1;
      const dx = e.key === 'ArrowLeft' ? -s : e.key === 'ArrowRight' ? s : 0;
      const dy = e.key === 'ArrowUp' ? -s : e.key === 'ArrowDown' ? s : 0;
      ed.patchLayer(l.id, { x: l.x + dx, y: l.y + dy });
      window.clearTimeout(timer.current);
      timer.current = window.setTimeout(ed.commit, 300);
    }
  };
}
