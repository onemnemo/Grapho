//! Grapho's platform-independent ink engine.
//!
//! Platform adapters feed raw stylus samples into a [`StrokeBuilder`] and get
//! back a triangle mesh to draw. The same code runs on every platform, so a
//! stroke looks identical on Android, Windows and Apple devices.

#![forbid(unsafe_code)]

mod brush;
mod dash;
pub mod diagram;
pub mod document;
pub mod export;
pub mod guide;
pub mod library;
mod mesh;
mod outline;
pub mod pdf;
mod rect;
mod sample;
pub mod shape;
mod stabilizer;
mod stroke;

pub use brush::Brush;
pub use dash::{dash_runs, outline_pieces_styled, outline_styled, tessellate_styled, LineStyle};
pub use mesh::{tessellate, tessellate_step};
pub use outline::{outline, pieces as outline_pieces};
pub use rect::Rect;
pub use sample::InputSample;
pub use stabilizer::Stabilizer;
pub use stroke::{Frame, Snapped, StrokeBuilder};
