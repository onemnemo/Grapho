import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { PageBackgroundPicker, PagePreview } from './PageBackground';
import type { PageBackgroundValue } from './PageBackground';

const meta: Meta<typeof PageBackgroundPicker> = {
  title: 'Blocks/PageBackground',
  component: PageBackgroundPicker,
  parameters: { layout: 'centered' },
  args: { value: { pattern: 'lined', colour: 'cream' } },
  argTypes: { value: { control: false }, onChange: { control: false } },
};
export default meta;
type Story = StoryObj<typeof PageBackgroundPicker>;

const Card = ({ initial }: { initial: PageBackgroundValue }) => {
  const [v, setV] = useState(initial);
  return (
    <div style={{ padding: 20, borderRadius: 'var(--r-md)', background: 'var(--overlay)', boxShadow: 'var(--e2)' }}>
      <PageBackgroundPicker value={v} onChange={setV} />
    </div>
  );
};

/** The picker as it sits in its popover. The tiles redraw in the paper colour you pick. */
export const Default: Story = { render: () => <Card initial={{ pattern: 'lined', colour: 'cream' }} /> };
export const DarkPaper: Story = { render: () => <Card initial={{ pattern: 'dotted', colour: 'dark' }} /> };

/** The four patterns on each paper. */
export const Previews: Story = {
  render: () => (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 96px)', gap: 12 }}>
      {(['white', 'cream', 'slate', 'dark', 'black'] as const).flatMap((c) => (['blank', 'lined', 'grid', 'dotted'] as const).map((p) => <PagePreview key={`${c}${p}`} pattern={p} colour={c} />))}
    </div>
  ),
};
