import { DesignSheet, type DesignSheetProps } from '../DesignSheet';

export type FolderSheetProps = Omit<DesignSheetProps, 'kind'>;

/** New folder and Edit folder: one sheet. Pick a preset, or set the colour, icon and finish yourself, and watch the folder change. */
export function FolderSheet(props: FolderSheetProps) {
  return <DesignSheet kind="folder" {...props} />;
}
