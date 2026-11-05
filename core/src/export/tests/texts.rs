//! Text boxes in the export.

use super::*;
use crate::document::{TextBox, TextStyle};

fn with_box(doc: &mut Document) -> Id {
    let t = TextBox::new(10.0, 10.0, 100.0, 30.0, "hi", TextStyle::default());
    let id = t.id;
    doc.pages[0].layers[0].texts.push(t);
    id
}

fn outlines(id: Id) -> ExportOptions {
    let square = vec![4.0, 10.0, 10.0, 30.0, 10.0, 30.0, 30.0, 10.0, 30.0];
    ExportOptions {
        texts: HashMap::from([(id, square)]),
        ..all()
    }
}

#[test]
fn a_text_box_is_drawn_from_its_outlines() {
    let mut doc = paged();
    let id = with_box(&mut doc);
    let pdf = run(&doc, &outlines(id));
    let content = text(&pdf, pages(&pdf)[0]);
    assert!(content.contains("0.102 0.114 0.149 rg"), "{content}");
    assert_eq!(path_starts(&pdf.get_page_content(pages(&pdf)[0])).len(), 1);
}

#[test]
fn a_box_without_outlines_is_left_out() {
    let mut doc = paged();
    with_box(&mut doc);
    let pdf = run(&doc, &all());
    assert!(path_starts(&pdf.get_page_content(pages(&pdf)[0])).is_empty());
}

#[test]
fn text_sits_under_the_strokes_of_its_layer() {
    let mut doc = paged();
    let id = with_box(&mut doc);
    draw(
        &mut doc,
        0,
        0,
        pen(0xFF00_00FF),
        &[(5.0, 5.0), (90.0, 90.0)],
    );
    let pdf = run(&doc, &outlines(id));
    let content = text(&pdf, pages(&pdf)[0]);
    let text_at = content.find("0.102 0.114 0.149 rg").unwrap();
    let ink_at = content.find("1 0 0 rg").unwrap();
    assert!(text_at < ink_at);
}
