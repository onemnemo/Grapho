import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { iconPaths, type IconName } from '@/assets/icons';
import { Banner, type BannerProps } from './Banner';

const meta: Meta<typeof Banner> = {
  title: 'Components/Banner',
  component: Banner,
  parameters: { layout: 'padded' },
  args: {
    title: 'You are offline',
    children: 'Changes are kept on this device and sync when you reconnect.',
    icon: 'cloud',
    tone: 'neutral',
    actionLabel: 'Details',
    closeLabel: 'Dismiss',
  },
  argTypes: {
    tone: { control: 'inline-radio', options: ['neutral', 'ok', 'warn', 'danger'] },
    icon: { control: 'select', options: Object.keys(iconPaths) as IconName[] },
    onAction: { control: false },
    onClose: { control: false },
  },
  decorators: [(Story) => <div style={{ maxWidth: 760 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Banner>;

export const Playground: Story = { args: { onClose: () => {} } };

export const Neutral: Story = { args: { onClose: () => {} } };
export const Danger: Story = {
  args: { tone: 'danger', icon: 'alert', title: 'Sync failed', children: 'Two notes could not be uploaded. Try again.', actionLabel: 'Try again', onClose: () => {} },
};
export const Ok: Story = {
  args: { tone: 'ok', icon: 'check', title: 'Back online', children: 'Everything is up to date.', actionLabel: undefined, onClose: () => {} },
};
export const Warn: Story = {
  args: { tone: 'warn', icon: 'alert', title: 'Storage is almost full', children: 'Free some space to keep syncing.', actionLabel: 'Manage', onClose: () => {} },
};
export const TitleOnly: Story = { args: { children: undefined, actionLabel: undefined, title: 'Trash empties itself after 30 days' } };

/** It sits in the page flow and pushes the content down. Close it and the content moves back up. */
export const InPageFlow: Story = {
  render: (args) => <InFlow {...args} />,
};
function InFlow(args: BannerProps) {
  const [open, setOpen] = useState(true);
  return (
    <div>
      <h2 style={{ margin: '0 0 var(--sp-4)', font: 'var(--t-display)', letterSpacing: 'var(--track-display)' }}>Library</h2>
      {open && <Banner {...args} onClose={() => setOpen(false)} style={{ marginBottom: 26 }} />}
      <div style={{ height: 120, borderRadius: 'var(--r-md)', background: 'var(--s2)' }} />
    </div>
  );
}
