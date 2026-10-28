import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildSampleContent } from '@/assets/content';
import { StoryStage } from '@/stories-support/StoryStage';
import { NewPackSheet, type NewPackStep } from './NewPackSheet';

const meta: Meta<typeof NewPackSheet> = { title: 'Blocks/NewPackSheet', component: NewPackSheet, parameters: { layout: 'fullscreen' } };
export default meta;
type Story = StoryObj<typeof NewPackSheet>;

const ALL = buildSampleContent();
const pick = (...types: string[]) => ALL.filter((i) => types.includes(i.type)).slice(0, 14).map((i) => i.id);

function Stage({ step, selected = [] }: { step: NewPackStep; selected?: string[] }) {
  const [open, setOpen] = useState(true);
  return (
    <StoryStage height={720}>
      <NewPackSheet open={open} onClose={() => setOpen(false)} items={ALL} initialStep={step} initialSelected={selected} placement="container" />
    </StoryStage>
  );
}

/** Step one: type tabs, checkable tiles. The button says what you will carry on with. */
export const PickItems: Story = { render: () => <Stage step="pick" selected={pick('stickers')} /> };
export const PickNothingYet: Story = { render: () => <Stage step="pick" /> };
/** Step two: name and cover. The count is in the Export button. */
export const NameAndCover: Story = { render: () => <Stage step="details" selected={[...pick('stickers').slice(0, 8), ...pick('pens').slice(0, 4), ...pick('colours').slice(0, 2)]} /> };
export const Exported: Story = { render: () => <Stage step="done" selected={pick('stickers')} /> };
