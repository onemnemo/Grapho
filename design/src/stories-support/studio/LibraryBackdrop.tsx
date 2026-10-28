import { useRef } from 'react';
import { Icon } from '@/assets/icons';
import { DesignArt, type CanvasSettings, type Design } from '@/assets/studio';
import { CanvasPreview } from '@/assets/covers/CanvasPreview';
import { Button } from '@/components/Button';
import { cx } from '@/lib/cx';
import styles from './LibraryBackdrop.module.css';

export interface BackdropItem {
  id: string;
  name: string;
  meta: string;
  design?: Design;
  canvas?: CanvasSettings;
  hidden?: boolean;
  /** Just created: it pops in. */
  fresh?: boolean;
}

export interface LibraryBackdropProps {
  kind: 'folder' | 'cover' | 'canvas';
  items: BackdropItem[];
  /** The first item can be edited: the three dots show, and a long press opens it. */
  editing?: boolean;
  onOpen: () => void;
  className?: string;
}

const NEW_LABEL = { folder: 'New folder', cover: 'New notebook', canvas: 'New canvas' };
const NOUN = { folder: 'a folder', cover: 'a cover', canvas: 'a canvas' };

/** A quiet library behind a sheet, so the sheet is seen in place. The first item can be edited. */
export function LibraryBackdrop({ kind, items, editing = false, onOpen, className }: LibraryBackdropProps) {
  const lp = useRef<number>(undefined);
  return (
    <div className={cx(styles.back, className)}>
      <div className={styles.head}>
        <h4>Library</h4>
        <Button variant="primary" size="sm" icon={editing ? 'more' : 'plus'} onClick={onOpen}>{editing ? 'Edit' : NEW_LABEL[kind]}</Button>
      </div>
      <div className={styles.row} data-kind={kind}>
        {items.filter((i) => !i.hidden).map((it, i) => {
          const edit = editing && i === 0;
          return (
            <div
              key={it.id} className={cx(styles.item, it.fresh && styles.fresh)} data-edit={edit || undefined}
              onPointerDown={edit ? (e) => { if ((e.target as HTMLElement).closest('button')) return; lp.current = window.setTimeout(onOpen, 520); } : undefined}
              onPointerUp={edit ? () => window.clearTimeout(lp.current) : undefined}
              onPointerLeave={edit ? () => window.clearTimeout(lp.current) : undefined}
            >
              <div className={styles.art} data-kind={kind}>
                {it.design && <div className={it.design.kind === 'folder' ? styles.fo : styles.cv}><DesignArt design={it.design} /></div>}
                {it.canvas && <CanvasPreview settings={it.canvas} width={150} height={it.canvas.size === 'fixed' ? 106 : 105} />}
              </div>
              {edit && <button type="button" className={styles.more} aria-label={`More, edit ${it.name}`} onClick={onOpen}><Icon name="more" size={18} /></button>}
              <div className={styles.name}>{it.name}</div>
              <div className={styles.meta}>{it.meta}</div>
            </div>
          );
        })}
      </div>
      {editing && <p className={styles.hint}>Long press {NOUN[kind]}, or tap the three dots, to edit it.</p>}
    </div>
  );
}
