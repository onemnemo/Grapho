import type { Meta, StoryObj } from '@storybook/react-vite';
import { Radio, RadioGroup } from './Radio';

const meta: Meta<typeof Radio> = {
  title: 'Components/Radio',
  component: Radio,
  args: { 'aria-label': 'Option', checked: false, disabled: false },
};
export default meta;
type Story = StoryObj<typeof Radio>;

export const Playground: Story = {};

export const States: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
      <Radio aria-label="Off" />
      <Radio aria-label="On" checked />
      <Radio aria-label="Disabled" disabled />
      <Radio aria-label="Disabled on" checked disabled />
    </div>
  ),
};

export const Group: Story = {
  render: () => (
    <div style={{ width: 340 }}>
      <RadioGroup
        aria-label="Shape detection"
        defaultValue="suggest"
        options={[
          { value: 'suggest', label: 'Suggest', hint: 'Show Did you mean after a pause' },
          { value: 'snap', label: 'Snap', hint: 'Replace the stroke at once' },
          { value: 'off', label: 'Off' },
          { value: 'later', label: 'Later', disabled: true },
        ]}
      />
    </div>
  ),
};
