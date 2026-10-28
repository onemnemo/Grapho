import type { Meta, StoryObj } from '@storybook/react-vite';
import { SearchField } from './SearchField';

const meta: Meta<typeof SearchField> = {
  title: 'Components/SearchField',
  component: SearchField,
  args: { placeholder: 'Search', size: 'md', width: 240, disabled: false },
  argTypes: { size: { control: 'inline-radio', options: ['sm', 'md'] }, width: { control: { type: 'range', min: 120, max: 480, step: 10 } } },
};
export default meta;
type Story = StoryObj<typeof SearchField>;

export const Playground: Story = {};

export const Sizes: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <SearchField size="md" placeholder="Search notes" />
      <SearchField size="sm" placeholder="Search or create a tag" width={280} />
    </div>
  ),
};

export const WithText: Story = { args: { defaultValue: 'photosynthesis' } };

export const Disabled: Story = { args: { disabled: true } };
