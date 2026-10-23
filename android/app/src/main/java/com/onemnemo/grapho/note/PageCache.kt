package com.onemnemo.grapho.note

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.onemnemo.grapho.engine.NativeNotebook
import com.onemnemo.grapho.engine.Tool

/** A stroke's identity in the document, from the engine's `[hi, lo]` pairs. */
data class StrokeId(val hi: Long, val lo: Long)

/**
 * A finished stroke ready to draw in page-local document units. Never changed after creation. A [quick]
 * stroke has overlapping contours: it fills identically but was cheap to build, and is replaced by the
 * exact outline once the page is idle.
 */
class CachedStroke(val id: StrokeId, val path: Path, val paint: Paint, val bounds: RectF, val quick: Boolean = false)

/**
 * The strokes, images and text boxes of one visible layer in drawing order, and the layer's opacity from 0 to 255.
 * The images are drawn first, then the text, so the strokes lie over both.
 */
class SceneLayer(
    val alpha: Int,
    val strokes: List<CachedStroke>,
    val images: List<SceneImage> = emptyList(),
    val texts: List<SceneText> = emptyList(),
)

/** The finished strokes and images of one page, grouped by visible layer from bottom to top. Immutable. */
class PageScene(val layers: List<SceneLayer>) {
    /** Every stroke of every layer, in drawing order. */
    val strokes: List<CachedStroke> = if (layers.size == 1) layers[0].strokes else layers.flatMap { it.strokes }

    /** Every image of every layer, in drawing order. */
    val images: List<SceneImage> = if (layers.size == 1) layers[0].images else layers.flatMap { it.images }

    /** Every text box of every layer, in drawing order. */
    val texts: List<SceneText> = if (layers.size == 1) layers[0].texts else layers.flatMap { it.texts }
    val hasQuick: Boolean = strokes.any { it.quick }

    /** The same scene with one layer drawn at another opacity. */
    fun withAlpha(layer: Int, alpha: Int) =
        PageScene(layers.mapIndexed { i, l -> if (i == layer) SceneLayer(alpha, l.strokes, l.images, l.texts) else l })

    /** The stroke [before] did not have, with its layer's opacity, or null if the layers do not line up. */
    fun addedSince(before: PageScene?): Pair<CachedStroke, Int>? {
        if (before == null || before.layers.size != layers.size) return null
        for (i in layers.indices) {
            if (layers[i].strokes.size > before.layers[i].strokes.size) {
                val stroke = layers[i].strokes.lastOrNull() ?: return null
                return stroke to layers[i].alpha
            }
        }
        return null
    }
}

/** A page read off the UI thread: the scene to publish and what it cost to build. */
class PageLoad(val scene: PageScene, val loaded: Int, val vertices: Int)

/**
 * Drawable outlines of every page's strokes, keyed by page id. Only the UI
 * thread calls [refresh], [sync] and [publish]; the render thread reads
 * [scenes], which is replaced as a whole and never changed in place.
 * [compute] may run on any thread, from a scene read earlier, and its result
 * goes live through [publish], which refuses it if the page was republished
 * in the meantime.
 */
class PageCache(private val notebook: Notebook) {

    @Volatile
    var scenes: Map<String, PageScene> = emptyMap()
        private set

    @Volatile private var paper = PaperColors.WHITE.argb

    /**
     * Sets the paper colour that stroke colours are adapted to. When it crosses between light and
     * dark, cached paints are stale, so every page is dropped and the next [sync] rebuilds it.
     */
    fun setPaper(argb: Int) {
        if (isDarkPaper(argb) != isDarkPaper(paper)) scenes = emptyMap()
        paper = argb
    }

    /**
     * Re-reads one page, fetching exact geometry only for strokes that are
     * new. The eraser does not come through here: its pieces go through
     * [compute] on [GeometryLoader]'s thread.
     */
    fun refresh(pageId: String, pageIndex: Int) {
        val handle = notebook.nativeHandle
        val layout = layout(handle, pageIndex)
        val known = scenes[pageId]?.strokes?.associateBy { it.id } ?: emptyMap()
        val missing = layout.flatMap { it.ids }.filter { it !in known }
        val fresh = if (missing.isEmpty()) emptyMap() else load(handle, pageIndex, missing, false)
        val scene = sceneOf(layout, images(handle, pageIndex), texts(handle, pageIndex)) {
            known[it] ?: fresh[it]?.stroke
        }
        scenes = scenes + (pageId to scene)
    }

    /**
     * Re-reads which layers and strokes a page shows, after a layer was hidden, shown, moved or merged. Returns
     * false, leaving the page as it was, when a stroke has to be built first; the caller then loads it in the
     * background.
     */
    fun relayer(pageId: String, pageIndex: Int): Boolean {
        val handle = notebook.nativeHandle
        val layout = layout(handle, pageIndex)
        val known = scenes[pageId]?.strokes?.associateBy { it.id } ?: emptyMap()
        if (layout.any { layer -> layer.ids.any { it !in known } }) return false
        val scene = sceneOf(layout, images(handle, pageIndex), texts(handle, pageIndex)) { known[it] }
        scenes = scenes + (pageId to scene)
        return true
    }

    /** Draws the [layer]th visible layer of a page at [alpha] (0 to 255) without reading anything else. */
    fun setLayerAlpha(pageId: String, layer: Int, alpha: Int) {
        val scene = scenes[pageId] ?: return
        if (layer !in scene.layers.indices) return
        scenes = scenes + (pageId to scene.withAlpha(layer, alpha))
    }

    /** Refreshes every page and forgets pages that no longer exist. */
    fun sync(pageIds: List<String>) {
        scenes = scenes.filterKeys { it in pageIds }
        pageIds.forEachIndexed { index, id -> refresh(id, index) }
    }

    /**
     * Builds a page's next scene from [base], the scene it was read from. New strokes get quick geometry;
     * up to [upgrade] quick strokes are rebuilt exactly. Does not touch [scenes].
     */
    fun compute(pageIndex: Int, base: PageScene?, upgrade: Int): PageLoad {
        val handle = notebook.nativeHandle
        val layout = layout(handle, pageIndex)
        val ids = layout.flatMap { it.ids }
        val known = base?.strokes?.associateBy { it.id } ?: emptyMap()
        val fresh = ids.filter { it !in known }
        val exact = ids.filter { known[it]?.quick == true }.take(upgrade)
        val loaded = HashMap<StrokeId, Loaded>()
        if (fresh.isNotEmpty()) loaded += load(handle, pageIndex, fresh, true)
        if (exact.isNotEmpty()) loaded += load(handle, pageIndex, exact, false)
        val scene = sceneOf(layout, images(handle, pageIndex), texts(handle, pageIndex)) {
            loaded[it]?.stroke ?: known[it]
        }
        return PageLoad(scene, loaded.size, loaded.values.sumOf { it.vertices })
    }

    /** Makes a computed scene live unless the page changed since [base] was read. Returns whether it did. */
    fun publish(pageId: String, base: PageScene?, scene: PageScene): Boolean {
        if (scenes[pageId] !== base) return false
        scenes = scenes + (pageId to scene)
        return true
    }

    private class Loaded(val stroke: CachedStroke, val vertices: Int)

    private fun layout(handle: Long, pageIndex: Int): List<LayerIds> =
        StrokeLayout.parse(NativeNotebook.nativeLayerStrokeIds(handle, pageIndex))

    private fun images(handle: Long, pageIndex: Int): List<List<SceneImage>> =
        ImageLayout.parse(NativeNotebook.nativePageImages(handle, pageIndex))

    private fun texts(handle: Long, pageIndex: Int): List<List<SceneText>> =
        TextLayout.parse(NativeNotebook.nativePageTexts(handle, pageIndex))

    private fun sceneOf(
        layout: List<LayerIds>,
        images: List<List<SceneImage>>,
        texts: List<List<SceneText>>,
        pick: (StrokeId) -> CachedStroke?,
    ) = PageScene(
        layout.mapIndexed { i, layer ->
            SceneLayer(
                layer.alpha,
                layer.ids.mapNotNull(pick),
                images.getOrElse(i) { emptyList() },
                texts.getOrElse(i) { emptyList() },
            )
        },
    )

    private fun load(handle: Long, pageIndex: Int, ids: List<StrokeId>, quick: Boolean): Map<StrokeId, Loaded> {
        val request = LongArray(ids.size * 2)
        ids.forEachIndexed { i, id ->
            request[2 * i] = id.hi
            request[2 * i + 1] = id.lo
        }
        val g = NativeNotebook.nativeStrokeGeometry(handle, pageIndex, request, TOLERANCE, quick)
        val out = HashMap<StrokeId, Loaded>()
        var i = 0
        for (id in ids) {
            val tool = g[i].toInt()
            val color = (g[i + 1].toInt() shl 16) or g[i + 2].toInt()
            val bounds = RectF(g[i + 3], g[i + 4], g[i + 5], g[i + 6])
            val n = g[i + 7].toInt()
            i += 8
            if (tool >= 0) {
                val paint = fillPaint(displayColor(color, paper))
                val stroke = CachedStroke(id, buildPath(g, i, n), paint, bounds, quick && tool != Tool.FILL)
                out[id] = Loaded(stroke, n / 2)
            }
            i += n
        }
        return out
    }

    /** One path from all the contours, which do not overlap and include any holes: filled with non-zero winding. */
    private fun buildPath(g: FloatArray, start: Int, length: Int): Path {
        val path = Path()
        path.fillType = Path.FillType.WINDING
        var j = start
        val end = start + length
        while (j < end) {
            val count = g[j].toInt()
            j++
            path.moveTo(g[j], g[j + 1])
            for (k in 1 until count) path.lineTo(g[j + 2 * k], g[j + 2 * k + 1])
            path.close()
            j += 2 * count
        }
        return path
    }

    private fun fillPaint(argb: Int) = Paint().apply {
        color = argb
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private companion object {
        /** How far an outline may stray from the true shape, in points. */
        const val TOLERANCE = 0.02f
    }
}
