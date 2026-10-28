import type { Meta, StoryObj } from '@storybook/react-vite';
import { ScrollRow } from './ScrollRow';

const meta: Meta<typeof ScrollRow> = {
  title: 'Components/ScrollRow',
  component: ScrollRow,
  parameters: { layout: 'padded' },
  args: { gap: 22 },
  argTypes: { gap: { control: { type: 'number', min: 0, max: 48 } } },
  decorators: [(Story) => <div style={{ width: 520 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof ScrollRow>;

/** Scrolls sideways with the far edge fading out. */
export const Default: Story = {
  render: (args) => (
    <ScrollRow {...args}>
      {Array.from({ length: 9 }, (_, i) => (
        <i key={i} style={{ flex: 'none', width: 120, height: 150, borderRadius: 'var(--r-md)', background: 'var(--s2)' }} />
      ))}
    </ScrollRow>
  ),
};
