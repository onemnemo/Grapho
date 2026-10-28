import { LibraryRoot, type LibraryRootProps } from '../Library/LibraryRoot';

export interface FolderViewProps extends Omit<LibraryRootProps, 'path' | 'startOnFolders'> {
  /** The folders you are inside, outermost first. The last one is the title. */
  path: string[];
}

/** Inside a folder: the path is the title and every part is a target. Subfolders first, then the notes. */
export function FolderView({ path, ...rest }: FolderViewProps) {
  return <LibraryRoot {...rest} path={path} />;
}
