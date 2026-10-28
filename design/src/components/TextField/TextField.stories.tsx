import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon, iconPaths, type IconName } from '@/assets/icons';
import { TextField } from './TextField';

const meta: Meta<typeof TextField> = {
  title: 'Components/TextField',
  component: TextField,
  args: { label: 'Folder name', variant: 'floating', hint: '', error: '', ok: false, disabled: false },
  argTypes: {
    variant: { control: 'inline-radio', options: ['floating', 'stacked', 'inline'] },
    icon: { control: 'select', options: [undefined, ...(Object.keys(iconPaths) as IconName[])] },
  },
  decorators: [(Story) => <div style={{ width: 340 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof TextField>;

const col = { display: 'flex', flexDirection: 'column', gap: 12 } as const;

export const Playground: Story = {};

export const Floating: Story = {
  render: () => (
    <div style={col}>
      <TextField label="Folder name" />
      <TextField label="Folder name" defaultValue="Biology" hint="Shown under the folder" />
      <TextField label="Folder name" defaultValue="Biology" ok hint="Looks good" />
      <TextField label="Folder name" defaultValue="Bio/logy" error="Names cannot use a slash" />
      <TextField label="Folder name" defaultValue="Biology" disabled />
    </div>
  ),
};

export const Stacked: Story = {
  render: () => (
    <div style={col}>
      <TextField variant="stacked" label="Name" placeholder="Untitled" />
      <TextField variant="stacked" label="Location" icon="folder" defaultValue="Library" readOnly trailing={<Icon name="chevron_down" size={16} />} />
      <TextField variant="stacked" label="Name" defaultValue="Bio/logy" error="Names cannot use a slash" />
      <TextField variant="stacked" label="Name" defaultValue="Biology" disabled />
    </div>
  ),
};

/** A bare field for renaming inside a panel. The label is not shown; it becomes the aria-label. */
export const Inline: Story = {
  render: () => (
    <div style={col}>
      <TextField variant="inline" label="Pen name" defaultValue="Fine liner" />
      <div style={{ background: 'var(--wh-well)', padding: 12, borderRadius: 'var(--r-md)' }}>
        <TextField variant="inline" size="sm" label="Set name" defaultValue="Warm" />
      </div>
    </div>
  ),
};
