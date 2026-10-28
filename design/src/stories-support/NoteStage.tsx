import type { ReactNode } from 'react';
import { NotePage, SampleInk } from '@/blocks/NotePage';
import { DeviceFrame } from '@/components/DeviceFrame';

export interface NoteStageProps {
  width?: number;
  height?: number;
  /** Space the page gives up, as a full line bar does. */
  inset?: { top?: number; right?: number; bottom?: number; left?: number };
  /** Replace the sample handwriting. */
  page?: ReactNode;
  children?: ReactNode;
}

/** Storybook helper: a black frame with a note page and the sample handwriting, for bars and panels to sit on. */
export function NoteStage({ width = 800, height = 460, inset, page, children }: NoteStageProps) {
  return (
    <DeviceFrame width={width} height={height}>
      <NotePage inset={inset}>{page ?? <SampleInk />}</NotePage>
      {children}
    </DeviceFrame>
  );
}
