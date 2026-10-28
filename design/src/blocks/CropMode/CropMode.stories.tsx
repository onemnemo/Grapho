import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { ILLUS } from '@/assets/studio';
import { NoteStage } from '@/stories-support/NoteStage';
import { CropBar, CropFrame, FULL_CROP, type CropRect } from './CropMode';

const BOX = { x: 250, y: 50, width: 300, height: 243 };

const meta: Meta<typeof CropFrame> = {
  title: 'Blocks/CropMode',
  component: CropFrame,
  parameters: { layout: 'padded' },
  args: { box: BOX },
  argTypes: { box: { control: false }, value: { control: false } },
};
export default meta;
type Story = StoryObj<typeof CropFrame>;

/** Drag a handle to size the frame, drag inside it to move it. Everything outside is dimmed. Done is the one solid button. */
export const Default: Story = {
  render: (args) => {
    const [rect, setRect] = useState<CropRect>({ l: 0.12, t: 0.1, r: 0.88, b: 0.9 });
    return (
      <NoteStage height={400} page={<img src={ILLUS} alt="" style={{ position: 'absolute', left: BOX.x, top: BOX.y, width: BOX.width, height: BOX.height }} />}>
        <CropFrame {...args} value={rect} onChange={setRect} />
        <CropBar x={BOX.x + BOX.width / 2} y={BOX.y + BOX.height + 14} onReset={() => setRect(FULL_CROP)} />
      </NoteStage>
    );
  },
};

export const Untouched: Story = {
  render: (args) => (
    <NoteStage height={400} page={<img src={ILLUS} alt="" style={{ position: 'absolute', left: BOX.x, top: BOX.y, width: BOX.width, height: BOX.height }} />}>
      <CropFrame {...args} />
      <CropBar x={BOX.x + BOX.width / 2} y={BOX.y + BOX.height + 14} />
    </NoteStage>
  ),
};
