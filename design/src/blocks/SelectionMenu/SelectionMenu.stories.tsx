import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { AnchorDemo } from '@/stories-support/AnchorDemo';
import { SelectionMenu } from './SelectionMenu';

const meta: Meta<typeof SelectionMenu> = {
  title: 'Blocks/SelectionMenu',
  component: SelectionMenu,
  parameters: { layout: 'fullscreen', docs: { story: { inline: false, height: '520px' } } },
  args: { kind: 'ink', locked: false, canPaste: false, boundary: 'viewport' },
  argTypes: { kind: { control: 'inline-radio', options: ['ink', 'image'] }, open: { control: false }, onClose: { control: false }, anchorRef: { control: false } },
  render: (args) => (
    <AnchorDemo label="More">
      {({ anchorRef, open, setOpen }) => <SelectionMenu {...args} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} />}
    </AnchorDemo>
  ),
};
export default meta;
type Story = StoryObj<typeof SelectionMenu>;

/** The More menu behind the floating bar. Order and Move to layer open a second level; Delete sits alone under a rule. */
export const Ink: Story = {};
export const WithPaste: Story = { args: { canPaste: true } };
/** A picture adds Replace image and drops the ink-only items. */
export const Picture: Story = { args: { kind: 'image' } };
export const Locked: Story = { args: { locked: true } };

/** Lock flips to Unlock. */
export const Wired: Story = {
  render: (args) => {
    const [locked, setLocked] = useState(false);
    return (
      <AnchorDemo label="More">
        {({ anchorRef, open, setOpen }) => (
          <SelectionMenu {...args} locked={locked} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} onAction={(a) => (a === 'lock' || a === 'unlock') && setLocked(a === 'lock')} />
        )}
      </AnchorDemo>
    );
  },
};
