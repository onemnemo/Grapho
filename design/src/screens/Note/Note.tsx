import { useEffect, useRef, useState, type MouseEvent, type ReactNode, type RefObject } from 'react';
import { ExportDialog } from '@/blocks/ExportDialog';
import { InsertMenu } from '@/blocks/InsertMenu';
import { LayersPopover, sampleLayers, type NoteLayer } from '@/blocks/LayersPanel';
import type { NotePaper, NotePattern } from '@/blocks/NotePage';
import { OverflowMenu, type OverflowAction } from '@/blocks/OverflowMenu';
import { PageBackground, type PageBackgroundValue } from '@/blocks/PageBackground';
import { SettingsDialogs, SettingsOverlay, useSettings, type SettingsSectionId, type SettingsValues } from '@/blocks/SettingsOverlay';
import { ToastProvider } from '@/components/Toast';
import { ImageScene } from './ImageScene';
import { LassoScene } from './LassoScene';
import { LineScene } from './LineScene';
import { ShapeScene } from './ShapeScene';
import { SuggestScene } from './SuggestScene';
import { WriteScene } from './WriteScene';
import { useNoteFlows } from './noteFlows';
import type { NoteScene, SceneProps } from './sceneTypes';
import styles from './Note.module.css';

export type NoteOverlay = 'overflow' | 'insert' | 'background' | 'export' | 'layers';

export interface NoteProps {
  /** write: the page with auto-hide. lasso: a selection. shape and line: a selected shape or line with its options. suggest: "Did you mean". image: a picture with its actions. */
  scene?: NoteScene;
  /** The starting toolbar and wheel settings. Settings opened from the ••• menu change them live. */
  initial?: Partial<SettingsValues>;
  /** The pen wheel, or a stand-in, placed on the wheel edge from Settings. */
  wheel?: ReactNode;
  /** Open Settings straight away, on a section. */
  settingsOpen?: SettingsSectionId | boolean;
  /** Open one of the note's own menus or panels straight away. */
  open?: NoteOverlay;
  /** Start with the ruler and the protractor on the page. The protractor reads an angle when given one. */
  guides?: { ruler?: boolean; protractor?: boolean; reading?: number };
  /** The page background to start with. */
  background?: { pattern: NotePattern; paper: NotePaper };
  /** Layers to start with, and the one that is active. */
  layers?: NoteLayer[];
  activeLayer?: string;
  /** Image scene: start in crop mode, or with the background removal or ink conversion running. */
  start?: 'crop' | 'removing' | 'converting';
  /** Make the insert and export flows end in their failure messages. */
  fail?: boolean;
  /** The export dialog opens already running. */
  exporting?: boolean;
  /** An infinite canvas has no pages to add, insert or choose between. */
  infinite?: boolean;
  width?: number;
  height?: number;
}

const SIZE: Record<NoteScene, [number, number]> = { write: [1040, 640], lasso: [800, 460], shape: [800, 580], line: [800, 580], suggest: [800, 400], image: [800, 460] };
const SCENES = { write: WriteScene, lasso: LassoScene, shape: ShapeScene, line: LineScene, suggest: SuggestScene, image: ImageScene };
const PATTERN_NAME: Record<NotePattern, string> = { blank: 'Blank', lined: 'Lined', grid: 'Grid', dotted: 'Dotted' };

function NoteInner({ scene = 'write', initial, wheel, settingsOpen = false, open: openAtStart, guides: guidesAtStart, background: bgAtStart, layers: layersAtStart, activeLayer, start, fail = false, exporting: exportingAtStart = false, infinite = false, width, height, host }: NoteProps & { host: RefObject<HTMLDivElement | null> }) {
  const settings = useSettings(initial);
  const flows = useNoteFlows(fail);
  const [menu, setMenu] = useState<NoteOverlay | null>(null);
  const [layersOpen, setLayersOpen] = useState(false);
  const [open, setOpen] = useState(!!settingsOpen);
  const [section, setSection] = useState<SettingsSectionId | null>(typeof settingsOpen === 'string' ? settingsOpen : 'general');
  const [guides, setGuides] = useState({ ruler: !!guidesAtStart?.ruler, protractor: !!guidesAtStart?.protractor });
  const [finger, setFinger] = useState(false);
  const [bg, setBg] = useState<PageBackgroundValue>({ pattern: bgAtStart?.pattern ?? 'blank', colour: bgAtStart?.paper ?? 'white' });
  const [changed, setChanged] = useState(!!bgAtStart);
  const [layers, setLayers] = useState<NoteLayer[]>(layersAtStart ?? sampleLayers);
  const [active, setActive] = useState(activeLayer ?? layers[layers.length - 1].id);
  const [exporting, setExporting] = useState(exportingAtStart);
  const anchor = useRef<HTMLElement | null>(null);
  const layersAnchor = useRef<HTMLElement | null>(null);
  const [w, h] = [width ?? SIZE[scene][0], height ?? SIZE[scene][1]];
  const phone = w < 640;

  // Menus that start open hang from the More button, so they need it to exist first.
  useEffect(() => {
    anchor.current = host.current?.querySelector<HTMLElement>('[aria-label="More"]') ?? null;
    layersAnchor.current = host.current?.querySelector<HTMLElement>('[aria-label="Layers"]') ?? null;
    if (openAtStart === 'layers') setLayersOpen(true);
    else if (openAtStart) setMenu(openAtStart);
  }, [host, openAtStart]);

  const onMore = (e: MouseEvent<HTMLButtonElement>) => {
    anchor.current = e.currentTarget;
    setMenu('overflow');
  };
  const onImageTool = () => {
    anchor.current = host.current?.querySelector<HTMLElement>('[data-tool="image"]') ?? anchor.current;
    setMenu('insert');
  };
  const onAction = (a: OverflowAction) => {
    if (a === 'settings') setOpen(true);
    else if (a === 'pageBackground') setMenu('background');
    else if (a === 'export') setMenu('export');
    else if (a === 'ruler' || a === 'protractor') setGuides((g) => ({ ...g, [a]: !g[a] }));
    else if (a === 'drawWithFinger') setFinger((f) => !f);
  };

  const activeLayerInfo = layers.find((l) => l.id === active);
  const blocked = activeLayerInfo && !activeLayerInfo.visible ? 'hidden' : activeLayerInfo?.locked ? 'locked' : null;
  const edge = settings.values.barPosition;
  const placement = edge === 'bottom' ? 'top-end' : edge === 'left' ? 'right-start' : edge === 'right' ? 'left-start' : 'bottom-end';
  // Layers sits mid-bar, so its popover lines up with the button's start edge rather than the bar's end.
  const layersPlacement = edge === 'bottom' ? 'top-start' : edge === 'top' ? 'bottom-start' : placement;

  const common: SceneProps = {
    width: w, height: h, settings, wheel: scene === 'write' ? wheel : undefined, onMore,
    background: changed ? { pattern: bg.pattern, paper: bg.colour === 'custom' ? 'cream' : bg.colour } : undefined,
    guides: { ...guides, reading: guidesAtStart?.reading },
    onGuideClose: (g) => setGuides((v) => ({ ...v, [g]: false })),
    onLayers: () => {
      layersAnchor.current = host.current?.querySelector<HTMLElement>('[aria-label="Layers"]') ?? null;
      setLayersOpen((v) => !v);
    },
    layersOpen,
    onImageTool,
    blocked,
    start,
    fail,
    overlays: (
      <>
        <OverflowMenu
          open={menu === 'overflow'} onClose={() => setMenu(null)} anchorRef={anchor} boundary="container" placement={placement}
          pageBackground={PATTERN_NAME[bg.pattern]} drawWithFinger={finger} rulerOn={guides.ruler} protractorOn={guides.protractor} onAction={onAction}
        />
        <InsertMenu open={menu === 'insert'} onClose={() => setMenu(null)} anchorRef={anchor} placement={placement} infinite={infinite} onAction={flows.insert} />
        <PageBackground open={menu === 'background'} onClose={() => setMenu(null)} anchorRef={anchor} placement={placement} value={bg} onChange={(v) => { setBg(v); setChanged(true); }} />
        <ExportDialog
          open={menu === 'export'} onClose={() => setMenu(null)} placement="container" infinite={infinite} exporting={exporting}
          onExport={() => { setExporting(true); flows.exportNote(() => { setExporting(false); setMenu(null); }); }}
        />
        <LayersPopover
          open={layersOpen} onClose={() => setLayersOpen(false)} anchorRef={layersAnchor} placement={layersPlacement}
          layers={layers} activeId={active} onChange={setLayers} onActiveChange={setActive}
        />
        <SettingsOverlay
          open={open} onClose={() => setOpen(false)} placement="container" variant={phone ? 'phone' : 'tablet'}
          section={phone && section === 'general' && settingsOpen === false ? null : section} onSectionChange={setSection} settings={settings}
        />
        <SettingsDialogs settings={settings} placement="container" />
      </>
    ),
  };
  const Scene = SCENES[scene];
  return <Scene {...common} />;
}

/** The note screen: a page of handwriting, the toolbar in the style and place chosen in Settings, and the ••• menu with everything that acts on the note. */
export function Note(props: NoteProps) {
  const host = useRef<HTMLDivElement>(null);
  const [w, h] = [props.width ?? SIZE[props.scene ?? 'write'][0], props.height ?? SIZE[props.scene ?? 'write'][1]];
  return (
    <div ref={host} className={styles.root}>
      <ToastProvider placement="container">
        <NoteInner {...props} width={w} height={h} host={host} />
      </ToastProvider>
    </div>
  );
}
