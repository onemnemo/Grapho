//! Images in the PDF export: embedding, sharing, clipping, cropping and
//! where they sit among a layer's strokes.

use super::*;
use crate::document::image_fixtures::{jpeg, png_with_alpha};
use crate::document::{Image, ImageSource};

const H: f32 = A4.1;

/// Adds a source for `bytes` to the note and returns its id.
fn source(doc: &mut Document, bytes: Vec<u8>) -> Id {
    let source = ImageSource::read(bytes).unwrap();
    let id = source.id;
    doc.images.push(source);
    id
}

fn place(doc: &mut Document, page: usize, layer: usize, source: Id, at: [f32; 4]) -> usize {
    let image = Image::new(source, at[0], at[1], at[2], at[3]);
    let images = &mut doc.pages[page].layers[layer].images;
    images.push(image);
    images.len() - 1
}

/// Every image XObject of the file with its dictionary.
fn image_streams(pdf: &Pdf) -> Vec<(ObjectId, &lopdf::Stream)> {
    pdf.objects
        .iter()
        .filter_map(|(&id, o)| match o {
            Object::Stream(s)
                if s.dict.get(b"Subtype").and_then(Object::as_name).ok() == Some(b"Image") =>
            {
                Some((id, s))
            }
            _ => None,
        })
        .collect()
}

fn name_of(stream: &lopdf::Stream, key: &[u8]) -> String {
    let name = stream.dict.get(key).unwrap().as_name().unwrap();
    String::from_utf8(name.to_vec()).unwrap()
}

/// The operator names of a page in order.
fn operators(pdf: &Pdf, page: ObjectId) -> Vec<String> {
    Ops::decode(&pdf.get_page_content(page))
        .unwrap()
        .operations
        .into_iter()
        .map(|op| op.operator)
        .collect()
}

/// Where the corners of an image XObject's unit square (top left, top
/// right, bottom right, bottom left of the picture) land in default user
/// space at each `Do`.
fn placed_corners(content: &[u8]) -> Vec<[(f32, f32); 4]> {
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
            "Do" => out.push(
                [(0.0, 1.0), (1.0, 1.0), (1.0, 0.0), (0.0, 0.0)].map(|(x, y)| {
                    (
                        x * ctm[0] + y * ctm[2] + ctm[4],
                        x * ctm[1] + y * ctm[3] + ctm[5],
                    )
                }),
            ),
            _ => {}
        }
    }
    out
}

#[test]
fn a_png_with_alpha_and_a_jpeg_are_embedded() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    let jpg = source(&mut doc, jpeg());
    place(&mut doc, 0, 0, png, [10.0, 10.0, 80.0, 60.0]);
    place(&mut doc, 0, 0, jpg, [100.0, 10.0, 40.0, 40.0]);
    let pdf = run(&doc, &all());

    let streams = image_streams(&pdf);
    assert_eq!(streams.len(), 3, "a PNG, its soft mask and a JPEG");
    let dct = streams
        .iter()
        .find(|(_, s)| {
            s.dict.get(b"Filter").ok().and_then(|f| f.as_name().ok()) == Some(b"DCTDecode")
        })
        .map(|(_, s)| s)
        .unwrap();
    assert_eq!(name_of(dct, b"ColorSpace"), "DeviceRGB");
    assert_eq!(dct.dict.get(b"Width").unwrap().as_i64().unwrap(), 8);
    assert_eq!(
        dct.content, *doc.images[1].bytes,
        "the JPEG is passed through"
    );

    let colour = streams
        .iter()
        .map(|(_, s)| *s)
        .find(|s| s.dict.has(b"SMask"))
        .unwrap();
    assert_eq!(name_of(colour, b"ColorSpace"), "DeviceRGB");
    assert_eq!(colour.dict.get(b"Width").unwrap().as_i64().unwrap(), 4);
    let pixels = colour.decompressed_content().unwrap();
    assert_eq!(pixels.len(), 4 * 3 * 3);
    assert_eq!(&pixels[..3], &[30, 120, 220]);
    let (_, mask) = pdf.dereference(colour.dict.get(b"SMask").unwrap()).unwrap();
    let mask = mask.as_stream().unwrap();
    assert_eq!(name_of(mask, b"ColorSpace"), "DeviceGray");
    let alphas = mask.decompressed_content().unwrap();
    assert_eq!(alphas.len(), 12);
    assert_eq!(&alphas[..4], &[0, 63, 127, 191]);
}

#[test]
fn the_page_lists_its_images_and_draws_each_clipped() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    place(&mut doc, 0, 0, png, [10.0, 10.0, 80.0, 60.0]);
    let pdf = run(&doc, &all());
    let page = pages(&pdf)[0];
    let table = resource(&pdf, page, b"XObject").unwrap();
    assert_eq!(table.len(), 1);
    let ops = operators(&pdf, page);
    let clip = ops.iter().position(|o| o == "W").unwrap();
    assert_eq!(ops[clip + 1], "n");
    assert_eq!(ops[clip + 2], "cm");
    assert_eq!(ops[clip + 3], "Do");
    assert_eq!(&ops[clip - 5..clip], ["m", "l", "l", "l", "h"]);
}

#[test]
fn an_image_lands_on_its_rectangle_in_page_space() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    place(&mut doc, 0, 0, png, [10.0, 20.0, 80.0, 60.0]);
    let pdf = run(&doc, &all());
    let corners = placed_corners(&pdf.get_page_content(pages(&pdf)[0]));
    let [tl, tr, br, bl] = corners[0];
    assert!(near(tl, (10.0, H - 20.0), 0.01));
    assert!(near(tr, (90.0, H - 20.0), 0.01));
    assert!(near(br, (90.0, H - 80.0), 0.01));
    assert!(near(bl, (10.0, H - 80.0), 0.01));
}

#[test]
fn a_crop_maps_the_whole_source_so_the_shown_part_fills_the_square() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    let i = place(&mut doc, 0, 0, png, [10.0, 20.0, 80.0, 60.0]);
    doc.pages[0].layers[0].images[i].crop = [0.5, 0.0, 1.0, 0.5];
    let pdf = run(&doc, &all());
    let corners = placed_corners(&pdf.get_page_content(pages(&pdf)[0]));
    // The picture's top left is twice as far left of the square as the
    // square is wide, and its bottom right the same beyond the other side.
    let [tl, tr, br, bl] = corners[0];
    assert!(near(tl, (10.0 - 80.0, H - 20.0), 0.01), "{tl:?}");
    assert!(near(tr, (10.0 + 80.0, H - 20.0), 0.01), "{tr:?}");
    assert!(near(br, (10.0 + 80.0, H - 20.0 - 120.0), 0.01), "{br:?}");
    assert!(near(bl, (10.0 - 80.0, H - 20.0 - 120.0), 0.01), "{bl:?}");
}

#[test]
fn a_turned_image_is_turned_in_the_file() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    let i = place(&mut doc, 0, 0, png, [0.0, 0.0, 1.0, 1.0]);
    // A quarter turn clockwise on the page: (u, v) -> (100 - 20v, 100 + 40u).
    doc.pages[0].layers[0].images[i].transform = [0.0, -20.0, 100.0, 40.0, 0.0, 100.0];
    let pdf = run(&doc, &all());
    let [tl, tr, _, bl] = placed_corners(&pdf.get_page_content(pages(&pdf)[0]))[0];
    assert!(near(tl, (100.0, H - 100.0), 0.01));
    assert!(near(tr, (100.0, H - 140.0), 0.01));
    assert!(near(bl, (80.0, H - 100.0), 0.01));
}

#[test]
fn images_sit_under_the_strokes_of_their_layer() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    draw(&mut doc, 0, 0, pen(BLACK), &[(5.0, 5.0), (50.0, 50.0)]);
    place(&mut doc, 0, 0, png, [10.0, 10.0, 80.0, 60.0]);
    let pdf = run(&doc, &plain());
    let ops = operators(&pdf, pages(&pdf)[0]);
    let image = ops.iter().position(|o| o == "Do").unwrap();
    let ink = ops.iter().position(|o| o == "f").unwrap();
    assert!(image < ink);
}

#[test]
fn a_faded_layer_draws_its_images_inside_its_group() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    let top = doc.pages[0].add_layer("Faded");
    doc.pages[0].layer_mut(top).unwrap().opacity = 0.5;
    place(&mut doc, 0, 1, png, [10.0, 10.0, 80.0, 60.0]);
    draw(&mut doc, 0, 1, pen(BLACK), &[(5.0, 5.0), (50.0, 50.0)]);
    let pdf = run(&doc, &all());
    let page = pages(&pdf)[0];
    let ops = operators(&pdf, page);
    assert!(
        !ops.contains(&"W".to_string()),
        "the page itself draws no image"
    );
    let forms = resource(&pdf, page, b"XObject").unwrap();
    let (_, form) = pdf.dereference(forms.get(b"Fm0").unwrap()).unwrap();
    let form = form.as_stream().unwrap();
    let inner = Ops::decode(&form.decompressed_content().unwrap()).unwrap();
    let names: Vec<&str> = inner
        .operations
        .iter()
        .map(|o| o.operator.as_str())
        .collect();
    assert!(names.iter().position(|&o| o == "Do") < names.iter().position(|&o| o == "f"));
    let (_, resources) = pdf
        .dereference(form.dict.get(b"Resources").unwrap())
        .unwrap();
    let (_, table) = pdf
        .dereference(resources.as_dict().unwrap().get(b"XObject").unwrap())
        .unwrap();
    assert_eq!(table.as_dict().unwrap().len(), 1);
}

#[test]
fn a_source_used_twice_or_on_two_pages_is_written_once() {
    let mut doc = paged();
    doc.add_page();
    let png = source(&mut doc, png_with_alpha(4, 3));
    place(&mut doc, 0, 0, png, [10.0, 10.0, 80.0, 60.0]);
    place(&mut doc, 0, 0, png, [100.0, 10.0, 80.0, 60.0]);
    place(&mut doc, 1, 0, png, [10.0, 10.0, 80.0, 60.0]);
    let pdf = run(&doc, &all());
    assert_eq!(image_streams(&pdf).len(), 2, "one picture and its mask");
    let ids = pages(&pdf);
    let first = resource(&pdf, ids[0], b"XObject").unwrap();
    let second = resource(&pdf, ids[1], b"XObject").unwrap();
    let shared = |t: &Dictionary| t.iter().next().unwrap().1.as_reference().unwrap();
    assert_eq!(shared(first), shared(second));
    assert_eq!(
        operators(&pdf, ids[0])
            .iter()
            .filter(|o| *o == "Do")
            .count(),
        2
    );
}

#[test]
fn hidden_layers_and_unused_sources_add_nothing() {
    let mut doc = paged();
    let png = source(&mut doc, png_with_alpha(4, 3));
    source(&mut doc, jpeg());
    place(&mut doc, 0, 0, png, [10.0, 10.0, 80.0, 60.0]);
    doc.pages[0].layers[0].visible = false;
    let pdf = run(&doc, &all());
    assert!(image_streams(&pdf).is_empty());
    doc.pages[0].layers[0].visible = true;
    let pdf = run(&doc, &plain());
    assert_eq!(
        image_streams(&pdf).len(),
        2,
        "images stay without backgrounds"
    );
}

#[test]
fn an_infinite_canvas_fits_its_images() {
    let mut doc = Document::new_infinite("Board");
    let png = source(&mut doc, png_with_alpha(4, 3));
    place(&mut doc, 0, 0, png, [100.0, 200.0, 300.0, 150.0]);
    let pdf = run(&doc, &all());
    let m = media(&pdf, pages(&pdf)[0]);
    assert!((m[2] - 348.0).abs() < 0.01 && (m[3] - 198.0).abs() < 0.01);
}

#[test]
fn a_jpeg_over_a_pdf_page_is_drawn_too() {
    let mut doc = Document::from_pdf(
        "S",
        "s.pdf",
        crate::pdf::fixtures::pdf_of(&[(612.0, 792.0)]),
    )
    .unwrap();
    let jpg = source(&mut doc, jpeg());
    place(&mut doc, 0, 0, jpg, [10.0, 10.0, 80.0, 60.0]);
    let pdf = run(&doc, &all());
    assert_eq!(image_streams(&pdf).len(), 1);
    let corners = placed_corners(&pdf.get_page_content(pages(&pdf)[0]));
    assert!(near(corners[0][0], (10.0, 792.0 - 10.0), 0.01));
}

#[test]
fn a_broken_png_fails_the_export_with_a_message() {
    let mut doc = paged();
    let mut bytes = png_with_alpha(4, 3);
    bytes.truncate(bytes.len() - 20);
    let id = source(&mut doc, bytes);
    place(&mut doc, 0, 0, id, [10.0, 10.0, 80.0, 60.0]);
    assert!(matches!(
        export_pdf(&doc, &all()),
        Err(ExportError::Image(_))
    ));
}
