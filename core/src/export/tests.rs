//! Tests for the PDF export: pages, ink, layers and paper. Source PDF
//! pages are covered in `tests/background.rs`.

mod background;
mod image;
mod paper;
mod sample;

use lopdf::content::Content as Ops;
use lopdf::{Dictionary, Document as Pdf, Object, ObjectId};

use super::*;
use crate::document::{
    Background, Canvas, Id, Orientation, PageSize, PaperSize, Pattern, Stroke, StrokeStyle, Tool,
    MM_TO_PT,
};
use crate::{InputSample, LineStyle};

pub(super) const A4: (f32, f32) = (595.27563, 841.8898);

pub(super) fn paged() -> Document {
    Document::new_paged("Notes", PaperSize::A4, Orientation::Portrait)
}

pub(super) fn style(tool: Tool, color: u32, width: f32, line: LineStyle) -> StrokeStyle {
    StrokeStyle {
        tool,
        color,
        width,
        line,
    }
}

pub(super) fn pen(color: u32) -> StrokeStyle {
    style(Tool::Pen, color, 2.0, LineStyle::Solid)
}

/// Puts a stroke through `points` on a layer of a page.
pub(super) fn draw(
    doc: &mut Document,
    page: usize,
    layer: usize,
    s: StrokeStyle,
    pts: &[(f32, f32)],
) {
    let points = pts
        .iter()
        .map(|&(x, y)| InputSample::new(x, y, 1.0, 0.0))
        .collect();
    doc.pages[page].layers[layer]
        .strokes
        .push(Stroke::new(s, 0, points));
}

pub(super) fn load(bytes: &[u8]) -> Pdf {
    Pdf::load_mem(bytes).expect("the export is a readable pdf")
}

pub(super) fn run(doc: &Document, options: &ExportOptions) -> Pdf {
    load(&export_pdf(doc, options).unwrap())
}

pub(super) fn all() -> ExportOptions {
    ExportOptions::default()
}

pub(super) fn plain() -> ExportOptions {
    ExportOptions {
        backgrounds: false,
        ..all()
    }
}

pub(super) fn pages(pdf: &Pdf) -> Vec<ObjectId> {
    pdf.page_iter().collect()
}

pub(super) fn dict(pdf: &Pdf, id: ObjectId) -> &Dictionary {
    pdf.get_dictionary(id).unwrap()
}

fn numbers(pdf: &Pdf, object: &Object) -> Vec<f32> {
    let (_, object) = pdf.dereference(object).unwrap();
    object
        .as_array()
        .unwrap()
        .iter()
        .map(|o| o.as_float().unwrap())
        .collect()
}

pub(super) fn media(pdf: &Pdf, id: ObjectId) -> Vec<f32> {
    numbers(pdf, dict(pdf, id).get(b"MediaBox").unwrap())
}

pub(super) fn text(pdf: &Pdf, id: ObjectId) -> String {
    String::from_utf8(pdf.get_page_content(id)).unwrap()
}

/// The resources dictionary entry `key` of a page, as a dictionary.
pub(super) fn resource<'a>(pdf: &'a Pdf, id: ObjectId, key: &[u8]) -> Option<&'a Dictionary> {
    let (_, resources) = pdf
        .dereference(dict(pdf, id).get(b"Resources").ok()?)
        .ok()?;
    let (_, table) = pdf
        .dereference(resources.as_dict().ok()?.get(key).ok()?)
        .ok()?;
    table.as_dict().ok()
}

type Matrix = [f32; 6];

fn then(m: &Matrix, c: &Matrix) -> Matrix {
    [
        m[0] * c[0] + m[1] * c[2],
        m[0] * c[1] + m[1] * c[3],
        m[2] * c[0] + m[3] * c[2],
        m[2] * c[1] + m[3] * c[3],
        m[4] * c[0] + m[5] * c[2] + c[4],
        m[4] * c[1] + m[5] * c[3] + c[5],
    ]
}

/// Where every path starts (`m`) in default user space, following `q`, `Q`
/// and `cm`. Drawing a single point stroke gives the point plus its radius.
pub(super) fn path_starts(content: &[u8]) -> Vec<(f32, f32)> {
    let identity = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];
    let (mut ctm, mut stack, mut out) = (identity, Vec::new(), Vec::new());
    for op in Ops::decode(content).unwrap().operations {
        let n: Vec<f32> = op
            .operands
            .iter()
            .filter_map(|o| o.as_float().ok())
            .collect();
        match op.operator.as_str() {
            "q" => stack.push(ctm),
            "Q" => ctm = stack.pop().unwrap_or(identity),
            "cm" => ctm = then(&[n[0], n[1], n[2], n[3], n[4], n[5]], &ctm),
            "m" => out.push((
                n[0] * ctm[0] + n[1] * ctm[2] + ctm[4],
                n[0] * ctm[1] + n[1] * ctm[3] + ctm[5],
            )),
            _ => {}
        }
    }
    out
}

pub(super) fn near(a: (f32, f32), b: (f32, f32), tol: f32) -> bool {
    (a.0 - b.0).abs() <= tol && (a.1 - b.1).abs() <= tol
}

const BLACK: u32 = 0x1A1D_26FF;
const RED: u32 = 0xFF00_00FF;

#[test]
fn a_page_has_its_own_size_and_flips_the_y_axis() {
    let mut doc = paged();
    draw(&mut doc, 0, 0, pen(BLACK), &[(100.0, 100.0)]);
    let pdf = run(&doc, &all());
    let ids = pages(&pdf);
    assert_eq!(ids.len(), 1);
    let m = media(&pdf, ids[0]);
    assert!((m[2] - A4.0).abs() < 0.01 && (m[3] - A4.1).abs() < 0.01);
    let starts = path_starts(&pdf.get_page_content(ids[0]));
    // A dot of radius 1 at (100, 100) down from the top is 100 up from the bottom.
    assert!(starts.iter().any(|&p| near(p, (100.0, A4.1 - 100.0), 1.1)));
}

#[test]
fn every_page_keeps_its_size_and_a_subset_comes_out_in_document_order() {
    let mut doc = paged();
    doc.add_page();
    doc.add_page();
    doc.pages[1].size = Some(PageSize {
        width: 300.0,
        height: 200.0,
    });
    doc.pages[2].size = Some(PageSize {
        width: 100.0,
        height: 400.0,
    });
    let sizes = |opts: &ExportOptions| -> Vec<(f32, f32)> {
        let pdf = run(&doc, opts);
        pages(&pdf)
            .iter()
            .map(|&id| {
                let m = media(&pdf, id);
                (m[2].round(), m[3].round())
            })
            .collect()
    };
    assert_eq!(
        sizes(&all()),
        vec![(595.0, 842.0), (300.0, 200.0), (100.0, 400.0)]
    );
    let subset = ExportOptions {
        pages: Some(vec![2, 0, 2]),
        ..all()
    };
    assert_eq!(sizes(&subset), vec![(595.0, 842.0), (100.0, 400.0)]);
}

#[test]
fn bad_selections_are_errors() {
    let doc = paged();
    let with = |pages| ExportOptions {
        pages: Some(pages),
        ..all()
    };
    assert_eq!(export_pdf(&doc, &with(vec![])), Err(ExportError::NoPages));
    assert_eq!(
        export_pdf(&doc, &with(vec![0, 1])),
        Err(ExportError::PageOutOfRange(1))
    );
}

#[test]
fn fills_sit_beneath_the_ink_of_their_layer() {
    let mut doc = paged();
    let green = 0x00AA_00FF;
    draw(&mut doc, 0, 0, pen(RED), &[(10.0, 10.0), (50.0, 50.0)]);
    let fill = style(Tool::Fill, green, 1.0, LineStyle::Solid);
    draw(
        &mut doc,
        0,
        0,
        fill,
        &[(0.0, 0.0), (80.0, 0.0), (80.0, 80.0), (0.0, 80.0)],
    );
    let pdf = run(&doc, &all());
    let content = text(&pdf, pages(&pdf)[0]);
    let fill_at = content.find("0 0.667 0 rg").expect("the fill colour");
    let ink_at = content.find("1 0 0 rg").expect("the pen colour");
    assert!(fill_at < ink_at, "fill first:\n{content}");
}

#[test]
fn dashed_and_dotted_lines_are_broken_up() {
    let line = [(20.0, 20.0), (220.0, 20.0)];
    let marks = |l: LineStyle| {
        let mut doc = paged();
        draw(&mut doc, 0, 0, style(Tool::Pen, BLACK, 3.0, l), &line);
        let pdf = run(&doc, &all());
        text(&pdf, pages(&pdf)[0])
            .lines()
            .filter(|l| *l == "h")
            .count()
    };
    let solid = marks(LineStyle::Solid);
    let dashed = marks(LineStyle::Dashed);
    let dotted = marks(LineStyle::Dotted);
    assert_eq!(solid, 1);
    assert!(dashed >= 10, "{dashed} dashes");
    assert!(dotted > dashed, "{dotted} dots");
}

#[test]
fn highlighter_alpha_uses_one_shared_graphics_state() {
    let mut doc = paged();
    let yellow = 0xFFE0_0080;
    let hl = style(Tool::Highlighter, yellow, 12.0, LineStyle::Solid);
    draw(&mut doc, 0, 0, hl, &[(10.0, 10.0), (100.0, 10.0)]);
    draw(&mut doc, 0, 0, hl, &[(10.0, 40.0), (100.0, 40.0)]);
    let pdf = run(&doc, &all());
    let id = pages(&pdf)[0];
    let content = text(&pdf, id);
    assert_eq!(content.matches("/GA128 gs").count(), 2);
    let states = resource(&pdf, id, b"ExtGState").unwrap();
    assert_eq!(states.len(), 1);
    let (_, gs) = pdf.dereference(states.get(b"GA128").unwrap()).unwrap();
    let ca = gs
        .as_dict()
        .unwrap()
        .get(b"ca")
        .unwrap()
        .as_float()
        .unwrap();
    assert!((ca - 128.0 / 255.0).abs() < 1e-4);
}

#[test]
fn a_faded_layer_is_one_transparency_group() {
    let mut doc = paged();
    doc.pages[0].layers[0].opacity = 0.5;
    draw(&mut doc, 0, 0, pen(RED), &[(10.0, 10.0), (100.0, 100.0)]);
    draw(&mut doc, 0, 0, pen(RED), &[(10.0, 100.0), (100.0, 10.0)]);
    let top = doc.pages[0].add_layer("Top");
    assert!(top != Id::nil());
    draw(
        &mut doc,
        0,
        1,
        pen(0x0000_FFFF),
        &[(200.0, 200.0), (210.0, 210.0)],
    );
    let pdf = run(&doc, &all());
    let id = pages(&pdf)[0];
    let content = text(&pdf, id);
    assert!(content.contains("/GA128 gs\n/Fm0 Do"), "{content}");
    // The faded layer's paths live in the group, the opaque layer's do not.
    assert_eq!(path_starts(content.as_bytes()).len(), 1);
    let forms = resource(&pdf, id, b"XObject").unwrap();
    assert_eq!(forms.len(), 1);
    let (_, form) = pdf.dereference(forms.get(b"Fm0").unwrap()).unwrap();
    let Object::Stream(form) = form else {
        panic!("the group is a form xobject");
    };
    assert_eq!(
        form.dict.get(b"Subtype").unwrap().as_name().unwrap(),
        b"Form"
    );
    let group = form.dict.get(b"Group").unwrap().as_dict().unwrap();
    assert_eq!(group.get(b"S").unwrap().as_name().unwrap(), b"Transparency");
    let inner = String::from_utf8(form.decompressed_content().unwrap()).unwrap();
    assert_eq!(
        inner.matches(" f\n").count() + inner.matches("\nf\n").count(),
        2
    );
    assert!(form.dict.get(b"Resources").is_ok());
}

#[test]
fn hidden_and_transparent_layers_are_skipped() {
    let mut doc = paged();
    draw(&mut doc, 0, 0, pen(RED), &[(10.0, 10.0), (50.0, 50.0)]);
    doc.pages[0].layers[0].visible = false;
    let l2 = doc.pages[0].add_layer("Ghost");
    draw(&mut doc, 0, 1, pen(RED), &[(10.0, 10.0), (50.0, 50.0)]);
    doc.pages[0].layer_mut(l2).unwrap().opacity = 0.0;
    let pdf = run(&doc, &plain());
    let content = text(&pdf, pages(&pdf)[0]);
    assert!(path_starts(content.as_bytes()).is_empty());
    assert!(!content.contains(" rg"));
}
