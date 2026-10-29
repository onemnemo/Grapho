package com.onemnemo.grapho.note

import com.onemnemo.grapho.engine.NativeLibrary
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/** A failed library call. [kind] is the core's error code, see [LibraryErrors]. */
class LibraryException(val kind: String, message: String) : IOException(message)

/** A folder. A null [parentId] is the top level. A folder with a [deletedAt] is the root of what was deleted. */
data class LibraryFolder(
    val id: String,
    val parentId: String?,
    val name: String,
    val styleRef: String?,
    val sortOrder: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val deletedAt: Long?,
    /** Shared with the items that went to Trash with this folder, or null while it is live. */
    val deleteBatchId: String? = null,
)

/** One document in the library. [fileRef] is the name of its `.grapho` file. Times are UTC milliseconds. */
data class LibraryItem(
    val id: String,
    val kind: LibraryKind,
    val title: String,
    val folderId: String?,
    val coverRef: String?,
    val pageCount: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val openedAt: Long?,
    val starred: Boolean,
    val deletedAt: Long?,
    val deletedFromFolderId: String?,
    val deleteBatchId: String?,
    val fileRef: String,
)

/** One page of a listing: [total] counts every match, [items] only the requested window. */
data class LibraryListing(val total: Int, val items: List<LibraryItem>)

/** What a permanent removal took out. The caller deletes the files named in [fileRefs]. */
data class LibraryRemoved(val fileRefs: List<String>, val items: Int, val folders: Int)

enum class LibrarySort(val key: String) { Edited("edited"), Name("name"), Size("size") }

enum class LibraryOrder(val key: String) { Asc("asc"), Desc("desc") }

/** Where a restored item goes when its folder is in Trash. */
enum class RestoreMode(val key: String) {
    Auto("auto"),
    WithFolder("withFolder"),
    ToLibrary("toLibrary"),
}

/** The core's answer to whether a folder may move to a target. */
enum class MoveVerdict {
    Ok,
    NameClashResolved,
    IntoSelf,
    IntoDescendant,
    InTrash,
    NotFound,
    ;

    val allowed: Boolean get() = this == Ok || this == NameClashResolved
}

/**
 * The library of folders, items and Trash, owned by the core. Every call is a named operation on the native
 * library; saving the JSON from [toJson] is the caller's job. Calls are serialised, so any thread may use it,
 * but the store calls it from one task at a time. Ids are 26 character text. Call [close] once.
 */
class Library private constructor(private var handle: Long) {

    /** The library as JSON, for writing to disk. */
    @Synchronized
    fun toJson(): String = NativeLibrary.nativeToJson(live())

    @Synchronized
    fun close() {
        if (handle != 0L) {
            NativeLibrary.nativeFree(handle)
            handle = 0L
        }
    }

    /** Makes a folder and returns it, with the name it got if a sibling had the same one. */
    fun createFolder(parentId: String?, name: String, now: Long): LibraryFolder {
        val id = call("createFolder", args("parentId" to parentId, "name" to name, "now" to now)).getString("id")
        return checkNotNull(folder(id)) { "created folder is missing" }
    }

    /** Renames a folder and returns the name it got. */
    fun renameFolder(id: String, name: String, now: Long): String =
        call("renameFolder", args("id" to id, "name" to name, "now" to now)).getString("name")

    /** Moves a folder under [parentId] (null for the top level) and returns its name there. */
    fun moveFolder(id: String, parentId: String?, now: Long): String =
        call("moveFolder", args("id" to id, "parentId" to parentId, "now" to now)).getString("name")

    /** Places a folder right after the sibling [afterId], or first when that is null. */
    fun reorderFolder(id: String, afterId: String?, now: Long) {
        call("reorderFolder", args("id" to id, "afterId" to afterId, "now" to now))
    }

    fun canMove(id: String, targetId: String?): MoveVerdict =
        when (call("canMove", args("id" to id, "targetId" to targetId)).getString("result")) {
            "ok" -> MoveVerdict.Ok
            "name_clash_resolved" -> MoveVerdict.NameClashResolved
            "into_self" -> MoveVerdict.IntoSelf
            "into_descendant" -> MoveVerdict.IntoDescendant
            "in_trash" -> MoveVerdict.InTrash
            else -> MoveVerdict.NotFound
        }

    /** Sends a folder and every live item inside it to Trash. */
    fun trashFolder(id: String, now: Long) {
        call("trashFolder", args("id" to id, "now" to now))
    }

    /** Brings a deleted folder back with the items that were deleted with it. */
    fun restoreFolder(id: String, now: Long) {
        call("restoreFolder", args("id" to id, "now" to now))
    }

    fun deleteFolderForever(id: String): LibraryRemoved = removed(call("deleteFolderForever", args("id" to id)))

    /** The live folders directly under [parentId] (null for the top level), in their manual order. */
    fun subfolders(parentId: String?): List<LibraryFolder> =
        folders(callRaw("subfolders", args("parentId" to parentId)))

    /** The deleted folders that are the roots of what was deleted, oldest first. */
    fun trashFolders(): List<LibraryFolder> = folders(callRaw("trashFolders", args()))

    fun folder(id: String): LibraryFolder? =
        callRaw("folder", args("id" to id)).let { if (it == NULL) null else parseFolder(JSONObject(it)) }

    /** Registers a document and returns its item id. Times of 0 or less use [now]. */
    fun addItem(
        kind: LibraryKind,
        title: String,
        folderId: String?,
        coverRef: String?,
        pageCount: Int,
        fileRef: String,
        createdAt: Long,
        modifiedAt: Long,
        now: Long,
    ): String = call(
        "addItem",
        args(
            "kind" to kind.key,
            "title" to title,
            "folderId" to folderId,
            "coverRef" to coverRef,
            "pageCount" to pageCount,
            "fileRef" to fileRef,
            "createdAt" to createdAt.takeIf { it > 0 },
            "modifiedAt" to modifiedAt.takeIf { it > 0 },
            "now" to now,
        ),
    ).getString("id")

    fun item(id: String): LibraryItem? =
        callRaw("item", args("id" to id)).let { if (it == NULL) null else parseItem(JSONObject(it)) }

    /** The item that points at the file named [fileRef], live or in Trash. */
    fun itemByFile(fileRef: String): LibraryItem? =
        callRaw("itemByFile", args("fileRef" to fileRef)).let { if (it == NULL) null else parseItem(JSONObject(it)) }

    /** The file names of every item. */
    fun fileRefs(): List<String> =
        JSONArray(callRaw("fileRefs", args())).let { a -> List(a.length()) { a.getString(it) } }

    /** Drops the items whose file is not in [existing] and returns their ids. */
    fun dropMissingFiles(existing: List<String>): List<String> =
        call("dropMissingFiles", args("files" to JSONArray(existing))).getJSONArray("ids").let { a ->
            List(a.length()) { a.getString(it) }
        }

    /** Sets the cached kind, for a document that changed what it is, such as a notebook that gained a PDF page. */
    fun setKind(id: String, kind: LibraryKind) {
        call("setKind", args("id" to id, "kind" to kind.key))
    }

    fun renameItem(id: String, title: String, now: Long) {
        call("renameItem", args("id" to id, "title" to title, "now" to now))
    }

    /** Moves live items to [folderId] (null for the top level). Nothing moves unless all of them can. */
    fun moveItems(ids: List<String>, folderId: String?) {
        call("moveItems", args("ids" to JSONArray(ids), "folderId" to folderId))
    }

    fun setStarred(id: String, starred: Boolean) {
        call("setStarred", args("id" to id, "starred" to starred))
    }

    fun setCover(id: String, coverRef: String?, now: Long) {
        call("setCover", args("id" to id, "coverRef" to coverRef, "now" to now))
    }

    /** Records that the item was opened, which Recent sorts by. */
    fun touchOpened(id: String, now: Long) {
        call("touchOpened", args("id" to id, "now" to now))
    }

    /** Records that the document was saved, with its page count if known. */
    fun touchModified(id: String, now: Long, pageCount: Int?) {
        call("touchModified", args("id" to id, "now" to now, "pageCount" to pageCount))
    }

    /** Sends live items to Trash. Nothing is trashed unless all of them can be. */
    fun trashItems(ids: List<String>, now: Long) {
        call("trashItems", args("ids" to JSONArray(ids), "now" to now))
    }

    /** True when restoring the item needs a [RestoreMode] other than [RestoreMode.Auto]. */
    fun restoreNeedsChoice(id: String): Boolean = call("restoreNeedsChoice", args("id" to id)).getBoolean("needsChoice")

    /** Takes an item out of Trash and returns the folder it went to, or null for the top level. */
    fun restoreItem(id: String, mode: RestoreMode, now: Long): String? {
        val reply = call("restoreItem", args("id" to id, "mode" to mode.key, "now" to now))
        return if (reply.isNull("folderId")) null else reply.getString("folderId")
    }

    fun deleteItemForever(id: String): LibraryRemoved = removed(call("deleteItemForever", args("id" to id)))

    fun emptyTrash(): LibraryRemoved = removed(call("emptyTrash", args()))

    /** Removes what has been in Trash for more than 30 days at [now]. */
    fun purge(now: Long): LibraryRemoved = removed(call("purge", args("now" to now)))

    fun inFolder(
        folderId: String?,
        sort: LibrarySort = LibrarySort.Edited,
        order: LibraryOrder = LibraryOrder.Desc,
        offset: Int = 0,
        limit: Int? = null,
    ): LibraryListing = listing("inFolder", args("folderId" to folderId), sort, order, offset, limit)

    /** Opened items, by time opened. */
    fun recent(
        sort: LibrarySort = LibrarySort.Edited,
        order: LibraryOrder = LibraryOrder.Desc,
        offset: Int = 0,
        limit: Int? = null,
    ): LibraryListing = listing("recent", args(), sort, order, offset, limit)

    fun starred(
        sort: LibrarySort = LibrarySort.Edited,
        order: LibraryOrder = LibraryOrder.Desc,
        offset: Int = 0,
        limit: Int? = null,
    ): LibraryListing = listing("starred", args(), sort, order, offset, limit)

    /** Items in Trash. [LibrarySort.Edited] orders by time deleted. */
    fun trash(
        sort: LibrarySort = LibrarySort.Edited,
        order: LibraryOrder = LibraryOrder.Desc,
        offset: Int = 0,
        limit: Int? = null,
    ): LibraryListing = listing("trash", args(), sort, order, offset, limit)

    /** Every live item in any folder. */
    fun all(
        sort: LibrarySort = LibrarySort.Edited,
        order: LibraryOrder = LibraryOrder.Desc,
        offset: Int = 0,
        limit: Int? = null,
    ): LibraryListing = listing("all", args(), sort, order, offset, limit)

    private fun live(): Long = check(handle != 0L) { "library is closed" }.let { handle }

    private fun call(op: String, args: JSONObject): JSONObject = JSONObject(callRaw(op, args))

    @Synchronized
    private fun callRaw(op: String, args: JSONObject): String = try {
        NativeLibrary.nativeCall(live(), op, args.toString())
    } catch (e: IOException) {
        val message = e.message.orEmpty()
        throw LibraryException(LibraryErrors.kindOf(message), message)
    }

    private fun listing(
        op: String,
        base: JSONObject,
        sort: LibrarySort,
        order: LibraryOrder,
        offset: Int,
        limit: Int?,
    ): LibraryListing {
        base.put("sort", sort.key).put("dir", order.key).put("offset", offset)
        if (limit != null) base.put("limit", limit)
        val reply = call(op, base)
        val items = reply.getJSONArray("items")
        return LibraryListing(reply.getInt("total"), List(items.length()) { parseItem(items.getJSONObject(it)) })
    }

    private fun removed(reply: JSONObject): LibraryRemoved {
        val refs = reply.getJSONArray("fileRefs")
        return LibraryRemoved(
            fileRefs = List(refs.length()) { refs.getString(it) },
            items = reply.getInt("items"),
            folders = reply.getInt("folders"),
        )
    }

    private fun folders(json: String): List<LibraryFolder> {
        val list = JSONArray(json)
        return List(list.length()) { parseFolder(list.getJSONObject(it)) }
    }

    companion object {
        private const val NULL = "null"

        /** Makes an empty library. */
        fun create(): Library = Library(NativeLibrary.nativeNew())

        /** Reads a library from its JSON. Throws [LibraryException] if it is damaged or from a newer version. */
        fun fromJson(json: String): Library = try {
            Library(NativeLibrary.nativeFromJson(json))
        } catch (e: IOException) {
            val message = e.message.orEmpty()
            throw LibraryException(LibraryErrors.kindOf(message), message)
        }

        private fun args(vararg pairs: Pair<String, Any?>): JSONObject = JSONObject().also { o ->
            pairs.forEach { (key, value) -> if (value != null) o.put(key, value) }
        }

        private fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)

        private fun JSONObject.time(key: String): Long? = if (isNull(key)) null else getLong(key)

        private fun parseFolder(o: JSONObject) = LibraryFolder(
            id = o.getString("id"),
            parentId = o.text("parentId"),
            name = o.getString("name"),
            styleRef = o.text("styleRef"),
            sortOrder = o.optLong("sortOrder"),
            createdAt = o.optLong("createdAt"),
            modifiedAt = o.optLong("modifiedAt"),
            deletedAt = o.time("deletedAt"),
            deleteBatchId = o.text("deleteBatchId"),
        )

        private fun parseItem(o: JSONObject) = LibraryItem(
            id = o.getString("id"),
            kind = LibraryKind.fromKey(o.getString("kind")),
            title = o.optString("title"),
            folderId = o.text("folderId"),
            coverRef = o.text("coverRef"),
            pageCount = o.optInt("pageCount"),
            createdAt = o.optLong("createdAt"),
            modifiedAt = o.optLong("modifiedAt"),
            openedAt = o.time("openedAt"),
            starred = o.optBoolean("starred"),
            deletedAt = o.time("deletedAt"),
            deletedFromFolderId = o.text("deletedFromFolderId"),
            deleteBatchId = o.text("deleteBatchId"),
            fileRef = o.getString("fileRef"),
        )
    }
}
