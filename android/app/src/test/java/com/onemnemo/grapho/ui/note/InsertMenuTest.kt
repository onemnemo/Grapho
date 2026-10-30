package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertMenuTest {
    @Test
    fun insertKeepsPagesWhenTheImageToolTakesPictures() {
        assertTrue(insertHasItems(imageTool = true, infinite = false))
    }

    @Test
    fun insertIsEmptyOnAnInfiniteCanvasWithTheImageTool() {
        assertFalse(insertHasItems(imageTool = true, infinite = true))
    }

    @Test
    fun insertHoldsPicturesWithoutTheImageTool() {
        assertTrue(insertHasItems(imageTool = false, infinite = true))
    }
}
