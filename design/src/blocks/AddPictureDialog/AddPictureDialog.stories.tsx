import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { AddPictureDialog } from './AddPictureDialog';

const meta: Meta<typeof AddPictureDialog> = {
  title: 'Blocks/AddPictureDialog',
  component: AddPictureDialog,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', onClose: () => undefined },
  argTypes: { kind: { control: 'inline-radio', options: ['cover', 'folder'] }, initialStep: { control: 'inline-radio', options: ['choose', 'crop', 'remove'] } },
  decorators: [(Story) => <StoryStage height={640}><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof AddPictureDialog>;

export const Choose: Story = {};
export const ChooseForFolder: Story = { args: { kind: 'folder' } };
export const CropAnImage: Story = { args: { initialStep: 'crop' } };
export const RemoveBackground: Story = { args: { initialStep: 'remove' } };
