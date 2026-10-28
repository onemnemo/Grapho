import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { AlphabetRail } from './AlphabetRail';

const AVAILABLE = 'ABCDEFGHJLMOPRSTW'.split('');

const meta: Meta<typeof AlphabetRail> = {
  title: 'Blocks/AlphabetRail',
  component: AlphabetRail,
  parameters: { layout: 'padded' },
  args: { available: AVAILABLE, active: 'C' },
  render: (args) => {
    const [active, setActive] = useState(args.active);
    return (
      <StoryStage height={640}>
        <AlphabetRail {...args} active={active} onSelect={setActive} />
      </StoryStage>
    );
  },
};
export default meta;
type Story = StoryObj<typeof AlphabetRail>;

export const Playground: Story = {};
export const FewLetters: Story = { args: { available: ['A', 'M', 'S'], active: 'M' } };
