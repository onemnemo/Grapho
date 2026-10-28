import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { StudioTopBar } from './StudioTopBar';

const meta: Meta<typeof StudioTopBar> = {
  title: 'Blocks/StudioTopBar', component: StudioTopBar, parameters: { layout: 'fullscreen' },
  args: { kind: 'cover', name: 'Physics 2', canUndo: true, canRedo: false, onClose: () => {}, onUndo: () => {}, onRedo: () => {}, onSavePreset: () => {}, onDone: () => {}, onName: () => {} },
};
export default meta;
type Story = StoryObj<typeof StudioTopBar>;

export const Cover: Story = {};
export const Folder: Story = { args: { kind: 'folder', name: 'Semester 2' } };
export const NothingToUndo: Story = { args: { canUndo: false } };
export const BothEnabled: Story = { args: { canRedo: true } };
export const Typing: Story = {
  render: (args) => { const [n, setN] = useState('Physics 2'); return <StudioTopBar {...args} name={n} onName={setN} />; },
};
