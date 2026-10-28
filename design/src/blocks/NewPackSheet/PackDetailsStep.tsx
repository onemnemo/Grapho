import { Icon } from '@/assets/icons';
import { packArt, type PackArtName } from '@/assets/packs';
import { contentTypes, type ContentItem } from '@/assets/content';
import { Checkbox } from '@/components/Checkbox';
import { TextField } from '@/components/TextField';
import { cx } from '@/lib/cx';
import { PackCover } from '../PackRow';
import styles from './PackDetailsStep.module.css';

export const coverChoices: PackArtName[] = ['amber', 'forest', 'blush', 'azure', 'graphite', 'midnight', 'tabs', 'dusk'];

export interface PackDetailsStepProps {
  name: string;
  onName: (name: string) => void;
  cover: PackArtName;
  onCover: (cover: PackArtName) => void;
  chosen: ContentItem[];
  includeUsed: boolean;
  onIncludeUsed: (v: boolean) => void;
  addName: boolean;
  onAddName: (v: boolean) => void;
}

/** Step two: a name, a cover, and what the file will hold by type. */
export function PackDetailsStep({ name, onName, cover, onCover, chosen, includeUsed, onIncludeUsed, addName, onAddName }: PackDetailsStepProps) {
  const by = contentTypes.map((t) => [t, chosen.filter((i) => i.type === t.id).length] as const).filter(([, n]) => n);
  return (
    <div className={styles.step}>
      <div className={styles.top}>
        <PackCover art={packArt[cover]} size={112} cell={7} />
        <div className={styles.fields}>
          <TextField variant="stacked" label="Pack name" value={name} maxLength={40} spellCheck={false} onChange={(e) => onName(e.target.value)} />
          <ul className={styles.types} aria-label="Included">
            {by.map(([t, n]) => <li key={t.id}><Icon name={t.icon} size={16} />{n}<span className={styles.sr}> {t.label}</span></li>)}
          </ul>
        </div>
      </div>
      <section>
        <h4>Cover</h4>
        <div className={styles.covers} role="radiogroup" aria-label="Cover">
          {coverChoices.map((c) => (
            <button key={c} type="button" role="radio" aria-checked={cover === c} aria-label={c} className={cx(styles.choice)} onClick={() => onCover(c)}>
              <PackCover art={packArt[c]} size={48} cell={3} />
            </button>
          ))}
        </div>
      </section>
      <div className={styles.opts}>
        <Checkbox checked={includeUsed} onChange={onIncludeUsed} label="Include what the items use" />
        <Checkbox checked={addName} onChange={onAddName} label="Add my name" />
      </div>
    </div>
  );
}
