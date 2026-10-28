import { useRef, type PointerEvent } from 'react';
import { Icon } from '@/assets/icons';
import { clamp, layerLabel, layerThumb, type Design, type Layer } from '@/assets/studio';
import { cx } from '@/lib/cx';
import { TileArtView } from '../StickerTile';
import styles from './LayerList.module.css';

export interface LayerListProps {
  design: Design;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  onToggleLock: (id: string) => void;
  onToggleHide: (id: string) => void;
  /** The new order of one group's layers, top first. */
  onReorder: (idsTopFirst: string[]) => void;
  className?: string;
}

const FOLDER_GROUPS: Array<[string, string]> = [['above', 'Above the front'], ['front', 'On the front'], ['behind', 'Behind the front']];

const inGroup = (d: Design, l: Layer, g: string) =>
  d.kind !== 'folder' ? true : g === 'above' ? !!l.pop && l.pop !== 'behind' : g === 'behind' ? l.pop === 'behind' : !l.pop;

interface Drag { row: HTMLElement; rows: HTMLElement[]; si: number; sy: number; rh: number; t: number }

/** Everything on the cover or folder, top first. Drag the grip to reorder, tap a row to select it. The last row opens the cover or folder settings. */
export function LayerList({ design: d, selectedId, onSelect, onToggleLock, onToggleHide, onReorder, className }: LayerListProps) {
  const drag = useRef<Drag | null>(null);
  const groups = d.kind === 'folder' ? FOLDER_GROUPS : ([['front', '']] as Array<[string, string]>);

  const start = (e: PointerEvent<HTMLButtonElement>) => {
    e.preventDefault();
    const row = e.currentTarget.closest('li') as HTMLElement;
    const rows = [...(row.parentElement?.children ?? [])] as HTMLElement[];
    drag.current = { row, rows, si: rows.indexOf(row), sy: e.clientY, rh: row.offsetHeight, t: rows.indexOf(row) };
    row.dataset.lift = 'true';
    e.currentTarget.setPointerCapture(e.pointerId);
  };
  const move = (e: PointerEvent<HTMLButtonElement>) => {
    const g = drag.current;
    if (!g) return;
    const scale = g.row.getBoundingClientRect().height / g.rh || 1;
    const dy = (e.clientY - g.sy) / scale;
    const t = clamp(g.si + Math.round(dy / g.rh), 0, g.rows.length - 1);
    g.t = t;
    g.row.style.transform = `translateY(${dy}px) scale(1.02)`;
    g.rows.forEach((r, j) => {
      if (r === g.row) return;
      const sh = g.si < t && j > g.si && j <= t ? -g.rh : g.si > t && j < g.si && j >= t ? g.rh : 0;
      r.style.transform = sh ? `translateY(${sh}px)` : '';
    });
  };
  const end = () => {
    const g = drag.current;
    if (!g) return;
    drag.current = null;
    g.rows.forEach((r) => { r.style.transform = ''; });
    delete g.row.dataset.lift;
    if (g.t !== g.si) {
      const ids = g.rows.map((r) => r.dataset.id || '');
      const [m] = ids.splice(g.si, 1);
      ids.splice(g.t, 0, m);
      onReorder(ids);
    }
  };

  return (
    <div className={cx(styles.layers, className)}>
      <div className={styles.head}><b>Layers</b></div>
      <div className={styles.scroll}>
        {groups.map(([g, title]) => {
          const list = d.layers.filter((l) => inGroup(d, l, g)).reverse();
          return (
            <div key={g}>
              {d.kind === 'folder' && (list.length > 0 || g === 'front') && <div className={styles.group}>{title}</div>}
              <ul className={styles.rows}>
                {list.map((l) => {
                  const label = layerLabel(l, d);
                  return (
                    <li key={l.id} data-id={l.id} className={styles.row} data-selected={l.id === selectedId || undefined} data-hidden={l.hide || undefined}>
                      <button type="button" className={styles.grip} aria-label={`Reorder ${label}`} title="Drag to reorder" onPointerDown={start} onPointerMove={move} onPointerUp={end} onPointerCancel={end}>
                        <Icon name="grip" size={16} />
                      </button>
                      <button type="button" className={styles.main} aria-pressed={l.id === selectedId} onClick={() => onSelect(l.id)}>
                        <span className={styles.thumb}><TileArtView art={layerThumb(l)} fit={72} iconPx={16} /></span>
                        <span className={styles.name}>{label}</span>
                      </button>
                      <button type="button" className={styles.btn} data-on={l.lock || undefined} aria-label={l.lock ? 'Unlock' : 'Lock'} title={l.lock ? 'Unlock' : 'Lock'} onClick={() => onToggleLock(l.id)}>
                        {l.lock && <Icon name="lock" size={16} />}
                      </button>
                      <button type="button" className={styles.btn} aria-label={l.hide ? 'Show' : 'Hide'} title={l.hide ? 'Show' : 'Hide'} onClick={() => onToggleHide(l.id)}>
                        <Icon name={l.hide ? 'eye_off' : 'eye'} size={16} />
                      </button>
                    </li>
                  );
                })}
              </ul>
            </div>
          );
        })}
      </div>
      <button type="button" className={styles.base} aria-pressed={selectedId === null} onClick={() => onSelect(null)}>
        <Icon name={d.kind === 'cover' ? 'notebook' : 'folder'} size={16} />
        <span>{d.kind === 'cover' ? 'Cover' : 'Folder'} settings</span>
      </button>
    </div>
  );
}
