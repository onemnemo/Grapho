import type { MouseEvent, ReactNode } from 'react';
import type { SettingsState } from '@/blocks/SettingsOverlay';
import type { NotePaper, NotePattern } from '@/blocks/NotePage';

export interface SceneProps {
  width: number;
  height: number;
  settings: SettingsState;
  wheel?: ReactNode;
  /** The bar's ••• button was pressed. */
  onMore: (event: MouseEvent<HTMLButtonElement>) => void;
  /** Menus and Settings that live inside the frame. */
  overlays: ReactNode;
  background?: { pattern: NotePattern; paper: NotePaper };
  guides: { ruler: boolean; protractor: boolean; reading?: number };
  onGuideClose: (guide: 'ruler' | 'protractor') => void;
  onLayers: () => void;
  /** The layers popover is open. */
  layersOpen?: boolean;
  onImageTool: () => void;
  /** The active layer cannot be drawn on. */
  blocked?: 'hidden' | 'locked' | null;
  /** Image scene: start in a working state or in crop mode. */
  start?: 'crop' | 'removing' | 'converting';
  /** Jobs end in their failure message. */
  fail?: boolean;
}

export type NoteScene = 'write' | 'lasso' | 'shape' | 'line' | 'suggest' | 'image';
