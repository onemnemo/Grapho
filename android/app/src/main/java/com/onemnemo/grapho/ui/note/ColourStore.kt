package com.onemnemo.grapho.ui.note

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.PenColors
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** A named group of opaque ARGB colours that sits on the wheel's rim, in order. */
internal data class ColourSet(val id: String, val name: String, val colours: List<Int>)

/** A rim slot that holds the quiet strip between two sets instead of a colour. No real colour is 0, as they are opaque. */
internal const val RIM_STRIP = 0

/** The colours the third built-in set starts with. */
private val MORE_COLOURS = listOf(
    Color(0xFF0D9488), Color(0xFFDB2777), Color(0xFF8B5E3C), Color(0xFFEAB308),
    Color(0xFF1E3A8A), Color(0xFF0EA5E9), Color(0xFF84CC16), Color(0xFF7F1D1D),
    Color(0xFFA1A1AA), Color(0xFF6B21A8), Color(0xFFF43F5E), Color(0xFFFDBA74),
    Color(0xFF7DD3FC), Color(0xFFC4B5FD), Color(0xFF6EE7B7), Color(0xFFFCA5A5),
    Color(0xFFBEF264), Color(0xFFE9D5FF),
)

internal fun newSetId(): String = UUID.randomUUID().toString()

/**
 * The rim as slots: every colour of every set in order, with [RIM_STRIP] between sets that have colours.
 * With more than one such set a strip also closes the circle, so the last set does not touch the first.
 * A set without colours adds nothing, so there is never a double strip.
 */
internal fun rimOf(sets: List<ColourSet>): List<Int> {
    val filled = sets.filter { it.colours.isNotEmpty() }
    val out = ArrayList<Int>()
    for ((i, set) in filled.withIndex()) {
        if (i > 0) out.add(RIM_STRIP)
        out.addAll(set.colours)
    }
    if (filled.size > 1) out.add(RIM_STRIP)
    return out
}

/** The rim slot of colour [colourIndex] in set [setIndex] of [sets], matching [rimOf]. */
internal fun rimIndexOf(sets: List<ColourSet>, setIndex: Int, colourIndex: Int): Int {
    var slot = 0
    var seen = 0
    for ((i, set) in sets.withIndex()) {
        if (set.colours.isEmpty()) continue
        if (seen > 0) slot++
        if (i == setIndex) return slot + colourIndex
        slot += set.colours.size
        seen++
    }
    return 0
}

/** The person's colour sets, kept in shared preferences as JSON. The first run starts with the built-in colours. */
internal class ColourStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The name a new set starts with, [number] being its place in the list. */
    fun defaultSetName(number: Int): String = appContext.getString(R.string.colour_set_default, number)

    /** The saved sets, or the built-in three when nothing usable is saved. */
    fun load(): List<ColourSet> {
        val stored = prefs.getString(KEY, null)?.let { parse(it) }
        if (!stored.isNullOrEmpty()) return stored
        return defaults().also { save(it) }
    }

    fun save(sets: List<ColourSet>) {
        val array = JSONArray()
        for (set in sets) {
            val colours = JSONArray()
            for (c in set.colours) colours.put(c)
            array.put(JSONObject().put("id", set.id).put("name", set.name).put("colours", colours))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun parse(text: String): List<ColourSet>? = try {
        val array = JSONArray(text)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val colours = o.getJSONArray("colours")
            ColourSet(
                id = o.getString("id"),
                name = o.getString("name"),
                colours = List(colours.length()) { j -> colours.getInt(j) or OPAQUE },
            )
        }
    } catch (e: org.json.JSONException) {
        null
    }

    private fun defaults(): List<ColourSet> {
        val pens = PenColors.pens.map { it.second.toArgb() }
        val highlighters = PenColors.highlighters.map { it.second.toArgb() }
        return listOf(
            ColourSet(newSetId(), appContext.getString(R.string.colour_set_pens), pens),
            ColourSet(newSetId(), appContext.getString(R.string.colour_set_highlighters), highlighters),
            ColourSet(newSetId(), appContext.getString(R.string.colour_set_more), MORE_COLOURS.map { it.toArgb() }),
        )
    }

    private companion object {
        const val PREFS = "grapho_colours"
        const val KEY = "sets"
        const val OPAQUE = 0xFF000000.toInt()
    }
}

/**
 * Edits the colour sets on a [ToolbarState] and saves each change, so the store stays the source of
 * truth and the rim reads what the store holds.
 */
internal class ColourBook(private val state: ToolbarState, private val store: ColourStore) {
    val sets: List<ColourSet> get() = state.sets

    init {
        if (state.sets.isEmpty()) state.sets.addAll(store.load())
    }

    private fun commit() = store.save(state.sets.toList())

    private fun edit(setIndex: Int, change: (List<Int>) -> List<Int>) {
        val set = state.sets.getOrNull(setIndex) ?: return
        state.sets[setIndex] = set.copy(colours = change(set.colours))
        commit()
    }

    fun rename(setIndex: Int, name: String) {
        val set = state.sets.getOrNull(setIndex) ?: return
        state.sets[setIndex] = set.copy(name = name)
        commit()
    }

    /** Appends [argb] to the set unless it holds it already. Returns the colour's index in the set. */
    fun add(setIndex: Int, argb: Int): Int {
        val set = state.sets.getOrNull(setIndex) ?: return 0
        val existing = set.colours.indexOf(argb)
        if (existing >= 0) return existing
        edit(setIndex) { it + argb }
        return set.colours.size
    }

    fun replace(setIndex: Int, colourIndex: Int, argb: Int) =
        edit(setIndex) { c -> c.mapIndexed { i, old -> if (i == colourIndex) argb else old } }

    fun remove(setIndex: Int, colourIndex: Int) = edit(setIndex) { c -> c.filterIndexed { i, _ -> i != colourIndex } }

    /** Moves a colour by [delta] places and returns its new index. */
    fun move(setIndex: Int, colourIndex: Int, delta: Int): Int {
        val size = state.sets.getOrNull(setIndex)?.colours?.size ?: return colourIndex
        val to = (colourIndex + delta).coerceIn(0, size - 1)
        if (to == colourIndex) return colourIndex
        edit(setIndex) { c -> c.toMutableList().also { it.add(to, it.removeAt(colourIndex)) } }
        return to
    }

    /** Adds an empty set and returns its index. */
    fun addSet(): Int {
        state.sets.add(ColourSet(newSetId(), store.defaultSetName(state.sets.size + 1), emptyList()))
        commit()
        return state.sets.size - 1
    }

    /** Removes a set and returns it, or null when it is the last one, which is never deleted. */
    fun removeSet(setIndex: Int): ColourSet? {
        if (state.sets.size < 2) return null
        val gone = state.sets.getOrNull(setIndex) ?: return null
        setAll(state.sets.toList().withoutAt(setIndex))
        return gone
    }

    /** Puts a deleted [set] back at [index], or at the end when there are fewer sets now. */
    fun restoreSet(index: Int, set: ColourSet) = setAll(state.sets.toList().restoredAt(index, set))

    /** Moves a set to the front, so its colours come first on the rim, and returns its new index. */
    fun moveSetFirst(setIndex: Int): Int {
        if (setIndex !in 1..state.sets.lastIndex) return setIndex
        setAll(state.sets.toList().movedFirst(setIndex))
        return 0
    }

    private fun setAll(next: List<ColourSet>) {
        state.sets.clear()
        state.sets.addAll(next)
        commit()
    }

    /** Moves a set by [delta] places and returns its new index. */
    fun moveSet(setIndex: Int, delta: Int): Int {
        val to = (setIndex + delta).coerceIn(0, state.sets.size - 1)
        if (to == setIndex || setIndex !in state.sets.indices) return setIndex
        state.sets.add(to, state.sets.removeAt(setIndex))
        commit()
        return to
    }
}
