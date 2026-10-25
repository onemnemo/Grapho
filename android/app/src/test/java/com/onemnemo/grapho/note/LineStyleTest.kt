package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class LineStyleTest {
    @Test
    fun codesMatchTheCore() {
        assertEquals(0, LineStyle.SOLID.code)
        assertEquals(1, LineStyle.DASHED.code)
        assertEquals(2, LineStyle.DOTTED.code)
    }

    @Test
    fun codesRoundTrip() {
        for (style in LineStyle.entries) assertEquals(style, LineStyle.fromCode(style.code))
    }

    @Test
    fun unknownCodeIsSolid() {
        assertEquals(LineStyle.SOLID, LineStyle.fromCode(7))
        assertEquals(LineStyle.SOLID, LineStyle.fromCode(-1))
    }

    @Test
    fun keysRoundTripAndMissingIsSolid() {
        for (style in LineStyle.entries) assertEquals(style, LineStyle.fromKey(style.key))
        assertEquals(LineStyle.SOLID, LineStyle.fromKey(null))
        assertEquals(LineStyle.SOLID, LineStyle.fromKey("wavy"))
    }
}
