package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class MathRunsTest {
    private fun plain(text: String) = MathRun.Plain(text)
    private fun inline(latex: String) = MathRun.Math(latex, display = false)
    private fun display(latex: String) = MathRun.Math(latex, display = true)

    @Test
    fun plainTextIsOneRun() {
        assertEquals(listOf(plain("just words")), MathRuns.split("just words"))
    }

    @Test
    fun emptyTextIsOneEmptyRun() {
        assertEquals(listOf(plain("")), MathRuns.split(""))
    }

    @Test
    fun inlineMathSitsBetweenWords() {
        assertEquals(
            listOf(plain("so "), inline("x^2 + y^2"), plain(" is round")),
            MathRuns.split("so \$x^2 + y^2\$ is round"),
        )
    }

    @Test
    fun displayMathIsItsOwnRun() {
        assertEquals(
            listOf(plain("area\n"), display("\\pi r^2"), plain("\nok")),
            MathRuns.split("area\n\$\$ \\pi r^2 \$\$\nok"),
        )
    }

    @Test
    fun pricesStayPlain() {
        assertEquals(listOf(plain("costs \$5 and \$10")), MathRuns.split("costs \$5 and \$10"))
    }

    @Test
    fun aSpaceAfterTheOpeningDollarIsNotMath() {
        assertEquals(listOf(plain("\$ x\$")), MathRuns.split("\$ x\$"))
    }

    @Test
    fun inlineMathDoesNotCrossLines() {
        assertEquals(listOf(plain("\$a\nb\$")), MathRuns.split("\$a\nb\$"))
    }

    @Test
    fun anEscapedDollarIsPlain() {
        assertEquals(listOf(plain("\$x\$")), MathRuns.split("\\\$x\\\$"))
    }

    @Test
    fun anEscapedDollarInsideMathStaysInTheFormula() {
        assertEquals(listOf(inline("a\\\$b")), MathRuns.split("\$a\\\$b\$"))
    }

    @Test
    fun anUnclosedDollarStaysAsTyped() {
        assertEquals(listOf(plain("half \$x + 1")), MathRuns.split("half \$x + 1"))
        assertEquals(listOf(plain("half \$\$x + 1")), MathRuns.split("half \$\$x + 1"))
    }

    @Test
    fun emptyDisplayMathStaysAsTyped() {
        assertEquals(listOf(plain("\$\$\$\$")), MathRuns.split("\$\$\$\$"))
    }
}
