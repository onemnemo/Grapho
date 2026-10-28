import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon, iconPaths, type IconName } from '@/assets/icons';
import { ListRow } from './ListRow';

const meta: Meta<typeof ListRow> = {
  title: 'Components/ListRow',
  component: ListRow,
  parameters: { layout: 'padded' },
  args: {
    title: 'Sunday sketches',
    description: 'Notebook, 12 pages',
    meta: 'Today',
    leadingIcon: 'notebook',
    selected: false,
    divided: false,
    onClick: () => {},
  },
  argTypes: {
    leadingIcon: { control: 'select', options: Object.keys(iconPaths) as IconName[] },
    leading: { control: false },
    trailing: { control: false },
  },
  decorators: [(Story) => <div style={{ width: 560 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof ListRow>;

export const Playground: Story = { args: { trailing: <Icon name="chevron_right" size={16} /> } };
export const Static: Story = { args: { onClick: undefined, trailing: undefined } };
export const Selected: Story = { args: { selected: true } };
export const TitleOnly: Story = { args: { description: undefined, meta: undefined } };
export const LongTitle: Story = {
  args: { title: 'A notebook with a very long name that never fits on one line however wide the row is', trailing: <Icon name="chevron_right" size={16} /> },
};

/** A settings-style list: rows stack with a hairline between them. */
export const Stacked: Story = {
  render: () => (
    <div>
      <ListRow divided leadingIcon="pen" title="Pen and wheel" description="Wheel edge, pen switching" trailing={<Icon name="chevron_right" size={16} />} onClick={() => {}} />
      <ListRow divided leadingIcon="pack" title="Packs and extensions" meta="3 installed" trailing={<Icon name="chevron_right" size={16} />} onClick={() => {}} />
      <ListRow divided leadingIcon="sync" title="Sync" meta="Up to date" trailing={<Icon name="chevron_right" size={16} />} onClick={() => {}} />
    </div>
  ),
};
