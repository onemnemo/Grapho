import { useRef, useState } from 'react';
import { Icon } from '@/assets/icons';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import type { ColourSet } from '@/blocks/PenWheel/engine/model';
import styles from './ColourPicker.module.css';

export interface AddToSetProps {
  sets: ColourSet[];
  /** Index of the set the colour goes to. */
  target: number;
  onTarget: (i: number) => void;
  /** The colour was just added: the label confirms it for a moment. */
  added: boolean;
  onAdd: () => void;
}

/**
 * One quiet action: "Add to Basic" with the colour wheel as its icon. The chevron beside it picks another set
 * from a menu, so ten sets cost the same room as two.
 */
export function AddToSet({ sets, target, onTarget, added, onAdd }: AddToSetProps) {
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLDivElement>(null);
  const name = sets[target]?.name ?? '';
  return (
    <div ref={anchor} className={styles.split}>
      <button type="button" className={styles.splitMain} onClick={onAdd}>
        {added ? <Icon name="check" size={16} /> : <i className={styles.wheelDot} aria-hidden="true" />}
        <span>{added ? 'Added' : `Add to ${name}`}</span>
      </button>
      {sets.length > 1 && (
        <>
          <button type="button" className={styles.splitMore} aria-label="Choose set" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen(!open)}>
            <Icon name="chevron_down" size={16} />
          </button>
          <Popover open={open} onClose={() => setOpen(false)} anchorRef={anchor} placement="top-start" offset={6}>
            <Menu label="Add to set" onClose={() => setOpen(false)}>
              {sets.map((s, i) => (
                <MenuItem key={s.id} checked={i === target} value={s.colours.length} onSelect={() => onTarget(i)}>{s.name}</MenuItem>
              ))}
            </Menu>
          </Popover>
        </>
      )}
    </div>
  );
}
