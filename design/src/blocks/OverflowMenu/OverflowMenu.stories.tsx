import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { AnchorDemo } from '@/stories-support/AnchorDemo';
import { OverflowMenu } from './OverflowMenu';

const meta: Meta<typeof OverflowMenu> = {
  title: 'Blocks/OverflowMenu',
  component: OverflowMenu,
  parameters: { layout: 'fullscreen', docs: { story: { inline: false, height: '640px' } } },
  args: { pageBackground: 'Lined', drawWithFinger: false, rulerOn: false, protractorOn: false, placement: 'bottom-end' },
  argTypes: { open: { control: false }, onClose: { control: false }, anchorRef: { control: false } },
};
export default meta;
type Story = StoryObj<typeof OverflowMenu>;

export const Playground: Story = {
  render: (args) => (
    <AnchorDemo label="More">
      {({ anchorRef, open, setOpen }) => <OverflowMenu {...args} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} />}
    </AnchorDemo>
  ),
};

/** Switches show a tick when on. */
export const ToolsOn: Story = { ...Playground, args: { rulerOn: true, protractorOn: true, drawWithFinger: true } };

/** Choosing Ruler, Protractor or Draw with finger flips its tick. Insert is the quick action that opens the Insert menu. */
export const Wired: Story = {
  render: (args) => {
    const [on, setOn] = useState({ ruler: false, protractor: false, drawWithFinger: false });
    const flip = (k: keyof typeof on) => setOn((v) => ({ ...v, [k]: !v[k] }));
    return (
      <AnchorDemo label="More">
        {({ anchorRef, open, setOpen }) => (
          <OverflowMenu
            {...args}
            rulerOn={on.ruler}
            protractorOn={on.protractor}
            drawWithFinger={on.drawWithFinger}
            anchorRef={anchorRef}
            open={open}
            onClose={() => setOpen(false)}
            onAction={(a) => (a === 'ruler' || a === 'protractor' || a === 'drawWithFinger') && flip(a)}
          />
        )}
      </AnchorDemo>
    );
  },
};
