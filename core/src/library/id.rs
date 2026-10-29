use std::fmt;

use serde::de::{self, Deserialize, Deserializer};
use serde::{Serialize, Serializer};
use uuid::Uuid;

const ALPHABET: &[u8; 32] = b"0123456789ABCDEFGHJKMNPQRSTVWXYZ";
/// Characters in the text form: 26 groups of 5 bits hold 128 bits.
const TEXT_LEN: usize = 26;

/// A 128 bit random id. It never changes and is never reused. The text form
/// is 26 Crockford base32 characters, as in the ffi and in deep links.
#[derive(Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Id(u128);

impl Id {
    /// A new random id.
    pub fn random() -> Id {
        Id(Uuid::new_v4().as_u128())
    }

    /// The id with these 128 bits.
    pub fn from_u128(bits: u128) -> Id {
        Id(bits)
    }

    /// The 128 bits of the id.
    pub fn as_u128(self) -> u128 {
        self.0
    }

    /// Reads the text form. Case is ignored, `I` and `L` read as 1 and `O` as
    /// 0, as Crockford base32 allows. Anything else, including a wrong length
    /// or a value above 128 bits, gives `None`.
    pub fn parse(text: &str) -> Option<Id> {
        if text.len() != TEXT_LEN {
            return None;
        }
        let mut bits: u128 = 0;
        for (i, byte) in text.bytes().enumerate() {
            let digit = match byte.to_ascii_uppercase() {
                b @ b'0'..=b'9' => b - b'0',
                b'O' => 0,
                b'I' | b'L' => 1,
                b => ALPHABET.iter().position(|&c| c == b)? as u8,
            };
            if i == 0 && digit > 7 {
                return None;
            }
            bits = (bits << 5) | u128::from(digit);
        }
        Some(Id(bits))
    }
}

impl fmt::Display for Id {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let mut out = [0u8; TEXT_LEN];
        for (i, slot) in out.iter_mut().enumerate() {
            let shift = 5 * (TEXT_LEN - 1 - i);
            *slot = ALPHABET[((self.0 >> shift) & 31) as usize];
        }
        f.write_str(std::str::from_utf8(&out).unwrap_or_default())
    }
}

impl fmt::Debug for Id {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "Id({self})")
    }
}

impl Serialize for Id {
    fn serialize<S: Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        s.collect_str(self)
    }
}

impl<'de> Deserialize<'de> for Id {
    fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Id, D::Error> {
        let text = String::deserialize(d)?;
        Id::parse(&text).ok_or_else(|| de::Error::custom("not a library id"))
    }
}
