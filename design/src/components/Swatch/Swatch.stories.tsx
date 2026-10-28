import type { Meta, StoryObj } from '@storybook/react-vite';
import { Swatch } from './Swatch';
import { SwatchRow, type SwatchOption } from './SwatchRow';

const inks: SwatchOption[] = [
  { value: 'ink', color: 'var(--ink)', label: 'Ink' },
  { value: 'pink', color: 'var(--pink)', label: 'Pink' },
  { value: 'red', color: 'var(--red)', label: 'Red' },
  { value: 'orange', color: 'var(--orange)', label: 'Orange' },
  { value: 'gold', color: 'var(--gold)', label: 'Gold' },
  { value: 'green', color: 'var(--green)', label: 'Green' },
  { value: 'blue', color: 'var(--blue)', label: 'Blue' },
  { value: 'lilac', color: 'var(--lilac)', label: 'Lilac' },
];

const meta: Meta<typeof Swatch> = {
  title: 'Components/Swatch',
  component: Swatch,
  args: { color: 'var(--pink)', label: 'Pink', selected: false, size: 'md', shape: 'round', kind: 'colour', check: false },
  argTypes: {
    size: { control: 'inline-radio', options: ['sm', 'md', 'lg'] },
    shape: { control: 'inline-radio', options: ['round', 'square'] },
    kind: { control: 'inline-radio', options: ['colour', 'none', 'add', 'custom'] },
    color: { control: 'color' },
  },
  decorators: [(Story) => <div style={{ padding: 16 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Swatch>;

const row = { display: 'flex', gap: 20, alignItems: 'center', flexWrap: 'wrap' } as const;

export const Playground: Story = {};

export const Sizes: Story = {
  render: () => (
    <div style={row}>
      <Swatch label="Small" color="var(--blue)" size="sm" selected />
      <Swatch label="Medium" color="var(--blue)" selected />
      <Swatch label="Large" color="var(--blue)" size="lg" selected />
    </div>
  ),
};

export const Shapes: Story = {
  render: () => (
    <div style={row}>
      <Swatch label="Round" color="var(--gold)" />
      <Swatch label="Round selected" color="var(--gold)" selected />
      <Swatch label="Square" color="var(--gold)" shape="square" />
      <Swatch label="Square selected" color="var(--gold)" shape="square" selected />
    </div>
  ),
};

export const Kinds: Story = {
  render: () => (
    <div style={row}>
      <Swatch label="Pink" color="var(--pink)" check selected />
      <Swatch label="No colour" kind="none" />
      <Swatch label="No colour selected" kind="none" selected />
      <Swatch label="Add colour" kind="add" />
    </div>
  ),
};

export const Row: Story = {
  render: () => (
    <SwatchRow
      aria-label="Ink colour"
      defaultValue="pink"
      swatches={[...inks, { value: 'none', label: 'No colour', kind: 'none' }, { value: 'add', label: 'Add colour', kind: 'add' }]}
    />
  ),
};

export const SquareRow: Story = {
  render: () => <SwatchRow aria-label="Cover colour" shape="square" size="lg" gap={14} check defaultValue="gold" swatches={inks} />,
};

export const SmallRow: Story = {
  render: () => <SwatchRow aria-label="Saved colours" size="sm" gap={10} defaultValue="blue" swatches={inks} />,
};
