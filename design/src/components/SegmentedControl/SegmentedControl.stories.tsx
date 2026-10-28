import type { Meta, StoryObj } from '@storybook/react-vite';
import { SegmentedControl, type SegmentOption } from './SegmentedControl';

const sets: SegmentOption[] = [
  { value: 'all', label: 'All' },
  { value: 'yours', label: 'Yours' },
  { value: 'packs', label: 'Packs' },
];
const views: SegmentOption[] = [
  { value: 'grid', label: 'Grid', icon: 'grid' },
  { value: 'list', label: 'List', icon: 'list' },
];

const meta: Meta<typeof SegmentedControl> = {
  title: 'Components/SegmentedControl',
  component: SegmentedControl,
  args: { options: sets, defaultValue: 'all', size: 'md', fit: false, iconsOnly: false, 'aria-label': 'Source' },
  argTypes: { size: { control: 'inline-radio', options: ['sm', 'md'] }, options: { control: 'object' } },
};
export default meta;
type Story = StoryObj<typeof SegmentedControl>;

const col = { display: 'flex', flexDirection: 'column', gap: 24, alignItems: 'flex-start' } as const;

export const Playground: Story = {};

export const Sizes: Story = {
  render: () => (
    <div style={col}>
      <SegmentedControl options={sets} defaultValue="all" aria-label="Medium" />
      <SegmentedControl options={sets} defaultValue="yours" size="sm" aria-label="Small" />
    </div>
  ),
};

export const WithIcons: Story = {
  render: () => (
    <div style={col}>
      <SegmentedControl options={views} defaultValue="grid" aria-label="Layout" />
      <SegmentedControl options={views} defaultValue="list" iconsOnly aria-label="View" />
    </div>
  ),
};

export const Fit: Story = {
  render: () => (
    <div style={{ width: 360 }}>
      <SegmentedControl
        fit
        size="sm"
        aria-label="Colour set"
        options={[
          { value: 'basic', label: 'Basic', icon: 'pen' },
          { value: 'highlight', label: 'Highlight', icon: 'highlighter' },
          { value: 'more', label: 'More', icon: 'palette' },
        ]}
        defaultValue="basic"
      />
    </div>
  ),
};

export const WithDisabledOption: Story = {
  args: { options: [...sets.slice(0, 2), { value: 'packs', label: 'Packs', disabled: true }] },
};
