import { useEffect } from 'react';
import type { Preview } from '@storybook/react-vite';
import '../src/styles/global.css';
import { LocaleProvider, type LocaleCode } from '../src/i18n';
import { applyTheme, DEFAULT_THEME } from '../src/lib/theme';
import { DEFAULT_CUSTOM, type ThemeId } from '../src/assets/themes';

function Shell({ theme, locale, children }: { theme: ThemeId; locale: LocaleCode; children: React.ReactNode }) {
  useEffect(() => {
    applyTheme({ id: theme, custom: DEFAULT_CUSTOM });
    document.documentElement.lang = locale === 'xx' ? 'en' : locale;
  }, [theme, locale]);
  return <LocaleProvider locale={locale}>{children}</LocaleProvider>;
}

const preview: Preview = {
  globalTypes: {
    theme: {
      description: 'Colour theme',
      toolbar: {
        title: 'Theme',
        icon: 'paintbrush',
        dynamicTitle: true,
        items: [
          { value: 'ink', title: 'Ink (default)' },
          { value: 'graphite', title: 'Graphite' },
          { value: 'paper', title: 'Paper' },
          { value: 'custom', title: 'Custom (blue accent)' },
        ],
      },
    },
    locale: {
      description: 'Language',
      toolbar: {
        title: 'Language',
        icon: 'globe',
        dynamicTitle: true,
        items: [
          { value: 'en', title: 'English' },
          { value: 'xx', title: 'Pseudo (xx, +35%)' },
          { value: 'nb', title: 'Norsk bokmål' },
          { value: 'fr', title: 'Français' },
          { value: 'de', title: 'Deutsch' },
          { value: 'es', title: 'Español' },
          { value: 'hi', title: 'हिन्दी' },
          { value: 'it', title: 'Italiano' },
          { value: 'sv', title: 'Svenska' },
        ],
      },
    },
  },
  initialGlobals: { theme: DEFAULT_THEME.id, locale: 'en' },
  decorators: [
    (Story, context) => (
      <Shell theme={(context.globals.theme ?? 'ink') as ThemeId} locale={(context.globals.locale ?? 'en') as LocaleCode}>
        <Story />
      </Shell>
    ),
  ],
  parameters: {
    layout: 'centered',
    backgrounds: { disable: true },
    controls: { expanded: true, matchers: { color: /(background|color)$/i } },
    options: {
      storySort: { order: ['Introduction', 'Foundations', 'Assets', 'Components', 'Blocks', 'Screens'] },
    },
  },
  tags: ['autodocs'],
};
export default preview;
