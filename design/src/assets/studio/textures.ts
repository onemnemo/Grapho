import { uri } from './svgUri';

/* Textures are generated from SVG filters, so they stay sharp at any size and cost almost nothing to store.
   Relief maps are mid-grey and used with a blend mode over a colour; material textures bring their own colour. */
const flatLight = (el: number) => Math.sin((el * Math.PI) / 180);
const remap = (g: number, el = 60) =>
  `<feColorMatrix type="matrix" values="${g} 0 0 0 ${(0.5 - g * flatLight(el)).toFixed(3)} ${g} 0 0 0 ${(0.5 - g * flatLight(el)).toFixed(3)} ${g} 0 0 0 ${(0.5 - g * flatLight(el)).toFixed(3)} 0 0 0 0 1"/>`;
const gray = (g: number, o: number) => `<feColorMatrix type="matrix" values="${g} 0 0 0 ${o} ${g} 0 0 0 ${o} ${g} 0 0 0 ${o} 0 0 0 0 1"/>`;
const light = (scale: number, el = 60, az = 50, c = 1) =>
  `<feDiffuseLighting surfaceScale="${scale}" diffuseConstant="${c}" lighting-color="#fff"><feDistantLight azimuth="${az}" elevation="${el}"/></feDiffuseLighting>`;
const TEXF = (inner: string) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><filter id="f" x="0" y="0" width="100%" height="100%" color-interpolation-filters="sRGB">${inner}</filter><rect width="256" height="256" filter="url(#f)"/></svg>`;
const turb = (f: string, o = 3, type = 'fractalNoise', seed = 2) =>
  `<feTurbulence type="${type}" baseFrequency="${f}" numOctaves="${o}" seed="${seed}" stitchTiles="stitch"/>`;
const table = (v: string) =>
  `<feComponentTransfer><feFuncR type="table" tableValues="${v}"/><feFuncG type="table" tableValues="${v}"/><feFuncB type="table" tableValues="${v}"/></feComponentTransfer>`;

export type TextureKey = 'paper' | 'linen' | 'leather' | 'felt' | 'cork' | 'kraft' | 'marble' | 'wash' | 'grain' | 'holo';
export interface Texture {
  name: string;
  /** Default tint for material textures, or the colour a relief texture was drawn for. */
  c: string | null;
  mode: 'relief' | 'material';
  /** Blend mode of a relief texture over what is beneath it. */
  blend: string;
  /** Blend mode of the grain over the tint of a material texture. */
  inner?: string;
  op: number;
  map?: string;
}

export const TEX: Record<TextureKey, Texture> = {
  paper: { name: 'Paper', c: '#F1EBDD', mode: 'relief', blend: 'overlay', op: 1, map: TEXF(turb('.07', 4) + light(0.9, 62) + remap(2.2, 62)) },
  linen: {
    name: 'Linen', c: '#6A61B4', mode: 'relief', blend: 'overlay', op: 1,
    map: TEXF(
      `<feTurbulence type="fractalNoise" baseFrequency=".04 .9" numOctaves="2" seed="4" stitchTiles="stitch" result="a"/><feTurbulence type="fractalNoise" baseFrequency=".9 .04" numOctaves="2" seed="9" stitchTiles="stitch" result="b"/><feComposite in="a" in2="b" operator="arithmetic" k1="0" k2=".5" k3=".5" k4="0"/>` +
        light(1.2, 62) + remap(1.8, 62),
    ),
  },
  leather: { name: 'Leather', c: '#5B1F2B', mode: 'relief', blend: 'overlay', op: 1, map: TEXF(turb('.11', 2, 'turbulence', 5) + light(1.5, 55) + remap(1.7, 55)) },
  felt: { name: 'Felt', c: '#B04D76', mode: 'relief', blend: 'overlay', op: 1, map: TEXF(turb('.62', 4, 'fractalNoise', 7) + light(0.8, 60) + '<feGaussianBlur stdDeviation=".35"/>' + remap(2.4)) },
  cork: {
    name: 'Cork', c: '#C89B63', mode: 'material', blend: 'multiply', inner: 'multiply', op: 1,
    map: TEXF(
      `<feTurbulence type="fractalNoise" baseFrequency=".28" numOctaves="3" seed="3" stitchTiles="stitch" result="n"/>` + gray(4.6, -1.62) +
        `<feComponentTransfer><feFuncR type="discrete" tableValues="1 1 1 1 1 1 .8 .52"/><feFuncG type="discrete" tableValues="1 1 1 1 1 1 .8 .52"/><feFuncB type="discrete" tableValues="1 1 1 1 1 1 .8 .52"/></feComponentTransfer>`,
    ),
  },
  kraft: {
    name: 'Kraft', c: '#B98B58', mode: 'material', blend: 'overlay', inner: 'overlay', op: 1,
    map: TEXF(
      `<feTurbulence type="fractalNoise" baseFrequency=".012 .55" numOctaves="3" seed="6" stitchTiles="stitch" result="a"/><feTurbulence type="fractalNoise" baseFrequency=".8" numOctaves="2" seed="1" stitchTiles="stitch" result="b"/><feComposite in="a" in2="b" operator="arithmetic" k1="0" k2=".7" k3=".3" k4="0"/>` +
        light(0.9, 58) + remap(1.8, 58),
    ),
  },
  marble: { name: 'Marble', c: '#E9E7E3', mode: 'material', blend: 'multiply', inner: 'multiply', op: 1, map: TEXF(turb('.006 .011', 5, 'turbulence', 11) + gray(1, 0) + table('.34 .72 .92 .98 1')) },
  wash: { name: 'Watercolour', c: '#9ED8F2', mode: 'relief', blend: 'soft-light', op: 0.8, map: TEXF(turb('.012 .016', 4, 'fractalNoise', 21) + gray(2.2, -0.6) + '<feGaussianBlur stdDeviation="1.2"/>') },
  grain: { name: 'Grain', c: '#808080', mode: 'relief', blend: 'overlay', op: 0.5, map: TEXF(turb('.9', 2, 'fractalNoise', 3) + gray(1.05, -0.02)) },
  holo: { name: 'Holographic', c: null, mode: 'material', blend: 'normal', inner: 'normal', op: 1 },
};
export const TEXKEYS = Object.keys(TEX) as TextureKey[];
/** CSS background-image for a texture's relief map. */
export const texMapURL = (k: TextureKey) => (TEX[k].map ? uri(TEX[k].map as string) : '');

/** Holographic fill. `--hx` is moved by the pointer to slide the sheen. */
export const HOLO =
  'linear-gradient(115deg, transparent calc(var(--hx,50%) - 22%), rgba(255,255,255,.55) var(--hx,50%), transparent calc(var(--hx,50%) + 22%)), conic-gradient(from 210deg at 28% 18%, #ffd1ec, #c9b6ff, #9ee6ff, #b6ffd9, #fff3b0, #ffc9a8, #ffd1ec)';

/** The colour each texture looks best on in a swatch. */
export const TEX_BASE: Record<TextureKey, string> = {
  paper: '#F1EBDD', linen: '#6A61B4', leather: '#7A2E3B', felt: '#B04D76', cork: '#C89B63', kraft: '#B98B58', marble: '#ECE9E4', wash: '#9ED8F2', grain: '#3C8FB5', holo: '#6A6D7B',
};
