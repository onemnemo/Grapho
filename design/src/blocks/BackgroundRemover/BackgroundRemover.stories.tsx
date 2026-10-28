import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { BackgroundRemover } from './BackgroundRemover';

const meta: Meta<typeof BackgroundRemover> = {
  title: 'Blocks/BackgroundRemover',
  component: BackgroundRemover,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', onClose: () => undefined },
  argTypes: { initial: { control: 'inline-radio', options: ['original', 'scanning', 'removed'] }, initialTool: { control: 'inline-radio', options: ['auto', 'erase', 'restore'] } },
  decorators: [(Story) => <StoryStage height={780}><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof BackgroundRemover>;

export const Original: Story = {};
export const Scanning: Story = { args: { initial: 'scanning' } };
export const Removed: Story = { args: { initial: 'removed' } };
export const EraseBrush: Story = { args: { initial: 'removed', initialTool: 'erase' } };
export const RestoreBrush: Story = { args: { initial: 'removed', initialTool: 'restore' } };
