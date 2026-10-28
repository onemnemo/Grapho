import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { AnchorDemo } from '@/stories-support/AnchorDemo';
import { StoryStage } from '@/stories-support/StoryStage';
import { Popover } from './Popover';

const meta: Meta<typeof Popover> = {
  title: 'Components/Popover',
  component: Popover,
  parameters: { layout: 'fullscreen', docs: { story: { inline: false, height: '460px' } } },
  args: { placement: 'bottom-start', offset: 8, kind: 'content', open: true },
  argTypes: {
    placement: {
      control: 'select',
      options: ['bottom-start', 'bottom', 'bottom-end', 'top-start', 'top', 'top-end', 'right-start', 'right', 'left-start', 'left'],
    },
    kind: { control: 'inline-radio', options: ['content', 'menu'] },
    boundary: { control: 'inline-radio', options: ['viewport', 'container'] },
    offset: { control: { type: 'number', min: 0, max: 32 } },
    anchorRef: { control: false },
    open: { control: false },
    onClose: { control: false },
    children: { control: false },
  },
};
export default meta;
type Story = StoryObj<typeof Popover>;

/** Anchored to the trigger. Press Escape or click away to close; it flips when the edge is near. */
export const Playground: Story = {
  render: (args) => (
    <AnchorDemo label="Page size">
      {({ anchorRef, open, setOpen }) => (
        <Popover {...args} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} aria-label="Page size">
          <div style={{ font: 'var(--t-heading)', letterSpacing: 'var(--track-heading)', marginBottom: 12 }}>Page size</div>
          <p style={{ margin: '0 0 16px', color: 'var(--mute)', font: 'var(--t-body)' }}>
            Pick a paper size for new pages in this notebook.
          </p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <Button variant="text" size="sm" onClick={() => setOpen(false)}>Cancel</Button>
            <Button variant="primary" size="sm" onClick={() => setOpen(false)}>Apply</Button>
          </div>
        </Popover>
      )}
    </AnchorDemo>
  ),
};

export const Above: Story = { ...Playground, args: { placement: 'top' } };
export const EndAligned: Story = { ...Playground, args: { placement: 'bottom-end' } };
export const Beside: Story = { ...Playground, args: { placement: 'right-start' } };

/** boundary="container" keeps the popover inside its frame instead of floating over the window, for framed screens. */
export const InFrame: Story = {
  args: { boundary: 'container', placement: 'bottom-start' },
  render: (args) => (
    <StoryStage height={460}>
      <div style={{ position: 'absolute', inset: 0 }}>
        <AnchorDemo label="Page size">
          {({ anchorRef, open, setOpen }) => (
            <Popover {...args} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} aria-label="Page size">
              <div style={{ font: 'var(--t-heading)', letterSpacing: 'var(--track-heading)' }}>Page size</div>
            </Popover>
          )}
        </AnchorDemo>
      </div>
    </StoryStage>
  ),
};
