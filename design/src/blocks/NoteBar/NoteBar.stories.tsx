import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { NoteBar } from './NoteBar';
import { defaultTools } from './noteTools';

const meta: Meta<typeof NoteBar> = {
  title: 'Blocks/NoteBar',
  component: NoteBar,
  parameters: { layout: 'padded' },
  args: { variant: 'pill', position: 'top', mode: 'edit', tools: defaultTools, activeTool: 'pen', title: 'Biology', hidden: false, showLayers: true },
  argTypes: {
    variant: { control: 'inline-radio', options: ['pill', 'line'] },
    position: { control: 'inline-radio', options: ['top', 'bottom', 'left', 'right'] },
    mode: { control: 'inline-radio', options: ['edit', 'select', 'read'] },
    activeTool: { control: 'select', options: defaultTools.filter((t) => t !== 'separator').map((t) => (t as { id: string }).id) },
  },
  render: (args) => (
    <NoteStage inset={args.variant === 'line' && !args.hidden ? { [args.position ?? 'top']: 40 } : undefined}>
      <NoteBar {...args} />
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof NoteBar>;

export const FloatingBar: Story = {};
export const FullLine: Story = { args: { variant: 'line' } };
export const Bottom: Story = { args: { position: 'bottom' } };
export const Left: Story = { args: { position: 'left' } };
export const Right: Story = { args: { position: 'right', variant: 'line' } };
export const LineLeft: Story = { args: { position: 'left', variant: 'line' } };

export const ReadMode: Story = { args: { mode: 'read', title: 'Biology', subtitle: 'Page 3 of 12', readCount: '3 / 12' } };
export const ReadModeLine: Story = { args: { mode: 'read', variant: 'line', title: 'Biology', subtitle: 'Page 3 of 12', readCount: '3 / 12' } };

/** Auto-hide: the bar has stepped out toward its edge. It leaves the page where it was. */
export const Hidden: Story = { args: { hidden: true } };
export const HiddenLine: Story = { args: { hidden: true, variant: 'line', position: 'bottom' } };
