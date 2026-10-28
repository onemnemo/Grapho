import { ConfirmDialog } from '@/components/Dialog';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import type { PackItem } from '../PackRow';

export interface RemovePackDialogProps {
  open: boolean;
  pack: Pick<PackItem, 'name'>;
  placement?: OverlayPlacement;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Asks before removing a pack, and says in one sentence that notes keep what they already use. */
export function RemovePackDialog({ open, pack, placement = 'viewport', onConfirm, onCancel }: RemovePackDialogProps) {
  return (
    <ConfirmDialog
      open={open}
      title={`Remove ${pack.name}?`}
      message="Notes keep what they already use."
      confirmLabel="Remove"
      confirmIcon="trash"
      tone="danger"
      placement={placement}
      onConfirm={onConfirm}
      onCancel={onCancel}
    />
  );
}
