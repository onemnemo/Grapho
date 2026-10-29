package com.onemnemo.grapho.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LicencesTest {
    @Test
    fun aFontFileNamesWhatItCoversAndItsKind() {
        assertEquals(Licence("Geist", "OFL", "licenses/geist-OFL.txt"), licenceFor("geist-OFL.txt"))
    }

    @Test
    fun theKindKeepsItsVersion() {
        val expected = Licence("Grapho", "Apache 2.0", "licenses/grapho-Apache-2.0.txt")
        assertEquals(expected, licenceFor("grapho-Apache-2.0.txt"))
    }

    @Test
    fun aFileWithNoKindShowsTheWholeName() {
        assertEquals(Licence("Notice", "", "licenses/notice.txt"), licenceFor("notice.txt"))
    }
}
