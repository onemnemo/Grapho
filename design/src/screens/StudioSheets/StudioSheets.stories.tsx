import type { Meta, StoryObj } from '@storybook/react-vite';
import { StudioSheets } from './StudioSheets';

const meta: Meta<typeof StudioSheets> = { title: 'Screens/StudioSheets', component: StudioSheets, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof StudioSheets>;

export const NewFolder: Story = { args: { sheet: 'folder', mode: 'new' } };
export const EditFolderIcon: Story = { args: { sheet: 'folder', mode: 'edit', tab: 'icon' } };
export const NewNotebook: Story = { args: { sheet: 'notebook', mode: 'new' } };
export const EditNotebookColour: Story = { args: { sheet: 'notebook', mode: 'edit', tab: 'colour' } };
export const NewCanvas: Story = { args: { sheet: 'canvas', mode: 'new' } };
export const EditCanvasSize: Story = { args: { sheet: 'canvas', mode: 'edit', tab: 'size' } };
