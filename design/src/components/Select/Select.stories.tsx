import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Select } from './Select';

const meta: Meta<typeof Select> = { title: 'Components/Select', component: Select, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof Select>;

const fonts = [{ value: 'sans', label: 'Sans' }, { value: 'serif', label: 'Serif' }, { value: 'hand', label: 'Handwritten' }, { value: 'mono', label: 'Mono' }];

export const Playground: Story = {
  render: () => {
    const [v, setV] = useState('sans');
    return <div style={{ width: 220 }}><Select aria-label="Font" options={fonts} value={v} onChange={setV} /></div>;
  },
};
export const Disabled: Story = {
  render: () => <div style={{ width: 220 }}><Select aria-label="Font" options={fonts} value="serif" onChange={() => undefined} disabled /></div>,
};
