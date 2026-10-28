import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { NoteBar, defaultTools } from '../NoteBar';
import { NoteBarHandle } from './NoteBarHandle';

const meta: Meta<typeof NoteBarHandle> = {
  title: 'Blocks/NoteBarHandle',
  component: NoteBarHandle,
  parameters: { layout: 'padded' },
  args: { position: 'top', visible: true },
  argTypes: { position: { control: 'inline-radio', options: ['top', 'bottom', 'left', 'right'] } },
  render: (args) => (
    <NoteStage height={260}>
      <NoteBar hidden variant="pill" position={args.position} tools={defaultTools} title="Biology" />
      <NoteBarHandle {...args} />
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof NoteBarHandle>;

export const Visible: Story = {};
export const Bottom: Story = { args: { position: 'bottom' } };
export const Left: Story = { args: { position: 'left' } };
export const Right: Story = { args: { position: 'right' } };
/** The handle fades out after a moment; the bar can still be called from the edge. */
export const Faded: Story = { args: { visible: false } };
