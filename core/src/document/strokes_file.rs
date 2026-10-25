//! The binary strokes file of one page (`pages/<id>.strokes`). The layout is
//! described in `docs/file-format.md`.

use uuid::Uuid;

use super::format::FormatError;
use super::{Page, Stroke, StrokeStyle, Tool};
use crate::{InputSample, LineStyle};

const STROKES_MAGIC: &[u8; 4] = b"GRST";
/// Newest strokes version read. Version 2 added the fill tool and
/// version 3 a line style byte per stroke. Each page is written as the
/// lowest version that holds it, so older readers open pages that do not
/// use the newer features.
const STROKES_VERSION: u16 = 3;
/// Bytes per stroke header (without the line style byte that version 3
/// adds) and per point in a strokes file.
const STROKE_HEADER_LEN: usize = 2 + 16 + 1 + 4 + 4 + 8 + 4;
const LINE_LEN: usize = 1;
const POINT_LEN: usize = 16;

pub(super) fn encode_strokes(page: &Page) -> Vec<u8> {
    let mut out = Vec::new();
    out.extend_from_slice(STROKES_MAGIC);
    let has_fill = page
        .layers
        .iter()
        .flat_map(|l| &l.strokes)
        .any(|s| s.style.tool == Tool::Fill);
    let has_line = page
        .layers
        .iter()
        .flat_map(|l| &l.strokes)
        .any(|s| s.style.line != LineStyle::Solid);
    let version: u16 = if has_line {
        3
    } else if has_fill {
        2
    } else {
        1
    };
    out.extend_from_slice(&version.to_le_bytes());
    out.extend_from_slice(&(page.stroke_count() as u32).to_le_bytes());
    for (li, layer) in page.layers.iter().enumerate() {
        for s in &layer.strokes {
            out.extend_from_slice(&(li as u16).to_le_bytes());
            out.extend_from_slice(s.id.as_bytes());
            out.push(s.style.tool as u8);
            if has_line {
                out.push(s.style.line as u8);
            }
            out.extend_from_slice(&s.style.color.to_le_bytes());
            out.extend_from_slice(&s.style.width.to_le_bytes());
            out.extend_from_slice(&s.started_at_ms.to_le_bytes());
            out.extend_from_slice(&(s.points.len() as u32).to_le_bytes());
            for p in &s.points {
                for v in [p.x, p.y, p.pressure, p.time_ms] {
                    out.extend_from_slice(&v.to_le_bytes());
                }
            }
        }
    }
    out
}

struct Cursor<'a> {
    bytes: &'a [u8],
}

impl<'a> Cursor<'a> {
    fn take<const N: usize>(&mut self) -> Result<[u8; N], FormatError> {
        let Some((head, rest)) = self.bytes.split_first_chunk::<N>() else {
            return Err(FormatError::Corrupt("stroke data ends early".into()));
        };
        self.bytes = rest;
        Ok(*head)
    }

    fn f32(&mut self) -> Result<f32, FormatError> {
        Ok(f32::from_le_bytes(self.take()?))
    }
}

pub(super) fn decode_strokes(
    bytes: &[u8],
    layer_count: usize,
) -> Result<Vec<(usize, Stroke)>, FormatError> {
    let mut c = Cursor { bytes };
    if &c.take::<4>()? != STROKES_MAGIC {
        return Err(FormatError::Corrupt(
            "stroke data has the wrong header".into(),
        ));
    }
    let version = u16::from_le_bytes(c.take()?);
    if version > STROKES_VERSION {
        return Err(FormatError::UnsupportedVersion(version as u32));
    }
    let count = u32::from_le_bytes(c.take()?) as usize;
    let header_len = STROKE_HEADER_LEN + if version >= 3 { LINE_LEN } else { 0 };
    if count > c.bytes.len() / header_len {
        return Err(FormatError::Corrupt("stroke count is too large".into()));
    }

    let mut strokes = Vec::with_capacity(count);
    for _ in 0..count {
        let layer = u16::from_le_bytes(c.take()?) as usize;
        if layer >= layer_count {
            return Err(FormatError::Corrupt(format!(
                "a stroke refers to layer {layer}, but the page has {layer_count}"
            )));
        }
        let id = Uuid::from_bytes(c.take()?);
        let tool = Tool::from_u8(c.take::<1>()?[0])
            .ok_or_else(|| FormatError::Corrupt("unknown tool".into()))?;
        let line = if version >= 3 {
            LineStyle::from_u8(c.take::<1>()?[0])
                .ok_or_else(|| FormatError::Corrupt("unknown line style".into()))?
        } else {
            LineStyle::Solid
        };
        let color = u32::from_le_bytes(c.take()?);
        let width = c.f32()?;
        let started_at_ms = i64::from_le_bytes(c.take()?);
        let n = u32::from_le_bytes(c.take()?) as usize;
        if n > c.bytes.len() / POINT_LEN {
            return Err(FormatError::Corrupt("point count is too large".into()));
        }
        let mut points = Vec::with_capacity(n);
        for _ in 0..n {
            points.push(InputSample::new(c.f32()?, c.f32()?, c.f32()?, c.f32()?));
        }
        strokes.push((
            layer,
            Stroke {
                id,
                style: StrokeStyle {
                    tool,
                    color,
                    width,
                    line,
                },
                started_at_ms,
                points,
            },
        ));
    }
    Ok(strokes)
}

#[cfg(test)]
mod tests {
    use std::io::Cursor as IoCursor;

    use super::super::format::tests::{sample_doc, to_bytes};
    use super::super::{load, Document};
    use super::*;

    fn with_line(line: LineStyle) -> Document {
        let mut doc = sample_doc();
        doc.pages[0].layers[0].strokes[0].style.line = line;
        doc
    }

    #[test]
    fn truncated_stroke_data_is_an_error_not_a_panic() {
        let page = &sample_doc().pages[0];
        let bytes = encode_strokes(page);
        for cut in [3, 10, bytes.len() / 2, bytes.len() - 1] {
            assert!(decode_strokes(&bytes[..cut], page.layers.len()).is_err());
        }
    }

    #[test]
    fn bad_layer_index_is_an_error() {
        let page = &sample_doc().pages[0];
        let bytes = encode_strokes(page);
        assert!(matches!(
            decode_strokes(&bytes, 1),
            Err(FormatError::Corrupt(_))
        ));
    }

    #[test]
    fn fills_bump_the_strokes_version() {
        let mut doc = sample_doc();
        let plain = encode_strokes(&doc.pages[0]);
        assert_eq!(u16::from_le_bytes([plain[4], plain[5]]), 1);
        let mut fill = doc.pages[0].layers[0].strokes[0].clone();
        fill.style.tool = Tool::Fill;
        doc.pages[0].layers[0].strokes.push(fill);
        let bytes = encode_strokes(&doc.pages[0]);
        assert_eq!(u16::from_le_bytes([bytes[4], bytes[5]]), 2);
        let back = decode_strokes(&bytes, doc.pages[0].layers.len()).unwrap();
        assert_eq!(back.len(), doc.pages[0].stroke_count());
        assert!(back.iter().any(|(_, s)| s.style.tool == Tool::Fill));
    }

    #[test]
    fn dashed_and_dotted_survive_a_round_trip() {
        for line in [LineStyle::Dashed, LineStyle::Dotted] {
            let doc = with_line(line);
            let bytes = encode_strokes(&doc.pages[0]);
            assert_eq!(u16::from_le_bytes([bytes[4], bytes[5]]), 3);
            let loaded = load(IoCursor::new(to_bytes(&doc))).unwrap();
            assert_eq!(loaded, doc);
            assert_eq!(loaded.pages[0].layers[0].strokes[0].style.line, line);
            assert_eq!(
                loaded.pages[0].layers[1].strokes[0].style.line,
                LineStyle::Solid
            );
        }
    }

    #[test]
    fn solid_pages_keep_the_older_versions() {
        let mut doc = sample_doc();
        let v = |d: &Document| {
            let b = encode_strokes(&d.pages[0]);
            u16::from_le_bytes([b[4], b[5]])
        };
        assert_eq!(v(&doc), 1);
        let mut fill = doc.pages[0].layers[0].strokes[0].clone();
        fill.style.tool = Tool::Fill;
        doc.pages[0].layers[0].strokes.push(fill);
        assert_eq!(v(&doc), 2);
        doc.pages[0].layers[0].strokes[0].style.line = LineStyle::Dotted;
        assert_eq!(v(&doc), 3);
    }

    #[test]
    fn an_unknown_line_style_is_corrupt() {
        let doc = with_line(LineStyle::Dashed);
        let mut bytes = encode_strokes(&doc.pages[0]);
        // Magic, version and count, then the first header's layer, id and tool.
        bytes[4 + 2 + 4 + 2 + 16 + 1] = 9;
        assert!(matches!(
            decode_strokes(&bytes, doc.pages[0].layers.len()),
            Err(FormatError::Corrupt(_))
        ));
    }

    #[test]
    fn version_two_pages_read_as_solid() {
        let mut doc = sample_doc();
        let mut fill = doc.pages[0].layers[0].strokes[0].clone();
        fill.style.tool = Tool::Fill;
        doc.pages[0].layers[0].strokes.push(fill);
        let bytes = encode_strokes(&doc.pages[0]);
        let back = decode_strokes(&bytes, doc.pages[0].layers.len()).unwrap();
        assert!(back.iter().all(|(_, s)| s.style.line == LineStyle::Solid));
    }
}
