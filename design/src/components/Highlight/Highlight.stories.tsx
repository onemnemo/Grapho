import type { Meta, StoryObj } from '@storybook/react-vite';
import { Highlight } from './Highlight';

const meta: Meta<typeof Highlight> = {
  title: 'Components/Highlight',
  component: Highlight,
  parameters: { layout: 'centered' },
  args: { text: 'Sunday sketches', query: 'sketch' },
};
export default meta;
type Story = StoryObj<typeof Highlight>;

/** The first match gets a soft gold mark. */
export const Match: Story = {};
export const NoMatch: Story = { args: { query: 'physics' } };
export const EmptyQuery: Story = { args: { query: '' } };
