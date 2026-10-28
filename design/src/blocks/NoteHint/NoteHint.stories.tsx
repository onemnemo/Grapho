import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { NoteHint } from './NoteHint';

const meta: Meta<typeof NoteHint> = {
  title: 'Blocks/NoteHint',
  component: NoteHint,
  args: { children: 'Write to hide the bar' },
  render: (args) => (
    <NoteStage height={160}>
      <div style={{ position: 'absolute', left: '50%', bottom: 16, translate: '-50% 0' }}><NoteHint {...args} /></div>
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof NoteHint>;

/** A few words in grey on the overlay. No outline, no second line. */
export const Default: Story = {};
export const WithIcon: Story = { args: { icon: 'lasso', children: 'Draw a loop to select' } };
export const Count: Story = { args: { children: 'Page 3 of 12' } };
