import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import styles from './StudioTopBar.module.css';

export interface StudioTopBarProps {
  kind: 'cover' | 'folder';
  name: string;
  onName: (name: string) => void;
  /** The name is done being typed: one undo step. */
  onNameCommit?: () => void;
  canUndo: boolean;
  canRedo: boolean;
  onClose: () => void;
  onUndo: () => void;
  onRedo: () => void;
  onSavePreset: () => void;
  onDone: () => void;
}

/** Close, what is being customised and its name, undo and redo, Save as preset, Done. */
export function StudioTopBar({ kind, name, onName, onNameCommit, canUndo, canRedo, onClose, onUndo, onRedo, onSavePreset, onDone }: StudioTopBarProps) {
  return (
    <header className={styles.bar}>
      <IconButton icon="close" label="Close" onClick={onClose} />
      <div className={styles.title}>
        <span className={styles.kind}>{kind === 'cover' ? 'Customise cover' : 'Customise folder'}</span>
        <input className={styles.name} aria-label="Name" spellCheck={false} value={name} onChange={(e) => onName(e.target.value)} onBlur={onNameCommit} onKeyDown={(e) => { if (e.key === 'Enter') e.currentTarget.blur(); }} />
      </div>
      <div className={styles.right}>
        <IconButton icon="undo" label="Undo" disabled={!canUndo} onClick={onUndo} />
        <IconButton icon="redo" label="Redo" disabled={!canRedo} onClick={onRedo} />
        <i className={styles.sep} />
        <Button onClick={onSavePreset}>Save as preset</Button>
        <Button variant="primary" onClick={onDone}>Done</Button>
      </div>
    </header>
  );
}
