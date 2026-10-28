import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildPresetItems } from '@/assets/studio';
import { StoryStage } from '@/stories-support/StoryStage';
import { PackExportSheet } from './PackExportSheet';

const meta: Meta<typeof PackExportSheet> = { title: 'Blocks/PackExportSheet', component: PackExportSheet, parameters: { layout: 'fullscreen' } };
export default meta;
type Story = StoryObj<typeof PackExportSheet>;

const ALL = buildPresetItems('folder');

function Stage({ count }: { count: number }) {
  const [open, setOpen] = useState(true);
  return (
    <StoryStage height={720}>
      <PackExportSheet open={open} onClose={() => setOpen(false)} items={ALL.slice(0, count)} placement="container" onExport={() => setOpen(false)} />
    </StoryStage>
  );
}

export const Few: Story = { render: () => <Stage count={3} /> };
export const Many: Story = { render: () => <Stage count={8} /> };
export const One: Story = { render: () => <Stage count={1} /> };
