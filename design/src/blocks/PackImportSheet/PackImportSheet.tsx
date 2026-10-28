import { useEffect, useRef, useState } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Checkbox } from '@/components/Checkbox';
import { Sheet } from '@/components/Sheet';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { packArt } from '@/assets/packs';
import { cx } from '@/lib/cx';
import { PackCover } from '../PackRow';
import { sampleImportGroups, type ImportGroup, type ImportSummary } from './importTypes';
import styles from './PackImportSheet.module.css';

export type ImportStage = 'drop' | 'opening' | 'review' | 'installing';

export interface PackImportSheetProps {
  open: boolean;
  onClose: () => void;
  groups?: ImportGroup[];
  /** Name and byline of the file that was chosen. */
  packName?: string;
  byline?: string;
  /** Start on a given step. The sheet moves on by itself when used. */
  initialStage?: ImportStage;
  /** Groups that start expanded. */
  initialOpen?: string[];
  placement?: OverlayPlacement;
  onInstall?: (summary: ImportSummary) => void;
}

const all = (groups: ImportGroup[]) => Object.fromEntries(groups.map((g) => [g.key, new Set(g.items.map((_, i) => i))])) as Record<string, Set<number>>;

/** Import a .graphopack: choose a file, see what is inside grouped by type, pick what to install. Nothing is added until Install. */
export function PackImportSheet({ open, onClose, groups = sampleImportGroups, packName = 'Botanica', byline = 'by Marit Holm · 4.2 MB', initialStage = 'drop', initialOpen = ['stickers'], placement = 'viewport', onInstall }: PackImportSheetProps) {
  const [stage, setStage] = useState<ImportStage>(initialStage);
  const [sel, setSel] = useState(() => all(groups));
  const [expanded, setExpanded] = useState<string[]>(initialOpen);
  const timer = useRef<number>(undefined);
  useEffect(() => () => window.clearTimeout(timer.current), []);

  const chosen = groups.reduce((a, g) => a + (sel[g.key]?.size ?? 0), 0);

  const toggleGroup = (g: ImportGroup) =>
    setSel((s) => ({ ...s, [g.key]: s[g.key].size === g.items.length ? new Set() : new Set(g.items.map((_, i) => i)) }));
  const toggleItem = (key: string, i: number) =>
    setSel((s) => {
      const next = new Set(s[key]);
      if (next.has(i)) next.delete(i); else next.add(i);
      return { ...s, [key]: next };
    });
  const choose = () => {
    setStage('opening');
    timer.current = window.setTimeout(() => setStage('review'), 900);
  };
  const install = () => {
    setStage('installing');
    timer.current = window.setTimeout(() => {
      onInstall?.({ count: chosen, kinds: groups.filter((g) => sel[g.key].size).map((g) => g.label) });
      onClose();
    }, 1100);
  };

  const footer =
    stage === 'drop' || stage === 'opening' ? (
      <Button variant="text" onClick={onClose}>Cancel</Button>
    ) : (
      <>
        <Button variant="text" onClick={onClose}>Cancel</Button>
        <Button variant="primary" icon="check" disabled={!chosen || stage === 'installing'} onClick={install}>
          {stage === 'installing' ? 'Installing' : chosen ? `Install ${chosen} item${chosen === 1 ? '' : 's'}` : 'Install'}
        </Button>
      </>
    );

  return (
    <Sheet open={open} onClose={onClose} title="Import a pack" width={820} placement={placement} footer={footer}>
      {stage === 'drop' || stage === 'opening' ? (
        <div className={styles.drop}>
          <span className={styles.dropIcon}><Icon name="import" size={24} /></span>
          <h3>Drop a .graphopack file</h3>
          <Button variant="secondary" disabled={stage === 'opening'} onClick={choose}>{stage === 'opening' ? 'Opening' : 'Choose file'}</Button>
        </div>
      ) : (
        <div>
          <div className={styles.file}>
            <PackCover art={packArt.botanica} />
            <span className={styles.fileText}>
              <b>{packName}</b>
              <small>{byline}</small>
            </span>
          </div>
          {groups.map((g) => {
            const c = sel[g.key].size;
            const state = c === 0 ? false : c === g.items.length ? true : 'mixed';
            const isOpen = expanded.includes(g.key);
            return (
              <section key={g.key} className={cx(styles.group, isOpen && styles.open)}>
                <div className={styles.head}>
                  <span className={styles.chk}>
                    <Checkbox checked={state} onChange={() => toggleGroup(g)} aria-label={`Select all ${g.label}`} />
                  </span>
                  <button
                    type="button"
                    className={styles.title}
                    aria-expanded={isOpen}
                    onClick={() => setExpanded(isOpen ? expanded.filter((k) => k !== g.key) : [...expanded, g.key])}
                  >
                    <span className={styles.ico}><Icon name={g.icon} size={20} /></span>
                    <span className={styles.tx}>
                      <b>{g.label}</b>
                    </span>
                    <span className={styles.n}>{g.items.length}</span>
                    <Icon name="chevron_down" size={18} />
                  </button>
                </div>
                {isOpen && (
                  <ul className={styles.items}>
                    {g.items.map((t, i) => (
                      <li key={t}>
                        <Checkbox checked={sel[g.key].has(i)} onChange={() => toggleItem(g.key, i)} label={t} />
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            );
          })}
        </div>
      )}
    </Sheet>
  );
}
