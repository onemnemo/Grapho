import { useRef, useState, type HTMLAttributes } from 'react';
import type { IconName } from '@/assets/icons';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { RollingNumber } from '@/components/RollingNumber';
import { cx } from '@/lib/cx';
import { MoveMenu, type FolderTarget } from '../FolderMenu';
import styles from './SelectionBar.module.css';

export interface SelectionBarProps extends HTMLAttributes<HTMLDivElement> {
  /** Slides up when true. */
  visible?: boolean;
  /** How many are picked. At zero the actions are muted. */
  count: number;
  /** Show "4 selected" in the bar. The library header already shows it, so this is off there. */
  showCount?: boolean;
  /** absolute pins to the nearest positioned parent; fixed pins to the window. */
  position?: 'absolute' | 'fixed';
  /** Folders to move into. When given, Move opens a list of them above the bar. */
  moveTargets?: FolderTarget[];
  onMoveTo?: (folderId: string) => void;
  onMove?: () => void;
  onShare?: () => void;
  onCopy?: () => void;
  onTag?: () => void;
  onDelete?: () => void;
}

const ICONS: Array<{ key: 'onShare' | 'onCopy' | 'onTag'; icon: IconName; label: string }> = [
  { key: 'onShare', icon: 'share', label: 'Share' },
  { key: 'onCopy', icon: 'copy', label: 'Copy' },
  { key: 'onTag', icon: 'tag', label: 'Tag' },
];

/** Move is the one solid action, Share, Copy and Tag are icons, Delete is red and gives an Undo. */
export function SelectionBar({ visible = true, count, showCount = false, position = 'absolute', className, moveTargets, onMoveTo, onMove, onShare, onCopy, onTag, onDelete, ...rest }: SelectionBarProps) {
  const handlers = { onShare, onCopy, onTag };
  const [moveOpen, setMoveOpen] = useState(false);
  const moveRef = useRef<HTMLButtonElement>(null);
  return (
    <div className={cx(styles.wrap, position === 'fixed' && styles.fixed)}>
      <div className={cx(styles.bar, count === 0 && styles.idle, className)} data-visible={visible} role="toolbar" aria-label="Selection actions" {...rest}>
        {showCount && (
          <span className={styles.count}>
            <b><RollingNumber value={count} /></b> selected
          </span>
        )}
        <Button
          ref={moveRef}
          variant="primary"
          size="bar"
          icon="folder_move"
          className={styles.main}
          aria-haspopup={moveTargets ? 'menu' : undefined}
          aria-expanded={moveTargets ? moveOpen : undefined}
          onClick={() => (moveTargets ? setMoveOpen((v) => !v) : onMove?.())}
          tabIndex={visible ? 0 : -1}
        >
          Move
        </Button>
        {ICONS.map((i) => (
          <IconButton key={i.key} variant="ghost" size="bar" icon={i.icon} label={i.label} title={i.label} className={styles.icon} onClick={handlers[i.key]} tabIndex={visible ? 0 : -1} />
        ))}
        <span className={styles.sep} />
        <Button variant="danger" size="bar" icon="trash" onClick={onDelete} tabIndex={visible ? 0 : -1}>Delete</Button>
      </div>
      {moveTargets && (
        <MoveMenu open={moveOpen} onClose={() => setMoveOpen(false)} anchorRef={moveRef} targets={moveTargets} onMove={(id) => { onMoveTo?.(id); setMoveOpen(false); }} />
      )}
    </div>
  );
}
