package com.onemnemo.grapho.ui.note

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.onemnemo.grapho.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A request to open the wheel and its customiser. A new instance is a new request. */
class CustomiseRequest

/**
 * Which places the wheel's tool ring has and what is in them, kept in shared preferences as a JSON
 * array: a tool id for a tool on its own, or an object for a group. A plain list of ids saved by an
 * earlier version still loads, as tools on their own. Nothing saved yet means the catalogue's first
 * tools in its order.
 */
internal class WheelToolStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The places on the ring in order, with any empty groups. */
    val slots = mutableStateListOf<WheelSlot>()

    init {
        slots.addAll(load())
    }

    /** How many places the ring uses now. */
    val shown: Int get() = shownCount(slots)

    val toolsOnCount: Int get() = toolsOn(slots).size

    /** Turns [id] on at the end of the ring, or off. False when the wheel is full or it is the last tool on. */
    fun toggle(id: String): Boolean = commit(withToolToggled(slots, id))

    /** Moves the place at [index] by [delta] places. */
    fun move(index: Int, delta: Int) {
        commit(withSlotMoved(slots, index, delta))
    }

    fun moveIn(groupId: String, toolId: String, delta: Int) {
        commit(withToolMovedIn(slots, groupId, toolId, delta))
    }

    /** Puts [toolId] in the group [groupId], or takes it out when null. False when there is no room to take it out. */
    fun setGroup(toolId: String, groupId: String?): Boolean = commit(withToolInGroup(slots, toolId, groupId))

    /** Adds an empty group and returns its id. */
    fun newGroup(): String {
        val id = "g" + System.nanoTime().toString(36)
        val number = slots.count { it is WheelSlot.Group } + 1
        commit(withNewGroup(slots, id, appContext.getString(R.string.wheel_group_default, number)))
        return id
    }

    fun rename(groupId: String, name: String) {
        commit(withGroupRenamed(slots, groupId, name))
    }

    fun removeGroup(groupId: String) {
        commit(withoutGroup(slots, groupId))
    }

    private fun commit(next: List<WheelSlot>?): Boolean {
        if (next == null) return false
        val keep = next.toList()
        slots.clear()
        slots.addAll(keep)
        save()
        return true
    }

    private fun save() {
        prefs.edit().putString(SLOTS_KEY, encode(slots)).apply()
    }

    private fun encode(list: List<WheelSlot>): String {
        val array = JSONArray()
        for (slot in list) {
            when (slot) {
                is WheelSlot.Single -> array.put(slot.toolId)
                is WheelSlot.Group -> array.put(
                    JSONObject()
                        .put("group", slot.id)
                        .put("name", slot.name)
                        .put("tools", JSONArray(slot.toolIds)),
                )
            }
        }
        return array.toString()
    }

    private fun load(): List<WheelSlot> {
        val known = TOOLS.map { it.id }
        val saved = prefs.getString(SLOTS_KEY, null)?.let { parse(it, known)?.let(::withoutRetired) }
        val layout = if (saved != null && toolsOn(saved).isNotEmpty()) saved else defaults(known)
        return withOffers(layout, known)
    }

    private fun defaults(known: List<String>): List<WheelSlot> {
        val legacy = prefs.getString(LEGACY_KEY, null)
            ?.split(',')
            ?.filter { it in known }
            ?.distinct()
            .orEmpty()
        return (legacy.ifEmpty { known }).take(MAX_WHEEL_TOOLS).map { WheelSlot.Single(it) }
    }

    // A layout saved before a tool existed would never show it, so each new tool is added once. After that
    // the person can turn it off and it stays off.
    private fun withOffers(layout: List<WheelSlot>, known: List<String>): List<WheelSlot> {
        val offered = prefs.getStringSet(OFFERED_KEY, null)?.toSet() ?: ORIGINAL_TOOL_IDS
        if (known.all { it in offered }) return layout
        val next = withNewToolsOffered(layout, known, offered)
        prefs.edit()
            .putString(SLOTS_KEY, encode(next))
            .putStringSet(OFFERED_KEY, known.toSet())
            .apply()
        return next
    }

    private fun parse(text: String, known: List<String>): List<WheelSlot>? = try {
        val array = JSONArray(text)
        val seen = mutableSetOf<String>()
        val out = mutableListOf<WheelSlot>()
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is JSONObject) {
                val ids = item.getJSONArray("tools")
                val tools = List(ids.length()) { ids.getString(it) }.filter { (it in known || it in RETIRED_TOOL_IDS) && seen.add(it) }
                out.add(WheelSlot.Group(item.getString("group"), item.getString("name"), tools))
            } else if (item is String && (item in known || item in RETIRED_TOOL_IDS) && seen.add(item)) {
                out.add(WheelSlot.Single(item))
            }
        }
        out
    } catch (e: JSONException) {
        null
    }

    private companion object {
        const val PREFS = "grapho_wheel_tools"
        const val LEGACY_KEY = "enabled"
        const val SLOTS_KEY = "slots"
        const val OFFERED_KEY = "offered"
    }
}
