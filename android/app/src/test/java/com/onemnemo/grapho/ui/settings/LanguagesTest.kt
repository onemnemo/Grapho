package com.onemnemo.grapho.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguagesTest {
    @Test
    fun theListIsTheEightLanguagesInOrder() {
        assertEquals(listOf("en", "nb", "fr", "de", "es", "hi", "it", "sv"), appLanguageTags)
    }

    @Test
    fun noChosenLocaleMeansTheSystemLanguage() {
        assertNull(chosenLanguage(emptyList()))
    }

    @Test
    fun aRegionalTagCountsAsItsLanguage() {
        assertEquals("nb", chosenLanguage(listOf("nb-NO")))
        assertEquals("de", chosenLanguage(listOf("de_AT")))
    }

    @Test
    fun theFirstListedLocaleWins() {
        assertEquals("fr", chosenLanguage(listOf("pt-BR", "fr-CA", "en")))
    }

    @Test
    fun aLanguageThatIsNotListedIsNotChosen() {
        assertNull(chosenLanguage(listOf("pt-BR", "ja")))
    }
}
