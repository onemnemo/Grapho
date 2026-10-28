const seen = new WeakMap<Element, boolean>();
let io: IntersectionObserver | null = null;
export function visibility(el: Element): () => boolean {
  if (typeof IntersectionObserver === 'undefined') return () => true;
  if (!io) io = new IntersectionObserver((es) => es.forEach((e) => seen.set(e.target, e.isIntersecting)), { rootMargin: '100px' });
  seen.set(el, true); io.observe(el);
  return () => seen.get(el) !== false;
}
export function forget(el: Element) { io?.unobserve(el); seen.delete(el); }
