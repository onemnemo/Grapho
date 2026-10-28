import type { Meta, StoryObj } from '@storybook/react-vite';
import { Note } from './Note';

/** Stand-in for the pen wheel. The real PenWheel fills its whole page and docks itself, while this layout reserves a 96px slot on the edge, so the slot keeps a flat ring. */
const WheelPlaceholder = () => (
  <div style={{ width: '100%', height: '100%', borderRadius: '50%', background: 'var(--wheel)', boxShadow: 'inset 0 0 0 1px var(--line2), var(--e2)', display: 'grid', placeItems: 'center', position: 'relative' }}>
    <div style={{ position: 'absolute', inset: 14, borderRadius: '50%', background: 'var(--wheel-well)' }} />
    <span style={{ position: 'relative', color: 'var(--dim)', font: 'var(--t-micro)' }}>Wheel</span>
  </div>
);

const meta: Meta<typeof Note> = {
  title: 'Screens/Note',
  component: Note,
  parameters: { layout: 'padded' },
  args: { scene: 'write', initial: { barStyle: 'pill', barPosition: 'top', autoHide: 'off', wheelEdge: 'right' }, wheel: <WheelPlaceholder /> },
  argTypes: { wheel: { control: false }, initial: { control: 'object' } },
  render: (args) => <Note key={JSON.stringify(args.initial) + args.scene} {...args} />,
};
export default meta;
type Story = StoryObj<typeof Note>;

/** The floating bar is the default. Tap More, then Settings, and the bar follows what you change. */
export const FloatingBar: Story = {};
export const FullLine: Story = { args: { initial: { barStyle: 'line', barPosition: 'top', autoHide: 'off', wheelEdge: 'right' } } };
export const Bottom: Story = { args: { initial: { barStyle: 'pill', barPosition: 'bottom', wheelEdge: 'right' } } };
export const LeftBar: Story = { args: { initial: { barStyle: 'pill', barPosition: 'left', wheelEdge: 'right' } } };
export const RightBarLine: Story = { args: { initial: { barStyle: 'line', barPosition: 'right', wheelEdge: 'right' } } };

/** While writing: write on the page. The bar leaves 600ms after the pen lifts and a faint handle shows. Move to the edge to bring it back. */
export const AutoHideWhileWriting: Story = { args: { initial: { barStyle: 'pill', barPosition: 'top', autoHide: 'while', wheelEdge: 'right' } } };
/** Always: starts hidden, with the handle showing for a moment. */
export const AutoHideAlways: Story = { args: { initial: { barStyle: 'pill', barPosition: 'top', autoHide: 'always', wheelEdge: 'right' } } };

export const SettingsOpen: Story = { args: { settingsOpen: 'general' } };
export const SettingsOpenOnPacks: Story = { args: { settingsOpen: 'packs' } };
export const Phone: Story = { args: { width: 390, height: 760, initial: { barStyle: 'pill', barPosition: 'bottom', wheelEdge: 'left' } } };

/** A selection: the floating bar with five actions, and the toolbar turned into a selection bar. */
export const LassoSelection: Story = { args: { scene: 'lasso' } };
export const LassoSelectionLine: Story = { args: { scene: 'lasso', initial: { barStyle: 'line', barPosition: 'top' } } };
/** A selected shape: the Shape button opens its options. */
export const ShapeOptions: Story = { args: { scene: 'shape' } };
/** A line or arrow: path, endings, outline, width. Swap exchanges the ends. */
export const LineOptions: Story = { args: { scene: 'line' } };
/** Draw a rough shape: "Did you mean" counts down for 5s. */
export const ShapeSuggestion: Story = { args: { scene: 'suggest' } };

/** A picture selected. The floating bar names the three picture actions; More holds Replace image and the arrange items. Try each one. */
export const ImageSelected: Story = { args: { scene: 'image' } };
/** Crop mode on the page: eight handles, a thirds grid, Reset and Cancel quiet, Done the one solid button. */
export const ImageCrop: Story = { args: { scene: 'image', start: 'crop' } };
/** Removing background: a veil, one line sweeping the picture, a pill with Cancel. */
export const ImageRemovingBackground: Story = { args: { scene: 'image', start: 'removing' } };
export const ImageConvertingToInk: Story = { args: { scene: 'image', start: 'converting' } };
/** Starts set to "Job fails": run Remove background or Convert to ink to see the failure toast. */
export const ImageJobFails: Story = { args: { scene: 'image', fail: true } };

/** The ••• menu with everything that acts on the note: Insert, page background, Ruler, Protractor, Draw with finger, Customise wheel. */
export const MoreMenu: Story = { args: { open: 'overflow', width: 1040, height: 640 } };
export const InsertMenuOpen: Story = { args: { open: 'insert', scene: 'lasso' } };
/** Insert runs and ends in a message. Choose Insert, then Insert PDF pages. */
export const InsertFails: Story = { args: { open: 'insert', scene: 'lasso', fail: true } };
export const PageBackgroundOpen: Story = { args: { open: 'background', scene: 'lasso', background: { pattern: 'lined', paper: 'cream' } } };
export const ExportOpen: Story = { args: { open: 'export', scene: 'lasso' } };
export const ExportInfiniteCanvas: Story = { args: { open: 'export', scene: 'lasso', infinite: true } };
export const ExportRunning: Story = { args: { open: 'export', scene: 'lasso', exporting: true } };

/** Layers open as a popover from the bar. Drag a grip to reorder; the dots hold Rename, Opacity, Merge down and Delete. */
export const LayersPanelOpen: Story = { args: { open: 'layers', scene: 'write', width: 1040, height: 640 } };
/** The active layer is hidden: drawing says so instead of doing nothing. Tap the page. */
export const DrawingOnHiddenLayer: Story = {
  args: {
    open: 'layers', scene: 'write', width: 1040, height: 640,
    layers: [{ id: 'l2', name: 'Diagram', visible: true, locked: false, opacity: 1 }, { id: 'l1', name: 'Layer 1', visible: false, locked: false, opacity: 1 }],
    activeLayer: 'l1',
  },
};
/** Ruler and Protractor, on the page. Turn them on and off from the ••• menu. */
export const RulerAndProtractor: Story = { args: { guides: { ruler: true, protractor: true, reading: 62 }, scene: 'write', width: 1040, height: 640 } };
