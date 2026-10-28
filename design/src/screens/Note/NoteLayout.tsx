import type { CSSProperties, HTMLAttributes, MouseEvent, ReactNode, Ref } from 'react';
import { NoteBar, defaultTools, type NoteBarProps } from '@/blocks/NoteBar';
import { NoteBarHandle } from '@/blocks/NoteBarHandle';
import { NoteHint } from '@/blocks/NoteHint';
import { Guides } from '@/blocks/Guides';
import { NotePage, type NotePaper, type NotePattern } from '@/blocks/NotePage';
import type { SettingsValues } from '@/blocks/SettingsOverlay';
import { DeviceFrame } from '@/components/DeviceFrame';
import styles from './Note.module.css';

export interface NoteLayoutProps {
  width: number;
  height: number;
  settings: SettingsValues;
  /** What the page shows. */
  page: ReactNode;
  /** Things that float over the page: a selection bar, a suggestion. */
  floating?: ReactNode;
  /** Overrides for the bar: mode, active tool, selection actions. */
  bar?: Partial<NoteBarProps>;
  /** The pen wheel, or a stand-in. It is placed on the chosen edge and steps aside for a bar on the same edge. */
  wheel?: ReactNode;
  hidden?: boolean;
  handle?: boolean;
  hint?: string;
  /** Pointer handlers for the whole page area, used by auto-hide. */
  bind?: HTMLAttributes<HTMLDivElement>;
  stageRef?: Ref<HTMLDivElement>;
  onMore?: (event: MouseEvent<HTMLButtonElement>) => void;
  /** Overlays that live inside the frame: menus, Settings, toasts. */
  overlays?: ReactNode;
  /** Controls above the frame, outside the screen. */
  controls?: ReactNode;
  /** Page background chosen from the ••• menu. */
  background?: { pattern: NotePattern; paper: NotePaper };
  /** Which guides are on the page, from the ••• menu. */
  guides?: { ruler: boolean; protractor: boolean; reading?: number };
  onGuideClose?: (guide: 'ruler' | 'protractor') => void;
  /** The Layers button of the bar. */
  onLayers?: () => void;
  layersOpen?: boolean;
  /** The Image tool of the bar. */
  onImageTool?: () => void;
}

const LINE_RESERVE = 40;

/** The note screen's frame: page, bar, handle, wheel and what floats over them. Scenes decide what goes in each. */
export function NoteLayout({ width, height, settings: v, page, floating, bar, wheel, hidden = false, handle = false, hint, bind, stageRef, onMore, overlays, controls, background, guides, onGuideClose, onLayers, layersOpen, onImageTool }: NoteLayoutProps) {
  const reserve = v.barStyle === 'line' && v.autoHide === 'off';
  const inset = reserve ? { [v.barPosition]: LINE_RESERVE } : undefined;
  const wheelOffset = v.wheelEdge === v.barPosition ? (v.barStyle === 'line' ? LINE_RESERVE : 56) : 0;
  return (
    <>
      {controls && <div className={styles.controls}>{controls}</div>}
      <DeviceFrame width={width} height={height}>
        <div ref={stageRef} className={styles.stage} {...bind}>
          <NotePage inset={inset} pattern={background?.pattern} paper={background?.paper}>{page}</NotePage>
          {wheel && (
            <div className={styles.wheel} data-edge={v.wheelEdge} style={{ '--wo': `${wheelOffset}px` } as CSSProperties}>
              {wheel}
            </div>
          )}
          <NoteBar
            variant={v.barStyle}
            position={v.barPosition}
            tools={defaultTools}
            activeTool="pen"
            title={v.showTitle ? 'Biology' : undefined}
            hidden={hidden}
            onMore={onMore}
            onLayers={onLayers}
            layersOpen={layersOpen}
            {...bar}
            onTool={(id) => (id === 'image' ? onImageTool?.() : bar?.onTool?.(id))}
          />
          <NoteBarHandle position={v.barPosition} visible={handle} />
          {floating}
          <Guides
            ruler={guides?.ruler ? { x: width * 0.31, y: height * 0.74, length: 10, angle: -6, onClose: () => onGuideClose?.('ruler') } : undefined}
            protractor={guides?.protractor ? { x: width * 0.79, y: height * 0.93, reading: guides.reading, onClose: () => onGuideClose?.('protractor') } : undefined}
          />
          {hint && (
            <div className={styles.hint} data-top={v.barPosition === 'bottom' || undefined}>
              <NoteHint>{hint}</NoteHint>
            </div>
          )}
        </div>
        {overlays}
      </DeviceFrame>
    </>
  );
}
