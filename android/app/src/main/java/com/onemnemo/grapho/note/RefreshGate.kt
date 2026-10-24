package com.onemnemo.grapho.note

/**
 * Keeps at most one background refresh running per key, with one more
 * remembered behind it. Requests that arrive while a refresh runs merge into
 * the one behind it, and an upgrade request is never lost in the merge.
 * Used from the UI thread only.
 */
class RefreshGate<K : Any> {
    private val running = HashSet<K>()
    private val queued = HashMap<K, Boolean>()

    /** True if the caller should start a refresh now; otherwise the request is queued behind the running one. */
    fun submit(key: K, upgrade: Boolean): Boolean {
        if (key in running) {
            queued[key] = upgrade || queued[key] == true
            return false
        }
        running += key
        return true
    }

    /** Ends the running refresh. Returns the queued request's upgrade flag (the key runs again), or null if none. */
    fun complete(key: K): Boolean? {
        val next = queued.remove(key)
        if (next == null) running -= key
        return next
    }

    fun clear() {
        running.clear()
        queued.clear()
    }
}
