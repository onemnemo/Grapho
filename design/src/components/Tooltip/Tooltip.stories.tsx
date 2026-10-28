import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { Tooltip } from './Tooltip';

const meta: Meta<typeof Tooltip> = {
  title: 'Components/Tooltip',
  component: Tooltip,
  parameters: { layout: 'centered', docs: { story: { inline: false, height: '200px' } } },
  args: { content: 'Undo', shortcut: undefined, side: 'top', delay: 380, open: true },
  argTypes: {
    side: { control: 'inline-radio', options: ['top', 'bottom', 'left', 'right'] },
    open: { control: 'boolean' },
    children: { control: false },
  },
  decorators: [(Story) => <div style={{ padding: '80px 120px' }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Tooltip>;

export const Playground: Story = { render: (args) => <Tooltip {...args}><Button>Hover me</Button></Tooltip> };
export const WithShortcut: Story = { args: { content: 'Undo', shortcut: 'Ctrl Z' }, render: Playground.render };
export const Bottom: Story = { args: { side: 'bottom' }, render: Playground.render };
export const Right: Story = { args: { side: 'right' }, render: Playground.render };
/** Not forced open: hover for a beat, or tab to it. */
export const OnHover: Story = { args: { open: undefined, content: 'Rename' }, render: Playground.render };
