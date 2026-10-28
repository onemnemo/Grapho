import type { Design, Layer, TextureKey } from '@/assets/studio';
import { Inspector, type InspectorAction } from '../Inspector';
import { LayerList } from '../LayerList';
import styles from './StudioSide.module.css';

export interface StudioSideProps {
  design: Design;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  onToggleLock: (id: string) => void;
  onToggleHide: (id: string) => void;
  onReorder: (idsTopFirst: string[]) => void;
  /** The selected layer, or null for the cover or folder settings. */
  layer: Layer | null;
  onEdit: (path: string, value: unknown, final: boolean) => void;
  onAction: (a: InspectorAction) => void;
  onMaterial?: (key: TextureKey) => void;
  safe: boolean;
  onSafe: (on: boolean) => void;
}

/** The right column: the layer list above, the inspector for whatever is selected below. */
export function StudioSide({ design, selectedId, onSelect, onToggleLock, onToggleHide, onReorder, layer, onEdit, onAction, onMaterial, safe, onSafe }: StudioSideProps) {
  return (
    <aside className={styles.side} aria-label="Layers and settings">
      <LayerList design={design} selectedId={selectedId} onSelect={onSelect} onToggleLock={onToggleLock} onToggleHide={onToggleHide} onReorder={onReorder} />
      <Inspector design={design} layer={layer} onEdit={onEdit} onAction={onAction} onMaterial={onMaterial} safe={safe} onSafe={onSafe} />
    </aside>
  );
}
