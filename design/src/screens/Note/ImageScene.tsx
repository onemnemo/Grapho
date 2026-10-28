import { useEffect, useRef, useState } from 'react';
import { ILLUS } from '@/assets/studio';
import { CropBar, CropFrame, FULL_CROP, type CropRect } from '@/blocks/CropMode';
import { LassoBar, PasteChip, lassoImageActions } from '@/blocks/LassoBar';
import { InkLayer, SelectionOutline } from '@/blocks/NotePage';
import { PictureWorking, type PictureJob } from '@/blocks/PictureWorking';
import { SelectionMenu } from '@/blocks/SelectionMenu';
import { Button } from '@/components/Button';
import { SegmentedControl } from '@/components/SegmentedControl';
import { useToast } from '@/components/Toast';
import { cutoutURL } from '@/lib/studio';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';
import styles from './Note.module.css';

const BOX = { x: 470, y: 164, width: 260, height: 210 };
const WORK_MS = 1900;
type Phase = 'selected' | 'crop' | 'removing' | 'converting' | 'idle';
type Look = 'original' | 'cutout' | 'ink';

/** The mug from the picture, traced as ink. The strokes draw themselves in when Convert to ink finishes. */
function MugInk() {
  return (
    <InkLayer>
      <g transform={`translate(${BOX.x} ${BOX.y}) scale(${BOX.width / 420})`} fill="none" strokeLinecap="round" strokeLinejoin="round" stroke="currentColor" strokeWidth="6">
        <path d="M118 130h174v92a64 64 0 0 1-64 64h-46a64 64 0 0 1-64-64z" />
        <path d="M296 152h20a34 34 0 0 1 0 68h-10" />
        <ellipse cx="205" cy="290" rx="116" ry="17" />
        <path d="M168 108c-16-14 14-24 0-44M206 108c-16-14 14-24 0-44M244 108c-16-14 14-24 0-44" />
        <path d="M120 196h170" />
      </g>
    </InkLayer>
  );
}

/**
 * A picture on the page. Tap Crop, Remove background or Convert to ink on the floating bar and watch the working state; the controls above
 * the frame make the next job fail so the toast can be seen. More holds Replace image and the arrange items.
 */
export function ImageScene({ settings, start, fail, ...rest }: SceneProps) {
  const toast = useToast();
  const [phase, setPhase] = useState<Phase>(start ?? 'selected');
  const [look, setLook] = useState<Look>('original');
  const [cut, setCut] = useState<string>(ILLUS);
  const [crop, setCrop] = useState<CropRect>(start === 'crop' ? { l: 0.14, t: 0.08, r: 0.86, b: 0.92 } : FULL_CROP);
  const [cropped, setCropped] = useState<CropRect>(FULL_CROP);
  const [gone, setGone] = useState(false);
  const [outcome, setOutcome] = useState<'works' | 'fails'>(fail ? 'fails' : 'works');
  const [paste, setPaste] = useState(false);
  const [menu, setMenu] = useState(false);
  const anchor = useRef<HTMLElement | null>(null);
  const timer = useRef(0);

  useEffect(() => { cutoutURL(ILLUS).then((c) => setCut(c.url)); return () => window.clearTimeout(timer.current); }, []);

  const work = (job: PictureJob) => {
    setPhase(job);
    timer.current = window.setTimeout(() => {
      if (outcome === 'fails') {
        toast.show(job === 'removing' ? "Couldn't clean up that picture." : "Couldn't find a drawing in this picture.", { kind: 'error', duration: 2600 });
        setPhase('selected');
        return;
      }
      setLook(job === 'removing' ? 'cutout' : 'ink');
      setPhase('selected');
    }, WORK_MS);
  };
  const cancel = () => { window.clearTimeout(timer.current); setPhase('selected'); };
  const reset = () => { cancel(); setLook('original'); setCropped(FULL_CROP); setCrop(FULL_CROP); setGone(false); setPaste(false); };

  const action = (id: string, e?: { currentTarget: HTMLElement }) => {
    if (id === 'crop') { setCrop(cropped); setPhase('crop'); }
    else if (id === 'removeBackground') work('removing');
    else if (id === 'convertToInk') work('converting');
    else if (id === 'more' && e) { anchor.current = e.currentTarget; setMenu(true); }
    else if (id === 'delete') { setGone(true); toast.deleted('picture', () => setGone(false)); }
    else toast.show({ copy: 'Copied', duplicate: 'Duplicated' }[id] ?? id, { kind: 'ok', duration: 1600 });
  };
  const menuAction = (a: string) => {
    if (a === 'delete') action('delete');
    else if (a === 'replace') toast.show('Choose a picture to swap in', { kind: 'info', duration: 1800 });
    else if (a === 'lock') toast.show('Locked. Unlock from the menu.', { kind: 'info', duration: 1800 });
    else toast.show(a === 'cut' ? 'Cut' : 'Done', { kind: 'ok', duration: 1400 });
  };

  const showing = !gone && phase !== 'idle';
  const cropping = phase === 'crop';
  const keep = cropping ? FULL_CROP : cropped;
  const box = { x: BOX.x + keep.l * BOX.width, y: BOX.y + keep.t * BOX.height, width: (keep.r - keep.l) * BOX.width, height: (keep.b - keep.t) * BOX.height };

  return (
    <NoteLayout
      {...rest}
      settings={settings.values}
      hint={gone ? 'Tap the page to paste' : undefined}
      bar={{ activeTool: 'lasso' }}
      controls={
        <>
          <SegmentedControl size="sm" aria-label="Next job" value={outcome} onChange={(v) => setOutcome(v as 'works' | 'fails')} options={[{ value: 'works', label: 'Job works' }, { value: 'fails', label: 'Job fails' }]} />
          <Button size="sm" onClick={() => setPaste(!paste)}>Paste chip</Button>
          <Button size="sm" onClick={reset}>Reset</Button>
        </>
      }
      page={
        <>
          <InkLayer>
            <text x="64" y="120" fontSize="46">Mornings</text>
            <text x="64" y="190" fontSize="30">1  coffee, no sugar</text>
            <text x="64" y="240" fontSize="30">2  ten minutes of reading</text>
            <text x="64" y="290" fontSize="30">3  plan the day</text>
          </InkLayer>
          {look !== 'ink' && !gone && (
            <div className={styles.picture} style={{ left: box.x, top: box.y, width: box.width, height: box.height }}>
              <img src={look === 'cutout' ? cut : ILLUS} alt="" draggable={false} style={{ width: BOX.width, height: BOX.height, left: -keep.l * BOX.width, top: -keep.t * BOX.height }} />
            </div>
          )}
          {look === 'ink' && !gone && phase !== 'converting' && <MugInk />}
          {showing && !cropping && (
            <InkLayer><SelectionOutline x={box.x} y={box.y} width={box.width} height={box.height} fill={false} /></InkLayer>
          )}
        </>
      }
      floating={
        <>
          {showing && !cropping && phase === 'selected' && <LassoBar x={box.x + box.width / 2} y={box.y - 56} items={lassoImageActions} onAction={action} />}
          {(phase === 'removing' || phase === 'converting') && <PictureWorking job={phase} box={box} onCancel={cancel} />}
          {cropping && (
            <>
              <CropFrame box={BOX} value={crop} onChange={setCrop} />
              <CropBar x={BOX.x + BOX.width / 2} y={BOX.y + BOX.height + 14} onReset={() => setCrop(FULL_CROP)} onCancel={() => setPhase('selected')} onDone={() => { setCropped(crop); setPhase('selected'); }} />
            </>
          )}
          {paste && <PasteChip x={300} y={330} onPaste={() => { setPaste(false); setGone(false); toast.show('Pasted', { kind: 'ok', duration: 1400 }); }} />}
        </>
      }
      overlays={
        <>
          {rest.overlays}
          <SelectionMenu open={menu} onClose={() => setMenu(false)} anchorRef={anchor} placement="bottom-end" kind="image" canPaste onAction={menuAction} />
        </>
      }
    />
  );
}
