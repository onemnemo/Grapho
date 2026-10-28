import type { Meta, StoryObj } from '@storybook/react-vite';
import { Settings } from './Settings';

const meta: Meta<typeof Settings> = {
  title: 'Screens/Settings',
  component: Settings,
  parameters: { layout: 'padded' },
  args: { variant: 'tablet', section: 'general' },
  argTypes: {
    variant: { control: 'inline-radio', options: ['tablet', 'phone'] },
    section: {
      control: 'select',
      options: [null, 'general', 'appearance', 'language', 'pen', 'content', 'content/pens', 'content/colours', 'content/covers', 'content/folders', 'content/stickers', 'content/templates', 'content/themes', 'packs', 'extensions', 'sync', 'about'],
    },
  },
  render: (args) => <Settings key={`${args.variant}-${args.section}`} {...args} />,
};
export default meta;
type Story = StoryObj<typeof Settings>;

/** Pick a style and a position: the tiles redraw to match. */
export const TabletGeneral: Story = {};
/** Pick a theme and the whole story repaints; Customise opens the four colours. */
export const TabletAppearance: Story = { args: { section: 'appearance' } };
/** Pick a language and the labels that have a translation switch at once. */
export const TabletLanguage: Story = { args: { section: 'language' } };
export const TabletPenAndInput: Story = { args: { section: 'pen' } };
/** Content opens into one page per type, with a source filter, Sort and Select. */
export const TabletStickers: Story = { args: { section: 'content/stickers' } };
export const TabletCovers: Story = { args: { section: 'content/covers' } };
/** Packs are sources: switch off, open for contents by type, New pack, import. */
export const TabletPacks: Story = { args: { section: 'packs' } };
export const TabletExtensions: Story = { args: { section: 'extensions' } };
export const TabletSync: Story = { args: { section: 'sync' } };
export const TabletAbout: Story = { args: { section: 'about' } };

export const PhoneList: Story = { args: { variant: 'phone', section: null } };
export const PhoneGeneral: Story = { args: { variant: 'phone', section: 'general' } };
export const PhoneAppearance: Story = { args: { variant: 'phone', section: 'appearance' } };
export const PhoneLanguage: Story = { args: { variant: 'phone', section: 'language' } };
export const PhonePacks: Story = { args: { variant: 'phone', section: 'packs' } };
export const PhoneStickers: Story = { args: { variant: 'phone', section: 'content/stickers' } };
