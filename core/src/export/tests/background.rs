//! Pages that carry a source PDF page: geometry, content and annotations.

use std::sync::Arc;

use lopdf::{dictionary, Document as Pdf, Object, Stream};

use super::*;
use crate::document::{Page, PdfPage, PdfSource};
use crate::pdf::fixtures::{build, bytes, Spec};
use crate::pdf::read_page_sizes;

/// A PDF whose page `i` paints a rectangle that starts at x = `mark` + `i`,
/// and whose pages carry a graphics state named G0 with this alpha.
fn source(specs: &[Spec], mark: u32, alpha: f32) -> Vec<u8> {
    let mut pdf = build(specs, None);
    let gs = pdf.add_object(dictionary! { "Type" => "ExtGState", "ca" => alpha });
    for (i, id) in pdf.get_pages().into_values().enumerate() {
        let content = format!("0 0 1 rg {} 11 50 50 re f", mark + i as u32);
        pdf.add_page_contents(id, content.into_bytes()).unwrap();
        let page = pdf.get_dictionary_mut(id).unwrap();
        page.set(
            "Resources",
            dictionary! { "ExtGState" => dictionary! { "G0" => gs } },
        );
    }
    bytes(&mut pdf)
}

/// A note whose pages show every page of each source, in order.
fn note(sources: Vec<Vec<u8>>) -> Document {
    let mut doc = paged();
    doc.pages.clear();
    for (n, data) in sources.into_iter().enumerate() {
        let sizes = read_page_sizes(&data).unwrap();
        let id = Id::new_v4();
        for (index, size) in sizes.iter().enumerate() {
            let mut page = Page::with_size(*size);
            page.pdf = Some(PdfPage {
                source: id,
                index: index as u32,
            });
            doc.pages.push(page);
        }
        doc.pdfs.push(PdfSource {
            id,
            name: format!("s{n}.pdf"),
            pages: sizes.len() as u32,
            bytes: Arc::new(data),
        });
    }
    doc
}

fn has_object_containing(pdf: &Pdf, needle: &str) -> bool {
    pdf.objects.values().any(|o| match o {
        Object::Stream(s) => {
            String::from_utf8_lossy(&s.decompressed_content().unwrap_or_default()).contains(needle)
        }
        _ => false,
    })
}

fn media_spec(w: f32, h: f32) -> Spec {
    Spec {
        media: Some([0.0, 0.0, w, h]),
        ..Spec::default()
    }
}

#[test]
fn ink_lands_where_it_was_drawn_on_every_rotation() {
    // A 200 by 400 crop box that is not at the origin of its 300 by 500 media box.
    let page = |turns| Spec {
        crop: Some([20.0, 30.0, 220.0, 430.0]),
        rotate: Some(turns),
        ..media_spec(300.0, 500.0)
    };
    // Where (50, 70) on the displayed page lands in the page's own space.
    let expected = [
        (0, (70.0, 360.0)),
        (90, (90.0, 80.0)),
        (180, (170.0, 100.0)),
        (270, (150.0, 380.0)),
    ];
    for (turns, point) in expected {
        let mut doc = note(vec![source(&[page(turns)], 70, 0.3)]);
        draw(&mut doc, 0, 0, pen(BLACK), &[(50.0, 70.0)]);
        let pdf = run(&doc, &all());
        let id = pages(&pdf)[0];
        let starts = path_starts(&pdf.get_page_content(id));
        assert!(
            starts.iter().any(|&p| near(p, point, 1.1)),
            "rotate {turns}: {starts:?} should hold {point:?}"
        );
        let page = dict(&pdf, id);
        assert_eq!(media(&pdf, id), vec![20.0, 30.0, 220.0, 430.0]);
        let rotate = page.get(b"Rotate").and_then(|r| r.as_i64()).unwrap_or(0);
        assert_eq!(rotate, turns);
    }
}

#[test]
fn original_content_is_kept_and_wrapped_with_backgrounds_only() {
    let mut doc = note(vec![source(&[media_spec(300.0, 500.0)], 70, 0.3)]);
    draw(&mut doc, 0, 0, pen(RED), &[(50.0, 70.0), (90.0, 90.0)]);
    let pdf = run(&doc, &all());
    let content = text(&pdf, pages(&pdf)[0]);
    let own = content
        .find("0 0 1 rg 70 11 50 50 re f")
        .expect("the original content");
    assert!(
        content.starts_with("q\nq\n1 1 1 rg\n0 0 300 500 re\nf\nQ\n"),
        "{content}"
    );
    let closed = content[own..]
        .find("\nQ\n")
        .map(|i| i + own)
        .expect("the original is closed");
    let ink = content.find("1 0 0 rg").unwrap();
    assert!(
        own < closed && closed < ink,
        "ink after the original:\n{content}"
    );

    let pdf = run(&doc, &plain());
    let id = pages(&pdf)[0];
    assert!(!text(&pdf, id).contains("50 50 re"));
    assert!(!has_object_containing(&pdf, "70 11 50 50"));
    assert_eq!(media(&pdf, id), vec![0.0, 0.0, 300.0, 500.0]);
    assert!(dict(&pdf, id).get(b"Rotate").is_err());
}

#[test]
fn without_backgrounds_a_turned_page_keeps_its_displayed_size() {
    let turned = Spec {
        rotate: Some(90),
        ..media_spec(300.0, 500.0)
    };
    let doc = note(vec![source(&[turned], 70, 0.3)]);
    let pdf = run(&doc, &plain());
    assert_eq!(media(&pdf, pages(&pdf)[0]), vec![0.0, 0.0, 500.0, 300.0]);
}

#[test]
fn sources_merge_without_clashing_and_only_exported_pages_are_copied() {
    let a = source(
        &[media_spec(300.0, 400.0), media_spec(200.0, 200.0)],
        70,
        0.3,
    );
    let b = source(
        &[media_spec(300.0, 400.0), media_spec(250.0, 250.0)],
        90,
        0.6,
    );
    let doc = note(vec![a, b]);
    assert_eq!(doc.pages.len(), 4);
    let subset = ExportOptions {
        pages: Some(vec![0, 3]),
        ..all()
    };
    let pdf = run(&doc, &subset);
    let ids = pages(&pdf);
    assert_eq!(ids.len(), 2);
    assert!(text(&pdf, ids[0]).contains("70 11 50 50 re"));
    assert!(text(&pdf, ids[1]).contains("91 11 50 50 re"));
    assert!(
        !has_object_containing(&pdf, "71 11 50 50"),
        "page 2 was not exported"
    );
    assert!(
        !has_object_containing(&pdf, "90 11 50 50"),
        "page 3 was not exported"
    );
    let alpha = |id| {
        let gs = resource(&pdf, id, b"ExtGState")
            .unwrap()
            .get(b"G0")
            .unwrap();
        let (_, gs) = pdf.dereference(gs).unwrap();
        gs.as_dict()
            .unwrap()
            .get(b"ca")
            .unwrap()
            .as_float()
            .unwrap()
    };
    assert!((alpha(ids[0]) - 0.3).abs() < 1e-4);
    assert!((alpha(ids[1]) - 0.6).abs() < 1e-4);
    // No catalog, outline or page tree of a source came along.
    let catalogs = pdf
        .objects
        .values()
        .filter(|o| {
            o.as_dict()
                .is_ok_and(|d| d.get(b"Type").and_then(|t| t.as_name()).ok() == Some(b"Catalog"))
        })
        .count();
    assert_eq!(catalogs, 1);
}

#[test]
fn a_page_shared_by_two_note_pages_keeps_working() {
    let data = source(&[media_spec(300.0, 400.0)], 70, 0.3);
    let mut doc = note(vec![data]);
    let again = doc.pages[0].clone();
    doc.pages.push(Page {
        id: Id::new_v4(),
        ..again
    });
    let pdf = run(&doc, &all());
    let ids = pages(&pdf);
    assert_eq!(ids.len(), 2);
    for id in ids {
        assert!(text(&pdf, id).contains("70 11 50 50 re"));
    }
}

#[test]
fn a_missing_source_is_an_error() {
    let mut doc = note(vec![source(&[media_spec(300.0, 400.0)], 70, 0.3)]);
    doc.pdfs.clear();
    assert_eq!(export_pdf(&doc, &all()), Err(ExportError::MissingSource));
    // Without backgrounds the source is not needed.
    assert!(export_pdf(&doc, &plain()).is_ok());
}

#[test]
fn a_faded_layer_on_a_pdf_page_is_grouped_in_displayed_units() {
    let turned = Spec {
        rotate: Some(90),
        ..media_spec(300.0, 500.0)
    };
    let mut doc = note(vec![source(&[turned], 70, 0.3)]);
    doc.pages[0].layers[0].opacity = 0.25;
    draw(&mut doc, 0, 0, pen(RED), &[(50.0, 70.0), (90.0, 90.0)]);
    let pdf = run(&doc, &all());
    let id = pages(&pdf)[0];
    let forms = resource(&pdf, id, b"XObject").unwrap();
    let (_, form) = pdf.dereference(forms.get(b"Fm0").unwrap()).unwrap();
    let Object::Stream(form) = form else {
        panic!("a group is a stream");
    };
    let bbox: Vec<f32> = form
        .dict
        .get(b"BBox")
        .unwrap()
        .as_array()
        .unwrap()
        .iter()
        .map(|o| o.as_float().unwrap())
        .collect();
    assert_eq!(bbox, vec![0.0, 0.0, 500.0, 300.0]);
    assert!(text(&pdf, id).contains("/GA64 gs\n/Fm0 Do"));
}

#[test]
fn links_to_the_web_stay_and_other_annotations_are_drawn_into_the_page() {
    let mut pdf = build(&[media_spec(300.0, 400.0), media_spec(300.0, 400.0)], None);
    let ids: Vec<_> = pdf.get_pages().into_values().collect();
    pdf.add_page_contents(ids[0], b"0 0 1 rg 70 11 50 50 re f".to_vec())
        .unwrap();
    let web = pdf.add_object(dictionary! {
        "Type" => "Annot", "Subtype" => "Link",
        "Rect" => vec![0.into(), 0.into(), 10.into(), 10.into()],
        "A" => dictionary! { "S" => "URI", "URI" => Object::string_literal("https://example.org") },
    });
    let jump = pdf.add_object(dictionary! {
        "Type" => "Annot", "Subtype" => "Link",
        "Rect" => vec![0.into(), 0.into(), 10.into(), 10.into()],
        "Dest" => vec![Object::Reference(ids[1]), "Fit".into()],
    });
    let look = Stream::new(
        dictionary! {
            "Type" => "XObject", "Subtype" => "Form",
            "BBox" => vec![0.into(), 0.into(), 10.into(), 10.into()],
        },
        b"1 0 0 rg 0 0 10 10 re f".to_vec(),
    );
    let look = pdf.add_object(look);
    let stamp = pdf.add_object(dictionary! {
        "Type" => "Annot", "Subtype" => "Stamp",
        "Rect" => vec![100.into(), 200.into(), 130.into(), 220.into()],
        "AP" => dictionary! { "N" => look },
        "Parent" => ids[1],
        "P" => ids[1],
    });
    let hidden = pdf.add_object(dictionary! {
        "Type" => "Annot", "Subtype" => "Stamp", "F" => 2,
        "Rect" => vec![0.into(), 0.into(), 30.into(), 20.into()],
        "AP" => dictionary! { "N" => look },
    });
    let page = pdf.get_dictionary_mut(ids[0]).unwrap();
    page.set(
        "Annots",
        vec![web.into(), jump.into(), stamp.into(), hidden.into()],
    );
    let doc = note(vec![bytes(&mut pdf)]);
    let first = ExportOptions {
        pages: Some(vec![0]),
        ..all()
    };
    let out = run(&doc, &first);
    let id = pages(&out)[0];

    let annots = dict(&out, id).get(b"Annots").unwrap().as_array().unwrap();
    assert_eq!(annots.len(), 1, "only the web link is kept");
    let (_, link) = out.dereference(&annots[0]).unwrap();
    let action = link
        .as_dict()
        .unwrap()
        .get(b"A")
        .unwrap()
        .as_dict()
        .unwrap();
    assert_eq!(action.get(b"S").unwrap().as_name().unwrap(), b"URI");

    let content = text(&out, id);
    assert_eq!(content.matches("/GrAnnot").count(), 1, "{content}");
    assert!(
        content.contains("3 0 0 2 100 200 cm\n/GrAnnot0 Do"),
        "{content}"
    );
    let table = resource(&out, id, b"XObject").unwrap();
    assert!(table.get(b"GrAnnot0").is_ok());
    // The page the stamp pointed at was not pulled in.
    let page_objects = out
        .objects
        .values()
        .filter(|o| {
            o.as_dict()
                .is_ok_and(|d| d.get(b"Type").and_then(|t| t.as_name()).ok() == Some(b"Page"))
        })
        .count();
    assert_eq!(page_objects, 1);
}
