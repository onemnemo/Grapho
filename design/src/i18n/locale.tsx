import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { en, type Catalogue, type Key } from './en';
import { xx } from './xx';
import { nb } from './nb';
import { fr } from './fr';
import { de } from './de';
import { es } from './es';
import { hi } from './hi';
import { it } from './it';
import { sv } from './sv';

export type LocaleCode = 'en' | 'xx' | 'nb' | 'fr' | 'de' | 'es' | 'hi' | 'it' | 'sv';

/** xx is the pseudo-locale for testing layouts: accented, about 35% longer. It is not offered to people. */
export const LOCALES: LocaleCode[] = ['en', 'xx', 'nb', 'fr', 'de', 'es', 'hi', 'it', 'sv'];

const catalogues: Record<LocaleCode, Catalogue> = { en, xx, nb, fr, de, es, hi, it, sv };

export type TFunction = (key: Key, vars?: Record<string, string | number>) => string;

interface LocaleValue {
  locale: LocaleCode;
  setLocale: (locale: LocaleCode) => void;
  t: TFunction;
}

function translator(locale: LocaleCode): TFunction {
  const table = catalogues[locale];
  return (key, vars) => {
    const raw = table[key] ?? en[key];
    return vars ? raw.replace(/\{(\w+)\}/g, (m, name: string) => (name in vars ? String(vars[name]) : m)) : raw;
  };
}

const Ctx = createContext<LocaleValue>({ locale: 'en', setLocale: () => {}, t: translator('en') });

export interface LocaleProviderProps {
  /** Controlled locale. Leave out to let the provider hold it. */
  locale?: LocaleCode;
  defaultLocale?: LocaleCode;
  onLocaleChange?: (locale: LocaleCode) => void;
  children: ReactNode;
}

/** Gives every `useT()` below it one language. Without a provider the app speaks English. */
export function LocaleProvider({ locale, defaultLocale = 'en', onLocaleChange, children }: LocaleProviderProps) {
  const [inner, setInner] = useState<LocaleCode>(defaultLocale);
  const current = locale ?? inner;
  const setLocale = useCallback((l: LocaleCode) => { setInner(l); onLocaleChange?.(l); }, [onLocaleChange]);
  const value = useMemo(() => ({ locale: current, setLocale, t: translator(current) }), [current, setLocale]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

/** The translate function: `const t = useT(); t('language.search')`. */
export function useT(): TFunction {
  return useContext(Ctx).t;
}

export function useLocale() {
  const { locale, setLocale } = useContext(Ctx);
  return { locale, setLocale };
}
