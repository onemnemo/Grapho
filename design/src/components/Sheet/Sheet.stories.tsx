import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { StoryStage } from '@/stories-support/StoryStage';
import { Sheet, type SheetProps } from './Sheet';

const meta: Meta<typeof Sheet> = {
  title: 'Components/Sheet',
  component: Sheet,
  parameters: { layout: 'fullscreen' },
  args: {
    open: true,
    title: 'New notebook',
    subtitle: 'Pick a cover and a name',
    variant: 'modal',
    width: 540,
    edge: 'end',
    placement: 'container',
    dismissOnScrim: true,
    flush: false,
    closeLabel: 'Close',
  },
  argTypes: {
    variant: { control: 'inline-radio', options: ['modal', 'side'] },
    width: { control: { type: 'number', min: 360, max: 900, step: 20 } },
    edge: { control: 'inline-radio', options: ['end', 'start'] },
    placement: { control: 'inline-radio', options: ['container', 'viewport'] },
    footer: { control: false },
    children: { control: false },
  },
  decorators: [(Story) => <StoryStage><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof Sheet>;

const Lines = ({ n = 3 }: { n?: number }) => (
  <>
    {Array.from({ length: n }, (_, i) => (
      <p key={i} style={{ margin: '0 0 16px', color: 'var(--mute)', font: 'var(--t-body)' }}>
        Covers, folders and canvases each get one sheet. Presets come first, then the details you can change.
      </p>
    ))}
  </>
);

const Footer = ({ lg }: { lg?: boolean }) => (
  <>
    <Button variant="text" size={lg ? 'lg' : 'md'}>Cancel</Button>
    <Button variant="primary" size={lg ? 'lg' : 'md'} icon="plus">Create</Button>
  </>
);

/** Open it, close it, press Escape. Controls change the variant live. */
export const Playground: Story = {
  render: (args) => <Controlled {...args} />,
};

function Controlled(args: SheetProps) {
  const [open, setOpen] = useState(true);
  return (
    <>
      <div style={{ position: 'absolute', left: 32, bottom: 32, zIndex: 1 }}>
        <Button variant="primary" onClick={() => setOpen(true)}>Open sheet</Button>
      </div>
      <Sheet {...args} open={open} onClose={() => setOpen(false)} footer={<Footer lg={args.variant === 'side'} />}>
        <Lines n={2} />
      </Sheet>
    </>
  );
}

export const Modal: Story = {
  args: { footer: <Footer />, children: <Lines n={2} /> },
};

export const ModalSmall: Story = {
  args: { width: 420, title: 'Rename folder', subtitle: undefined, footer: <Footer />, children: <Lines n={1} /> },
};

export const ModalWide: Story = {
  args: { width: 820, title: 'Presets and library', footer: <Footer />, children: <Lines n={2} /> },
};

/** A long body scrolls under a fixed header and footer; the header line appears once it moves. */
export const ScrollingBody: Story = {
  args: { title: 'Settings', footer: <Footer />, children: <Lines n={14} /> },
  decorators: [(Story) => <StoryStage height={560}><Story /></StoryStage>],
};

/** flush drops the body padding so the content brings its own layout, as the Settings overlay does with a rail and a page. */
export const Flush: Story = {
  args: {
    title: 'Settings', subtitle: undefined, width: 760, flush: true,
    children: (
      <div style={{ display: 'grid', gridTemplateColumns: '200px 1fr', minHeight: 280 }}>
        <nav style={{ padding: 'var(--sp-4)', background: 'var(--s2)', color: 'var(--mute)', font: 'var(--t-label)' }}>Toolbar</nav>
        <div style={{ padding: 'var(--sp-6)' }}><Lines n={2} /></div>
      </div>
    ),
  },
};

/** The library filter sheet: slides in from the right edge, one solid action at the foot. */
export const SideSheet: Story = {
  args: {
    variant: 'side',
    title: 'Filters',
    subtitle: undefined,
    footer: (
      <>
        <Button variant="text" size="lg">Clear</Button>
        <Button variant="primary" size="lg">Show 24 notes</Button>
      </>
    ),
    children: (
      <>
        <div>
          <div style={{ font: 'var(--t-caption)', color: 'var(--mute)', marginBottom: 10 }}>Type</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <Button size="sm">Notebooks</Button><Button size="sm">Canvases</Button><Button size="sm">PDF</Button>
          </div>
        </div>
        <Lines n={2} />
      </>
    ),
  },
};

export const SideSheetStart: Story = { ...SideSheet, args: { ...SideSheet.args, edge: 'start' } };

export const Closed: Story = { args: { open: false, children: <Lines /> } };
