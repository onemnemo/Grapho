import { LibraryRoot, type LibraryRootProps } from '../Library/LibraryRoot';

export type AllFoldersProps = Omit<LibraryRootProps, 'startOnFolders'>;

/** Every folder in one place: pinned first, then A to Z with the rail. Same header and heading as Home. */
export function AllFolders(props: AllFoldersProps) {
  return <LibraryRoot {...props} startOnFolders />;
}
