import { useRef, type HTMLAttributes } from 'react';
import { THEME_IDS, THEME_INPUTS, type BuiltInThemeId, type ThemeInputs } from '@/assets/themes';
import { Button } from '@/components/Button';
import { ColourDisc } from '@/components/ColourDisc';
import { Swatch } from '@/components/Swatch';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import type { ThemeChoice } from '@/lib/theme';
import { useT } from '@/i18n';
import { ThemePreview, ThemeTile } from '../ThemeTile';
import styles from './ThemeEditor.module.css';

export interface ThemeEditorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  value?: ThemeChoice;
  defaultValue?: ThemeChoice;
  onChange?: (choice: ThemeChoice) => void;
}

const FIELDS: { key: keyof ThemeInputs; label: 'theme.accent' | 'theme.background' | 'theme.surface' | 'theme.page' }[] = [
  { key: 'bg', label: 'theme.background' },
  { key: 'surface', label: 'theme.surface' },
  { key: 'paper', label: 'theme.page' },
  { key: 'accent', label: 'theme.accent' },
];

/** Pick a theme from tiles that show the app in it, then make it your own: four colours, everything else follows. */
export function ThemeEditor({ value, defaultValue, onChange, className, ...rest }: ThemeEditorProps) {
  const t = useT();
  const [choice, setChoice] = useControllable(value, defaultValue ?? { id: 'ink', custom: THEME_INPUTS.ink }, onChange);
  const [field, setField] = useControllable<keyof ThemeInputs>(undefined, 'bg');
  const base = useRef<ThemeInputs>(THEME_INPUTS.ink);
  const custom = choice.id === 'custom';

  const pick = (id: ThemeChoice['id']) => setChoice({ ...choice, id });
  const customise = () => {
    const from = choice.id === 'custom' ? choice.custom : THEME_INPUTS[choice.id as BuiltInThemeId];
    base.current = from;
    setChoice({ id: 'custom', custom: from });
  };
  const edit = (hex: string) => setChoice({ id: 'custom', custom: { ...choice.custom, [field]: hex } });

  return (
    <div className={cx(styles.editor, className)} {...rest}>
      <div className={styles.tiles} role="radiogroup" aria-label={t('theme.title')}>
        {THEME_IDS.map((id) => (
          <ThemeTile key={id} theme={id} inputs={choice.custom} selected={choice.id === id} onClick={() => pick(id)} />
        ))}
      </div>

      {custom ? (
        <div className={styles.custom}>
          <ThemePreview className={styles.live} theme="custom" inputs={choice.custom} aria-label={t('theme.preview')} />
          <div className={styles.tools}>
            <div className={styles.fields} role="radiogroup" aria-label={t('theme.customise')}>
              {FIELDS.map((f) => (
                <div key={f.key} className={styles.field}>
                  <Swatch size="lg" color={choice.custom[f.key]} label={t(f.label)} selected={field === f.key} onClick={() => setField(f.key)} />
                  <span className={styles.name}>{t(f.label)}</span>
                </div>
              ))}
            </div>
            <ColourDisc value={choice.custom[field]} size={184} saved={[]} onChange={(hex) => edit(hex)} />
            <Button variant="text" size="sm" onClick={() => setChoice({ id: 'custom', custom: base.current })}>{t('common.reset')}</Button>
          </div>
        </div>
      ) : (
        <Button className={styles.action} variant="secondary" icon="tune" onClick={customise}>{t('theme.customise')}</Button>
      )}
    </div>
  );
}
