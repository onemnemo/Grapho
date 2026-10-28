import { Button } from '../Button';
import { Dialog, type DialogProps } from './Dialog';

export interface UnsavedChangesDialogProps extends Omit<DialogProps, 'title' | 'actions' | 'onClose' | 'role' | 'children'> {
  /** The name of the thing being closed. When empty the sentence says "this" and the kind. */
  name: string;
  /** What kind of thing it is, for the sentence: notebook, cover, folder, canvas. */
  kind?: string;
  onSave: () => void;
  onDiscard: () => void;
  onKeepEditing: () => void;
}

/** "Save changes to Sunday sketches?" with Save, Discard and Keep editing. Escape keeps editing. */
export function UnsavedChangesDialog({ name, kind = 'item', onSave, onDiscard, onKeepEditing, ...rest }: UnsavedChangesDialogProps) {
  return (
    <Dialog
      role="alertdialog"
      title={`Save changes to ${name || `this ${kind}`}?`}
      onClose={onKeepEditing}
      actions={
        <>
          <Button variant="text" onClick={onKeepEditing}>Keep editing</Button>
          <Button onClick={onDiscard}>Discard</Button>
          <Button variant="primary" onClick={onSave}>Save</Button>
        </>
      }
      {...rest}
    >
      Your changes to this {kind} are not saved yet.
    </Dialog>
  );
}
