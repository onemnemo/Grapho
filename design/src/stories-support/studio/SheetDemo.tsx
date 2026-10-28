import { useRef, useState, type ReactNode } from 'react';
import { Button } from '@/components/Button';
import { ToastProvider, useToast } from '@/components/Toast';
import { LibraryBackdrop, type BackdropItem } from './LibraryBackdrop';
import styles from './SheetDemo.module.css';

export interface DemoApi {
  open: boolean;
  onClose: () => void;
  /** A new item was made: it joins the library, and Undo takes it back. */
  created: (item: Omit<BackdropItem, 'id'>) => void;
  /** The edited item was saved. */
  saved: (item: Omit<BackdropItem, 'id'>) => void;
  /** The edited item was deleted, with Undo. */
  removed: () => void;
}

export interface SheetDemoProps {
  kind: 'folder' | 'cover' | 'canvas';
  mode: 'new' | 'edit';
  items: Array<Omit<BackdropItem, 'id'>>;
  /** Show the sheet at the start. */
  startOpen?: boolean;
  sheet: (api: DemoApi) => ReactNode;
}

const MAX = { folder: 3, cover: 4, canvas: 3 };
let n = 0;

function Inner({ kind, mode, items: first, startOpen = true, sheet }: SheetDemoProps) {
  const toast = useToast();
  const [open, setOpen] = useState(startOpen);
  const [items, setItems] = useState<BackdropItem[]>(() => first.map((i) => ({ ...i, id: `i${++n}` })));
  const prev = useRef<BackdropItem[]>(items);

  const api: DemoApi = {
    open,
    onClose: () => setOpen(false),
    created: (item) => {
      const before = items;
      const made: BackdropItem = { ...item, id: `i${++n}`, fresh: true };
      setItems([made, ...before].slice(0, MAX[kind]));
      setOpen(false);
      toast.show(`${item.name} created`, { undo: () => { setItems(before); setOpen(true); } });
    },
    saved: (item) => {
      setItems((l) => l.map((x, i) => (i === 0 ? { ...x, ...item } : x)));
      setOpen(false);
      toast.show(`${item.name} saved`);
    },
    removed: () => {
      prev.current = items;
      setItems((l) => l.map((x, i) => (i === 0 ? { ...x, hidden: true } : x)));
      setOpen(false);
      toast.deleted(items[0]?.name ?? 'Item', () => { setItems(prev.current); setOpen(true); });
    },
  };

  return (
    <>
      <LibraryBackdrop kind={kind} items={items} editing={mode === 'edit'} onOpen={() => setOpen(true)} />
      {sheet(api)}
      {!open && <div className={styles.reopen}><Button size="sm" onClick={() => setOpen(true)}>Open the sheet again</Button></div>}
    </>
  );
}

/** Story harness: a library with a sheet open over it, where creating, saving and deleting really change the library and show their toast. */
export function SheetDemo(props: SheetDemoProps) {
  return (
    <div className={styles.stage}>
      <ToastProvider placement="container">
        <Inner {...props} />
      </ToastProvider>
    </div>
  );
}
