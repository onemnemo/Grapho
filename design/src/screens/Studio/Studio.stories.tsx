import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates } from '@/assets/studio';
import { Studio } from './Studio';

const meta: Meta<typeof Studio> = {
  title: 'Screens/Studio', component: Studio, parameters: { layout: 'centered' },
  render: (args) => <Studio key={JSON.stringify([args.tool, args.dialog, args.selectIndex, args.design.kind])} {...args} />,
};
export default meta;
type Story = StoryObj<typeof Studio>;

const T = buildTemplates();
const cover = () => { const d = structuredClone(T.covers[2]); d.name = 'Physics 2'; return d; };
const folder = () => { const d = structuredClone(T.folders[0]); d.name = 'Semester 2'; d.meta = 'Empty'; return d; };

export const Cover: Story = { args: { design: cover() } };
export const CoverLayerSelected: Story = { args: { design: cover(), selectIndex: 1 } };
export const Folder: Story = { args: { design: folder() } };
export const FolderLayerSelected: Story = { args: { design: folder(), selectIndex: 0 } };
export const Text: Story = { args: { design: cover(), tool: 'text' } };
export const Images: Story = { args: { design: cover(), tool: 'images' } };
export const Shapes: Story = { args: { design: cover(), tool: 'shapes' } };
export const Textures: Story = { args: { design: folder(), tool: 'textures' } };
export const PresetsAndLibrary: Story = { args: { design: cover(), dialog: 'presets' } };
export const ExportAsPack: Story = { args: { design: cover(), dialog: 'export' } };
export const ImportPack: Story = { args: { design: cover(), dialog: 'import' } };
export const AddPicture: Story = { args: { design: cover(), dialog: 'picture' } };
export const CropImage: Story = { args: { design: cover(), dialog: 'crop' } };
