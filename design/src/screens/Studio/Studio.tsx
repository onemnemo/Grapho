import { useState } from 'react';
import { DIM, type Design, type ToolKey } from '@/assets/studio';
import { AddPictureDialog } from '@/blocks/AddPictureDialog';
import { CropDialog } from '@/blocks/CropDialog';
import { PackExportSheet } from '@/blocks/PackExportSheet';
import { PackImportSheet } from '@/blocks/PackImportSheet';
import { PresetPicker } from '@/blocks/PresetPicker';
import { StudioCanvas } from '@/blocks/StudioCanvas';
import { StudioPanel } from '@/blocks/StudioPanel';
import { StudioRail } from '@/blocks/StudioRail';
import { StudioSide } from '@/blocks/StudioSide';
import { StudioTopBar } from '@/blocks/StudioTopBar';
import { UnsavedChangesDialog } from '@/components/Dialog';
import { ToastProvider, useToast } from '@/components/Toast';
import { AppFrame } from '@/components/AppFrame';
import { useStudio, type StudioDialog } from './useStudio';
import styles from './Studio.module.css';

export interface StudioProps {
  /** The cover or folder to customise. */
  design: Design;
  /** The tool open in the left panel. */
  tool?: ToolKey;
  /** Open a dialog or sheet straight away, for stories. */
  dialog?: StudioDialog;
  /** Select a layer by its position in the stack, for stories. */
  selectIndex?: number;
  onClose?: () => void;
}

function StudioInner({ design, tool, dialog, selectIndex, onClose }: StudioProps) {
  const toast = useToast();
  const s = useStudio({ design, tool, dialog, onClose });
  const [back, setBack] = useState<StudioDialog>(null);
  const [selected, setSelected] = useState(false);
  if (!selected && selectIndex !== undefined) { setSelected(true); const l = s.d.layers[selectIndex]; if (l) s.ed.select(l.id); }

  const d = s.d;
  const place = 'container' as const;
  const open = (x: StudioDialog) => s.setDialog(x);
  const frameAspect = d.kind === 'cover' ? DIM.cover.W / DIM.cover.H : DIM.folder.W / (DIM.folder.H - DIM.folder.fy);
  const closeTo = () => { s.setDialog(back); setBack(null); };

  return (
    <AppFrame framed className={styles.root} tabIndex={-1} onKeyDown={s.keys}>
      <div className={styles.grid} data-carrying={s.carrying || undefined}>
        <div className={styles.top}>
          <StudioTopBar
            kind={d.kind} name={d.name} onName={(name) => s.ed.live((x) => { x.name = name; })} onNameCommit={s.ed.commit}
            canUndo={s.ed.canUndo} canRedo={s.ed.canRedo} onClose={s.close} onUndo={s.ed.undo} onRedo={s.ed.redo} onSavePreset={s.savePreset} onDone={s.done}
          />
        </div>
        <div className={styles.rail}><StudioRail tool={s.tool} onTool={s.setTool} /></div>
        <div className={styles.panel}>
          <StudioPanel
            key={s.tool} tool={s.tool} kind={d.kind} iconStyle={s.iconStyle} onIconStyle={s.setIconStyle} bind={s.bind}
            onImport={s.onImport} onGetPacks={() => toast.show('Opens Settings, Packs')}
          />
        </div>
        <StudioCanvas
          className={styles.main} design={d} selectedId={s.ed.selectedId} onSelect={s.ed.select} onLive={s.ed.live} onCommit={s.ed.commit}
          playing={s.playing} onPlaying={s.setPlaying} showNameArea={s.safe} editingId={s.editingId} onEditStart={(id) => { s.ed.select(id); s.setEditingId(id); }}
          onEditInput={s.editText} onEditEnd={s.endEdit} appearId={s.appearId} swapKey={s.swapKey} carryingOver={s.over} onFile={s.handleFile}
          onPresets={() => open('presets')} canvasRef={(el) => { s.canvas.current = el; }}
        />
        <div className={styles.side}>
          <StudioSide
            design={d} selectedId={s.ed.selectedId} onSelect={s.ed.select} layer={s.ed.selected} safe={s.safe} onSafe={s.setSafe}
            onToggleLock={(id) => s.ed.patchLayer(id, { lock: !d.layers.find((l) => l.id === id)?.lock }, true)}
            onToggleHide={(id) => s.ed.patchLayer(id, { hide: !d.layers.find((l) => l.id === id)?.hide }, true)}
            onReorder={s.ed.reorder} onEdit={s.edit} onAction={s.action} onMaterial={s.material}
          />
        </div>
      </div>
      <input ref={s.file} type="file" hidden onChange={(e) => { const f = e.target.files?.[0]; if (f) s.handleFile(f); e.target.value = ''; }} />

      <UnsavedChangesDialog open={s.dialog === 'unsaved'} name={d.name} kind={d.kind} placement={place} onKeepEditing={s.keep} onDiscard={s.discard} onSave={s.save} />
      <PresetPicker
        open={s.dialog === 'presets'} onClose={s.keep} kind={d.kind} yours={s.yours} placement={place} onUse={s.usePreset}
        onImport={() => { setBack('presets'); open('import'); }} onExport={(items) => { s.setExporting(items); setBack('presets'); open('export'); }}
      />
      <PackExportSheet
        open={s.dialog === 'export'} onClose={closeTo} items={s.exporting} placement={place}
        onExport={(file) => { s.keep(); setBack(null); toast.show(`Saved ${file}`); }}
      />
      <PackImportSheet
        open={s.dialog === 'import'} onClose={closeTo} placement={place}
        onInstall={(r) => { setBack(null); toast.show(`Added ${r.count} items`); }}
      />
      <AddPictureDialog
        open={s.dialog === 'picture'} onClose={s.keep} src={s.picture} kind={d.kind} frameAspect={frameAspect} placement={place}
        onAddSticker={s.addSticker} onAddImage={s.addImage}
      />
      <CropDialog
        open={s.dialog === 'crop'} onClose={s.keep} src={s.ed.selected?.src} aspect={s.ed.selected ? s.ed.selected.w / s.ed.selected.h : undefined} crop={s.ed.selected?.crop}
        frameLabel={d.kind === 'cover' ? 'Cover' : 'Front'} frameAspect={frameAspect} placement={place} onApply={s.applyCrop}
      />
    </AppFrame>
  );
}

/** The studio: a full-screen editor for a notebook cover or a folder front. Pick a tool on the left, drag things onto the canvas, and set them on the right. */
export function Studio(props: StudioProps) {
  return (
    <ToastProvider placement="container" offset="above-bar">
      <StudioInner {...props} />
    </ToastProvider>
  );
}
