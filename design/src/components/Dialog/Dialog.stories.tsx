import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { StoryStage } from '@/stories-support/StoryStage';
import { Dialog } from './Dialog';
import { ConfirmDialog } from './ConfirmDialog';
import { UnsavedChangesDialog } from './UnsavedChangesDialog';

const meta: Meta<typeof Dialog> = {
  title: 'Components/Dialog',
  component: Dialog,
  parameters: { layout: 'fullscreen' },
  args: {
    open: true,
    title: 'Rename folder',
    children: 'Folders keep their notes when you rename them.',
    placement: 'container',
    role: 'dialog',
    dismissOnScrim: true,
  },
  argTypes: {
    role: { control: 'inline-radio', options: ['dialog', 'alertdialog'] },
    placement: { control: 'inline-radio', options: ['container', 'viewport'] },
    actions: { control: false },
  },
  decorators: [(Story) => <StoryStage height={460}><Story /></StoryStage>],
};
export default meta;
type Story = StoryObj<typeof Dialog>;

export const Playground: Story = {
  args: {
    actions: (
      <>
        <Button variant="text">Cancel</Button>
        <Button variant="primary">Rename</Button>
      </>
    ),
  },
};

export const Compact: Story = {
  args: {
    width: 520,
    density: 'compact',
    children: 'A wide dialog with a tighter frame, for a picture and a few controls.',
    actions: (
      <>
        <Button variant="text">Cancel</Button>
        <Button variant="primary">Continue</Button>
      </>
    ),
  },
};

function ConfirmDemo(props: { tone: 'default' | 'danger' }) {
  const [open, setOpen] = useState(true);
  return (
    <>
      <div style={{ position: 'absolute', left: 32, bottom: 32 }}>
        <Button onClick={() => setOpen(true)}>Ask again</Button>
      </div>
      <ConfirmDialog
        open={open}
        placement="container"
        title={props.tone === 'danger' ? 'Delete 3 notes?' : 'Move to Work?'}
        message={props.tone === 'danger' ? 'They go to the trash and stay there for 30 days.' : 'The notes keep their tags and covers.'}
        confirmLabel={props.tone === 'danger' ? 'Delete' : 'Move'}
        confirmIcon={props.tone === 'danger' ? 'trash' : 'move'}
        tone={props.tone}
        onConfirm={() => setOpen(false)}
        onCancel={() => setOpen(false)}
      />
    </>
  );
}

export const Confirm: Story = { render: () => <ConfirmDemo tone="default" /> };
export const ConfirmDanger: Story = { render: () => <ConfirmDemo tone="danger" /> };

function UnsavedDemo() {
  const [open, setOpen] = useState(true);
  return (
    <>
      <div style={{ position: 'absolute', left: 32, bottom: 32 }}>
        <Button onClick={() => setOpen(true)}>Close the studio</Button>
      </div>
      <UnsavedChangesDialog
        open={open}
        placement="container"
        name="Sunday sketches"
        kind="notebook"
        onSave={() => setOpen(false)}
        onDiscard={() => setOpen(false)}
        onKeepEditing={() => setOpen(false)}
      />
    </>
  );
}

/** Shown when the studio closes with edits. Keep editing is the quiet default, Escape and the scrim choose it. */
export const UnsavedChanges: Story = { render: () => <UnsavedDemo /> };

export const UnsavedChangesFolder: Story = {
  render: () => <UnsavedChangesDialog open placement="container" name="Semester 2" kind="folder" onSave={() => {}} onDiscard={() => {}} onKeepEditing={() => {}} />,
};

/** Without a name the sentence falls back to "this" and the kind. */
export const UnsavedChangesNoName: Story = {
  render: () => <UnsavedChangesDialog open placement="container" name="" kind="cover" onSave={() => {}} onDiscard={() => {}} onKeepEditing={() => {}} />,
};
