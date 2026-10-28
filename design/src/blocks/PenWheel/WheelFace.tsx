/* The markup of one wheel face. React renders it once per ring or tool-list change; the engine
   then writes transforms onto these elements every frame (see engine/face.ts). */
import { memo, useSyncExternalStore, type CSSProperties } from 'react';
import { Icon } from '@/assets/icons';
import { PenGlyph } from '@/assets/wheel/PenGlyph';
import { DOT_NUMBERS, ERASER_STEPS, dotRadius, eraserRadius, makeGeo } from './engine/geometry';
import { TOOLS, type WheelModel } from './engine/model';
import type { FaceRefs } from './engine/face';
import styles from './PenWheel.module.css';

interface Props {
  model: WheelModel;
  refs: FaceRefs;
  full: boolean;
}

const disc = (size: number): CSSProperties => ({ width: size * 2, height: size * 2, left: -size, top: -size });

export const WheelFace = memo(function WheelFace({ model, refs, full }: Props) {
  useSyncExternalStore(model.subscribe, () => model.structVersion);
  const g = makeGeo(full);
  const band = (g.rimR + 15) * 2;
  const ringD = (g.swatchR + 7.5) * 2;
  const tools = model.tools.map((id) => TOOLS[id]);
  return (
    <div ref={(el) => { refs.root = el; }} className={styles.wf} data-full={full || undefined} data-view="colour" style={{ display: 'none' }}>
      <div className={styles.shadow} style={disc(g.r)} />
      <div ref={(el) => { refs.lift = el; }} className={styles.lift} style={disc(g.r)} />
      <div className={styles.body} style={disc(g.r)} />
      <div className={styles.well} style={disc(g.inner)} />
      <div className={styles.hub} style={disc(g.core)} />
      <div className={styles.hit} style={disc(g.r + 2)} />

      <svg ref={(el) => { refs.scrollSvg = el; }} className={styles.scroll} width="2" height="2" aria-hidden="true">
        <path ref={(el) => { refs.scrollTrack = el; }} />
        <path ref={(el) => { refs.scrollThumb = el; }} />
      </svg>

      <div className={`${styles.group} ${styles.cols}`}>
        {model.rim.map((c, i) =>
          c === null ? (
            <div key={`s${i}`} ref={(el) => { refs.sw[i] = el; }} className={styles.strip} />
          ) : (
            <div key={`c${i}`} ref={(el) => { refs.sw[i] = el; }} className={styles.sw} style={{ ...disc(g.swatchR), '--c': c } as CSSProperties}>
              <i className={styles.swIn} style={{ width: g.swatchR * 2, height: g.swatchR * 2 }} />
            </div>
          ),
        )}
        <i ref={(el) => { refs.ringSw = el; }} className={styles.selRing} style={disc(ringD / 2)} />
      </div>

      <div className={`${styles.group} ${styles.sizes}`}>
        {DOT_NUMBERS.map((n, i) => {
          const r = dotRadius(n);
          return (
            <div key={n} ref={(el) => { refs.dots[i] = el; }} className={styles.sd} style={{ '--i': i } as CSSProperties}>
              <i className={styles.sdIn} style={disc(r)} />
            </div>
          );
        })}
        <canvas ref={(el) => { refs.band = el; }} className={styles.band} width={band * 2} height={band * 2} style={{ ...disc(band / 2) }} />
        <div ref={(el) => { refs.marker = el; }} className={styles.marker} />
        <i ref={(el) => { refs.ringSd = el; }} className={styles.selRing} style={disc(14)} />
      </div>

      <div className={`${styles.group} ${styles.erase}`}>
        {ERASER_STEPS.map((pt, i) => (
          <div key={pt} ref={(el) => { refs.edots[i] = el; }} className={styles.sd} style={{ '--i': i } as CSSProperties}>
            <i className={styles.sdIn} style={disc(eraserRadius(i))} />
          </div>
        ))}
        <div ref={(el) => { refs.btnWhole = el; }} className={`${styles.btn} ${styles.btnMode}`} role="button" aria-label="Whole strokes" data-tip="Whole strokes" data-hit>
          <span className={styles.btnIn}><Icon name="stroke_whole" size={20} /></span>
        </div>
        <div ref={(el) => { refs.btnPart = el; }} className={`${styles.btn} ${styles.btnMode}`} role="button" aria-label="Part of stroke" data-tip="Part of stroke" data-hit>
          <span className={styles.btnIn}><Icon name="stroke_part" size={20} /></span>
        </div>
        <i ref={(el) => { refs.ringEd = el; }} className={styles.selRing} style={disc(14)} />
      </div>

      <svg ref={(el) => { refs.stem = el; }} className={styles.stem} width="2" height="2" aria-hidden="true">
        <path ref={(el) => { refs.stemPath = el; }} />
      </svg>

      <div className={styles.tools}>
        <div ref={(el) => { refs.plate = el; }} className={styles.plate} />
        {tools.map((t, i) => (
          <div key={t.id} ref={(el) => { refs.tb[i] = el; }} className={styles.tb} role="button" aria-label={t.label} data-tip={t.label} data-hit>
            <span className={styles.tbIn}><Icon name={t.icon} size={20} /></span>
          </div>
        ))}
      </div>

      <div ref={(el) => { refs.read = el; }} className={styles.read}>
        <b ref={(el) => { refs.readText = el; }} />
      </div>

      <div ref={(el) => { refs.nibBox = el; }} className={styles.nib} data-kind="pen">
        <PenGlyph kind="pen" tipRef={(el) => { refs.tipPen = el; }} data-glyph="pen" />
        <PenGlyph kind="highlighter" tipRef={(el) => { refs.tipHl = el; }} data-glyph="highlighter" />
      </div>
      <div className={styles.core} style={disc(g.core)} role="button" aria-label="Pens" data-hit />

      <div className={`${styles.group} ${styles.fixed}`}>
        <div ref={(el) => { refs.btnSize = el; }} className={`${styles.btn} ${styles.btnSize}`} role="button" aria-label="Size" data-tip="Size" data-hit>
          <span className={styles.btnIn}><i className={styles.szd} /></span>
        </div>
        <div ref={(el) => { refs.btnRain = el; }} className={`${styles.btn} ${styles.btnRain}`} role="button" aria-label="Custom colour" data-tip="Custom colour" data-hit>
          <span className={styles.btnIn}><i className={styles.rnb} /></span>
        </div>
      </div>
    </div>
  );
});
