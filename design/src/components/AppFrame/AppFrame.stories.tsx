import type { Meta, StoryObj } from '@storybook/react-vite';
import { AppFrame } from './AppFrame';

const meta: Meta<typeof AppFrame> = {
  title: 'Components/AppFrame',
  component: AppFrame,
  parameters: { layout: 'centered' },
  args: { framed: true, children: <p style={{ margin: 24, color: 'var(--mute)' }}>A screen puts its layout in here.</p> },
  decorators: [(Story) => <div style={{ width: 480, height: 260 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof AppFrame>;

/** The surface a full screen sits on. */
export const Default: Story = { args: { framed: false } };
/** Rounded with a hairline, for a screen shown inside a page. */
export const Framed: Story = {};
