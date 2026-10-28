import type { Meta, StoryObj } from '@storybook/react-vite';
import { Logo } from './Logo';

const meta: Meta<typeof Logo> = {
  title: 'Assets/Logo',
  component: Logo,
  args: { variant: 'mark-on-dark', height: 64 },
};
export default meta;

export const Playground: StoryObj<typeof Logo> = {};
export const AppIcon: StoryObj<typeof Logo> = { args: { variant: 'app-icon', height: 128 } };
