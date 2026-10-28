import type { Meta, StoryObj } from '@storybook/react-vite';
import { Toggle } from './Toggle';

const meta: Meta<typeof Toggle> = {
  title: 'Components/Toggle',
  component: Toggle,
  args: { 'aria-label': 'Auto-hide', defaultChecked: true, variant: 'plain', disabled: false },
  argTypes: { variant: { control: 'inline-radio', options: ['plain', 'marked'] } },
};
export default meta;
type Story = StoryObj<typeof Toggle>;

const row = { display: 'flex', gap: 24, alignItems: 'center' } as const;

export const Playground: Story = {};

export const States: Story = {
  render: () => (
    <div style={row}>
      <Toggle aria-label="Off" />
      <Toggle aria-label="On" defaultChecked />
      <Toggle aria-label="Disabled off" disabled />
      <Toggle aria-label="Disabled on" defaultChecked disabled />
    </div>
  ),
};

export const Variants: Story = {
  render: () => (
    <div style={row}>
      <Toggle variant="plain" aria-label="Plain off" />
      <Toggle variant="plain" aria-label="Plain on" defaultChecked />
      <Toggle variant="marked" aria-label="Marked off" />
      <Toggle variant="marked" aria-label="Marked on" defaultChecked />
    </div>
  ),
};

export const WithLabel: Story = {
  render: () => (
    <div style={{ width: 320 }}>
      <Toggle label="Pen hover near the edge" hint="Within 56dp of the bar" defaultChecked />
      <Toggle label="Swipe in from the edge" />
      <Toggle label="Two-finger tap" disabled />
    </div>
  ),
};
