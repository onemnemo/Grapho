package com.onemnemo.grapho.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Notebook cover artwork: a base colour under a drawing. A notebook file only stores a cover colour, so a
 * cover shows a preset's drawing exactly when its colour is that preset's base colour. Artwork is content,
 * so none of this follows the theme.
 */
enum class CoverPreset(val base: Color) {
    Calculus(Color(0xFFF2C14E)),
    Biology(Color(0xFFB7A5F2)),
    Physics(Color(0xFF27275A)),
    Journal(Color(0xFF23244F)),
    Chemistry(Color(0xFF0F3328)),
    Sketches(Color(0xFFE8A2BE)),
    Spanish(Color(0xFFE9873A)),
    History(Color(0xFF5A8CF0)),
    Moss(Color(0xFF2F7F5E)),
    Dusk(Color(0xFF3E3A6D)),
    Plain(Color(0xFF3A3C46)),
}

/** What a cover is drawn with: its base colour and the drawing over it. */
data class CoverLook(val base: Color, val preset: CoverPreset)

object CoverPalette {
    /** The colours of the Colour tab, in reading order. */
    val swatches: List<Color> = listOf(
        0xFFF2C14E, 0xFFE9873A, 0xFFE0402E, 0xFFE8A2BE, 0xFFB7A5F2, 0xFF5A8CF0, 0xFF3C8FB5,
        0xFF2F7F5E, 0xFF0F3328, 0xFF27275A, 0xFF5B1F2B, 0xFFB98B58, 0xFF17181C, 0xFFECE9E4,
    ).map { Color(it) }

    /** Older cover colours and the design colour each one shows as. */
    private val legacy: Map<Int, Color> = mapOf(
        FolderColors.grey to Color(0xFFECE9E4),
        FolderColors.blue to Color(0xFF5A8CF0),
        FolderColors.lilac to Color(0xFFB7A5F2),
        FolderColors.pink to Color(0xFFE8A2BE),
        FolderColors.green to Color(0xFF2F7F5E),
        FolderColors.orange to Color(0xFFE9873A),
        FolderColors.yellow to Color(0xFFF2C14E),
        FolderColors.teal to Color(0xFF3C8FB5),
    ).mapKeys { it.key.toArgb() }

    /** A notebook saved without a cover shows the plain one. */
    fun resolve(argb: Int?): CoverLook {
        if (argb == null) return CoverLook(CoverPreset.Plain.base, CoverPreset.Plain)
        legacy[argb]?.let { return CoverLook(it, CoverPreset.Plain) }
        val preset = CoverPreset.entries.firstOrNull { it.base.toArgb() == argb }
        return CoverLook(Color(argb), preset ?: CoverPreset.Plain)
    }

    /**
     * The colour to store for a colour chosen on its own. One that is exactly a preset's base would pick up that
     * preset's drawing, so it moves one step in blue, which no eye can tell apart.
     */
    fun solid(color: Color): Int {
        val argb = color.toArgb()
        return if (CoverPreset.entries.any { it.base.toArgb() == argb }) argb xor 1 else argb
    }
}

/** A whiteboard preset: a paper colour and one of the four patterns (0 blank, 1 lined, 2 grid, 3 dotted). */
class CanvasPreset(val background: Color, val pattern: Int)

object CanvasPalette {
    /** Presets in the order of the Presets tab; the names live in resources. */
    val presets: List<CanvasPreset> = listOf(
        CanvasPreset(Color(0xFFF7F6F2), 0),
        CanvasPreset(Color(0xFF1B1C21), 3),
        CanvasPreset(Color(0xFF1F3A6B), 2),
        CanvasPreset(Color(0xFFF2EEDF), 2),
        CanvasPreset(Color(0xFF24382F), 0),
        CanvasPreset(Color(0xFF17181C), 1),
        CanvasPreset(Color(0xFFE9DFC9), 3),
        CanvasPreset(Color(0xFFDCEBFA), 3),
    )

    val lightSwatches: List<Color> = listOf(
        0xFFFFFFFF,
        0xFFF7F6F2,
        0xFFF2EEDF,
        0xFFE9DFC9,
        0xFFDCEBFA,
        0xFFF6E3EA,
        0xFFE2F0E6,
    ).map { Color(it) }

    val darkSwatches: List<Color> = listOf(
        0xFF17181C,
        0xFF1B1C21,
        0xFF1F3A6B,
        0xFF24382F,
        0xFF3B2A4F,
        0xFF5B1F2B,
        0xFF2B2B30,
    ).map { Color(it) }
}
