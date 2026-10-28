import { useMemo, type ReactNode } from 'react';
import { WheelNoteStage as NoteStage } from '@/stories-support/wheel/WheelNoteStage';
import { WheelModel, type ModelOptions } from '@/blocks/PenWheel/engine/model';
import { ToastProvider, useToast } from '@/components/Toast';
import { PanelModal } from '@/blocks/WheelPanel';

interface BodyProps {
  children: (p: { model: WheelModel; undoable: (text: string, undo: () => void) => void }) => ReactNode;
  model: WheelModel;
  modal: boolean;
}

function Body({ children, model, modal }: BodyProps) {
  const toasts = useToast();
  const body = children({ model, undoable: (text, undo) => toasts.show(text, { undo }) });
  return modal ? <PanelModal>{body}</PanelModal> : <div style={{ position: 'absolute', left: 16, top: 16 }}>{body}</div>;
}

/** Storybook frame for the wheel's panels: a note behind, a scrim, the panel centred, and an undo toast when it asks for one. */
export function PanelStage({ children, height = 700, modal = true, options }: {
  children: BodyProps['children'];
  height?: number;
  modal?: boolean;
  options?: ModelOptions;
}) {
  const model = useMemo(() => new WheelModel(options), [options]);
  return (
    <NoteStage width={640} height={height}>
      <ToastProvider placement="container" variant="pill">
        <Body model={model} modal={modal}>{children}</Body>
      </ToastProvider>
    </NoteStage>
  );
}
