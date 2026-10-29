package com.onemnemo.grapho.ui.settings

import com.onemnemo.grapho.ui.note.NoteBarTool
import org.junit.Assert.assertEquals
import org.junit.Test

class BarToolEditsTest {
    private val all = NoteBarTool.defaults

    @Test
    fun aToolSwitchedBackOnReturnsToItsUsualPlace() {
        val without = barToolsWithout(all, NoteBarTool.Highlighter)
        assertEquals(all, barToolsWith(without, NoteBarTool.Highlighter))
    }

    @Test
    fun aToolSwitchedOnLandsBeforeTheFirstOneThatFollowsIt() {
        val on = listOf(NoteBarTool.Lasso, NoteBarTool.Pen)
        assertEquals(
            listOf(NoteBarTool.Eraser, NoteBarTool.Lasso, NoteBarTool.Pen),
            barToolsWith(on, NoteBarTool.Eraser),
        )
    }

    @Test
    fun aToolAlreadyOnIsLeftAlone() {
        assertEquals(all, barToolsWith(all, NoteBarTool.Pen))
    }

    @Test
    fun movingStopsAtTheEnds() {
        assertEquals(all, barToolsMoved(all, NoteBarTool.Pen, -1))
        assertEquals(all, barToolsMoved(all, NoteBarTool.Image, 1))
    }

    @Test
    fun movingSwapsNeighbours() {
        val moved = barToolsMoved(all, NoteBarTool.Pen, 1)
        assertEquals(NoteBarTool.Highlighter, moved[0])
        assertEquals(NoteBarTool.Pen, moved[1])
    }

    @Test
    fun movingAToolThatIsOffChangesNothing() {
        val on = listOf(NoteBarTool.Pen)
        assertEquals(on, barToolsMoved(on, NoteBarTool.Lasso, 1))
    }
}
