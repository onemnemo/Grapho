package com.onemnemo.grapho.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Kotlin tokens must say what `tokens.css` says, for every theme variant and both densities. */
class GraphoTokensSnapshotTest {
    private val css = TokensCss.load()

    @Test
    fun everyThemeVariantIsPresent() {
        val expected = setOf("ink-dark", "ink-light", "graphite-dark", "graphite-light", "paper-light", "paper-dark")
        assertEquals(expected, GraphoThemes.all.keys)
    }

    @Test
    fun colourTokensMatchTokensCssInEveryVariant() {
        for ((id, colours) in GraphoThemes.all) {
            val scope = css.scope("[data-theme=\"$id\"]")
            val actual = colourFields(colours)
            assertTrue("$id has colours", actual.size > 80)
            for ((name, bits) in actual) {
                assertEquals("$id $name", argb(css.literal(kebab(name), scope)), bits)
            }
        }
    }

    @Test
    fun everyThemedColourInTokensCssIsInKotlin() {
        for (id in GraphoThemes.all.keys) {
            val scope = css.scope("[data-theme=\"$id\"]")
            val known = colourFields(GraphoThemes.all.getValue(id)).keys.map { kebab(it) }.toSet()
            val missing = scope.keys.filter { css.literal(it, scope).startsWith("#") && it !in known }
            assertTrue("$id is missing $missing", missing.isEmpty())
        }
    }

    @Test
    fun touchMetricsMatchTokensCssWithAndroidOverrides() {
        val touch = css.scope("[data-density=\"touch\"]")
        val android = css.scope("[data-platform=\"android\"][data-density=\"touch\"]")
        assertMetrics("touch", GraphoTouch, touch + android)
    }

    @Test
    fun compactMetricsMatchTokensCss() {
        assertMetrics("compact", GraphoCompact, css.scope("[data-density=\"compact\"]"))
    }

    @Test
    fun spaceRadiusAndSizeTokensMatchTokensCss() {
        // The Android file bakes in the platform rule (hit target 48, wheel sizes) over the root values.
        val scope = css.scope(":root[data-platform=\"android\"]:not([data-density=\"compact\"])")
        var compared = 0
        for ((name, dp) in dpFields(GraphoSpace)) {
            val key = kebab(name)
            if (key !in css.root && key !in scope) continue
            assertEquals(name, px(css.literal(key, scope)), dp, 0.001f)
            compared++
        }
        assertTrue("compared $compared dimensions", compared > 50)
    }

    private fun assertMetrics(label: String, metrics: GraphoMetrics, scope: Map<String, String>) {
        val actual = dpFields(metrics)
        assertTrue("$label has metrics", actual.size > 15)
        for ((name, dp) in actual) {
            assertEquals("$label $name", px(css.literal(kebab(name), scope)), dp, 0.001f)
        }
    }

    // Compose colours and dimensions are inline classes, so they sit in the fields as a long and a float.
    private fun colourFields(target: Any): Map<String, Int> =
        target.javaClass.declaredFields.filter { it.type == java.lang.Long.TYPE }.associate {
            it.isAccessible = true
            // An sRGB Color keeps its 8 bit ARGB in the upper half of the long.
            it.name to (it.getLong(target) ushr 32).toInt()
        }

    private fun dpFields(target: Any): Map<String, Float> =
        target.javaClass.declaredFields.filter { it.type == java.lang.Float.TYPE }.associate {
            it.isAccessible = true
            it.name to it.getFloat(target)
        }

    private fun kebab(name: String) = name.replace(Regex("(?<=[a-z])(?=[A-Z0-9])"), "-").lowercase()

    private fun px(value: String) = value.removeSuffix("px").toFloat()

    /** `#RRGGBB` or `#RRGGBBAA` as an ARGB int. */
    private fun argb(hex: String): Int {
        val digits = hex.removePrefix("#")
        val v = digits.toLong(16)
        return when (digits.length) {
            6 -> (0xFF000000L or v).toInt()
            8 -> (((v and 0xFF) shl 24) or (v ushr 8)).toInt()
            else -> error("Not a colour: $hex")
        }
    }
}
