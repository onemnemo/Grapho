import type { Meta, StoryObj } from '@storybook/react-vite';
import { CloseButton } from './CloseButton';

const meta: Meta<typeof CloseButton> = {
  title: 'Components/CloseButton',
  component: CloseButton,
  parameters: { layout: 'centered' },
  args: { 'aria-label': 'Close' },
};
export default meta;
type Story = StoryObj<typeof CloseButton>;

/** The bare round close control in sheet headers and banners. */
export const Default: Story = {};
export const Disabled: Story = { args: { disabled: true } };
