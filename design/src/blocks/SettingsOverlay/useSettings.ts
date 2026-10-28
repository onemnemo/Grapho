import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { buildSampleContent, sampleSources, type ContentItem, type ContentSource } from '@/assets/content';
import { packArt } from '@/assets/packs';
import { useToast } from '@/components/Toast';
import { applyTheme, DEFAULT_THEME, type ThemeChoice } from '@/lib/theme';
import type { PackItem, PackRowAction } from '../PackRow';
import type { ImportSummary } from '../PackImportSheet';
import type { CataloguePack, DiscoverKind } from '../PackBrowseSheet';
import { sampleExtensions, type ExtensionItem } from '../ExtensionsPage';
import { defaultSettings, type SettingsValues, type SyncStatus } from './settingsTypes';

const initialPacks: PackItem[] = sampleSources.filter((s) => s.kind === 'pack').map((s) => ({ id: s.id, name: s.name, art: s.art as PackItem['art'], enabled: s.enabled }));

/**
 * State for Settings and the flows around it: the values, content, packs and extensions, and the sheets and questions they open.
 * Needs a ToastProvider above it for the notices and the Undo after a removal.
 */
export function useSettings(initial: Partial<SettingsValues> = {}) {
  const toast = useToast();
  const [values, setValues] = useState<SettingsValues>({ ...defaultSettings, ...initial });
  const [theme, setTheme] = useState<ThemeChoice>(() => ({ ...DEFAULT_THEME, id: (document.documentElement.dataset.theme as ThemeChoice['id']) || DEFAULT_THEME.id }));
  const [packs, setPacks] = useState<PackItem[]>(initialPacks);
  const [content, setContent] = useState<ContentItem[]>(buildSampleContent);
  const [extensions, setExtensions] = useState<ExtensionItem[]>(sampleExtensions);
  const [removing, setRemoving] = useState<PackItem | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [browseOpen, setBrowseOpen] = useState(false);
  const [browseKind, setBrowseKind] = useState<DiscoverKind | 'all'>('all');
  const [fromCatalogue, setFromCatalogue] = useState<string[]>([]);
  const [newPack, setNewPack] = useState<{ ids: string[] } | null>(null);
  const [sync, setSync] = useState<{ status: SyncStatus; last: string }>({ status: 'ok', last: '2 minutes ago' });
  const timer = useRef<number>(undefined);
  // the theme can also change from outside (the host app, Storybook's toolbar): follow it
  useEffect(() => {
    const root = document.documentElement;
    const sync = () => { const id = root.dataset.theme as ThemeChoice['id'] | undefined; if (id) setTheme((c) => (c.id === id ? c : { ...c, id })); };
    sync();
    const watch = new MutationObserver(sync);
    watch.observe(root, { attributes: true, attributeFilter: ['data-theme'] });
    return () => watch.disconnect();
  }, []);
  useEffect(() => () => window.clearTimeout(timer.current), []);

  /** Choosing a theme repaints the whole document, as the app does. */
  const chooseTheme = useCallback((choice: ThemeChoice) => { setTheme(choice); applyTheme(choice); }, []);

  const change = useCallback(
    (patch: Partial<SettingsValues>) => {
      setValues((v) => ({ ...v, ...patch }));
      if (patch.batterySaver !== undefined) toast.show(patch.batterySaver ? 'Battery saver on' : 'Battery saver off', { kind: 'ok', duration: 1500 });
    },
    [toast],
  );

  /** Built in, your own, then the packs in their order. A pack that is off stays listed but its content leaves the pickers. */
  const sources = useMemo<ContentSource[]>(
    () => [...sampleSources.filter((s) => s.kind !== 'pack'), ...packs.map((p) => ({ id: p.id, name: p.name, kind: 'pack' as const, art: p.art, enabled: p.enabled }))],
    [packs],
  );
  const off = useMemo(() => new Set(packs.filter((p) => !p.enabled).map((p) => p.id)), [packs]);
  const visibleContent = useMemo(() => content.filter((i) => !off.has(i.source)), [content, off]);

  const togglePack = (id: string, enabled: boolean) => {
    const pack = packs.find((p) => p.id === id);
    setPacks((l) => l.map((p) => (p.id === id ? { ...p, enabled } : p)));
    if (pack && !enabled) toast.show(`${pack.name} is off`, { detail: 'Notes keep what they already use.', undo: () => setPacks((l) => l.map((p) => (p.id === id ? { ...p, enabled: true } : p))) });
  };
  const packAction = (id: string, action: Exclude<PackRowAction, 'open'>) => {
    const pack = packs.find((p) => p.id === id);
    if (!pack) return;
    if (action === 'remove') setRemoving(pack);
    else toast.show(`Saved ${pack.name}.graphopack`, { kind: 'ok', duration: 2200 });
  };
  const confirmRemove = () => {
    const pack = removing;
    if (!pack) return;
    const at = packs.findIndex((p) => p.id === pack.id);
    const gone = content.filter((i) => i.source === pack.id);
    setPacks((l) => l.filter((p) => p.id !== pack.id));
    setContent((l) => l.filter((i) => i.source !== pack.id));
    setRemoving(null);
    toast.show(`${pack.name} removed`, { undo: () => { setPacks((l) => [...l.slice(0, at), pack, ...l.slice(at)]); setContent((l) => [...l, ...gone]); } });
  };
  const installImported = (s: ImportSummary) => {
    setPacks((l) => [{ id: `botanica-${l.length}`, name: 'Botanica', art: packArt.botanica, enabled: true }, ...l]);
    toast.show('Botanica installed', { detail: `${s.count} items added`, kind: 'ok', duration: 2400 });
  };
  const installCatalogue = (c: CataloguePack) => {
    setFromCatalogue((l) => [...l, c.id]);
    // an extension is not a pack: it goes to Extensions, with what it can reach listed there
    if (c.kind === 'extensions') setExtensions((l) => [...l, { id: `${c.id}-added`, icon: 'code', name: c.name, description: c.meta, enabled: true, group: 'grapho', version: '1.0', access: [{ icon: 'page', label: 'The page you are on' }] }]);
    else setPacks((l) => [...l, { id: `${c.id}-added`, name: c.name, art: c.art, enabled: true }]);
    toast.show(`${c.name} installed`, { kind: 'ok', duration: 1800 });
  };

  const removeExtension = (id: string) => {
    const at = extensions.findIndex((e) => e.id === id);
    const ext = extensions[at];
    if (!ext) return;
    setExtensions((l) => l.filter((e) => e.id !== id));
    toast.show(`${ext.name} removed`, { undo: () => setExtensions((l) => [...l.slice(0, at), ext, ...l.slice(at)]) });
  };
  const installExtensionFile = () => {
    setExtensions((l) => [...l, { id: `file-${l.length}`, icon: 'code', name: 'Lecture capture', description: 'Adds a capture panel beside the page.', enabled: true, group: 'user', version: '0.1', access: [{ icon: 'camera', label: 'The camera, while capturing' }, { icon: 'page', label: 'The page you capture on' }] }]);
    toast.show('Lecture capture installed', { kind: 'ok', duration: 2000 });
  };

  const syncNow = () => {
    setSync((s) => ({ ...s, status: 'syncing' }));
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setSync({ status: 'ok', last: 'just now' }), 1400);
  };

  return {
    values, change, theme, chooseTheme, content, visibleContent, sources, packs, extensions, sync, syncNow,
    togglePack, packAction, removing, confirmRemove, cancelRemove: () => setRemoving(null),
    toggleExtension: (id: string, enabled: boolean) => setExtensions((l) => l.map((e) => (e.id === id ? { ...e, enabled } : e))),
    removeExtension, installExtensionFile,
    importOpen, openImport: () => setImportOpen(true), closeImport: () => setImportOpen(false), installImported,
    browseOpen, browseKind, openBrowse: (kind: DiscoverKind | 'all' = 'all') => { setBrowseKind(kind); setBrowseOpen(true); }, closeBrowse: () => setBrowseOpen(false), installCatalogue, fromCatalogue,
    newPack, openNewPack: (ids: string[] = []) => setNewPack({ ids }), closeNewPack: () => setNewPack(null),
    /** Exported pack: nothing is installed, the file is just saved. */
    exportedPack: (file: string) => toast.show(`Saved ${file}`, { kind: 'ok', duration: 2200 }),
  };
}

export type SettingsState = ReturnType<typeof useSettings>;
