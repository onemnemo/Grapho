//! Formatting laid over the words of a text box. The box keeps its plain
//! `text`, so search, export and anything that only wants the words keep
//! working. `RichText` says which stretches of it look different and what
//! kind of line each paragraph is. A box saved before this existed has none,
//! and reads as plain.

use serde::{Deserialize, Serialize};

use super::{MAX_TEXT_SIZE, MIN_TEXT_SIZE};

/// Marks on a stretch of text. Persisted: append only.
pub mod mark {
    pub const BOLD: u16 = 1;
    pub const ITALIC: u16 = 1 << 1;
    pub const UNDERLINE: u16 = 1 << 2;
    pub const STRIKE: u16 = 1 << 3;
    pub const CODE: u16 = 1 << 4;
    pub const SUPER: u16 = 1 << 5;
    pub const SUB: u16 = 1 << 6;
    /// Every bit that means something.
    pub const ALL: u16 = (1 << 7) - 1;
}

/// A stretch of the text, in characters, with its own look. What a span leaves
/// out comes from the box.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Span {
    pub start: u32,
    pub end: u32,
    #[serde(default)]
    pub marks: u16,
    /// 0xRRGGBBAA text colour.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub color: Option<u32>,
    /// 0xRRGGBBAA marker colour behind the text.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub highlight: Option<u32>,
    /// Font size in points.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub size: Option<f32>,
    /// A typeface for this stretch, as the shell numbers its typefaces.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub font: Option<u8>,
}

/// The most typefaces the shells offer. A number past it reads as the first.
pub const MAX_FONT: u8 = 15;

impl Span {
    fn plain(&self) -> bool {
        self.marks == 0 && self.color.is_none() && self.highlight.is_none() && self.size.is_none()
    }

    /// Whether two spans look the same, apart from where they are.
    fn same_look(&self, other: &Span) -> bool {
        self.marks == other.marks
            && self.color == other.color
            && self.highlight == other.highlight
            && self.size == other.size
            && self.font == other.font
    }
}

/// What a paragraph is. Persisted: append only.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ParaKind {
    #[default]
    Body,
    Heading1,
    Heading2,
    Heading3,
    Bullet,
    Numbered,
    Check,
    Quote,
}

impl ParaKind {
    pub fn from_u8(v: u8) -> Option<Self> {
        Some(match v {
            0 => Self::Body,
            1 => Self::Heading1,
            2 => Self::Heading2,
            3 => Self::Heading3,
            4 => Self::Bullet,
            5 => Self::Numbered,
            6 => Self::Check,
            7 => Self::Quote,
            _ => return None,
        })
    }

    pub fn is_list(self) -> bool {
        matches!(self, Self::Bullet | Self::Numbered | Self::Check)
    }
}

/// The most a list is indented, in steps.
pub const MAX_INDENT: u8 = 6;

/// One line of the text between line breaks.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct Para {
    #[serde(default)]
    pub kind: ParaKind,
    #[serde(default, skip_serializing_if = "is_zero")]
    pub indent: u8,
    /// For a checklist line: whether it is ticked.
    #[serde(default, skip_serializing_if = "is_false")]
    pub checked: bool,
}

fn is_zero(v: &u8) -> bool {
    *v == 0
}

fn is_false(v: &bool) -> bool {
    !*v
}

/// The formatting of a text box. `paras[i]` belongs to the i-th line of the
/// text, and a line past the end is a plain one.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct RichText {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub spans: Vec<Span>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub paras: Vec<Para>,
    /// The typeface of the whole box, as the shell numbers its typefaces. 0 is the first.
    #[serde(default, skip_serializing_if = "is_zero")]
    pub font: u8,
}

impl RichText {
    pub fn is_empty(&self) -> bool {
        self.spans.is_empty() && self.paras.is_empty() && self.font == 0
    }

    /// The same formatting made fit for `text`: spans cut to its length,
    /// sorted, plain and empty ones dropped, neighbours alike merged, sizes
    /// held to their limits and a line for each paragraph no longer than the
    /// text has. Never fails, so a damaged file still opens.
    pub fn normalised(mut self, text: &str) -> Self {
        let len = text.chars().count() as u32;
        let lines = text.split('\n').count();
        self.spans.retain_mut(|s| {
            s.end = s.end.min(len);
            s.marks &= mark::ALL;
            s.size = s
                .size
                .filter(|v| v.is_finite())
                .map(|v| v.clamp(MIN_TEXT_SIZE, MAX_TEXT_SIZE));
            s.font = s.font.filter(|f| *f <= MAX_FONT);
            s.start < s.end && !s.plain()
        });
        self.spans.sort_by_key(|s| (s.start, s.end));
        // Overlaps are cut so each character has one look: a later span
        // starts where the one before ends.
        let mut out: Vec<Span> = Vec::with_capacity(self.spans.len());
        for mut s in self.spans {
            if let Some(last) = out.last() {
                s.start = s.start.max(last.end);
            }
            if s.start >= s.end {
                continue;
            }
            match out.last_mut() {
                Some(last) if last.end == s.start && last.same_look(&s) => last.end = s.end,
                _ => out.push(s),
            }
        }
        self.spans = out;
        if self.font > MAX_FONT {
            self.font = 0;
        }
        self.paras.truncate(lines);
        for p in &mut self.paras {
            p.indent = p.indent.min(MAX_INDENT);
            if p.kind != ParaKind::Check {
                p.checked = false;
            }
        }
        while self.paras.last() == Some(&Para::default()) {
            self.paras.pop();
        }
        self
    }

    /// The paragraph of line `i`.
    pub fn para(&self, i: usize) -> Para {
        self.paras.get(i).copied().unwrap_or_default()
    }

    /// The same formatting with every explicit size multiplied by `k`.
    pub fn scaled(&self, k: f32) -> Self {
        let mut out = self.clone();
        for s in &mut out.spans {
            s.size = s.size.map(|v| (v * k).clamp(MIN_TEXT_SIZE, MAX_TEXT_SIZE));
        }
        out
    }

    /// The number shown at the start of numbered line `i`: how many lines of
    /// the same indent have been numbered since the run began. A deeper line
    /// does not break the run, a shallower or different one does.
    pub fn number_at(&self, i: usize) -> u32 {
        let me = self.para(i);
        let mut n = 1;
        for j in (0..i).rev() {
            let p = self.para(j);
            if p.indent > me.indent && p.kind.is_list() {
                continue;
            }
            if p.kind == ParaKind::Numbered && p.indent == me.indent {
                n += 1;
            } else {
                break;
            }
        }
        n
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn span(start: u32, end: u32, marks: u16) -> Span {
        Span {
            start,
            end,
            marks,
            ..Span::default()
        }
    }

    #[test]
    fn normalising_cuts_sorts_and_merges() {
        let rich = RichText {
            spans: vec![
                span(5, 9, mark::BOLD),
                span(0, 3, mark::BOLD),
                span(3, 5, mark::BOLD),
                span(8, 99, mark::ITALIC),
                span(2, 2, mark::BOLD),
                span(0, 4, 0),
            ],
            paras: vec![Para::default(); 9],
            ..RichText::default()
        }
        .normalised("hello\nworld");
        assert_eq!(rich.spans.len(), 2);
        assert_eq!((rich.spans[0].start, rich.spans[0].end), (0, 9));
        assert_eq!(rich.spans[0].marks, mark::BOLD);
        assert_eq!((rich.spans[1].start, rich.spans[1].end), (9, 11));
        assert!(rich.paras.is_empty());
    }

    #[test]
    fn plain_text_has_nothing_to_save() {
        let rich = RichText::default().normalised("abc");
        assert!(rich.is_empty());
        assert_eq!(serde_json::to_string(&rich).unwrap(), "{}");
    }

    #[test]
    fn paragraphs_never_outnumber_lines() {
        let rich = RichText {
            spans: vec![],
            paras: vec![
                Para::default(),
                Para {
                    kind: ParaKind::Bullet,
                    indent: 40,
                    checked: true,
                },
                Para {
                    kind: ParaKind::Quote,
                    ..Para::default()
                },
            ],
            font: 99,
        }
        .normalised("a\nb");
        assert_eq!(rich.font, 0);
        assert_eq!(rich.paras.len(), 2);
        assert_eq!(rich.paras[1].indent, MAX_INDENT);
        assert!(!rich.paras[1].checked);
    }

    #[test]
    fn numbers_continue_through_deeper_lines_only() {
        let num = |indent| Para {
            kind: ParaKind::Numbered,
            indent,
            checked: false,
        };
        let rich = RichText {
            spans: vec![],
            paras: vec![num(0), num(1), num(1), num(0), Para::default(), num(0)],
            ..RichText::default()
        };
        assert_eq!(rich.number_at(0), 1);
        assert_eq!(rich.number_at(1), 1);
        assert_eq!(rich.number_at(2), 2);
        assert_eq!(rich.number_at(3), 2);
        assert_eq!(rich.number_at(5), 1);
    }

    #[test]
    fn scaling_changes_only_explicit_sizes() {
        let mut s = span(0, 2, mark::BOLD);
        s.size = Some(10.0);
        let rich = RichText {
            spans: vec![s, span(2, 4, mark::ITALIC)],
            paras: vec![],
            ..RichText::default()
        }
        .scaled(2.0);
        assert_eq!(rich.spans[0].size, Some(20.0));
        assert_eq!(rich.spans[1].size, None);
    }
}
