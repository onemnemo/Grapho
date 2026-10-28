/** Small helpers to turn generated SVG into CSS and image URLs. */
export const enc = (s: string) => encodeURIComponent(s).replace(/'/g, '%27').replace(/\(/g, '%28').replace(/\)/g, '%29');
/** A CSS `url()` for background-image. */
export const uri = (s: string) => `url('data:image/svg+xml,${enc(s)}')`;
/** A plain data URL for img src. */
export const dataURI = (s: string) => `data:image/svg+xml,${enc(s)}`;
