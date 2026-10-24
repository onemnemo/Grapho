package com.onemnemo.grapho.ui.note

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Where the pen wheel lives: docked on [edge] with its disc [along] dp along it, or the free circle at [floatX], [floatY] dp. */
internal data class WheelPlacement(
    val docked: Boolean,
    val edge: DockEdge,
    val along: Float,
    val floatX: Float,
    val floatY: Float,
)

/** Keeps the wheel's [WheelPlacement] in shared preferences, so it is where the person left it next time. */
internal class WheelPlacementStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): WheelPlacement {
        val edge = runCatching { DockEdge.valueOf(prefs.getString(EDGE, null).orEmpty()) }.getOrDefault(DockEdge.Left)
        return WheelPlacement(
            docked = prefs.getBoolean(DOCKED, true),
            edge = edge,
            along = prefs.getFloat(ALONG, TAB_DEFAULT_Y),
            floatX = prefs.getFloat(FLOAT_X, 240f),
            floatY = prefs.getFloat(FLOAT_Y, 240f),
        )
    }

    /**
     * Docks the wheel on [edge], from Settings. Bumps [revision] so an open note moves its wheel at once.
     * The distance along the edge is kept when the edge runs the same way, and starts at its default otherwise.
     */
    fun dockOn(edge: DockEdge) {
        save(placementOnEdge(load(), edge))
        revision++
    }

    fun loadPinned(): Boolean = prefs.getBoolean(PINNED, false)

    fun savePinned(pinned: Boolean) = prefs.edit().putBoolean(PINNED, pinned).apply()

    fun save(p: WheelPlacement) {
        prefs.edit()
            .putBoolean(DOCKED, p.docked)
            .putString(EDGE, p.edge.name)
            .putFloat(ALONG, p.along)
            .putFloat(FLOAT_X, p.floatX)
            .putFloat(FLOAT_Y, p.floatY)
            .apply()
    }

    companion object {
        /** Counts placements written from outside the wheel, so a wheel that is showing can load the new one. */
        var revision by mutableIntStateOf(0)
            private set

        private const val PREFS = "grapho_wheel"
        private const val DOCKED = "docked"
        private const val EDGE = "edge"
        private const val ALONG = "along"
        private const val FLOAT_X = "float_x"
        private const val FLOAT_Y = "float_y"
        private const val PINNED = "pinned"
    }
}

/** [p] docked on [edge], with its along position kept on an edge that runs the same way and reset on one that does not. */
internal fun placementOnEdge(p: WheelPlacement, edge: DockEdge): WheelPlacement =
    p.copy(docked = true, edge = edge, along = if (edge.vertical == p.edge.vertical) p.along else TAB_DEFAULT_Y)
