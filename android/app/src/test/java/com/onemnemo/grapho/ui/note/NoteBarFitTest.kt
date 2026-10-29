package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.note.ToolMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBarFitTest {
    private fun measure(width: Float, fixed: Int = 6) = BarMeasure(
        barLengthDp = width,
        fixedSlots = fixed,
        slotDp = 50f,
        separatorDp = 10f,
        paddingDp = 8f,
        titleMinDp = 96f,
    )

    private fun fit(
        width: Float,
        active: NoteBarTool? = NoteBarTool.Pen,
        compact: Boolean = false,
        title: Boolean = true,
        fixed: Int = 6,
    ) = fitBar(NoteBarTool.defaults, active, compact, title, measure(width, fixed))

    @Test
    fun aWideBarHoldsEveryToolAndTheTitle() {
        val fit = fit(800f)
        assertEquals(NoteBarTool.defaults, fit.tools)
        assertTrue(fit.showTitle)
    }

    @Test
    fun toolsAreDroppedFromTheEndRatherThanOverflowing() {
        val fit = fit(420f, active = NoteBarTool.Pen)
        assertEquals(listOf(NoteBarTool.Pen, NoteBarTool.Highlighter), fit.tools)
        assertFalse(fit.showTitle)
    }

    @Test
    fun theActiveToolStaysOnTheBar() {
        val fit = fit(420f, active = NoteBarTool.Lasso)
        assertEquals(listOf(NoteBarTool.Pen, NoteBarTool.Lasso), fit.tools)
    }

    @Test
    fun theCompactBarLeavesImageOut() {
        val fit = fit(600f, compact = true, fixed = 5)
        assertFalse(NoteBarTool.Image in fit.tools)
        assertTrue(NoteBarTool.Shapes in fit.tools)
        assertTrue(NoteBarTool.Text in fit.tools)
        assertEquals(6, fit.tools.size)
    }

    @Test
    fun theTitleHidesWhenOffEvenWithRoom() {
        assertFalse(fit(800f, title = false).showTitle)
    }

    @Test
    fun aBarWithNoRoomHoldsNoTools() {
        assertTrue(fit(300f).tools.isEmpty())
    }

    @Test
    fun aRuleCountsAgainstTheWidthBetweenGroups() {
        // Seven tools and one rule: 7 * 50 + 10 = 360 of room needed, with 6 * 50 and 8 spare beside them.
        assertEquals(7, fit(8f + 300f + 360f).tools.size)
        assertEquals(6, fit(8f + 300f + 359f).tools.size)
    }

    @Test
    fun storedIdsKeepTheirOrderAndDropWhatIsUnknown() {
        val stored = NoteBarTool.encode(listOf(NoteBarTool.Lasso, NoteBarTool.Pen))
        assertEquals("lasso,pen", stored)
        assertEquals(listOf(NoteBarTool.Lasso, NoteBarTool.Pen), NoteBarTool.decode("lasso,sticker,pen,pen"))
        assertTrue(NoteBarTool.decode("").isEmpty())
    }

    @Test
    fun theCanvasToolPicksTheBarTool() {
        assertEquals(NoteBarTool.Eraser, NoteBarTool.forMode(ToolMode.ERASER))
        assertEquals(null, NoteBarTool.forMode(ToolMode.FILL))
    }

    @Test
    fun shapesSitBetweenTheDrawingToolsAndImage() {
        val tools = NoteBarTool.defaults
        assertEquals(NoteBarTool.Image, tools[tools.indexOf(NoteBarTool.Shapes) + 1])
        assertEquals(BarToolGroup.Insert, NoteBarTool.Shapes.group)
        assertEquals(NoteBarTool.Shapes, NoteBarTool.forMode(ToolMode.SHAPE))
    }

    @Test
    fun textSitsBeforeShapesInTheInsertGroup() {
        val tools = NoteBarTool.defaults
        assertEquals(NoteBarTool.Shapes, tools[tools.indexOf(NoteBarTool.Text) + 1])
        assertEquals(BarToolGroup.Insert, NoteBarTool.Text.group)
        assertEquals(NoteBarTool.Text, NoteBarTool.forMode(ToolMode.TEXT))
    }

    @Test
    fun aBarSavedBeforeTextGetsItJustBeforeShapesOrImage() {
        assertEquals(
            listOf(NoteBarTool.Pen, NoteBarTool.Text, NoteBarTool.Shapes, NoteBarTool.Image),
            NoteBarTool.withTextOffered(listOf(NoteBarTool.Pen, NoteBarTool.Shapes, NoteBarTool.Image)),
        )
        assertEquals(
            listOf(NoteBarTool.Pen, NoteBarTool.Text, NoteBarTool.Image),
            NoteBarTool.withTextOffered(listOf(NoteBarTool.Pen, NoteBarTool.Image)),
        )
        assertEquals(listOf(NoteBarTool.Pen, NoteBarTool.Text), NoteBarTool.withTextOffered(listOf(NoteBarTool.Pen)))
        val has = listOf(NoteBarTool.Text, NoteBarTool.Pen)
        assertEquals(has, NoteBarTool.withTextOffered(has))
    }

    @Test
    fun aBarSavedBeforeShapesGetsThemJustBeforeImage() {
        val old = listOf(NoteBarTool.Lasso, NoteBarTool.Pen, NoteBarTool.Image)
        assertEquals(
            listOf(NoteBarTool.Lasso, NoteBarTool.Pen, NoteBarTool.Shapes, NoteBarTool.Image),
            NoteBarTool.withShapesOffered(old),
        )
        assertEquals(
            listOf(NoteBarTool.Pen, NoteBarTool.Shapes),
            NoteBarTool.withShapesOffered(listOf(NoteBarTool.Pen)),
        )
        assertEquals(listOf(NoteBarTool.Shapes), NoteBarTool.withShapesOffered(emptyList()))
    }

    @Test
    fun aBarThatHasShapesIsLeftAlone() {
        val tools = listOf(NoteBarTool.Shapes, NoteBarTool.Pen)
        assertEquals(tools, NoteBarTool.withShapesOffered(tools))
    }

    @Test
    fun fixedSlotsFollowTheAxisAndTheInsertButton() {
        assertEquals(6, barFixedSlots(insertOnBar = true, vertical = false))
        assertEquals(5, barFixedSlots(insertOnBar = false, vertical = false))
        assertEquals(5, barFixedSlots(insertOnBar = true, vertical = true))
        assertEquals(4, barFixedSlots(insertOnBar = false, vertical = true))
    }

    @Test
    fun aVerticalBarDropsToolsFromTheEndOfItsHeight() {
        // Height plays the part the width does on a horizontal bar: the tools that do not fit are dropped from the end.
        val tall = fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, false, measure(600f, barFixedSlots(true, true)))
        assertFalse(tall.showTitle)
        assertEquals(NoteBarTool.defaults.take(tall.tools.size), tall.tools)
        val short =
            fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, false, measure(400f, barFixedSlots(true, true)))
        assertTrue(short.tools.size < tall.tools.size)
        assertTrue(NoteBarTool.Pen in short.tools)
    }

    @Test
    fun aVerticalBarNeverShowsATitleEvenWithRoom() {
        val fit = fitBar(NoteBarTool.defaults, null, false, false, measure(2000f, barFixedSlots(true, true)))
        assertFalse(fit.showTitle)
        assertEquals(NoteBarTool.defaults, fit.tools)
    }
}
