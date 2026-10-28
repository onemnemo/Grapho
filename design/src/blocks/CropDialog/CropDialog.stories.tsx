import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { CropDialog } from './CropDialog';

const meta: Meta<typeof CropDialog> = {
  title: 'Blocks/CropDialog',
  component: CropDialog,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', onClose: () => undefined },
  argTypes: { mode: { control: 'inline-radio', options: ['edit', 'adding'] }, frameLabel: { control: 'inline-radio', options: ['Cover', 'Front'] } },
  decorators: [(Story) => <StoryStage height={640}><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof CropDialog>;

export const Edit: Story = {};
export const ZoomedIn: Story = { args: { crop: { z: 2, px: 12, py: -8 } } };
export const CoverShape: Story = { args: { aspect: 0.75 } };
export const FrontShape: Story = { args: { frameLabel: 'Front', aspect: 2.3 } };
export const Adding: Story = { args: { mode: 'adding', onBack: () => undefined } };
