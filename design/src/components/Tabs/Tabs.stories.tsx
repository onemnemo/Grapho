import type { Meta, StoryObj } from '@storybook/react-vite';
import { Tabs, type TabItem } from './Tabs';

const sheet: TabItem[] = [
  { value: 'presets', label: 'Presets' },
  { value: 'colour', label: 'Colour' },
  { value: 'icon', label: 'Icon' },
  { value: 'finish', label: 'Finish' },
];

const meta: Meta<typeof Tabs> = {
  title: 'Components/Tabs',
  component: Tabs,
  args: { tabs: sheet, defaultValue: 'presets', layout: 'fill', 'aria-label': 'Folder sheet' },
  argTypes: { layout: { control: 'inline-radio', options: ['fill', 'hug'] }, tabs: { control: 'object' } },
  decorators: [(Story) => <div style={{ width: 480 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Tabs>;

export const Playground: Story = {};

export const Hug: Story = { args: { layout: 'hug' } };

export const WithCounts: Story = {
  args: {
    layout: 'hug',
    tabs: [
      { value: 'all', label: 'All', count: 36 },
      { value: 'yours', label: 'Yours', count: 12 },
      { value: 'packs', label: 'Packs', count: 3 },
    ],
    defaultValue: 'all',
  },
};

export const WithIcons: Story = {
  args: {
    tabs: [
      { value: 'presets', label: 'Presets', icon: 'sparkle' },
      { value: 'colour', label: 'Colour', icon: 'palette' },
      { value: 'icon', label: 'Icon', icon: 'star' },
    ],
  },
};

export const WithDisabledTab: Story = {
  args: { tabs: [...sheet.slice(0, 3), { value: 'finish', label: 'Finish', disabled: true }] },
};
