import { useRef, useState, type KeyboardEvent, type RefObject } from 'react';
import { Icon } from '@/assets/icons';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem, MenuSeparator } from '@/components/Menu';
import { Panel, type PanelProps } from '@/components/Panel';
import { Popover } from '@/components/Popover';
import type { Placement } from '@/lib/anchor';
import { ReorderList } from '@/components/ReorderList';
import { Slider } from '@/components/Slider';
import { nextLayerName, type NoteLayer } from './layers';
import styles from './LayersPanel.module.css';

interface LayersBodyProps {
  /** Top first, as they stack on the page. */
  layers: NoteLayer[];
  activeId: string;
  onChange: (layers: NoteLayer[]) => void;
  onActiveChange: (id: string) => void;
  /** Where the menus open: inside a framed screen, or over the window. */
  boundary?: 'viewport' | 'container';
}

export interface LayersPanelProps extends LayersBodyProps, Omit<PanelProps, 'title' | 'children' | 'onChange' | 'icon'> {}

export interface LayersPopoverProps extends LayersBodyProps {
  open: boolean;
  onClose: () => void;
  /** The bar's Layers button. */
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
}

/** Layers as they open from the note bar: a popover under the Layers button, so the page keeps its full width. */
export function LayersPopover({ open, onClose, anchorRef, placement = 'bottom-end', boundary = 'container', ...body }: LayersPopoverProps) {
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} boundary={boundary} kind="content" className={styles.pop} aria-label="Layers">
      <h3 className={styles.title}>Layers</h3>
      <LayersBody {...body} boundary={boundary} />
    </Popover>
  );
}

/**
 * Layers of a note. Drag the grip to reorder. The eye hides, the padlock locks, the dots hold Rename, Opacity, Merge down and Delete.
 * Add layer is a quiet action row under the list, not a tile.
 */
export function LayersPanel({ layers, activeId, onChange, onActiveChange, boundary = 'container', ...panel }: LayersPanelProps) {
  return (
    <Panel title="Layers" icon="layers" {...panel}>
      <LayersBody layers={layers} activeId={activeId} onChange={onChange} onActiveChange={onActiveChange} boundary={boundary} />
    </Panel>
  );
}

function LayersBody({ layers, activeId, onChange, onActiveChange, boundary = 'container' }: LayersBodyProps) {
  const [renaming, setRenaming] = useState<string | null>(null);
  const [menu, setMenu] = useState<string | null>(null);
  const anchor = useRef<HTMLElement | null>(null);

  const patch = (id: string, p: Partial<NoteLayer>) => onChange(layers.map((l) => (l.id === id ? { ...l, ...p } : l)));
  const add = () => {
    const l: NoteLayer = { id: `l${Date.now()}`, name: nextLayerName(layers), visible: true, locked: false, opacity: 1 };
    onChange([l, ...layers]);
    onActiveChange(l.id);
  };
  const remove = (id: string) => {
    const next = layers.filter((l) => l.id !== id);
    onChange(next);
    if (id === activeId && next[0]) onActiveChange(next[0].id);
  };
  const mergeDown = (id: string) => {
    const i = layers.findIndex((l) => l.id === id);
    if (i < 0 || i === layers.length - 1) return;
    const next = layers.filter((l) => l.id !== id);
    onChange(next);
    if (id === activeId) onActiveChange(layers[i + 1].id);
  };
  const open = layers.find((l) => l.id === menu);
  const openIndex = open ? layers.indexOf(open) : -1;
  const keys = (id: string) => (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') { patch(id, { name: e.currentTarget.value.trim() || layers.find((l) => l.id === id)?.name || '' }); setRenaming(null); }
    if (e.key === 'Escape') setRenaming(null);
  };

  return (
    <>
      <ReorderList
        items={layers}
        onChange={onChange}
        getId={(l) => l.id}
        getLabel={(l) => l.name}
        renderItem={(l, { handle }) => (
          <div className={styles.row} data-active={l.id === activeId || undefined} data-hidden={!l.visible || undefined}>
            {handle}
            {renaming === l.id ? (
              <input
                className={styles.rename}
                aria-label="Layer name"
                autoFocus
                defaultValue={l.name}
                onFocus={(e) => e.currentTarget.select()}
                onBlur={(e) => { patch(l.id, { name: e.currentTarget.value.trim() || l.name }); setRenaming(null); }}
                onKeyDown={keys(l.id)}
              />
            ) : (
              <button type="button" className={styles.name} aria-pressed={l.id === activeId} onClick={() => onActiveChange(l.id)} onDoubleClick={() => setRenaming(l.id)}>
                {l.name}
                {l.opacity < 1 && <small>{Math.round(l.opacity * 100)}%</small>}
              </button>
            )}
            <IconButton variant="ghost" size="sm" iconSize={18} icon={l.visible ? 'eye' : 'eye_off'} label={l.visible ? 'Hide layer' : 'Show layer'} className={styles.state} data-off={!l.visible || undefined} onClick={() => patch(l.id, { visible: !l.visible })} />
            <IconButton variant="ghost" size="sm" iconSize={18} icon={l.locked ? 'lock' : 'unlock'} label={l.locked ? 'Unlock layer' : 'Lock layer'} className={styles.state} data-off={!l.locked || undefined} onClick={() => patch(l.id, { locked: !l.locked })} />
            <IconButton variant="ghost" size="sm" iconSize={18} icon="more" label="Layer options" className={styles.more} onClick={(e) => { anchor.current = e.currentTarget; setMenu(l.id); }} />
          </div>
        )}
      />
      <button type="button" className={styles.add} onClick={add}>
        <Icon name="plus" size={18} />
        Add layer
      </button>
      <Popover open={!!open} onClose={() => setMenu(null)} anchorRef={anchor} placement="bottom-end" boundary={boundary}>
        {open && (
          <Menu onClose={() => setMenu(null)} label="Layer options">
            <MenuItem icon="rename" onSelect={() => setRenaming(open.id)}>Rename</MenuItem>
            <MenuItem
              icon="droplet"
              value={`${Math.round(open.opacity * 100)}%`}
              submenu={
                <div className={styles.opacity}>
                  <Slider aria-label="Opacity" min={10} max={100} unit="%" bubble value={Math.round(open.opacity * 100)} onChange={(v) => patch(open.id, { opacity: v / 100 })} />
                </div>
              }
            >
              Opacity
            </MenuItem>
            <MenuItem icon="merge" disabled={openIndex === layers.length - 1} onSelect={() => mergeDown(open.id)}>Merge down</MenuItem>
            <MenuSeparator />
            <MenuItem icon="trash" tone="danger" disabled={layers.length < 2} onSelect={() => remove(open.id)}>Delete</MenuItem>
          </Menu>
        )}
      </Popover>
    </>
  );
}
