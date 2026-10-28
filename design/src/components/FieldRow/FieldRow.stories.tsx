import type { Meta, StoryObj } from '@storybook/react-vite';
import { SegmentedControl } from '../SegmentedControl';
import { Slider } from '../Slider';
import { Toggle } from '../Toggle';
import { Icon } from '@/assets/icons';
import { FieldRow } from './FieldRow';

const meta: Meta<typeof FieldRow> = {
  title: 'Components/FieldRow',
  component: FieldRow,
  parameters: { layout: 'padded' },
  args: { label: 'Show note title', hint: 'Hidden automatically on a left or right bar.', control: 'right' },
  decorators: [(Story) => <div style={{ width: 420 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof FieldRow>;

export const WithToggle: Story = { render: (a) => <FieldRow {...a}><Toggle variant="marked" defaultChecked aria-label="Show note title" /></FieldRow> };

export const ControlBelow: Story = {
  args: { label: 'Auto-hide', hint: 'The bar steps away while you write and returns at the edge.', control: 'below' },
  render: (a) => (
    <FieldRow {...a}>
      <SegmentedControl size="sm" fit aria-label="Auto-hide" options={[{ value: 'off', label: 'Off' }, { value: 'while', label: 'While writing' }, { value: 'always', label: 'Always' }]} />
    </FieldRow>
  ),
};

export const Stacked: Story = {
  render: () => (
    <div>
      <FieldRow label="Outline" control="below"><SegmentedControl size="sm" fit aria-label="Outline" options={[{ value: 'solid', label: 'Solid' }, { value: 'dashed', label: 'Dashed' }]} /></FieldRow>
      <FieldRow label="Width" control="below"><Slider aria-label="Width" min={1} max={12} defaultValue={4} /></FieldRow>
      <FieldRow label="Fill"><Toggle aria-label="Fill" /></FieldRow>
    </div>
  ),
};

export const Pressable: Story = {
  render: () => (
    <FieldRow label="Tools in the bar" hint="Pen, highlighter, eraser, lasso, text, shapes, image" onClick={() => {}}>
      <Icon name="chevron_right" size={18} />
    </FieldRow>
  ),
};
