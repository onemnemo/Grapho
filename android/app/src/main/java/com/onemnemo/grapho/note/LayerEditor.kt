package com.onemnemo.grapho.note

import android.util.Log
import com.onemnemo.grapho.engine.NativeNotebook
import kotlin.math.roundToInt
import org.json.JSONException

/** What the layer editor needs from the canvas it belongs to. */
internal interface LayerHost {
    /** Layers of [page] changed and the edit needs saving. [reload] when what is drawn may differ. */
    fun layersEdited(page: Int, reload: Boolean)

    /**
     * The opacity of the [visibleIndex]th visible layer of [page] (negative when the layer is hidden) is now
     * [alpha], from 0 to 255. [commit] is false while a slider is still moving.
     */
    fun opacityChanged(page: Int, visibleIndex: Int, alpha: Int, commit: Boolean)
}

/**
 * Reads and edits the layers of the page in the middle of the view. Indices are the engine's, bottom to top.
 * UI thread only. The layers of a page are read once and kept until something changes them, so the canvas can
 * ask about the active layer on every touch.
 */
class LayerEditor internal constructor(private val notebook: Notebook, private val host: LayerHost) {

    /** Called whenever the layers shown for [page] may differ: an edit, undo, or scrolling to another page. */
    var onChanged: (() -> Unit)? = null

    /** The page the layers belong to: the one most in view. */
    var page = 0
        private set

    private val known = HashMap<Int, PageLayers>()

    fun current(): PageLayers? = of(page)

    /** Why drawing on [page]'s active layer would be refused. */
    fun block(page: Int): LayerBlock = of(page)?.block ?: LayerBlock.NONE

    private fun of(page: Int): PageLayers? {
        known[page]?.let { return it }
        val parsed = try {
            PageLayers.parse(NativeNotebook.nativeLayers(notebook.nativeHandle, page))
        } catch (e: JSONException) {
            Log.e(TAG, "layers of page $page could not be read", e)
            null
        }
        if (parsed != null) known[page] = parsed
        return parsed
    }

    /** The view moved: follow the page in the middle of it. */
    internal fun viewMoved(vp: Viewport) {
        val centre = vp.centrePage()
        if (centre == page) return
        page = centre
        onChanged?.invoke()
    }

    /** The document changed underneath, for example by undo: read the layers again. */
    internal fun refresh() {
        known.clear()
        onChanged?.invoke()
    }

    fun setActive(index: Int) = edit(false) { h, p -> NativeNotebook.nativeSetActiveLayer(h, p, index) }

    /** Adds an empty layer above the active one. Returns false at the limit of [PageLayers.MAX_LAYERS]. */
    fun add(name: String) = edit(true) { h, p -> NativeNotebook.nativeAddLayer(h, p, name) >= 0 }

    fun remove(index: Int) = edit(true) { h, p -> NativeNotebook.nativeRemoveLayer(h, p, index) }

    fun move(from: Int, to: Int) = edit(true) { h, p -> NativeNotebook.nativeMoveLayer(h, p, from, to) }

    fun mergeDown(index: Int) = edit(true) { h, p -> NativeNotebook.nativeMergeDown(h, p, index) }

    fun rename(index: Int, name: String) = edit(false) { h, p -> NativeNotebook.nativeRenameLayer(h, p, index, name) }

    fun setVisible(index: Int, visible: Boolean) =
        edit(true) { h, p -> NativeNotebook.nativeSetLayerVisible(h, p, index, visible) }

    fun setLocked(index: Int, locked: Boolean) =
        edit(false) { h, p -> NativeNotebook.nativeSetLayerLocked(h, p, index, locked) }

    /**
     * Sets a layer's opacity from 0 to 1. Call with [commit] false while a slider moves, which updates the picture
     * without refreshing the rest of the screen, and once more with true when it is released.
     */
    fun setOpacity(index: Int, opacity: Float, commit: Boolean) {
        val p = page
        val state = of(p) ?: return
        val value = opacity.coerceIn(0f, 1f)
        if (!NativeNotebook.nativeSetLayerOpacity(notebook.nativeHandle, p, index, value)) return
        known.remove(p)
        val visibleIndex = if (state.layers.getOrNull(index)?.visible == true) {
            state.layers.take(index).count { it.visible }
        } else {
            -1
        }
        host.opacityChanged(p, visibleIndex, (value * LAYER_OPAQUE).roundToInt(), commit)
        onChanged?.invoke()
    }

    private inline fun edit(reload: Boolean, call: (handle: Long, page: Int) -> Boolean): Boolean {
        val p = page
        if (!call(notebook.nativeHandle, p)) return false
        known.remove(p)
        host.layersEdited(p, reload)
        onChanged?.invoke()
        return true
    }

    private companion object {
        const val TAG = "LayerEditor"
    }
}
