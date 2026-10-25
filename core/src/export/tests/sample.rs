//! Writes sample exports for looking at in a PDF viewer. Run with
//! `GRAPHO_SAMPLE_DIR=/some/dir cargo test -p grapho-core sample -- --ignored`.

use std::sync::Arc;

use lopdf::{dictionary, Object, Stream};

use super::*;
use crate::document::{Layer, Page, PdfPage, PdfSource};
use crate::pdf::fixtures::{build, bytes, Spec};

fn wave(x0: f32, y0: f32, len: f32, amp: f32) -> Vec<(f32, f32)> {
    (0..=80)
        .map(|i| {
            let t = i as f32 / 80.0;
            (x0 + t * len, y0 + amp * (t * 12.0).sin())
        })
        .collect()
}

fn inked(doc: &mut Document, page: usize) {
    let blue = 0x1F4E_D8FF;
    draw(
        doc,
        page,
        0,
        pen(0x0000_00FF),
        &wave(40.0, 80.0, 300.0, 14.0),
    );
    draw(doc, page, 0, pen(blue), &wave(40.0, 130.0, 300.0, 10.0));
    let dashed = style(Tool::Pen, RED, 3.0, LineStyle::Dashed);
    draw(doc, page, 0, dashed, &[(40.0, 180.0), (340.0, 180.0)]);
    let dotted = style(Tool::Pen, 0x0B8A_3EFF, 3.0, LineStyle::Dotted);
    draw(doc, page, 0, dotted, &[(40.0, 200.0), (340.0, 200.0)]);
    let fill = style(Tool::Fill, 0xFFB3_0080, 1.0, LineStyle::Solid);
    draw(
        doc,
        page,
        0,
        fill,
        &[(60.0, 230.0), (200.0, 230.0), (130.0, 330.0)],
    );
    let marker = style(Tool::Highlighter, 0xFFE6_0066, 22.0, LineStyle::Solid);
    draw(doc, page, 0, marker, &[(40.0, 90.0), (340.0, 100.0)]);
    draw(doc, page, 0, marker, &[(40.0, 100.0), (340.0, 85.0)]);

    let mut faded = Layer::new("Faded");
    faded.opacity = 0.4;
    doc.pages[page].layers.push(faded);
    let last = doc.pages[page].layers.len() - 1;
    let thick = style(Tool::Pen, 0xC026_D3FF, 18.0, LineStyle::Solid);
    draw(doc, page, last, thick, &[(200.0, 240.0), (320.0, 320.0)]);
    draw(doc, page, last, thick, &[(200.0, 320.0), (320.0, 240.0)]);

    let mut hidden = Layer::new("Hidden");
    hidden.visible = false;
    doc.pages[page].layers.push(hidden);
    let last = last + 1;
    draw(doc, page, last, pen(RED), &[(0.0, 0.0), (400.0, 400.0)]);
}

fn lecture() -> Vec<u8> {
    let mut pdf = build(
        &[
            Spec {
                media: Some([0.0, 0.0, 400.0, 300.0]),
                ..Spec::default()
            },
            Spec {
                media: Some([0.0, 0.0, 400.0, 300.0]),
                rotate: Some(90),
                ..Spec::default()
            },
            Spec {
                media: Some([0.0, 0.0, 612.0, 792.0]),
                crop: Some([100.0, 100.0, 500.0, 400.0]),
                ..Spec::default()
            },
        ],
        None,
    );
    let font = pdf.add_object(dictionary! {
        "Type" => "Font", "Subtype" => "Type1", "BaseFont" => "Helvetica",
    });
    for id in pdf.get_pages().into_values().collect::<Vec<_>>() {
        let content = "0.9 0.95 1 rg 20 20 360 260 re f 0 g\n\
            BT /F1 24 Tf 120 250 Td (Original page) Tj ET\n\
            BT /F1 12 Tf 120 220 Td (Vector text stays text) Tj ET\n\
            1 0 0 RG 2 w 20 20 360 260 re S";
        pdf.add_page_contents(id, content.as_bytes().to_vec())
            .unwrap();
        let page = pdf.get_dictionary_mut(id).unwrap();
        page.set(
            "Resources",
            dictionary! { "Font" => dictionary! { "F1" => font } },
        );
    }
    let look = Stream::new(
        dictionary! {
            "Type" => "XObject", "Subtype" => "Form",
            "BBox" => vec![Object::Integer(0), Object::Integer(0), Object::Integer(10), Object::Integer(10)],
        },
        b"0.2 0.7 0.2 rg 0 0 10 10 re f".to_vec(),
    );
    let look = pdf.add_object(look);
    let note = pdf.add_object(dictionary! {
        "Type" => "Annot", "Subtype" => "Stamp",
        "Rect" => vec![Object::Integer(300), Object::Integer(30), Object::Integer(360), Object::Integer(60)],
        "AP" => dictionary! { "N" => look },
    });
    let first = pdf.get_pages()[&1];
    pdf.get_dictionary_mut(first)
        .unwrap()
        .set("Annots", vec![Object::Reference(note)]);
    bytes(&mut pdf)
}

fn write(name: &str, doc: &Document, options: &ExportOptions) {
    let dir = std::env::var("GRAPHO_SAMPLE_DIR").expect("set GRAPHO_SAMPLE_DIR");
    let data = export_pdf(doc, options).unwrap();
    std::fs::write(format!("{dir}/{name}.pdf"), data).unwrap();
}

#[test]
#[ignore = "writes files for looking at"]
fn write_samples() {
    let mut cream = paged();
    cream.canvas.set_background(Background {
        color: 0xF6F0_E1FF,
        pattern: Pattern::Grid { spacing_mm: 6.0 },
    });
    inked(&mut cream, 0);
    write("paper-cream-grid", &cream, &all());
    write("paper-cream-grid-plain", &cream, &plain());

    let mut dark = paged();
    dark.canvas.set_background(Background {
        color: 0x1C1D_21FF,
        pattern: Pattern::Dotted { spacing_mm: 6.0 },
    });
    inked(&mut dark, 0);
    write("paper-dark-dotted", &dark, &all());

    let data = lecture();
    let sizes = crate::pdf::read_page_sizes(&data).unwrap();
    let mut note = paged();
    note.pages.clear();
    let id = Id::new_v4();
    for (index, size) in sizes.iter().enumerate() {
        let mut page = Page::with_size(*size);
        page.pdf = Some(PdfPage {
            source: id,
            index: index as u32,
        });
        note.pages.push(page);
    }
    note.pdfs.push(PdfSource {
        id,
        name: "lecture.pdf".into(),
        pages: 3,
        bytes: Arc::new(data),
    });
    for page in 0..3 {
        // A cross at the displayed top left and a line along the top.
        draw(
            &mut note,
            page,
            0,
            pen(0x1F4E_D8FF),
            &[(10.0, 10.0), (60.0, 60.0)],
        );
        draw(
            &mut note,
            page,
            0,
            pen(0x1F4E_D8FF),
            &[(60.0, 10.0), (10.0, 60.0)],
        );
        draw(&mut note, page, 0, pen(RED), &[(70.0, 20.0), (300.0, 20.0)]);
        let marker = style(Tool::Highlighter, 0xFFE6_0066, 18.0, LineStyle::Solid);
        draw(&mut note, page, 0, marker, &[(115.0, 55.0), (280.0, 55.0)]);
    }
    write("pdf-pages", &note, &all());
    write("pdf-pages-plain", &note, &plain());

    let mut board = Document::new_infinite("Board");
    board.canvas.set_background(Background {
        color: 0xFFFF_FFFF,
        pattern: Pattern::Lined { spacing_mm: 8.0 },
    });
    draw(
        &mut board,
        0,
        0,
        pen(0x1F4E_D8FF),
        &wave(500.0, 800.0, 300.0, 30.0),
    );
    draw(&mut board, 0, 0, pen(RED), &wave(520.0, 900.0, 260.0, 20.0));
    write("infinite", &board, &all());
}
