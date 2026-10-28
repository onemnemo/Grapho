import { useCallback, useEffect, useRef, useState, type DragEvent, type Ref } from 'react';
import { Icon } from '@/assets/icons';
import { DesignArt, DIM, type Design, type Layer } from '@/assets/studio';
import { Button } from '@/components/Button';
import { useCanvasGestures } from '@/lib/studio';
import { cx } from '@/lib/cx';
import { SelectionBox } from './SelectionBox';
import styles from './StudioCanvas.module.css';

export interface StudioCanvasProps {
  design: Design;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  /** Change the design while a gesture is going on. */
  onLive: (fn: (d: Design) => void) => void;
  /** The gesture is over: one undo step. */
  onCommit: () => void;
  playing?: boolean;
  onPlaying?: (on: boolean) => void;
  /** Draw the dashed "Name area" on a folder whose name sits left or in the centre. */
  showNameArea?: boolean;
  editingId?: string | null;
  onEditStart?: (id: string) => void;
  onEditInput?: (text: string) => void;
  onEditEnd?: () => void;
  /** The layer that was just added: it pops in. */
  appearId?: string | null;
  /** Changes when a preset is applied, so the canvas plays its swap animation. */
  swapKey?: number;
  /** A tile from the panel is being carried over the canvas. */
  carryingOver?: boolean;
  /** A picture or a video file was dropped on the stage. */
  onFile?: (file: File) => void;
  onPresets?: () => void;
  /** The box the art is in, for working out where a dropped tile lands. */
  canvasRef?: Ref<HTMLDivElement>;
  className?: string;
}

const hasFiles = (e: DragEvent) => [...(e.dataTransfer?.types ?? [])].includes('Files');

/** The stage: the cover or folder at the size that fits, with its selection box, guides and handles, a dock with Presets and library, and a drop veil for files. */
export function StudioCanvas({
  design, selectedId, onSelect, onLive, onCommit, playing = false, onPlaying, showNameArea = true, editingId = null, onEditStart, onEditInput, onEditEnd,
  appearId = null, swapKey = 0, carryingOver = false, onFile, onPresets, canvasRef, className,
}: StudioCanvasProps) {
  const stageRef = useRef<HTMLDivElement>(null);
  const boxRef = useRef<HTMLDivElement | null>(null);
  const [K, setK] = useState(1);
  const [heights, setHeights] = useState<Record<string, number>>({});
  const [fileOver, setFileOver] = useState(false);
  const { W, H } = DIM[design.kind];

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const fit = () => setK(Math.max(0.1, Math.min((el.clientWidth - 96) / W, (el.clientHeight - 84) / H)));
    fit();
    const ro = new ResizeObserver(fit);
    ro.observe(el);
    return () => ro.disconnect();
  }, [W, H]);

  const heightOf = useCallback((l: Layer) => (l.type === 'text' ? heights[l.id] ?? l.h : l.h), [heights]);
  const selected = design.layers.find((l) => l.id === selectedId) ?? null;
  const { view, bind } = useCanvasGestures({
    design, selected, K, hostRef: boxRef, heightOf, onSelect, onLive, onCommit, editing: !!editingId, onEndEdit: () => onEditEnd?.(),
  });

  const setBox = (el: HTMLDivElement | null) => {
    boxRef.current = el;
    if (typeof canvasRef === 'function') canvasRef(el);
    else if (canvasRef) (canvasRef as { current: HTMLDivElement | null }).current = el;
  };

  const f = design.folder;
  const nameArea = showNameArea && design.kind === 'folder' && !!f && (f.name === 'left' || f.name === 'centre');
  const editingLayer = design.layers.find((l) => l.id === editingId);

  return (
    <div className={cx(styles.main, className)}>
      <div
        ref={stageRef} className={styles.stage} data-file={fileOver || undefined}
        onPointerDown={(e) => { if (e.target === e.currentTarget) onSelect(null); }}
        onDragEnter={(e) => { if (hasFiles(e)) { e.preventDefault(); setFileOver(true); } }}
        onDragOver={(e) => { if (hasFiles(e)) { e.preventDefault(); setFileOver(true); } }}
        onDragLeave={(e) => { if (e.target === e.currentTarget) setFileOver(false); }}
        onDrop={(e) => { e.preventDefault(); setFileOver(false); const file = e.dataTransfer.files[0]; if (file) onFile?.(file); }}
      >
        <div
          ref={setBox} className={styles.canvas} data-over={carryingOver || undefined} style={{ width: W * K, height: H * K }} {...bind}
          onDoubleClick={(e) => {
            const el = (e.target as HTMLElement).closest('[data-layer-id]') as HTMLElement | null;
            const l = el && design.layers.find((x) => x.id === el.dataset.layerId);
            if (l && !l.lock && (l.type === 'text' || l.type === 'card')) onEditStart?.(l.id);
          }}
        >
          <div key={swapKey} className={cx(styles.host, swapKey > 0 && styles.swap)}>
            <DesignArt
              design={design} shadow={false} playing={playing} appearId={appearId} onMeasure={setHeights}
              editingId={editingId} editingStart={editingLayer?.text ?? ''} onEditInput={onEditInput} onEditEnd={onEditEnd}
            />
          </div>
          {nameArea && <i className={styles.safe} data-centre={f?.name === 'centre' || undefined}><span>Name area</span></i>}
          <SelectionBox design={design} selected={selected} K={K} heightOf={heightOf} view={view} playing={playing} onTogglePlay={() => onPlaying?.(!playing)} />
        </div>
        <div className={styles.veil}><b><Icon name="upload" size={20} /></b>Drop to add</div>
      </div>
      <div className={styles.dock}>
        <Button onClick={onPresets}><Icon name="bookmark" size={18} />Presets and library</Button>
      </div>
    </div>
  );
}
