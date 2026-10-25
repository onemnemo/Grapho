//! Paper, pattern, infinite canvases and the file information.

use super::*;

#[test]
fn dark_paper_flips_black_ink_only_with_backgrounds() {
    let mut doc = paged();
    doc.canvas.set_background(Background {
        color: 0x1C1D_21FF,
        pattern: Pattern::Lined { spacing_mm: 8.0 },
    });
    draw(
        &mut doc,
        0,
        0,
        pen(0x0000_00FF),
        &[(10.0, 10.0), (50.0, 50.0)],
    );
    let on = run(&doc, &all());
    let content = text(&on, pages(&on)[0]);
    assert!(content.contains("0.925 0.925 0.925 rg"), "{content}");
    assert!(content.contains("0.11 0.114 0.129 rg"), "the paper");
    let off = run(&doc, &plain());
    let content = text(&off, pages(&off)[0]);
    assert!(!content.contains("0.925"));
    assert!(content.contains("0 0 0 rg"));
    assert!(
        !content.contains("0.11 0.114 0.129"),
        "no paper without backgrounds"
    );
}

#[test]
fn patterns_follow_the_screen_spacing() {
    let count = |pattern| {
        let mut doc = paged();
        doc.canvas.set_background(Background {
            color: 0xFFFF_FFFF,
            pattern,
        });
        let pdf = run(&doc, &all());
        path_starts(&pdf.get_page_content(pages(&pdf)[0])).len()
    };
    let spacing_mm = 10.0;
    let step = spacing_mm * MM_TO_PT;
    let rows = (A4.1 / step).floor() as usize + 1;
    let cols = (A4.0 / step).floor() as usize + 1;
    assert_eq!(count(Pattern::Blank), 0);
    assert_eq!(count(Pattern::Lined { spacing_mm }), rows);
    assert_eq!(count(Pattern::Grid { spacing_mm }), rows + cols);
    assert_eq!(count(Pattern::Dotted { spacing_mm }), rows * cols);
}

#[test]
fn an_infinite_canvas_is_one_page_around_its_ink() {
    let mut doc = Document::new_infinite("Board");
    draw(
        &mut doc,
        0,
        0,
        pen(BLACK),
        &[(1000.0, 2000.0), (1100.0, 2060.0)],
    );
    let pdf = run(&doc, &all());
    let ids = pages(&pdf);
    assert_eq!(ids.len(), 1);
    let m = media(&pdf, ids[0]);
    // 100 by 60 of ink, 2 wide, plus a 24 margin on each side.
    assert!(
        (m[2] - 150.0).abs() < 0.2 && (m[3] - 110.0).abs() < 0.2,
        "{m:?}"
    );
    // The left end of the line sits one margin plus a radius in from the edge.
    let starts = path_starts(&pdf.get_page_content(ids[0]));
    assert!(starts
        .iter()
        .all(|p| p.0 >= 0.0 && p.0 <= m[2] && p.1 >= 0.0 && p.1 <= m[3]));
    assert!(starts
        .iter()
        .any(|&p| p.0 < 26.0 && (p.1 - (m[3] - 24.0)).abs() < 2.0));
}

#[test]
fn an_empty_infinite_canvas_is_a_default_page() {
    let doc = Document::new_infinite("Board");
    let pdf = run(&doc, &all());
    assert_eq!(media(&pdf, pages(&pdf)[0]), vec![0.0, 0.0, 595.0, 842.0]);
}

#[test]
fn the_info_names_the_producer_and_the_note() {
    let mut doc = paged();
    doc.title = "Lecture 3".into();
    let pdf = run(&doc, &all());
    let (_, info) = pdf.dereference(pdf.trailer.get(b"Info").unwrap()).unwrap();
    let info = info.as_dict().unwrap();
    assert_eq!(info.get(b"Producer").unwrap().as_str().unwrap(), b"Grapho");
    assert_eq!(info.get(b"Title").unwrap().as_str().unwrap(), b"Lecture 3");

    doc.title = "Brøk".into();
    let pdf = run(&doc, &all());
    let (_, info) = pdf.dereference(pdf.trailer.get(b"Info").unwrap()).unwrap();
    let title = info
        .as_dict()
        .unwrap()
        .get(b"Title")
        .unwrap()
        .as_str()
        .unwrap();
    assert_eq!(title, [0xFE, 0xFF, 0, b'B', 0, b'r', 0, 0xF8, 0, b'k']);
}

#[test]
fn content_is_compressed() {
    let mut doc = paged();
    let wave: Vec<(f32, f32)> = (0..400)
        .map(|i| (10.0 + i as f32, 100.0 + 20.0 * (i as f32 * 0.1).sin()))
        .collect();
    draw(&mut doc, 0, 0, pen(BLACK), &wave);
    let pdf = run(&doc, &all());
    let id = pages(&pdf)[0];
    let contents = pdf.get_page_contents(id);
    let Object::Stream(s) = pdf.get_object(contents[0]).unwrap() else {
        panic!("page content is a stream");
    };
    assert_eq!(
        s.dict.get(b"Filter").unwrap().as_name().unwrap(),
        b"FlateDecode"
    );
    assert!(matches!(doc.canvas, Canvas::Paged { .. }));
}
