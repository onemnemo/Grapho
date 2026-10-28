import type { Meta, StoryObj } from '@storybook/react-vite';
import { ANIM_NAMES } from '@/assets/mascot';
import { EmptyState } from './EmptyState';

const meta: Meta<typeof EmptyState> = {
  title: 'Components/EmptyState',
  component: EmptyState,
  parameters: { layout: 'centered' },
  args: {
    title: 'Nothing here yet',
    children: 'Start a notebook and your writing will show up here.',
    bubble: 'hi there',
    anim: 'idle',
    size: 'md',
    primary: { label: 'New notebook', icon: 'plus' },
    secondary: { label: 'Import' },
  },
  argTypes: {
    anim: { control: 'select', options: ANIM_NAMES },
    size: { control: 'inline-radio', options: ['md', 'sm'] },
    seed: { control: 'number' },
  },
};
export default meta;
type Story = StoryObj<typeof EmptyState>;

export const Playground: Story = {};
export const EmptyLibrary: Story = {};
export const NoSearchResults: Story = {
  args: { title: 'No notes match', children: 'Try another word or clear the filters.', bubble: 'hm', primary: undefined, secondary: { label: 'Clear filters' } },
};
export const EmptyTrash: Story = {
  args: { title: 'The trash is empty', children: 'Deleted notes stay here for 30 days.', bubble: 'all clean', primary: undefined, secondary: undefined },
};
export const Offline: Story = {
  args: { title: 'You are offline', children: 'Your notes are safe on this device and will sync when you reconnect.', bubble: 'no signal', primary: { label: 'Try again' }, secondary: undefined },
};
export const Small: Story = { args: { size: 'sm', title: 'No folders', children: 'Folders you make show up here.', primary: undefined, secondary: undefined } };
export const NoBubble: Story = { args: { bubble: undefined } };
