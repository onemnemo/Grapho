import type { Meta, StoryObj } from '@storybook/react-vite';
import { ActionRow } from './ActionRow';

const meta: Meta<typeof ActionRow> = {
  title: 'Components/ActionRow',
  component: ActionRow,
  args: { icon: 'plus', children: 'New pack', tone: 'default', disabled: false },
  argTypes: { tone: { control: 'inline-radio', options: ['default', 'danger'] } },
  decorators: [(Story) => <div style={{ width: 360 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof ActionRow>;

export const Playground: Story = {};
export const WithValue: Story = { args: { icon: 'cloud', children: 'Back up now', value: 'Yesterday, 21:40' } };
export const Danger: Story = { args: { icon: 'trash', children: 'Remove', tone: 'danger' } };
export const Stack: Story = {
  render: () => (
    <div>
      <ActionRow icon="plus">New pack</ActionRow>
      <ActionRow icon="import">Import .graphopack</ActionRow>
      <ActionRow icon="grid">Browse catalogue</ActionRow>
    </div>
  ),
};
