import { DesignSheet, type DesignSheetProps } from '../DesignSheet';

export type NotebookSheetProps = Omit<DesignSheetProps, 'kind'>;

/** New notebook and Edit notebook: one sheet. Pick a preset, or set the colour, texture and icon yourself, and watch the cover change. */
export function NotebookSheet(props: NotebookSheetProps) {
  return <DesignSheet kind="cover" {...props} />;
}
