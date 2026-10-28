import { buildTemplates, CURRENT_COVER, CURRENT_FOLDER, CV_CURRENT, CV_LIB, type CanvasSettings, type Design } from '@/assets/studio';
import { CanvasSheet } from '@/blocks/CanvasSheet';
import { FolderSheet } from '@/blocks/FolderSheet';
import { NotebookSheet } from '@/blocks/NotebookSheet';
import { makeDesign } from '@/blocks/DesignSheet';
import { SheetDemo } from '@/stories-support/studio/SheetDemo';

export interface StudioSheetsProps {
  /** Which sheet is open over its library. */
  sheet: 'folder' | 'notebook' | 'canvas';
  /** New starts from a preset; Edit starts from an item that is already there and adds Delete. */
  mode: 'new' | 'edit';
  /** The tab the sheet opens on. */
  tab?: string;
}

const design = (d: Design) => ({ name: d.name, meta: d.meta, design: d });
const canvas = (c: CanvasSettings) => ({ name: c.name, meta: c.meta ?? '', canvas: c });

/** A New or Edit sheet in place, over the library it belongs to. Creating, saving and deleting change the library and show their toast. */
export function StudioSheets({ sheet, mode, tab }: StudioSheetsProps) {
  const T = buildTemplates();
  const edit = mode === 'edit';

  if (sheet === 'canvas') {
    const items = (edit ? [CV_CURRENT, CV_LIB[1], CV_LIB[2]] : CV_LIB).map(canvas);
    return (
      <SheetDemo
        kind="canvas" mode={mode} items={items}
        sheet={({ open, onClose, created, saved, removed }) => (
          <CanvasSheet
            open={open} onClose={onClose} mode={mode} placement="container" initial={edit ? CV_CURRENT : undefined} initialTab={tab as 'size' | undefined}
            onCreate={(c) => (edit ? saved(canvas(c)) : created(canvas(c)))} onDelete={removed}
          />
        )}
      />
    );
  }

  const isF = sheet === 'folder';
  const kind = isF ? 'folder' : 'cover';
  const cur = isF ? CURRENT_FOLDER : CURRENT_COVER;
  const pool = isF ? T.folders : T.covers;
  const items = (edit ? [makeDesign(kind, cur), pool[2], pool[3], ...(isF ? [] : [pool[4]])] : [pool[0], pool[2], pool[3]]).map(design);
  const Sheet = isF ? FolderSheet : NotebookSheet;
  return (
    <SheetDemo
      kind={kind} mode={mode} items={items}
      sheet={({ open, onClose, created, saved, removed }) => (
        <Sheet
          open={open} onClose={onClose} mode={mode} placement="container" initial={edit ? cur : undefined} initialTab={tab}
          onCreate={(c, d) => (edit ? saved(design({ ...d, name: c.name })) : created(design({ ...d, name: c.name })))} onDelete={removed} onCustomise={() => onClose()}
        />
      )}
    />
  );
}
