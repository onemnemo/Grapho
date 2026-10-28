import type { StorybookConfig } from '@storybook/react-vite';

// Stories live next to their component and are discovered by these globs.
// Adding a component folder with a .stories.tsx file is all it takes to list it.
const config: StorybookConfig = {
  stories: ['../src/**/*.mdx', '../src/**/*.stories.@(ts|tsx)'],
  addons: ['@storybook/addon-docs'],
  framework: { name: '@storybook/react-vite', options: {} },
  core: { disableTelemetry: true },
};
export default config;
