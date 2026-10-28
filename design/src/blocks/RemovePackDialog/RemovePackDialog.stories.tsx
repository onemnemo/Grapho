import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { RemovePackDialog } from './RemovePackDialog';

const meta: Meta<typeof RemovePackDialog> = {
  title: 'Blocks/RemovePackDialog',
  component: RemovePackDialog,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', pack: { name: 'Pixel friends' }, onConfirm: () => {}, onCancel: () => {} },
  decorators: [(Story) => <StoryStage height={420}><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof RemovePackDialog>;

/** One plain sentence: notes keep what they already use. */
export const Default: Story = {};
