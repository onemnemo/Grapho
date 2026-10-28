/* Pieces of the pen editor: the row of eight size dots, and the colour field with its inline picker. */
import { useRef, useState, type CSSProperties } from 'react';
import { hexToHsv, hsvToHex } from '@/assets/wheel/colour';
import { Swatch } from '@/components/Swatch';
import { TextField } from '@/components/TextField';
import { ColourBar, SVSquare, useDrag } from '@/blocks/ColourPicker';
import { DOT_NUMBERS, dotRadius, nearestDot } from '@/blocks/PenWheel/engine/geometry';
import { clamp } from '@/blocks/PenWheel/engine/math';
import styles from './CustomiseWheel.module.css';

const PAD = 16;
const posOf = (n: number) => `calc(${PAD}px + (100% - ${PAD * 2}px) * ${(n - 1) / 99})`;

/** The wheel's eight size dots in a row; tap one, or drag along the row for any size. */
export function SizeDots({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  const root = useRef<HTMLDivElement>(null);
  const sel = nearestDot(value);
  const { props } = useDrag((x, _y, down) => {
    const w = root.current?.getBoundingClientRect().width ?? 1;
    const n = 1 + clamp((x * w - PAD) / (w - PAD * 2), 0, 1) * 99;
    onChange(down ? nearestDot(n) : n);
  });
  const r = dotRadius(sel) + 5;
  return (
    <div ref={root} className={styles.sizeRow} role="slider" aria-label="Size" aria-valuemin={1} aria-valuemax={100} aria-valuenow={value} tabIndex={0}
      onKeyDown={(e) => {
        const d = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0;
        if (d) { e.preventDefault(); const i = DOT_NUMBERS.indexOf(sel) + d; onChange(DOT_NUMBERS[clamp(i, 0, DOT_NUMBERS.length - 1)]); }
      }}
      {...props}>
      {DOT_NUMBERS.map((n) => {
        const dr = dotRadius(n);
        return <i key={n} className={styles.sizeDot} data-on={n === sel || undefined} style={{ width: dr * 2, height: dr * 2, margin: `${-dr}px 0 0 ${-dr}px`, left: posOf(n) }} />;
      })}
      <i className={styles.sizeRing} style={{ width: r * 2, height: r * 2, margin: `${-r}px 0 0 ${-r}px`, left: posOf(sel) }} />
    </div>
  );
}

/** A grid of the palette's colours ending in the rainbow, which opens a small picker inline. */
export function ColourField({ color, palette, onChange }: { color: string; palette: string[]; onChange: (hex: string) => void }) {
  const [custom, setCustom] = useState(false);
  const [[hue, sat, val], setHsv] = useState(() => hexToHsv(color));
  const [hexText, setHexText] = useState(color.slice(1));
  const swatches = [...new Set([...palette, color.toUpperCase()])];
  const inSw = swatches.includes(color.toUpperCase());
  const push = (h: number, s: number, v: number) => {
    setHsv([h, s, v]);
    const c = hsvToHex(h, s, v);
    setHexText(c.slice(1));
    onChange(c);
  };
  return (
    <div>
      <div className={styles.cgrid}>
        {swatches.map((c) => (
          <span key={c} className={styles.cell}>
            <Swatch color={c} label={`Colour ${c}`} size="lg" selected={!custom && c.toUpperCase() === color.toUpperCase()} onClick={() => { setCustom(false); onChange(c); }} />
          </span>
        ))}
        <span className={styles.cell}>
          <Swatch
            color="var(--wh-rainbow)"
            label="Custom colour"
            size="lg"
            selected={custom || !inSw}
            onClick={() => { if (!custom) { setHsv(hexToHsv(color)); setHexText(color.slice(1)); } setCustom(!custom); }}
          />
        </span>
      </div>
      {custom && (
        <div className={styles.inline}>
          <SVSquare width={300} height={130} hue={hue} sat={sat} val={val} onChange={(s, v) => push(hue, s, v)} />
          <ColourBar kind="hue" label="Hue" value={hue / 360} onChange={(x) => push(x * 360, sat, val)} />
          <div className={styles.hexRow}>
            <i className={styles.preview} style={{ background: color } as CSSProperties} />
            <span className={styles.hash}>#</span>
            <TextField
              variant="inline"
              className={styles.field}
              label="Hex colour"
              maxLength={6}
              value={hexText}
              onChange={(e) => {
                const v = e.target.value.toUpperCase().replace(/[^0-9A-F]/g, '');
                setHexText(v);
                if (v.length === 6) { setHsv(hexToHsv('#' + v)); onChange('#' + v); }
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
