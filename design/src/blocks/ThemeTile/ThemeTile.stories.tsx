import type { Meta, StoryObj } from '@storybook/react-vite';
import { SAMPLE_CUSTOM } from '@/assets/themes';
import { ThemeTile, ThemePreview } from './ThemeTile';

const meta: Meta<typeof ThemeTile> = {
  title: 'Blocks/ThemeTile',
  component: ThemeTile,
  args: { theme: 'ink', selected: false },
  argTypes: { theme: { control: 'inline-radio', options: ['ink', 'graphite', 'paper', 'custom'] } },
  decorators: [(Story) => <div style={{ width: 200 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof ThemeTile>;

export const Playground: Story = {};
export const Selected: Story = { args: { selected: true } };

export const AllThemes: Story = {
  render: () => (
    <div role="radiogroup" aria-label="Theme" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 200px)', gap: 12, width: 'auto' }}>
      <ThemeTile theme="ink" selected />
      <ThemeTile theme="graphite" />
      <ThemeTile theme="paper" />
      <ThemeTile theme="custom" inputs={SAMPLE_CUSTOM.night} />
    </div>
  ),
};

/** The picture alone, at a larger size. It scales with its width. */
export const PreviewLarge: Story = {
  render: () => <div style={{ width: 480 }}><ThemePreview theme="graphite" /></div>,
};
