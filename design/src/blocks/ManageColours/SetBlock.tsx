import { useRef, useState, type ReactNode } from 'react';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { Swatch } from '@/components/Swatch';
import type { ColourSet } from '@/blocks/PenWheel/engine/model';
import styles from './ManageColours.module.css';

export interface SetBlockProps {
  set: ColourSet;
  /** The grip from ReorderList. */
  handle: ReactNode;
  first: boolean;
  /** Index of the chosen colour in this set, if one is chosen. */
  chosen: number | null;
  canDelete: boolean;
  onRename: (name: string) => void;
  onChoose: (i: number | null) => void;
  onAdd: () => void;
  onEdit: () => void;
  onNudge: (by: -1 | 1) => void;
  onDeleteColour: () => void;
  onDeleteSet: () => void;
}

/** One colour set as plain content: a name that edits in place, the count, and a fixed grid of swatches that ends in the colour wheel. */
export function SetBlock({ set, handle, first, chosen, canDelete, onRename, onChoose, onAdd, onEdit, onNudge, onDeleteColour, onDeleteSet }: SetBlockProps) {
  const [menu, setMenu] = useState(false);
  const more = useRef<HTMLSpanElement>(null);
  const name = useRef<HTMLInputElement>(null);
  const n = set.colours.length;
  return (
    <section className={styles.set} aria-label={set.name}>
      <header className={styles.head}>
        {handle}
        <input
          ref={name}
          className={styles.name}
          aria-label="Set name"
          maxLength={24}
          value={set.name}
          onChange={(e) => onRename(e.target.value)}
          onBlur={(e) => { if (!e.target.value.trim()) onRename('Set'); }}
          onKeyDown={(e) => { if (e.key === 'Enter') e.currentTarget.blur(); }}
        />
        <span className={styles.count} aria-label={`${n} ${n === 1 ? 'colour' : 'colours'}`}>{n}</span>
        {first && <span className={styles.tag}>First on the ring</span>}
        <span ref={more} className={styles.more}>
          <IconButton icon="more" label={`${set.name} options`} size="sm" variant="ghost" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)} />
        </span>
        <Popover open={menu} onClose={() => setMenu(false)} anchorRef={more} placement="bottom-end" kind="menu">
          <Menu label={`${set.name} options`} onClose={() => setMenu(false)}>
            <MenuItem icon="rename" onSelect={() => name.current?.focus()}>Rename</MenuItem>
            {canDelete && <MenuItem icon="trash" tone="danger" onSelect={onDeleteSet}>Delete set</MenuItem>}
          </Menu>
        </Popover>
      </header>
      <div className={styles.grid} role="radiogroup" aria-label={`${set.name} colours`}>
        {set.colours.map((c, ci) => (
          <span key={ci} className={styles.cell}>
            <Swatch color={c} label={`Colour ${c}`} size="lg" selected={chosen === ci} onClick={() => onChoose(chosen === ci ? null : ci)} />
          </span>
        ))}
        <span className={styles.cell}>
          <Swatch color="var(--wh-rainbow)" label="Add colour" size="lg" role="button" aria-checked={undefined} onClick={onAdd} />
        </span>
      </div>
      {chosen != null && (
        <div className={styles.actions}>
          <Button size="xs" variant="text" icon="edit" onClick={onEdit}>Edit</Button>
          <IconButton icon="arrow_left" label="Move earlier" size="sm" variant="ghost" disabled={chosen === 0} onClick={() => onNudge(-1)} />
          <IconButton icon="arrow_right" label="Move later" size="sm" variant="ghost" disabled={chosen === n - 1} onClick={() => onNudge(1)} />
          <IconButton icon="trash" label="Delete colour" size="sm" variant="ghost" onClick={onDeleteColour} />
        </div>
      )}
    </section>
  );
}
