package com.onemnemo.grapho.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Keeps every translation in step with the default strings: same keys, same placeholders, same house voice. */
class StringResourcesTest {
    private class Entry(val plural: Boolean, val items: Map<String, String>, val translatable: Boolean)

    private val resDir = File("src/main/res")

    private fun parse(file: File): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = LinkedHashMap<String, Entry>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            val translatable = node.getAttribute("translatable") != "false"
            val entry = when (node.tagName) {
                "string" -> Entry(false, mapOf("" to node.textContent), translatable)

                "plurals" -> {
                    val items = LinkedHashMap<String, String>()
                    val children = node.getElementsByTagName("item")
                    for (j in 0 until children.length) {
                        val item = children.item(j) as Element
                        items[item.getAttribute("quantity")] = item.textContent
                    }
                    Entry(true, items, translatable)
                }

                else -> continue
            }
            assertFalse("${file.path}: $name is defined twice", out.containsKey(name))
            out[name] = entry
        }
        return out
    }

    private val default = parse(File(resDir, "values/strings.xml"))

    private val languages: List<String> = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values-") }!!
        .map { it.name.removePrefix("values-") }
        .filter { File(resDir, "values-$it/strings.xml").exists() }
        .sorted()

    private val placeholder = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]")

    private fun placeholders(text: String): List<String> =
        placeholder.findAll(text.replace("%%", "")).map { it.value }.sorted().toList()

    private fun translated(lang: String) = parse(File(resDir, "values-$lang/strings.xml"))

    @Test
    fun everyLanguageInTheLocaleConfigHasStrings() {
        val config = File(resDir, "xml/locales_config.xml").readText()
        val listed = Regex("android:name=\"([A-Za-z-]+)\"").findAll(config).map { it.groupValues[1] }.toSet()
        assertEquals(listed - "en", languages.toSet())
    }

    @Test
    fun everyLanguageHasExactlyTheDefaultKeys() {
        val expected = default.filterValues { it.translatable }.keys
        for (lang in languages) {
            val keys = translated(lang).keys
            assertEquals("$lang is missing keys", emptySet<String>(), expected - keys)
            assertEquals("$lang has keys the default does not", emptySet<String>(), keys - expected)
        }
    }

    @Test
    fun placeholdersMatchTheDefault() {
        for (lang in languages) {
            for ((key, entry) in translated(lang)) {
                val base = default.getValue(key)
                assertEquals("$lang/$key kind", base.plural, entry.plural)
                if (!entry.plural) {
                    assertEquals(
                        "$lang/$key placeholders",
                        placeholders(base.items.getValue("")),
                        placeholders(entry.items.getValue("")),
                    )
                } else {
                    val want = placeholders(base.items.getValue("other"))
                    for ((quantity, text) in entry.items) {
                        val got = placeholders(text)
                        if (quantity == "other") {
                            assertEquals("$lang/$key other placeholders", want, got)
                        } else {
                            assertTrue("$lang/$key $quantity placeholders", want.containsAll(got))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun pluralsHaveAnOtherItem() {
        for ((lang, strings) in listOf("default" to default) + languages.map { it to translated(it) }) {
            for ((key, entry) in strings) {
                if (!entry.plural) continue
                assertTrue("$lang/$key needs an other item", "other" in entry.items)
                assertTrue("$lang/$key needs a one item", "one" in entry.items)
            }
        }
    }

    @Test
    fun noTextIsEmpty() {
        for ((lang, strings) in listOf("default" to default) + languages.map { it to translated(it) }) {
            for ((key, entry) in strings) {
                for ((quantity, text) in entry.items) {
                    assertTrue("$lang/$key $quantity is empty", text.isNotBlank())
                }
            }
        }
    }

    @Test
    fun noEmOrEnDashes() {
        val files =
            listOf(File(resDir, "values/strings.xml")) + languages.map { File(resDir, "values-$it/strings.xml") }
        for (file in files) {
            val text = file.readText()
            assertFalse("${file.path} has an em dash", text.contains(EM_DASH))
            assertFalse("${file.path} has an en dash", text.contains(EN_DASH))
        }
    }

    private companion object {
        val EM_DASH = 0x2014.toChar()
        val EN_DASH = 0x2013.toChar()
    }
}
