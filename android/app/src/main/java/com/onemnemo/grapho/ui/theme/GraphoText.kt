package com.onemnemo.grapho.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.onemnemo.grapho.R

/** Geist. Glyphs it lacks fall back to the system sans through Compose's per glyph fallback. */
val Geist = FontFamily(
    Font(R.font.geist_400, FontWeight.Normal),
    Font(R.font.geist_500, FontWeight.Medium),
)

/** A line box is never below this multiple of the size for Latin, Cyrillic and Greek. */
private const val LINE_HEIGHT_FLOOR = 1.25f

private fun style(spec: GraphoTextSpec) = TextStyle(
    fontFamily = Geist,
    fontWeight = FontWeight(spec.weight),
    fontSize = spec.sizeSp.sp,
    lineHeight = (spec.sizeSp * maxOf(spec.lineHeightMultiple, LINE_HEIGHT_FLOOR)).sp,
    letterSpacing = spec.trackingEm.em,
)

/** Compose text styles built from the generated [GraphoType] specs. */
object GraphoText {
    val display = style(GraphoType.display)
    val title = style(GraphoType.title)
    val heading = style(GraphoType.heading)
    val body = style(GraphoType.body)
    val label = style(GraphoType.label)
    val caption = style(GraphoType.caption)
    val micro = style(GraphoType.micro)

    /** The title printed on a notebook cover. */
    val cover = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    )

    /** Numbers that change in place keep a fixed width. */
    val tabular = caption.copy(fontFeatureSettings = "tnum")
}
