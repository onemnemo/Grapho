import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { LocationField, NameField, QuickSheet } from './QuickSheet';

const meta: Meta<typeof QuickSheet> = { title: 'Blocks/QuickSheet', component: QuickSheet, parameters: { layout: 'fullscreen' } };
export default meta;
type Story = StoryObj<typeof QuickSheet>;

function Demo({ mode }: { mode: 'new' | 'edit' }) {
  const [tab, setTab] = useState('one');
  const [name, setName] = useState('Sunday sketches');
  return (
    <StoryStage height={640}>
      <QuickSheet
        open onClose={() => {}} placement="container" title={mode === 'new' ? 'New item' : 'Edit item'} kind="cover" mode={mode}
        createLabel={mode === 'new' ? 'Create' : 'Save'} onCreate={() => {}} onDelete={() => {}} onCustomise={() => {}}
        preview={<i style={{ display: 'block', width: 96, height: 128, borderRadius: 'var(--r-sm)', background: 'var(--s3)' }} />}
        fields={<><NameField value={name} onChange={setName} /><LocationField /></>}
        tabs={[{ value: 'one', label: 'Presets' }, { value: 'two', label: 'Colour' }]} tab={tab} onTab={setTab}
        panes={<p style={{ margin: 0, color: 'var(--mute)' }}>The open tab goes here.</p>}
      />
    </StoryStage>
  );
}

/** The frame the Folder, Notebook and Canvas sheets share. New and Edit are the same sheet. */
export const New: Story = { render: () => <Demo mode="new" /> };
export const Edit: Story = { render: () => <Demo mode="edit" /> };
