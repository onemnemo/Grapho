import { LibraryRoot, type LibraryRootProps } from './LibraryRoot';

export type LibraryProps = LibraryRootProps;

/**
 * The library home: header, a row of folders, and Recent below. Search, filter, sort, select mode with its bar,
 * delete with Undo and the inline notice all work. Needs no provider above it.
 */
export function Library(props: LibraryProps) {
  return <LibraryRoot {...props} />;
}
