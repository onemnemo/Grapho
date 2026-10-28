import { useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@/components/Button';
import { Surface } from '@/stories-support/Surface';
import { NewMenu } from './NewMenu';

const meta: Meta<typeof NewMenu> = {
  title: 'Blocks/NewMenu',
  component: NewMenu,
  parameters: { layout: 'padded' },
};
export default meta;
type Story = StoryObj<typeof NewMenu>;

function Demo({ startOpen }: { startOpen: boolean }) {
  const [open, setOpen] = useState(startOpen);
  const [last, setLast] = useState('');
  const ref = useRef<HTMLSpanElement>(null);
  return (
    <Surface style={{ minHeight: 420, display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-start', gap: 16 }}>
      <span style={{ font: 'var(--t-caption)', color: 'var(--dim)', alignSelf: 'center' }}>{last && `Chose ${last}`}</span>
      <span ref={ref}><Button variant="primary" icon="plus" onClick={() => setOpen((v) => !v)}>New</Button></span>
      <NewMenu open={open} onClose={() => setOpen(false)} anchorRef={ref} onChoose={setLast} />
    </Surface>
  );
}

export const Closed: Story = { render: () => <Demo startOpen={false} /> };
export const Open: Story = { render: () => <Demo startOpen /> };
