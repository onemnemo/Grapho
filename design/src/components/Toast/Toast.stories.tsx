import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { StoryStage } from '@/stories-support/StoryStage';
import { Toast } from './Toast';
import { ToastProvider, useToast } from './ToastProvider';

const meta: Meta<typeof Toast> = {
  title: 'Components/Toast',
  component: Toast,
  parameters: { layout: 'centered' },
  args: { children: 'Moved Sunday sketches to Work', icon: 'move', duration: 6000, paused: true, leaving: false, undoLabel: 'Undo' },
  argTypes: { onUndo: { control: false } },
};
export default meta;
type Story = StoryObj<typeof Toast>;

/** With Undo: the ring drains over six seconds. It is paused here so you can see it; clear Paused to watch it run. */
export const WithUndo: Story = { args: { onUndo: () => {} } };

/** Without Undo there is no ring; it leaves after two seconds. */
export const Plain: Story = { args: { children: 'Saved as preset', detail: 'My cover 2', kind: 'ok' } };

export const ErrorKind: Story = { args: { children: 'Could not import the pack', detail: 'The file is not a .graphopack.', kind: 'error' } };

export const LongMessage: Story = {
  args: { children: 'Moved a notebook with a very long name that keeps going to a folder with a longer one', onUndo: () => {} },
  decorators: [(Story) => <div style={{ width: 420 }}><Story /></div>],
};

function Demo() {
  const toast = useToast();
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', padding: 'var(--sp-8)' }}>
      <Button onClick={() => toast.show('Moved Sunday sketches to Work', { undo: () => toast.show('Move undone') })}>Move with Undo</Button>
      <Button onClick={() => toast.show('Saved as preset', { kind: 'ok', detail: 'My cover 2' })}>Plain toast</Button>
      <Button variant="danger" icon="trash" onClick={() => toast.deleted('Sunday sketches', () => toast.show('Restored'))}>
        Delete a note
      </Button>
    </div>
  );
}

/** Press Delete a note a few times: repeated deletes merge into "Deleted N items" and one Undo restores all. */
export const Provider: StoryObj = {
  parameters: { layout: 'fullscreen' },
  render: () => (
    <StoryStage height={420}>
      <ToastProvider placement="container">
        <Demo />
      </ToastProvider>
    </StoryStage>
  ),
};

/** With a selection bar showing the toast lifts above it. */
export const AboveSelectionBar: StoryObj = {
  parameters: { layout: 'fullscreen' },
  render: () => (
    <StoryStage height={420}>
      <ToastProvider placement="container" offset="above-bar">
        <Demo />
      </ToastProvider>
    </StoryStage>
  ),
};

/** The one-line pill, for canvases such as the pen wheel. The ring drains for the length of the --d-toast token. */
export const Pill: Story = {
  args: { variant: 'pill', children: 'Pen deleted', onUndo: () => {}, paused: false },
  decorators: [(Story) => <div style={{ minWidth: 320, display: 'grid', placeItems: 'center' }}><Story /></div>],
};
