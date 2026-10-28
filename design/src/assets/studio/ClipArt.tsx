import type { CSSProperties } from 'react';
import { CLIPS, type ClipKey } from './clips';
import styles from './ClipArt.module.css';

export interface ClipArtProps {
  clip: ClipKey;
  /** Poster time in seconds: the frame shown while paused. */
  poster?: number;
  playing?: boolean;
  className?: string;
  style?: CSSProperties;
}

const v = (o: Record<string, string | number>) => o as CSSProperties;

function scene(clip: ClipKey) {
  switch (clip) {
    case 'aurora':
      return <><i className={`${styles.vb} ${styles.vb1}`} /><i className={`${styles.vb} ${styles.vb2}`} /><i className={`${styles.vb} ${styles.vb3}`} /></>;
    case 'embers':
      return <>{Array.from({ length: 18 }, (_, i) => <i key={i} className={styles.em} style={v({ '--x': `${(i * 37) % 100}%`, '--s': `${3 + ((i * 7) % 6)}px`, '--d': `${-(i * 0.9) % 6}s`, '--t': `${4 + (i % 4)}s` })} />)}</>;
    case 'rain':
      return <>{Array.from({ length: 26 }, (_, i) => <i key={i} className={styles.rn} style={v({ '--x': `${(i * 41) % 100}%`, '--d': `${-(i * 0.37) % 2.4}s`, '--t': `${1.6 + (i % 3) * 0.5}s` })} />)}</>;
    case 'waves':
      return (
        <>
          {[[`M0 50Q50 20 100 50T200 50T300 50T400 50V100H0z`, styles.wv1], [`M0 56Q50 30 100 56T200 56T300 56T400 56V100H0z`, styles.wv2], [`M0 64Q50 42 100 64T200 64T300 64T400 64V100H0z`, styles.wv3]].map(([d, c]) => (
            <svg key={d} className={`${styles.wv} ${c}`} viewBox="0 0 400 100" preserveAspectRatio="none"><path d={d} /></svg>
          ))}
        </>
      );
    case 'clouds':
      return <><i className={`${styles.cl} ${styles.cl1}`} /><i className={`${styles.cl} ${styles.cl2}`} /><i className={`${styles.cl} ${styles.cl3}`} /></>;
    default:
      return <i className={styles.swl} />;
  }
}

/** A looping scene that fills its parent. Paused it shows the poster frame. */
export function ClipArt({ clip, poster = 0, playing = false, className, style }: ClipArtProps) {
  const c = CLIPS[clip];
  return (
    <div
      className={[styles.vid, clip === 'clouds' && styles.clouds, className].filter(Boolean).join(' ')}
      data-playing={playing || undefined}
      style={{ background: c.bg, ['--po' as string]: `-${poster}s`, ...style }}
      aria-hidden="true"
    >
      {scene(clip)}
    </div>
  );
}
