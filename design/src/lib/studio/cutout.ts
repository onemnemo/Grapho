/**
 * Background removal: the whole background goes, not a chosen shape. It starts from the edges of the picture and clears
 * everything that is close to the background colour and connected to the edge, so a white page around an illustration
 * becomes transparent while the white inside the illustration stays. `tol` sets how close "close" is.
 */
export function cutoutURL(src: string, width = 420, tol = 40): Promise<{ url: string; ratio: number }> {
  return new Promise((res) => {
    const im = new Image();
    im.onload = () => {
      const r = im.naturalWidth / im.naturalHeight, W = Math.min(width, im.naturalWidth), H = Math.round(W / r);
      const c = document.createElement('canvas');
      c.width = W; c.height = H;
      const g = c.getContext('2d', { willReadFrequently: true });
      if (!g) { res({ url: src, ratio: r }); return; }
      g.drawImage(im, 0, 0, W, H);
      const id = g.getImageData(0, 0, W, H), d = id.data, N = W * H;
      const cs = [[0, 0], [W - 1, 0], [0, H - 1], [W - 1, H - 1]].map(([x, y]) => (y * W + x) * 4);
      const bg = [0, 1, 2].map((k) => cs.reduce((a, i) => a + d[i + k], 0) / 4);
      const dist = (i: number) => Math.hypot(d[i] - bg[0], d[i + 1] - bg[1], d[i + 2] - bg[2]);
      const gone = new Uint8Array(N), q = new Int32Array(N);
      let h = 0, t = 0;
      const push = (p: number) => { if (!gone[p] && dist(p * 4) <= tol) { gone[p] = 1; q[t++] = p; } };
      for (let x = 0; x < W; x++) { push(x); push((H - 1) * W + x); }
      for (let y = 0; y < H; y++) { push(y * W); push(y * W + W - 1); }
      while (h < t) {
        const p = q[h++], x = p % W, y = (p / W) | 0;
        if (x > 0) push(p - 1);
        if (x < W - 1) push(p + 1);
        if (y > 0) push(p - W);
        if (y < H - 1) push(p + W);
      }
      for (let p = 0; p < N; p++) {
        const i = p * 4;
        if (gone[p]) { d[i + 3] = 0; continue; }
        const x = p % W, y = (p / W) | 0;
        const edge = (x > 0 && gone[p - 1]) || (x < W - 1 && gone[p + 1]) || (y > 0 && gone[p - W]) || (y < H - 1 && gone[p + W]);
        if (edge) d[i + 3] = Math.round(255 * Math.min(1, 0.45 + dist(i) / (tol * 3 + 1)));
      }
      g.putImageData(id, 0, 0);
      res({ url: c.toDataURL('image/png'), ratio: W / H });
    };
    im.onerror = () => res({ url: src, ratio: 1 });
    im.src = src;
  });
}
