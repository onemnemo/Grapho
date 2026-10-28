import { useState } from 'react';
import { Icon } from '@/assets/icons';
import type { PackArtName } from '@/assets/packs';
import type { ContentItem } from '@/assets/content';
import { Button } from '@/components/Button';
import { Sheet } from '@/components/Sheet';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { cx } from '@/lib/cx';
import { PackDetailsStep } from './PackDetailsStep';
import { PackPicker } from './PackPicker';
import styles from './NewPackSheet.module.css';

export type NewPackStep = 'pick' | 'details' | 'done';

export interface NewPackSheetProps {
  open: boolean;
  onClose: () => void;
  /** Everything that can go into a pack. */
  items: ContentItem[];
  /** Items already picked, as when you arrive from Select on a Content page. */
  initialSelected?: string[];
  initialStep?: NewPackStep;
  initialName?: string;
  /** Called with the file name and the picked ids when Export is pressed. */
  onExport?: (file: string, ids: string[], options: { includeUsed: boolean; addName: boolean }) => void;
  placement?: OverlayPlacement;
}

const STEPS: Array<[NewPackStep, string]> = [['pick', 'Items'], ['details', 'Name and cover'], ['done', 'Export']];
const plural = (n: number) => `${n} item${n === 1 ? '' : 's'}`;

/** Make a pack: pick items across types, give it a name and a cover, then export a .graphopack. The count lives in the button. */
export function NewPackSheet({ open, onClose, items, initialSelected = [], initialStep = 'pick', initialName = 'My pack', onExport, placement = 'viewport' }: NewPackSheetProps) {
  const [step, setStep] = useState<NewPackStep>(initialStep);
  const [picked, setPicked] = useState(() => new Set(initialSelected));
  const [name, setName] = useState(initialName);
  const [cover, setCover] = useState<PackArtName>('amber');
  const [includeUsed, setIncludeUsed] = useState(true);
  const [addName, setAddName] = useState(false);
  const n = picked.size;
  const file = `${name.trim() || 'My pack'}.graphopack`;
  const at = STEPS.findIndex(([s]) => s === step);

  const footer =
    step === 'pick' ? (
      <>
        <Button variant="text" onClick={onClose}>Cancel</Button>
        <Button variant="primary" icon="arrow_right" disabled={!n} onClick={() => setStep('details')}>{n ? `Continue with ${plural(n)}` : 'Pick items'}</Button>
      </>
    ) : step === 'details' ? (
      <>
        <Button variant="text" onClick={() => setStep('pick')}>Back</Button>
        <Button variant="primary" icon="export" disabled={!n || !name.trim()} onClick={() => { onExport?.(file, [...picked], { includeUsed, addName }); setStep('done'); }}>{`Export ${plural(n)}`}</Button>
      </>
    ) : (
      <Button variant="primary" icon="check" onClick={onClose}>Done</Button>
    );

  return (
    <Sheet open={open} onClose={onClose} title="New pack" width={820} placement={placement} footer={footer}>
      <ol className={styles.steps} aria-label="Steps">
        {STEPS.map(([id, label], i) => (
          <li key={id} className={cx(i === at && styles.now, i < at && styles.past)} aria-current={i === at ? 'step' : undefined}>
            {i < at ? <Icon name="check" size={14} /> : null}{label}
          </li>
        ))}
      </ol>
      {step === 'pick' && <PackPicker items={items} picked={picked} onChange={setPicked} />}
      {step === 'details' && (
        <PackDetailsStep
          name={name} onName={setName} cover={cover} onCover={setCover} chosen={items.filter((i) => picked.has(i.id))}
          includeUsed={includeUsed} onIncludeUsed={setIncludeUsed} addName={addName} onAddName={setAddName}
        />
      )}
      {step === 'done' && (
        <div className={styles.done}>
          <span className={styles.tick}><Icon name="check" size={28} /></span>
          <h3>{file}</h3>
          <p>{plural(n)}</p>
        </div>
      )}
    </Sheet>
  );
}
