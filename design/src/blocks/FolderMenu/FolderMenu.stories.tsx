import { useRef } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@/components/Button';
import { Surface } from '@/stories-support/Surface';
import { FolderMoreButton, MoveMenu, type FolderTarget } from './FolderMenu';

const targets: FolderTarget[] = [
  { id: 'a', name: 'Semester 1', colour: 'indigo' },
  { id: 'b', name: 'Physics', colour: 'sky', parent: 'Semester 1' },
  { id: 'c', name: 'Readings', colour: 'forest' },
  { id: 'd', name: 'Recipes', colour: 'ember' },
  { id: 'e', name: 'Travel', colour: 'gold' },
];

const meta: Meta<typeof FolderMoreButton> = {
  title: 'Blocks/FolderMenu',
  component: FolderMoreButton,
  args: { pinned: false, variant: 'row', show: true },
  argTypes: { variant: { control: 'inline-radio', options: ['media', 'row'] } },
  render: (args) => <Surface style={{ minHeight: 360, minWidth: 360, display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-start' }}><FolderMoreButton {...args} onAction={() => {}} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof FolderMoreButton>;

export const Playground: Story = {};
export const Open: Story = { args: { defaultOpen: true } };
export const OpenPinned: Story = { args: { defaultOpen: true, pinned: true } };
export const WithMoveList: Story = { args: { defaultOpen: true, targets, onMove: () => {} } };

function MoveDemo() {
  const ref = useRef<HTMLSpanElement>(null);
  return (
    <Surface style={{ minHeight: 400, minWidth: 360, display: 'flex', alignItems: 'flex-end' }}>
      <span ref={ref}><Button>Move to</Button></span>
      <MoveMenu open onClose={() => {}} anchorRef={ref} targets={targets} onMove={() => {}} />
    </Surface>
  );
}

/** The destinations drawn as the folders themselves, so you recognise them by their look. */
export const MoveTo: Story = { render: () => <MoveDemo /> };
