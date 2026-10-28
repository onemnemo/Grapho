import type { IconName } from '@/assets/icons';

export type BarPosition = 'top' | 'bottom' | 'left' | 'right';
export type BarVariant = 'pill' | 'line';
export type BarMode = 'edit' | 'read';

export interface NoteTool {
  id: string;
  icon: IconName;
  label: string;
}
export type NoteToolItem = NoteTool | 'separator';

/** The tools every note starts with. Image is dropped from the compact set. */
export const defaultTools: NoteToolItem[] = [
  { id: 'pen', icon: 'pen', label: 'Pen' },
  { id: 'highlighter', icon: 'highlighter', label: 'Highlighter' },
  { id: 'eraser', icon: 'eraser', label: 'Eraser' },
  { id: 'lasso', icon: 'lasso', label: 'Lasso' },
  'separator',
  { id: 'text', icon: 'text', label: 'Text' },
  { id: 'shapes', icon: 'shapes', label: 'Shapes' },
  { id: 'image', icon: 'image', label: 'Image' },
];

export const tipSide = (pos: BarPosition) => (pos === 'top' ? 'bottom' : pos === 'bottom' ? 'top' : pos === 'left' ? 'right' : 'left');
