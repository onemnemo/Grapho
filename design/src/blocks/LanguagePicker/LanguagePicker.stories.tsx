import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { LANGUAGES, LocaleProvider, type LocaleCode } from '@/i18n';
import { LanguagePicker } from './LanguagePicker';

const meta: Meta<typeof LanguagePicker> = {
  title: 'Blocks/LanguagePicker',
  component: LanguagePicker,
  parameters: { layout: 'centered' },
  args: { searchAbove: 12 },
  argTypes: { searchAbove: { control: { type: 'number', min: 0, max: 60 } } },
  decorators: [
    (Story) => (
      <div style={{ width: 360, height: 620, background: 'var(--s1)', borderRadius: 'var(--r-lg)', padding: 'var(--sp-4)' }}>
        <Story />
      </div>
    ),
  ],
};
export default meta;
type Story = StoryObj<typeof LanguagePicker>;

const EIGHT = LANGUAGES.slice(0, 8);

/** The eight first languages. Two are set on the device, so they sit at the top. No search field. */
export const EightLanguages: Story = {
  args: { languages: EIGHT, deviceLanguage: 'nb', defaultValue: 'nb' },
};

/** Forty languages: the search field appears and the list scrolls inside its height. */
export const FortyLanguages: Story = {
  args: { languages: LANGUAGES, deviceLanguage: 'nb', defaultValue: 'hi' },
};

/** Choosing a language here changes the language of the whole story (this label and the search text). */
export const Live: Story = {
  args: { languages: EIGHT.concat(LANGUAGES.slice(8, 20)), deviceLanguage: 'en' },
  render: (args) => {
    const [code, setCode] = useState('en');
    const has = ['en', 'nb', 'fr', 'de', 'es', 'hi', 'it', 'sv'].includes(code);
    return (
      <LocaleProvider locale={(has ? code : 'en') as LocaleCode}>
        <LanguagePicker {...args} value={code} onChange={setCode} />
      </LocaleProvider>
    );
  },
};

/** No device language: one plain list. */
export const NothingPinned: Story = {
  args: { languages: EIGHT, defaultValue: 'en' },
};
