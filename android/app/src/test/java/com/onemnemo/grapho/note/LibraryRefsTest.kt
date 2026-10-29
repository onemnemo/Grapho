package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryRefsTest {
    @Test
    fun aColourBecomesASixDigitUpperCaseRefWithoutAlpha() {
        assertEquals("color:#FF8800", CoverRef.color(0xFFFF8800.toInt()))
        assertEquals("color:#000000", CoverRef.color(0xFF000000.toInt()))
        assertEquals("color:#00000A", CoverRef.color(0x1000000A))
    }

    @Test
    fun aColourRefReadsBackAsOpaqueArgb() {
        assertEquals(0xFF3366CC.toInt(), CoverRef.colorOf("color:#3366CC"))
        assertEquals(0xFF3366CC.toInt(), CoverRef.colorOf("color:#3366cc"))
        assertEquals(0xFFFF8800.toInt(), CoverRef.colorOf(CoverRef.color(0x80FF8800.toInt())))
    }

    @Test
    fun otherRefsAndDamagedColourRefsAreNotColours() {
        assertNull(CoverRef.colorOf(null))
        assertNull(CoverRef.colorOf(""))
        assertNull(CoverRef.colorOf("preset:meadow"))
        assertNull(CoverRef.colorOf("color:#FFF"))
        assertNull(CoverRef.colorOf("color:#3366CCFF"))
        assertNull(CoverRef.colorOf("color:#-12345"))
        assertNull(CoverRef.colorOf("color:#GGGGGG"))
        assertNull(CoverRef.colorOf("COLOR:#3366CC"))
    }

    @Test
    fun kindFollowsTheCanvasAndThePdfPages() {
        assertEquals(LibraryKind.Canvas, LibraryKind.of(infinite = true, hasPdf = false))
        assertEquals(LibraryKind.Canvas, LibraryKind.of(infinite = true, hasPdf = true))
        assertEquals(LibraryKind.Pdf, LibraryKind.of(infinite = false, hasPdf = true))
        assertEquals(LibraryKind.Notebook, LibraryKind.of(infinite = false, hasPdf = false))
    }

    @Test
    fun kindKeysRoundTripAndAnUnknownKeyIsANotebook() {
        LibraryKind.values().forEach { assertEquals(it, LibraryKind.fromKey(it.key)) }
        assertEquals(LibraryKind.Notebook, LibraryKind.fromKey("sketch"))
    }

    @Test
    fun theErrorCodeIsReadFromTheFrontOfTheMessage() {
        assertEquals("not_found", LibraryErrors.kindOf("not_found: no such folder or item"))
        assertEquals("into_descendant", LibraryErrors.kindOf("into_descendant: a folder cannot move"))
        assertEquals("", LibraryErrors.kindOf(null))
        assertEquals("", LibraryErrors.kindOf(""))
        assertEquals("", LibraryErrors.kindOf("something went wrong"))
        assertEquals("", LibraryErrors.kindOf("Something: odd"))
    }
}
