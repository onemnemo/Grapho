import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { TextArea } from './TextArea';

const meta: Meta<typeof TextArea> = { title: 'Components/TextArea', component: TextArea, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof TextArea>;

export const Playground: Story = {
  render: () => {
    const [v, setV] = useState('Physics 2');
    return <div style={{ width: 268 }}><TextArea aria-label="Text" value={v} onChange={setV} /></div>;
  },
};
export const Empty: Story = {
  render: () => <div style={{ width: 268 }}><TextArea aria-label="Text" value="" placeholder="Write something here" onChange={() => undefined} /></div>,
};
