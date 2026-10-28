import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { CrispShape, InkLayer, SketchStroke } from '@/blocks/NotePage';
import { ShapeSuggest, shapeGlyphs, type ShapeSuggestion } from '@/blocks/ShapeSuggest';
import { Button } from '@/components/Button';
import { SegmentedControl } from '@/components/SegmentedControl';
import { useToast } from '@/components/Toast';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';
import { CLEAN, roughPoints, toPath, type SampleName } from './sketches';

const opt = (value: string, label: string, glyph: keyof typeof shapeGlyphs): ShapeSuggestion => ({ value, label, glyph: shapeGlyphs[glyph] });
const SAMPLES: Record<SampleName, { label: string; best: ShapeSuggestion; others: ShapeSuggestion[] }> = {
  circle: { label: 'Circle', best: opt('circle', 'Circle', 'circle'), others: [opt('ellipse', 'Ellipse', 'ellipse')] },
  rect: { label: 'Rectangle', best: opt('rect', 'Rectangle', 'rect'), others: [opt('rrect', 'Rounded', 'rrect')] },
  tri: { label: 'Triangle', best: opt('tri', 'Triangle', 'tri'), others: [opt('poly', 'Polygon', 'poly')] },
  arrow: { label: 'Arrow', best: opt('arrow', 'Arrow', 'arrow'), others: [opt('line', 'Line', 'line')] },
};
const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Draw a rough shape and a small chip appears under it: "Did you mean". The cross keeps the drawing; accepting replaces it, with Undo. */
export function SuggestScene({ settings, ...rest }: SceneProps) {
  const toast = useToast();
  const [sample, setSample] = useState<SampleName>('circle');
  const [run, setRun] = useState(0);
  const [phase, setPhase] = useState<'draw' | 'suggest' | 'done'>('draw');
  const [chosen, setChosen] = useState<string | null>(null);

  useEffect(() => {
    setPhase('draw');
    setChosen(null);
    const t = window.setTimeout(() => setPhase('suggest'), reduced() ? 50 : 1000);
    return () => window.clearTimeout(t);
  }, [sample, run]);

  const pts = useMemo(() => roughPoints(sample), [sample]);
  const box = useMemo(() => {
    const xs = pts.map((p) => p[0]), ys = pts.map((p) => p[1]);
    return { cx: (Math.min(...xs) + Math.max(...xs)) / 2, bottom: Math.max(...ys) };
  }, [pts]);
  const s = SAMPLES[sample];
  const scale = rest.width / 800;
  const accept = (value: string) => {
    const all = [s.best, ...s.others];
    setChosen(value);
    setPhase('done');
    toast.show(`Replaced with ${(all.find((o) => o.value === value)?.label ?? value).toLowerCase()}`, { undo: () => setChosen(null) });
  };

  const page: ReactNode = (
    <InkLayer width={800} height={400}>
      <text x="64" y="76" fontSize="38">Sketch the idea</text>
      <SketchStroke key={`${sample}-${run}`} d={toPath(pts)} animate faded={chosen !== null} />
      {chosen && <CrispShape>{CLEAN[chosen]}</CrispShape>}
    </InkLayer>
  );

  return (
    <NoteLayout
      {...rest}
      settings={settings.values}
      page={page}
      controls={
        <>
          <SegmentedControl size="sm" aria-label="Sample shape" value={sample} onChange={(v) => setSample(v as SampleName)} options={Object.entries(SAMPLES).map(([value, x]) => ({ value, label: x.label }))} />
          <Button size="sm" onClick={() => setRun(run + 1)}>Draw again</Button>
        </>
      }
      floating={
        phase === 'suggest' && (
          <ShapeSuggest key={`${sample}-${run}`} x={box.cx * scale} y={box.bottom * scale + 22} best={s.best} others={s.others} onChoose={accept} onDismiss={() => setPhase('done')} onExpire={() => setPhase('done')} />
        )
      }
    />
  );
}
