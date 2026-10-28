import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '../icons';
import { COVER_COLOURS, type CoverPreset } from './coverPresets';
import styles from './FolderArt.module.css';

export type FolderColour = 'indigo' | 'rose' | 'ember' | 'forest' | 'sea' | 'gold' | 'sky' | 'slate';
export const FOLDER_COLOURS: FolderColour[] = ['indigo', 'rose', 'ember', 'forest', 'sea', 'gold', 'sky', 'slate'];

/** One paper peeking out: a cover preset (its colour is used) or 'plain' for white paper. */
export type FolderPaper = CoverPreset | 'plain-paper';

export interface FolderArtProps extends HTMLAttributes<HTMLDivElement> {
  colour?: FolderColour;
  /** Up to three papers, left to right. Fewer shows plain paper in the gaps. */
  papers?: FolderPaper[];
  name?: ReactNode;
  meta?: ReactNode;
  /** Small pin mark at the bottom right, for pinned folders. */
  pinned?: boolean;
  /** A subject icon at the top right of the front. */
  icon?: IconName;
  /** Lift the papers when the pointer is over the folder. */
  interactive?: boolean;
  /** Fixed look of a state: hover pose, or the wave shown while something is dragged over it. */
  state?: 'rest' | 'hover' | 'over';
  /** Extra content drawn over the front (badges, menu buttons). */
  children?: ReactNode;
}

const ROT = [-3, 1, 4];
const DY = [4, 0, 6];

/** A 3D folder. It fills its parent's width (ratio 228:140) and scales its text with it. */
export function FolderArt({
  colour = 'indigo',
  papers = ['plain-paper', 'plain-paper', 'plain-paper'],
  name,
  meta,
  pinned = false,
  icon,
  interactive = false,
  state = 'rest',
  children,
  className,
  ...rest
}: FolderArtProps) {
  const three = [0, 1, 2].map((i) => papers[i] ?? 'plain-paper');
  return (
    <div
      className={[styles.folder, styles[colour], interactive && styles.interactive, state !== 'rest' && styles[state], className]
        .filter(Boolean).join(' ')}
      {...rest}
    >
      <div className={styles.back} />
      <div className={styles.papers}>
        {three.map((p, i) => (
          <div
            key={i}
            className={styles.paper}
            style={{
              ['--rot' as string]: `${ROT[i]}deg`,
              ['--dy' as string]: `${DY[i]}%`,
              ...(p !== 'plain-paper' ? ({ background: COVER_COLOURS[p] } as CSSProperties) : null),
            }}
          />
        ))}
      </div>
      <div className={styles.front}>
        {(name || meta) && (
          <div className={styles.text}>
            {name && <div className={styles.name}>{name}</div>}
            {meta && <div className={styles.meta}>{meta}</div>}
          </div>
        )}
        {icon && <Icon name={icon} size={20} className={styles.icon} />}
        {pinned && <span className={styles.mark}><Icon name="pin" size={16} /></span>}
      </div>
      {children}
    </div>
  );
}
