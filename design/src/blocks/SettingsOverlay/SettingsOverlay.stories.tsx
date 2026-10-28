import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { ToastProvider } from '@/components/Toast';
import { DeviceFrame } from '@/components/DeviceFrame';
import { SettingsDialogs } from './SettingsDialogs';
import { SettingsPanel } from './SettingsPanel';
import { SettingsOverlay } from './SettingsOverlay';
import type { SettingsSectionId } from './settingsTypes';
import { useSettings } from './useSettings';
import { SyncSection } from './sections/SyncSection';
import { defaultSettings } from './settingsTypes';

function WiredInner({ variant, start, overlay = false }: { variant: 'tablet' | 'phone'; start: SettingsSectionId | null; overlay?: boolean }) {
  const s = useSettings();
  const [section, setSection] = useState<SettingsSectionId | null>(start);
  const [open, setOpen] = useState(true);
  const props = { variant, section, onSectionChange: setSection, settings: s } as const;
  const phone = variant === 'phone';
  return (
    <DeviceFrame tone="screen" shape={phone ? 'phone' : 'tablet'} width={phone ? 390 : 1040} height={phone ? 760 : 700}>
      <div style={phone ? { position: 'absolute', inset: 0 } : { position: 'absolute', inset: 24 }}>
        {overlay ? null : <SettingsPanel {...props} embedded={!phone} placement="container" />}
      </div>
      {overlay && <SettingsOverlay {...props} open={open} onClose={() => setOpen(false)} placement="container" />}
      <SettingsDialogs settings={s} placement="container" />
    </DeviceFrame>
  );
}

function Wired(props: { variant: 'tablet' | 'phone'; start: SettingsSectionId | null; overlay?: boolean }) {
  return (
    <div style={{ position: 'relative', width: 'fit-content', maxWidth: '100%' }}>
      <ToastProvider placement="container"><WiredInner {...props} /></ToastProvider>
    </div>
  );
}

const meta: Meta<typeof SettingsPanel> = {
  title: 'Blocks/SettingsOverlay',
  component: SettingsPanel,
  parameters: { layout: 'padded' },
  render: () => <Wired variant="tablet" start="general" />,
};
export default meta;
type Story = StoryObj<typeof SettingsPanel>;

/** Sections on the left, nothing under their names. Pick a style, a position or an edge and the same state drives every note screen. */
export const TabletGeneral: Story = {};
export const TabletAppearance: Story = { render: () => <Wired variant="tablet" start="appearance" /> };
export const TabletLanguage: Story = { render: () => <Wired variant="tablet" start="language" /> };
export const TabletPenAndInput: Story = { render: () => <Wired variant="tablet" start="pen" /> };
/** Content opens into its seven types. */
export const TabletContent: Story = { render: () => <Wired variant="tablet" start="content" /> };
/** 120 stickers from built in, your own and five packs. */
export const TabletStickers: Story = { render: () => <Wired variant="tablet" start="content/stickers" /> };
export const TabletCovers: Story = { render: () => <Wired variant="tablet" start="content/covers" /> };
export const TabletPens: Story = { render: () => <Wired variant="tablet" start="content/pens" /> };
export const TabletColourSets: Story = { render: () => <Wired variant="tablet" start="content/colours" /> };
export const TabletTemplates: Story = { render: () => <Wired variant="tablet" start="content/templates" /> };
export const TabletThemes: Story = { render: () => <Wired variant="tablet" start="content/themes" /> };
export const TabletPacks: Story = { render: () => <Wired variant="tablet" start="packs" /> };
export const TabletExtensions: Story = { render: () => <Wired variant="tablet" start="extensions" /> };
export const TabletSync: Story = { render: () => <Wired variant="tablet" start="sync" /> };
export const TabletAbout: Story = { render: () => <Wired variant="tablet" start="about" /> };
export const PhoneList: Story = { render: () => <Wired variant="phone" start={null} /> };
export const PhoneGeneral: Story = { render: () => <Wired variant="phone" start="general" /> };
export const PhonePacks: Story = { render: () => <Wired variant="phone" start="packs" /> };
export const PhoneStickers: Story = { render: () => <Wired variant="phone" start="content/stickers" /> };
/** The overlay as a centred sheet over the screen behind it. */
export const AsOverlay: Story = { render: () => <Wired variant="tablet" start="general" overlay /> };
export const AsPhoneOverlay: Story = { render: () => <Wired variant="phone" start={null} overlay /> };

/** Sync as one calm line: up to date, syncing, offline, failed. No chips. */
export const SyncStates: Story = {
  render: () => (
    <div style={{ display: 'grid', gap: 24, width: 560, background: 'var(--overlay)', padding: 24, borderRadius: 24 }}>
      {(['ok', 'syncing', 'offline', 'error'] as const).map((status) => (
        <SyncSection key={status} values={defaultSettings} onChange={() => undefined} status={status} last="2 minutes ago" />
      ))}
    </div>
  ),
};
