import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import type { ToolKey } from '@/assets/studio';
import { StudioRail } from './StudioRail';

const meta: Meta<typeof StudioRail> = { title: 'Blocks/StudioRail', component: StudioRail, parameters: { layout: 'centered' }, args: { tool: 'stickers', onTool: () => {} } };
export default meta;
type Story = StoryObj<typeof StudioRail>;

export const Stickers: Story = {};
export const Text: Story = { args: { tool: 'text' } };
export const Textures: Story = { args: { tool: 'textures' } };
export const Interactive: Story = {
  render: () => { const [t, setT] = useState<ToolKey>('stickers'); return <div style={{ height: 520, width: 64 }}><StudioRail tool={t} onTool={setT} /></div>; },
};
