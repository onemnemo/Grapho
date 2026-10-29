package com.onemnemo.grapho.ui.library

import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.CoverPreset

@StringRes
fun presetName(preset: CoverPreset): Int = when (preset) {
    CoverPreset.Calculus -> R.string.cover_calculus
    CoverPreset.Biology -> R.string.cover_biology
    CoverPreset.Physics -> R.string.cover_physics
    CoverPreset.Journal -> R.string.cover_journal
    CoverPreset.Chemistry -> R.string.cover_chemistry
    CoverPreset.Sketches -> R.string.cover_sketches
    CoverPreset.Spanish -> R.string.cover_spanish
    CoverPreset.History -> R.string.cover_history
    CoverPreset.Moss -> R.string.cover_moss
    CoverPreset.Dusk -> R.string.cover_dusk
    CoverPreset.Plain -> R.string.cover_plain
}

@StringRes
fun canvasPresetName(index: Int): Int = when (index) {
    0 -> R.string.canvas_whiteboard
    1 -> R.string.canvas_dot_grid
    2 -> R.string.canvas_blueprint
    3 -> R.string.canvas_graph_paper
    4 -> R.string.canvas_chalkboard
    5 -> R.string.canvas_midnight_lines
    6 -> R.string.canvas_sand_dots
    else -> R.string.canvas_sky
}
