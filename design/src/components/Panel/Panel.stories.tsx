import type { Meta, StoryObj } from '@storybook/react-vite';
import { FieldRow } from '../FieldRow';
import { Toggle } from '../Toggle';
import { Panel } from './Panel';

const meta: Meta<typeof Panel> = {
  title: 'Components/Panel',
  component: Panel,
  parameters: { layout: 'padded' },
  args: { title: 'Panel title', icon: 'sliders' },
  decorators: [(Story) => <div style={{ width: 320, height: 300 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Panel>;

export const Default: Story = {
  render: (a) => (
    <Panel {...a} onClose={() => {}}>
      <FieldRow label="Fill"><Toggle aria-label="Fill" defaultChecked /></FieldRow>
      <FieldRow label="Snap to grid"><Toggle aria-label="Snap to grid" /></FieldRow>
    </Panel>
  ),
};
export const Docked: Story = { args: { shape: 'docked' }, render: (a) => <Panel {...a} onClose={() => {}}><FieldRow label="Fill"><Toggle aria-label="Fill" /></FieldRow></Panel> };
export const WithoutClose: Story = { render: (a) => <Panel {...a}><FieldRow label="Fill"><Toggle aria-label="Fill" /></FieldRow></Panel> };
