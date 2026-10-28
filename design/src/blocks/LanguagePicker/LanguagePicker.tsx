import { useMemo, useRef, useState, type HTMLAttributes, type KeyboardEvent } from 'react';
import { Icon } from '@/assets/icons';
import { SearchField } from '@/components/SearchField';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import { useT, type Language } from '@/i18n';
import styles from './LanguagePicker.module.css';

/** The value that follows the device's language instead of naming one. */
export const DEVICE = 'device';

export interface LanguagePickerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'defaultValue'> {
  /** The languages Grapho is translated into. All of them ship with the app; nothing is downloaded. */
  languages: Language[];
  /** Code of the language in use, or DEVICE to follow the device. */
  value?: string;
  defaultValue?: string;
  onChange?: (code: string) => void;
  /** The device's own language. When set, the first row follows it. */
  deviceLanguage?: string;
  /** The search field appears when there are more languages than this. */
  searchAbove?: number;
}

const plain = (s: string) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

/** A full height list of languages, not a dropdown: follow the device, then every language in its own name. */
export function LanguagePicker({
  languages,
  value,
  defaultValue,
  onChange,
  deviceLanguage,
  searchAbove = 12,
  className,
  ...rest
}: LanguagePickerProps) {
  const t = useT();
  const [current, setCurrent] = useControllable(value, defaultValue ?? (deviceLanguage ? DEVICE : languages[0]?.code ?? ''), onChange);
  const [query, setQuery] = useState('');
  const list = useRef<HTMLDivElement>(null);
  const searchable = languages.length > searchAbove;
  const device = languages.find((l) => l.code === deviceLanguage);

  const found = useMemo(() => {
    const q = plain(query.trim());
    return q ? languages.filter((l) => plain(l.name).includes(q) || plain(l.english).includes(q) || l.code.startsWith(q)) : null;
  }, [languages, query]);

  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowDown: 1, ArrowUp: -1, Home: -9999, End: 9999 } as Record<string, number>)[e.key];
    if (!d) return;
    const items = Array.from(list.current?.querySelectorAll<HTMLElement>('[role="radio"]') ?? []);
    const at = items.indexOf(document.activeElement as HTMLElement);
    const next = items[Math.max(0, Math.min(items.length - 1, (at < 0 ? 0 : at) + d))];
    if (!next) return;
    e.preventDefault();
    next.focus();
  };

  const option = (key: string, code: string, main: string, sub: string | null, lang?: string) => (
    <button key={key} type="button" role="radio" aria-checked={code === current} className={styles.row} onClick={() => setCurrent(code)}>
      <span className={styles.names}>
        <span className={styles.native} lang={lang} dir="auto">{main}</span>
        {sub && <span className={styles.english}>{sub}</span>}
      </span>
      <Icon name="check" size={20} className={styles.tick} />
    </button>
  );
  const row = (prefix: string) => (l: Language) => option(`${prefix}${l.code}`, l.code, l.name, l.english !== l.name ? l.english : null, l.code);

  return (
    <div className={cx(styles.picker, className)} {...rest}>
      {searchable && (
        <div className={styles.search}>
          <SearchField size="sm" value={query} onChange={setQuery} placeholder={t('language.search')} />
        </div>
      )}
      <div ref={list} className={styles.list} role="radiogroup" aria-label={t('language.title')} onKeyDown={move}>
        {found ? (
          found.length ? found.map(row('f')) : <p className={styles.none}>{t('language.none', { query: query.trim() })}</p>
        ) : (
          <>
            {device && <section className={styles.group}>{option('device', DEVICE, t('language.device'), device.name)}</section>}
            <section className={styles.group}>
              {device && <h4 className={styles.label}>{t('language.all')}</h4>}
              {languages.map(row('a'))}
            </section>
          </>
        )}
      </div>
    </div>
  );
}
