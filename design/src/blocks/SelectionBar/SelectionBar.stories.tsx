import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@/components/Button';
import { StoryStage } from '@/stories-support/StoryStage';
import { SelectionBar } from './SelectionBar';

const meta: Meta<typeof SelectionBar> = {
  title: 'Blocks/SelectionBar',
  component: SelectionBar,
  parameters: { layout: 'padded' },
  args: { count: 4, visible: true, showCount: false },
  render: (args) => <StoryStage height={220}><SelectionBar {...args} /></StoryStage>,
};
export default meta;
type Story = StoryObj<typeof SelectionBar>;

export const Playground: Story = {};
export const WithCount: Story = { args: { showCount: true } };
export const NothingPicked: Story = { args: { count: 0, showCount: true } };
export const Hidden: Story = { args: { visible: false } };

export const RollingCount: Story = {
  args: { showCount: true },
  render: (args) => {
    const [n, setN] = useState(3);
    return (
      <StoryStage height={260}>
        <div style={{ position: 'absolute', top: 16, left: 16, display: 'flex', gap: 8 }}>
          <Button onClick={() => setN(n + 1)}>Pick one more</Button>
          <Button onClick={() => setN(Math.max(0, n - 1))}>Pick one less</Button>
        </div>
        <SelectionBar {...args} count={n} />
      </StoryStage>
    );
  },
};
