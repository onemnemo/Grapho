import type { ComponentType } from 'react';

/** Storybook decorator: gives a full screen the whole height of the preview window. */
export const fullHeight = (Story: ComponentType) => (
  <div style={{ height: '100vh', minHeight: 560 }}>
    <Story />
  </div>
);
