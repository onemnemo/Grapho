import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates } from '@/assets/studio';
import { StoryStage } from '@/stories-support/StoryStage';
import { PresetPicker } from './PresetPicker';

const meta: Meta<typeof PresetPicker> = { title: 'Blocks/PresetPicker', component: PresetPicker, parameters: { layout: 'fullscreen' } };
export default meta;
type Story = StoryObj<typeof PresetPicker>;

function Stage(props: Partial<React.ComponentProps<typeof PresetPicker>>) {
  const [open, setOpen] = useState(true);
  return (
    <StoryStage height={820}>
      <PresetPicker open={open} onClose={() => setOpen(false)} kind="folder" placement="container" {...props} />
    </StoryStage>
  );
}

export const Folders: Story = { render: () => <Stage /> };
export const Covers: Story = { render: () => <Stage kind="cover" /> };
export const WithYours: Story = { render: () => <Stage yours={[{ ...buildTemplates().folders[1], name: 'My semester' }]} initialSource="yours" /> };
export const Selecting: Story = { render: () => <Stage initialSelecting /> };
export const Searching: Story = { render: () => <Stage initialQuery="bot" /> };
export const Filtered: Story = { render: () => <Stage initialSource="pack" initialType="Textured" /> };
export const Empty: Story = { render: () => <Stage initialQuery="zzzz" /> };
