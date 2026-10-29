package com.onemnemo.grapho.ui.note

/** The list without the item at [index], or the same list when [index] is out of range. */
internal fun <T> List<T>.withoutAt(index: Int): List<T> =
    if (index in indices) filterIndexed { i, _ -> i != index } else this

/** The list with [item] put back at [index], kept within the list so a restore after other edits still lands. */
internal fun <T> List<T>.restoredAt(index: Int, item: T): List<T> =
    toMutableList().also { it.add(index.coerceIn(0, size), item) }

/** The list with the item at [index] moved to the front, or the same list when it is there already. */
internal fun <T> List<T>.movedFirst(index: Int): List<T> =
    if (index in 1..lastIndex) toMutableList().also { it.add(0, it.removeAt(index)) } else this
