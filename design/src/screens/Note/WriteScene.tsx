import { useState } from 'react';
import { SampleInk, StrokeLayer } from '@/blocks/NotePage';
import { useToast } from '@/components/Toast';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';
import styles from './Note.module.css';
import { useAutoHide } from './useAutoHide';

/** Writing on the page. Pick a tool, write with a mouse or pen, and watch auto-hide at work. */
export function WriteScene({ settings, blocked, ...rest }: SceneProps) {
  const toast = useToast();
  const v = settings.values;
  const [tool, setTool] = useState('pen');
  const [strokes, setStrokes] = useState<string[]>([]);
  const auto = useAutoHide(v.autoHide, v.barPosition, v.barStyle);
  return (
    <NoteLayout
      {...rest}
      settings={v}
      stageRef={auto.host}
      bind={auto.bind}
      hidden={auto.hidden}
      handle={auto.handle}
      hint={v.autoHide === 'off' ? undefined : auto.hint}
      bar={{ activeTool: tool, onTool: setTool }}
      page={
        <>
          <SampleInk />
          {blocked && (
            <div className={styles.blocked} onPointerDown={() => toast.show(blocked === 'hidden' ? 'This layer is hidden' : 'This layer is locked', { kind: 'info', duration: 1600 })} />
          )}
          <StrokeLayer strokes={strokes} onStart={auto.strokeStart} onCommit={(d) => { setStrokes((s) => [...s, d]); auto.strokeEnd(); }} />
        </>
      }
    />
  );
}
