//! Turning a page's strokes into vector paths, ordered and grouped the way
//! the screen draws them.

use std::collections::BTreeSet;

use super::colour::display_color;
use super::content::Content;
use super::image::{self, ImageInk};
use crate::document::{ImageSource, Page, Tool};
use crate::{outline_styled, Rect};

/// How far an outline may stray from the true shape, in points. The screen
/// uses the same value.
const TOLERANCE: f32 = 0.02;

/// One stroke as filled polygons in document units.
pub(super) struct StrokeInk {
    /// `0xRRGGBBAA`, as stored.
    color: u32,
    /// Packed polygons, see [`Content::polygons`].
    polys: Vec<f32>,
}

/// A visible layer's images and strokes (fills first), and its opacity as 1
/// to 255.
pub(super) struct LayerInk {
    alpha: u8,
    images: Vec<ImageInk>,
    strokes: Vec<StrokeInk>,
}

/// Everything a page draws, before any colour adaption.
pub(super) struct PageInk {
    layers: Vec<LayerInk>,
    /// Bounds of all ink including stroke width and image corners, empty
    /// when there is none.
    pub bounds: Rect,
}

/// The outlines of the visible layers of a page. Hidden and fully
/// transparent layers are left out, and within a layer the images come
/// first, then the fills, so both sit beneath its ink, as on screen. An
/// image whose source is not in `sources` is left out.
pub(super) fn collect(page: &Page, sources: &[ImageSource]) -> PageInk {
    let mut bounds = Rect::EMPTY;
    let mut layers = Vec::new();
    for layer in page.layers.iter().filter(|l| l.visible) {
        let alpha = (layer.opacity.clamp(0.0, 1.0) * 255.0).round() as u8;
        if alpha == 0 {
            continue;
        }
        let is_fill = |tool: Tool| tool == Tool::Fill;
        let fills = layer.strokes.iter().filter(|s| is_fill(s.style.tool));
        let inks = layer.strokes.iter().filter(|s| !is_fill(s.style.tool));
        let mut images = Vec::with_capacity(layer.images.len());
        for i in &layer.images {
            let Some(source) = sources.iter().position(|s| s.id == i.source) else {
                continue;
            };
            for (x, y) in i.corners() {
                bounds.add_circle(x, y, 0.0);
            }
            images.push(ImageInk {
                source,
                transform: i.transform,
                crop: i.crop,
            });
        }
        let mut strokes = Vec::with_capacity(layer.strokes.len());
        for s in fills.chain(inks) {
            if s.style.color & 0xFF == 0 {
                continue;
            }
            let mut polys = Vec::new();
            let b = if s.style.tool == Tool::Fill {
                // A fill's points are already its polygon.
                let mut b = Rect::EMPTY;
                polys.push(s.points.len() as f32);
                for p in &s.points {
                    polys.extend_from_slice(&[p.x, p.y]);
                    b.add_circle(p.x, p.y, 0.0);
                }
                b
            } else {
                outline_styled(
                    &s.points,
                    &s.style.brush(),
                    s.style.line,
                    TOLERANCE,
                    &mut polys,
                )
            };
            if polys.is_empty() {
                continue;
            }
            bounds = bounds.union(&b);
            strokes.push(StrokeInk {
                color: s.style.color,
                polys,
            });
        }
        if !strokes.is_empty() || !images.is_empty() {
            layers.push(LayerInk {
                alpha,
                images,
                strokes,
            });
        }
    }
    PageInk { layers, bounds }
}

/// A faded layer's content, drawn once as a transparency group.
pub(super) struct Group {
    pub content: Vec<u8>,
    /// The image sources it draws.
    pub images: BTreeSet<usize>,
}

/// Ink rendered to content stream text.
pub(super) struct InkContent {
    /// Draws the whole page's ink in document units.
    pub content: Content,
    /// The groups it paints, as `/Fm{index}`.
    pub groups: Vec<Group>,
    /// The alphas it needs as ExtGStates, as `/GA{alpha}`.
    pub alphas: BTreeSet<u8>,
    /// The image sources the page itself draws, outside any group.
    pub images: BTreeSet<usize>,
}

impl PageInk {
    pub fn is_empty(&self) -> bool {
        self.layers.is_empty()
    }

    /// Writes the ink with colours adapted to `paper`. A layer below full
    /// opacity becomes a group of its own, painted at that opacity, so
    /// overlaps inside it don't show through each other.
    pub fn render(&self, paper: u32) -> InkContent {
        let mut out = InkContent {
            content: Content::new(),
            groups: Vec::new(),
            alphas: BTreeSet::new(),
            images: BTreeSet::new(),
        };
        for layer in &self.layers {
            if layer.alpha == u8::MAX {
                write_images(&mut out.content, &layer.images, &mut out.images);
                write_strokes(&mut out.content, &layer.strokes, paper, &mut out.alphas);
                continue;
            }
            let mut inner = Content::new();
            let mut used = BTreeSet::new();
            write_images(&mut inner, &layer.images, &mut used);
            write_strokes(&mut inner, &layer.strokes, paper, &mut out.alphas);
            out.alphas.insert(layer.alpha);
            out.content.raw("q");
            out.content.raw(&format!("/GA{} gs", layer.alpha));
            out.content.raw(&format!("/Fm{} Do", out.groups.len()));
            out.content.raw("Q");
            out.groups.push(Group {
                content: inner.into_bytes(),
                images: used,
            });
        }
        out
    }
}

fn write_images(c: &mut Content, images: &[ImageInk], used: &mut BTreeSet<usize>) {
    for i in images {
        used.insert(i.source);
        image::draw(c, i);
    }
}

fn write_strokes(c: &mut Content, strokes: &[StrokeInk], paper: u32, alphas: &mut BTreeSet<u8>) {
    // The fill colour carries over between opaque strokes.
    let mut current = None;
    for s in strokes {
        let color = display_color(s.color, paper);
        let alpha = (color & 0xFF) as u8;
        if alpha == u8::MAX {
            if current != Some(color) {
                c.color(color, "rg");
                current = Some(color);
            }
            c.polygons(&s.polys);
            c.raw("f");
        } else {
            alphas.insert(alpha);
            c.raw("q");
            c.raw(&format!("/GA{alpha} gs"));
            c.color(color, "rg");
            c.polygons(&s.polys);
            c.raw("f");
            c.raw("Q");
        }
    }
}
