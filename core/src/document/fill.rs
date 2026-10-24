//! Finding the area a bucket tap should fill.
//!
//! The ink is drawn onto a coarse grid, the empty cells around the tap are
//! flood filled, and the outline of that region becomes a polygon in document
//! units. Working on a grid keeps this simple and robust: overlapping
//! strokes, curves and small gaps all behave the same way.

use super::{Page, Tool};
use crate::outline::pieces;
use crate::Brush;

/// Most grid cells allocated; a coarser grid is used above this.
const MAX_CELLS: f32 = 8_000_000.0;
/// How far the region grows, in cells, so the fill tucks under the ink line.
const GROW: usize = 2;
/// Finest and coarsest grid size, in document units.
const MIN_CELL: f32 = 0.05;
const MAX_CELL: f32 = 2.0;
/// Size of one grid cell on screen, in pixels.
const CELL_PX: f32 = 0.5;
/// Free cells kept around the ink of an infinite canvas, so the flood can
/// go around the outside of a shape that isn't closed.
const MARGIN_CELLS: f32 = 4.0;
/// How far past the visible rectangle the search reaches, as a fraction of
/// its size on each side.
const WINDOW_GROW: f32 = 0.1;

/// What the user can see: the zoom, and the visible rectangle in the page's
/// document units as `(left, top, right, bottom)`.
#[derive(Clone, Copy, Debug)]
pub struct FillView {
    /// Screen pixels per document unit.
    pub scale: f32,
    pub visible: (f32, f32, f32, f32),
}

/// Which edges of the search area are walls, as `(left, top, right, bottom)`.
/// A region reaching an edge that isn't a wall is not enclosed.
pub type Walls = [bool; 4];

/// The area to search, its grid size and its walls.
#[derive(Clone, Copy, Debug)]
pub struct SearchArea {
    pub bounds: (f32, f32, f32, f32),
    pub cell: f32,
    pub walls: Walls,
}

const EMPTY: u8 = 0;
const INK: u8 = 1;
const REGION: u8 = 2;

struct Grid {
    w: usize,
    h: usize,
    cells: Vec<u8>,
}

impl Grid {
    fn at(&self, x: i64, y: i64) -> u8 {
        if x < 0 || y < 0 || x >= self.w as i64 || y >= self.h as i64 {
            return EMPTY;
        }
        self.cells[y as usize * self.w + x as usize]
    }
}

/// Picks the area to search and the grid size for a tap at `(x, y)`.
///
/// The grid follows the zoom so a cell is about [`CELL_PX`] on screen, which
/// keeps the fill close to the ink however far the view is zoomed in. The
/// area is cut to the visible rectangle grown by [`WINDOW_GROW`] on each
/// side; an edge that the cut made is not a wall, so a region reaching it
/// leaks past what the user can see and is not enclosed.
///
/// `page_size_pt` is the size of the page in points on a paged note (its own
/// size for a PDF page, else the paper size), whose page rectangle is
/// the area, with its own edges as walls. An infinite canvas has no edge, so
/// the area is the bounds of the page's visible ink plus a small margin; a
/// tap outside those can't be enclosed and gives `None`. Searching the ink
/// bounds rather than a larger area means the grid only coarsens (see
/// `MAX_CELLS`) as much as the drawing needs.
pub fn search_area(
    page: &Page,
    x: f32,
    y: f32,
    view: FillView,
    page_size_pt: Option<(f32, f32)>,
) -> Option<SearchArea> {
    if !(x.is_finite() && y.is_finite()) {
        return None;
    }
    let scale = if view.scale.is_finite() && view.scale > 0.0 {
        view.scale
    } else {
        1.0
    };
    let cell = (CELL_PX / scale).clamp(MIN_CELL, MAX_CELL);
    let (base, mut walls) = match page_size_pt {
        Some((w, h)) => ((0.0, 0.0, w, h), [true; 4]),
        None => {
            let (x0, y0, x1, y1) = ink_bounds(page)?;
            if x < x0 || x > x1 || y < y0 || y > y1 {
                return None;
            }
            // The grid coarsens on a big drawing; keep the margin a few
            // cells wide at the coarser size too.
            let c = cell.max((((x1 - x0) * (y1 - y0)) / MAX_CELLS).sqrt());
            let m = MARGIN_CELLS * c;
            ((x0 - m, y0 - m, x1 + m, y1 + m), [false; 4])
        }
    };
    let mut bounds = base;
    let (vl, vt, vr, vb) = view.visible;
    if [vl, vt, vr, vb].iter().all(|v| v.is_finite()) && vr > vl && vb > vt {
        let (gx, gy) = ((vr - vl) * WINDOW_GROW, (vb - vt) * WINDOW_GROW);
        let win = (vl - gx, vt - gy, vr + gx, vb + gy);
        let cuts = [
            win.0 > base.0,
            win.1 > base.1,
            win.2 < base.2,
            win.3 < base.3,
        ];
        bounds = (
            base.0.max(win.0),
            base.1.max(win.1),
            base.2.min(win.2),
            base.3.min(win.3),
        );
        for (wall, cut) in walls.iter_mut().zip(cuts) {
            *wall &= !cut;
        }
    }
    Some(SearchArea {
        bounds,
        cell,
        walls,
    })
}

/// Bounds of the visible, non-fill ink on a page, including half the pen
/// width. `None` if there is none.
fn ink_bounds(page: &Page) -> Option<(f32, f32, f32, f32)> {
    let mut r: Option<(f32, f32, f32, f32)> = None;
    for layer in page.layers.iter().filter(|l| l.visible) {
        for s in layer.strokes.iter().filter(|s| s.style.tool != Tool::Fill) {
            let half = s.style.brush().max_width * 0.5;
            for p in s
                .points
                .iter()
                .filter(|p| p.x.is_finite() && p.y.is_finite())
            {
                let b = r.get_or_insert((p.x, p.y, p.x, p.y));
                b.0 = b.0.min(p.x - half);
                b.1 = b.1.min(p.y - half);
                b.2 = b.2.max(p.x + half);
                b.3 = b.3.max(p.y + half);
            }
        }
    }
    r
}

/// Finds the closed area around `(x, y)` that ink encloses and returns its
/// outline as a polygon in document units.
///
/// `bounds` is `(min_x, min_y, max_x, max_y)` of the area to search and
/// `cell` the grid size in the same units. An edge of `bounds` that is a wall
/// (a page edge) closes the region; a region that reaches any other edge is
/// not enclosed and the result is `None`. Fills already on the page
/// don't block, and neither do hidden layers' strokes.
pub fn find_fill_region(
    page: &Page,
    x: f32,
    y: f32,
    bounds: (f32, f32, f32, f32),
    cell: f32,
    walls: Walls,
) -> Option<Vec<(f32, f32)>> {
    let (x0, y0, x1, y1) = bounds;
    if !(x1 > x0 && y1 > y0 && cell > 0.0 && x.is_finite() && y.is_finite()) {
        return None;
    }
    let area = (x1 - x0) * (y1 - y0);
    let cell = cell.max((area / MAX_CELLS).sqrt());
    let w = ((x1 - x0) / cell).ceil() as usize;
    let h = ((y1 - y0) / cell).ceil() as usize;
    let tap = (
        ((x - x0) / cell).floor() as i64,
        ((y - y0) / cell).floor() as i64,
    );
    if tap.0 < 0 || tap.1 < 0 || tap.0 >= w as i64 || tap.1 >= h as i64 {
        return None;
    }

    let mut grid = Grid {
        w,
        h,
        cells: vec![EMPTY; w * h],
    };
    draw_ink(page, &mut grid, (x0, y0), cell);
    if grid.at(tap.0, tap.1) == INK {
        return None;
    }
    if !flood(&mut grid, tap, walls) {
        return None;
    }
    for _ in 0..GROW {
        grow(&mut grid);
    }
    let ring = trace(&grid)?;
    let pts: Vec<(f32, f32)> = ring
        .iter()
        .map(|&(i, j)| (x0 + i as f32 * cell, y0 + j as f32 * cell))
        .collect();
    let out = simplify_ring(&pts, cell);
    (out.len() >= 3).then_some(out)
}

/// Marks every cell whose centre lies inside some stroke's outline.
fn draw_ink(page: &Page, grid: &mut Grid, origin: (f32, f32), cell: f32) {
    let mut polys = Vec::new();
    let mut edges: Vec<(f32, f32, f32, f32)> = Vec::new();
    let mut hits: Vec<(f32, i32)> = Vec::new();
    for layer in page.layers.iter().filter(|l| l.visible) {
        for s in layer.strokes.iter().filter(|s| s.style.tool != Tool::Fill) {
            // Thicken by one cell so thin lines can't slip between cell centres.
            let b = s.style.brush();
            let m = b.max_width + cell;
            let brush = Brush {
                max_width: m,
                min_ratio: (b.max_width * b.min_ratio + cell) / m,
                gamma: b.gamma,
            };
            polys.clear();
            let r = pieces(&s.points, &brush, cell * 0.5, &mut polys);
            if r.is_empty() {
                continue;
            }
            // Contours in grid coordinates (cell centres at half integers).
            edges.clear();
            let mut i = 0;
            while i < polys.len() {
                let n = polys[i] as usize;
                i += 1;
                let v = |k: usize| {
                    (
                        (polys[i + 2 * k] - origin.0) / cell,
                        (polys[i + 2 * k + 1] - origin.1) / cell,
                    )
                };
                for k in 0..n {
                    let (a, b) = (v(k), v((k + 1) % n));
                    edges.push((a.0, a.1, b.0, b.1));
                }
                i += 2 * n;
            }
            let row0 = (((r.min_y - origin.1) / cell).floor().max(0.0)) as usize;
            let row1 = ((((r.max_y - origin.1) / cell).ceil()).max(0.0) as usize).min(grid.h);
            for row in row0..row1 {
                let cy = row as f32 + 0.5;
                hits.clear();
                for &(ax, ay, bx, by) in &edges {
                    if (ay <= cy) != (by <= cy) {
                        let t = (cy - ay) / (by - ay);
                        hits.push((ax + t * (bx - ax), if by > ay { 1 } else { -1 }));
                    }
                }
                hits.sort_by(|a, b| a.0.total_cmp(&b.0));
                let mut winding = 0;
                for k in 0..hits.len() {
                    winding += hits[k].1;
                    if winding != 0 && k + 1 < hits.len() {
                        // Cells whose centre is in [hits[k], hits[k+1]).
                        let from = (hits[k].0 - 0.5).ceil().max(0.0) as usize;
                        let to = ((hits[k + 1].0 - 0.5).ceil().max(0.0) as usize).min(grid.w);
                        for c in from..to {
                            grid.cells[row * grid.w + c] = INK;
                        }
                    }
                }
            }
        }
    }
}

/// Marks the empty cells connected to `start` as the region. Returns false
/// if the region touches a grid edge that is not a wall.
fn flood(grid: &mut Grid, start: (i64, i64), walls: Walls) -> bool {
    let mut stack = vec![start];
    grid.cells[start.1 as usize * grid.w + start.0 as usize] = REGION;
    let mut open = false;
    while let Some((x, y)) = stack.pop() {
        if (x == 0 && !walls[0])
            || (y == 0 && !walls[1])
            || (x == grid.w as i64 - 1 && !walls[2])
            || (y == grid.h as i64 - 1 && !walls[3])
        {
            open = true;
        }
        for (nx, ny) in [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)] {
            if nx < 0 || ny < 0 || nx >= grid.w as i64 || ny >= grid.h as i64 {
                continue;
            }
            let i = ny as usize * grid.w + nx as usize;
            if grid.cells[i] == EMPTY {
                grid.cells[i] = REGION;
                stack.push((nx, ny));
            }
        }
    }
    !open
}

/// Adds the ink cells next to the region to it, for one ring of cells.
fn grow(grid: &mut Grid) {
    let mut add = Vec::new();
    for y in 0..grid.h as i64 {
        for x in 0..grid.w as i64 {
            if grid.at(x, y) == INK
                && [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)]
                    .iter()
                    .any(|&(nx, ny)| grid.at(nx, ny) == REGION)
            {
                add.push(y as usize * grid.w + x as usize);
            }
        }
    }
    for i in add {
        grid.cells[i] = REGION;
    }
}

/// Walks the outer boundary of the region on the grid's corner lattice, with
/// the region on the right-hand side. Returns the corner points.
fn trace(grid: &Grid) -> Option<Vec<(i64, i64)>> {
    let first = grid.cells.iter().position(|&c| c == REGION)?;
    let start = ((first % grid.w) as i64, (first / grid.w) as i64);
    let in_region = |x: i64, y: i64| grid.at(x, y) == REGION;
    // Directions as (dx, dy); the right-hand side on screen is (-dy, dx).
    let mut d = (1i64, 0i64);
    let mut p = start;
    let mut ring = vec![p];
    loop {
        p = (p.0 + d.0, p.1 + d.1);
        let right = (-d.1, d.0);
        let left = (d.1, -d.0);
        // The cell ahead on a side is the one whose corner is `p` and which
        // lies half a step along `d` and half a step toward that side.
        let cell_at = |s: (i64, i64)| {
            let off = |c: i64| if c < 0 { -1 } else { 0 };
            (p.0 + off(d.0 + s.0), p.1 + off(d.1 + s.1))
        };
        let ahead_right = cell_at(right);
        let ahead_left = cell_at(left);
        let nd = if !in_region(ahead_right.0, ahead_right.1) {
            right
        } else if in_region(ahead_left.0, ahead_left.1) {
            left
        } else {
            d
        };
        if p == start && nd == (1, 0) {
            break;
        }
        if nd != d {
            ring.push(p);
        }
        d = nd;
        if ring.len() > grid.w * grid.h + 4 {
            return None;
        }
    }
    Some(ring)
}

/// Douglas-Peucker on a closed ring: split at two far-apart points and
/// simplify each half.
fn simplify_ring(pts: &[(f32, f32)], tol: f32) -> Vec<(f32, f32)> {
    if pts.len() < 4 {
        return pts.to_vec();
    }
    let far = (1..pts.len())
        .max_by(|&a, &b| dist2(pts[0], pts[a]).total_cmp(&dist2(pts[0], pts[b])))
        .unwrap_or(1);
    let first = pts[..=far].to_vec();
    let mut second = pts[far..].to_vec();
    second.push(pts[0]);
    let mut out = simplify_line(&first, tol);
    out.pop();
    out.extend(simplify_line(&second, tol));
    out.pop();
    out
}

fn dist2(a: (f32, f32), b: (f32, f32)) -> f32 {
    (a.0 - b.0).powi(2) + (a.1 - b.1).powi(2)
}

fn simplify_line(pts: &[(f32, f32)], tol: f32) -> Vec<(f32, f32)> {
    let n = pts.len();
    let mut keep = vec![false; n];
    keep[0] = true;
    keep[n - 1] = true;
    let mut stack = vec![(0usize, n - 1)];
    while let Some((a, b)) = stack.pop() {
        let mut best = (0.0f32, a);
        for (i, &p) in pts.iter().enumerate().take(b).skip(a + 1) {
            let d = seg_dist(p, pts[a], pts[b]);
            if d > best.0 {
                best = (d, i);
            }
        }
        if best.0 > tol {
            keep[best.1] = true;
            stack.push((a, best.1));
            stack.push((best.1, b));
        }
    }
    pts.iter()
        .zip(keep)
        .filter_map(|(&p, k)| k.then_some(p))
        .collect()
}

fn seg_dist(p: (f32, f32), a: (f32, f32), b: (f32, f32)) -> f32 {
    let (dx, dy) = (b.0 - a.0, b.1 - a.1);
    let len2 = dx * dx + dy * dy;
    let t = if len2 > 0.0 {
        (((p.0 - a.0) * dx + (p.1 - a.1) * dy) / len2).clamp(0.0, 1.0)
    } else {
        0.0
    };
    ((p.0 - a.0 - dx * t).hypot(p.1 - a.1 - dy * t)).abs()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::{Stroke, StrokeStyle, Tool};
    use crate::InputSample;

    fn line(pts: &[(f32, f32)]) -> Stroke {
        Stroke::new(
            StrokeStyle {
                tool: Tool::Pen,
                color: 0x0000_00FF,
                width: 2.0,
                line: crate::LineStyle::Solid,
            },
            0,
            pts.iter()
                .map(|&(x, y)| InputSample::new(x, y, 1.0, 0.0))
                .collect(),
        )
    }

    fn area(poly: &[(f32, f32)]) -> f32 {
        let n = poly.len();
        (0..n)
            .map(|i| {
                let (a, b) = (poly[i], poly[(i + 1) % n]);
                a.0 * b.1 - b.0 * a.1
            })
            .sum::<f32>()
            .abs()
            / 2.0
    }

    fn page_with(strokes: Vec<Stroke>) -> Page {
        let mut p = Page::new();
        p.layers[0].strokes = strokes;
        p
    }

    const WORLD: (f32, f32, f32, f32) = (0.0, 0.0, 200.0, 200.0);

    #[test]
    fn fills_a_square_drawn_with_four_strokes() {
        let page = page_with(vec![
            line(&[(50.0, 50.0), (150.0, 50.0)]),
            line(&[(150.0, 50.0), (150.0, 150.0)]),
            line(&[(150.0, 150.0), (50.0, 150.0)]),
            line(&[(50.0, 150.0), (50.0, 50.0)]),
        ]);
        let poly = find_fill_region(&page, 100.0, 100.0, WORLD, 0.5, [false; 4]).unwrap();
        // The inner square is 98 wide; the fill reaches about a point under the ink.
        let a = area(&poly);
        assert!((9000.0..10500.0).contains(&a), "area {a}");
    }

    #[test]
    fn open_shape_is_not_enclosed() {
        let page = page_with(vec![line(&[
            (150.0, 50.0),
            (50.0, 50.0),
            (50.0, 150.0),
            (150.0, 150.0),
        ])]);
        assert!(find_fill_region(&page, 100.0, 100.0, WORLD, 0.5, [false; 4]).is_none());
    }

    #[test]
    fn tapping_ink_fills_nothing() {
        let page = page_with(vec![line(&[(50.0, 100.0), (150.0, 100.0)])]);
        assert!(find_fill_region(&page, 100.0, 100.0, WORLD, 0.5, [true; 4]).is_none());
    }

    #[test]
    fn empty_paged_page_fills_to_the_edges() {
        let page = Page::new();
        let b = (0.0, 0.0, 100.0, 100.0);
        let poly = find_fill_region(&page, 30.0, 30.0, b, 0.5, [true; 4]).unwrap();
        let a = area(&poly);
        assert!((9900.0..10100.0).contains(&a), "area {a}");
    }

    #[test]
    fn existing_fills_do_not_block() {
        let mut fill = line(&[(50.0, 50.0), (150.0, 50.0), (150.0, 150.0), (50.0, 150.0)]);
        fill.style.tool = Tool::Fill;
        let page = page_with(vec![fill]);
        let poly = find_fill_region(&page, 100.0, 100.0, WORLD, 0.5, [true; 4]).unwrap();
        assert!(area(&poly) > 39000.0);
    }

    /// Fills on an infinite canvas the way the editor does.
    fn fill_infinite(page: &Page, x: f32, y: f32) -> Option<Vec<(f32, f32)>> {
        let view = FillView {
            scale: 1.0,
            visible: (x - 5000.0, y - 5000.0, x + 5000.0, y + 5000.0),
        };
        let a = search_area(page, x, y, view, None)?;
        find_fill_region(page, x, y, a.bounds, a.cell, a.walls)
    }

    struct Rng(u64);

    impl Rng {
        fn next(&mut self) -> f32 {
            self.0 = self
                .0
                .wrapping_mul(6364136223846793005)
                .wrapping_add(1442695040888963407);
            ((self.0 >> 40) as f32) / (1u64 << 24) as f32
        }

        fn range(&mut self, a: f32, b: f32) -> f32 {
            a + (b - a) * self.next()
        }
    }

    /// A hand-drawn arc of a circle from angle `a0` to `a1` with a point every
    /// 1 to 3 units, a slight wobble, and pressure ramping up and down over
    /// the first and last 15 units.
    fn arc(
        rng: &mut Rng,
        (cx, cy, r): (f32, f32, f32),
        (a0, a1): (f32, f32),
        width: f32,
    ) -> Stroke {
        let len = (a1 - a0).abs() * r;
        let mut pts = Vec::new();
        let mut d = 0.0;
        while d <= len {
            let a = a0 + (a1 - a0) * d / len;
            let rr = r + rng.range(-0.3, 0.3);
            let ramp = (d.min(len - d) / 15.0).clamp(0.0, 1.0);
            let pressure = 0.05 + 0.55 * ramp;
            pts.push(InputSample::new(
                cx + rr * a.cos(),
                cy + rr * a.sin(),
                pressure,
                0.0,
            ));
            d += rng.range(1.0, 3.0);
        }
        let mut s = line(&[]);
        s.style.width = width;
        s.points = pts;
        s
    }

    #[test]
    fn fills_a_circle_larger_than_the_old_search_window() {
        let mut rng = Rng(1);
        let page = page_with(vec![arc(
            &mut rng,
            (0.0, 0.0, 1500.0),
            (0.0, std::f32::consts::TAU + 0.05),
            2.0,
        )]);
        let poly = fill_infinite(&page, 0.0, 0.0).unwrap();
        let a = area(&poly);
        let full = std::f32::consts::PI * 1500.0 * 1500.0;
        assert!(a > full * 0.98 && a < full * 1.02, "area {a}");
    }

    #[test]
    fn fills_a_shape_far_from_the_origin() {
        let mut rng = Rng(2);
        let page = page_with(vec![arc(
            &mut rng,
            (50_000.0, 50_000.0, 300.0),
            (0.0, std::f32::consts::TAU + 0.05),
            2.0,
        )]);
        assert!(fill_infinite(&page, 50_000.0, 50_000.0).is_some());
    }

    #[test]
    fn tap_outside_all_ink_is_not_enclosed() {
        let page = page_with(vec![line(&[
            (50.0, 50.0),
            (150.0, 50.0),
            (150.0, 150.0),
            (50.0, 150.0),
            (50.0, 50.0),
        ])]);
        assert!(fill_infinite(&page, 400.0, 100.0).is_none());
        assert!(fill_infinite(&page, 100.0, 100.0).is_some());
        assert!(fill_infinite(&Page::new(), 0.0, 0.0).is_none());
    }

    #[test]
    fn open_shape_on_an_infinite_canvas_is_not_enclosed() {
        let page = page_with(vec![line(&[
            (150.0, 50.0),
            (50.0, 50.0),
            (50.0, 150.0),
            (150.0, 150.0),
        ])]);
        assert!(fill_infinite(&page, 100.0, 100.0).is_none());
    }

    #[test]
    fn fills_a_loop_drawn_as_tapered_strokes() {
        use std::f32::consts::TAU;
        let mut rng = Rng(3);
        let c = (400.0, 300.0, 120.0);
        // Three strokes whose tapered ends overlap, as in a hand-drawn loop.
        let page = page_with(vec![
            arc(&mut rng, c, (0.0, TAU / 3.0 + 0.2), 2.0),
            arc(&mut rng, c, (TAU / 3.0 - 0.1, 2.0 * TAU / 3.0 + 0.2), 2.0),
            arc(&mut rng, c, (2.0 * TAU / 3.0 - 0.1, TAU + 0.1), 2.0),
        ]);
        assert!(fill_infinite(&page, 400.0, 300.0).is_some());
    }

    fn view(scale: f32, around: (f32, f32), half: f32) -> FillView {
        FillView {
            scale,
            visible: (
                around.0 - half,
                around.1 - half,
                around.0 + half,
                around.1 + half,
            ),
        }
    }

    fn fill_at(
        page: &Page,
        x: f32,
        y: f32,
        view: FillView,
        paper: Option<(f32, f32)>,
    ) -> Option<Vec<(f32, f32)>> {
        let a = search_area(page, x, y, view, paper)?;
        find_fill_region(page, x, y, a.bounds, a.cell, a.walls)
    }

    fn circle_stroke(c: (f32, f32), r: f32, width: f32) -> Stroke {
        let pts: Vec<(f32, f32)> = (0..=720)
            .map(|i| {
                let a = i as f32 / 720.0 * std::f32::consts::TAU;
                (c.0 + r * a.cos(), c.1 + r * a.sin())
            })
            .collect();
        let mut s = line(&pts);
        s.style.width = width;
        s
    }

    #[test]
    fn zoomed_in_fill_hugs_the_ink() {
        let page = page_with(vec![circle_stroke((100.0, 100.0), 15.0, 3.0)]);
        let v = view(10.0, (100.0, 100.0), 60.0);
        let poly = fill_at(&page, 100.0, 100.0, v, None).unwrap();
        let cell = 0.05;
        let far = poly
            .iter()
            .map(|p| (p.0 - 100.0).hypot(p.1 - 100.0))
            .fold(0.0f32, f32::max);
        // The ink's inner edge is at 15 - 1.5; the fill tucks under it a little.
        assert!(far < 15.0 + 1.5 + 2.0 * cell, "reaches {far}");
        assert!(far > 15.0 - 1.5 - 2.0 * cell, "reaches {far}");
    }

    #[test]
    fn small_eye_fills_when_zoomed_in() {
        // A loop of radius 3 drawn 4 wide leaves a hole about 2 across.
        let page = page_with(vec![circle_stroke((50.0, 50.0), 3.0, 4.0)]);
        let v = view(10.0, (50.0, 50.0), 60.0);
        let poly = fill_at(&page, 50.0, 50.0, v, None).unwrap();
        assert!(poly.len() >= 3);
    }

    #[test]
    fn page_edges_are_walls_but_a_cut_window_is_not() {
        let pt = 100.0 * 72.0 / 25.4;
        let paper = Some((pt, pt));
        let page = Page::new();
        // The window covers the whole page: its edges wall the region in.
        let whole = view(1.0, (pt / 2.0, pt / 2.0), pt);
        assert!(fill_at(&page, 30.0, 30.0, whole, paper).is_some());
        // A small window cuts the page: the region leaks past the visible area.
        let small = view(1.0, (30.0, 30.0), 20.0);
        assert!(fill_at(&page, 30.0, 30.0, small, paper).is_none());
    }

    #[test]
    fn a_shape_inside_the_window_fills_on_a_page() {
        let paper = Some((200.0 * 72.0 / 25.4, 200.0 * 72.0 / 25.4));
        let page = page_with(vec![circle_stroke((200.0, 200.0), 30.0, 2.0)]);
        let v = view(4.0, (200.0, 200.0), 60.0);
        assert!(fill_at(&page, 200.0, 200.0, v, paper).is_some());
    }

    #[test]
    fn small_thin_circles_fill_close_to_the_ink() {
        for r in [3.0f32, 6.0, 12.0] {
            for width in [0.5f32, 1.0, 2.0] {
                // Zoomed so the radius is at least 40 screen px.
                let scale = 40.0 / r;
                let page = page_with(vec![circle_stroke((20.0, 20.0), r, width)]);
                let v = view(scale, (20.0, 20.0), 300.0 / scale);
                let poly = fill_at(&page, 20.0, 20.0, v, None)
                    .unwrap_or_else(|| panic!("r {r} width {width} not filled"));
                let tol = 2.0 / scale;
                let far = poly
                    .iter()
                    .map(|p| (p.0 - 20.0).hypot(p.1 - 20.0))
                    .fold(0.0f32, f32::max);
                assert!(
                    far <= r + width / 2.0 + tol,
                    "r {r} width {width} reaches {far}"
                );
                assert!(
                    far >= r - width / 2.0 - tol,
                    "r {r} width {width} reaches {far}"
                );
            }
        }
    }
}
