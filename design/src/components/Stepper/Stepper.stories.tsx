import type { Meta, StoryObj } from '@storybook/react-vite';
import { Stepper } from './Stepper';

const meta: Meta<typeof Stepper> = {
  title: 'Components/Stepper',
  component: Stepper,
  args: { min: 3, max: 12, defaultValue: 5, 'aria-label': 'Points' },
};
export default meta;
type Story = StoryObj<typeof Stepper>;

export const Playground: Story = {};
export const AtMinimum: Story = { args: { defaultValue: 3 } };
export const AtMaximum: Story = { args: { defaultValue: 12 } };
