import type { ReactNode } from 'react';
import { Button } from '../Button';
import type { IconName } from '@/assets/icons';
import { Dialog, type DialogProps } from './Dialog';

export interface ConfirmDialogProps extends Omit<DialogProps, 'actions' | 'onClose' | 'role' | 'children'> {
  /** The sentence under the title. */
  message?: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  /** danger turns the confirm button red for destructive answers. */
  tone?: 'default' | 'danger';
  confirmIcon?: IconName;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Ask once, with a clear answer. Cancel is the quiet button, confirm is the solid one. */
export function ConfirmDialog({
  message,
  confirmLabel,
  cancelLabel = 'Cancel',
  tone = 'default',
  confirmIcon,
  onConfirm,
  onCancel,
  ...rest
}: ConfirmDialogProps) {
  return (
    <Dialog
      role="alertdialog"
      onClose={onCancel}
      actions={
        <>
          <Button variant="text" onClick={onCancel}>{cancelLabel}</Button>
          <Button variant={tone === 'danger' ? 'danger' : 'primary'} icon={confirmIcon} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
      {...rest}
    >
      {message}
    </Dialog>
  );
}
