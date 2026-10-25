package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class LayersTest {
    private fun layer(name: String, visible: Boolean = true, locked: Boolean = false) =
        LayerInfo(name, visible, locked, 1f, 0)

    private fun named(n: Int) = "Layer $n"

    @Test
    fun layoutReadsEachLayerWithItsOpacity() {
        val raw = longArrayOf(255, 2, 1, 2, 3, 4, 128, 1, 5, 6, 40, 0)
        val layout = StrokeLayout.parse(raw)
        assertEquals(3, layout.size)
        assertEquals(255, layout[0].alpha)
        assertEquals(listOf(StrokeId(1, 2), StrokeId(3, 4)), layout[0].ids)
        assertEquals(128, layout[1].alpha)
        assertEquals(listOf(StrokeId(5, 6)), layout[1].ids)
        assertEquals(emptyList<StrokeId>(), layout[2].ids)
        assertEquals(3, StrokeLayout.count(raw))
    }

    @Test
    fun layoutIgnoresATruncatedTail() {
        val layout = StrokeLayout.parse(longArrayOf(255, 3, 1, 2))
        assertEquals(listOf(StrokeId(1, 2)), layout[0].ids)
        assertEquals(1, StrokeLayout.count(longArrayOf(255, 3, 1, 2)))
    }

    @Test
    fun newLayerIsNamedAfterTheHighestNumber() {
        val layers = PageLayers(0, listOf(layer("Layer 1"), layer("Sketch"), layer("Layer 7")))
        assertEquals("Layer 8", layers.nextName { named(it) })
    }

    @Test
    fun newLayerFallsBackToTheCount() {
        val layers = PageLayers(0, listOf(layer("Ink"), layer("Sketch")))
        assertEquals("Layer 3", layers.nextName { named(it) })
    }

    @Test
    fun activeLayerBlocksDrawingWhenHiddenOrLocked() {
        val layers = listOf(layer("a"), layer("b", visible = false), layer("c", locked = true))
        assertEquals(LayerBlock.NONE, PageLayers(0, layers).block)
        assertEquals(LayerBlock.HIDDEN, PageLayers(1, layers).block)
        assertEquals(LayerBlock.LOCKED, PageLayers(2, layers).block)
    }
}
