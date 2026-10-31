package com.onemnemo.grapho.engine

import java.io.IOException

/** JNI bridge to grapho-core's StrokeBuilder: the stroke being drawn. See ffi-android/src/lib.rs. */
object NativeInk {
    init {
        System.loadLibrary("grapho_android")
    }

    /** Starts a stroke and returns a handle. Must end with [nativeFinish]. [tool] is a [Tool] code. */
    @JvmStatic external fun nativeNew(width: Float, stabilizer: Float, tool: Int, line: Int): Long

    /** Adds samples packed as `[x, y, pressure, timeMs, ...]`, in view pixels. */
    @JvmStatic external fun nativePush(handle: Long, samples: FloatArray)

    /**
     * Changes since the last call: `[minX, minY, maxX, maxY, n, <n new triangle
     * floats>, <predicted tip triangle floats>]`. Vertices are `[x, y]` pairs.
     */
    @JvmStatic external fun nativeFrame(handle: Long, predicted: FloatArray?): FloatArray

    /** How long the pen has stayed within a few pixels of one spot, at [nowMs] on the samples' clock. */
    @JvmStatic external fun nativeStillMs(handle: Long, nowMs: Float): Float

    /**
     * The stroke redrawn as the shape it looks like: `[line, x0, y0, x1, y1, n, <n point
     * floats packed as x, y, pressure, timeMs>, <triangle floats>]`, or empty if it matches
     * no shape. `line` is 1 when the shape is a straight line from (x0, y0) to (x1, y1).
     * [shaft] is null or `[x0, y0, x1, y1]`, a line drawn just before; a stroke that looks
     * like an arrow head at its end becomes that head. The stroke itself is left unchanged.
     */
    @JvmStatic external fun nativeSnap(handle: Long, shaft: FloatArray?): FloatArray

    /**
     * After [nativeSnap], reshapes the snapped shape for the pen at ([x], [y]) in view pixels:
     * a line follows the pen, an arc continues around its circle and closed shapes scale and
     * turn. Same layout as [nativeSnap], empty if nothing has snapped.
     */
    @JvmStatic external fun nativeReshape(handle: Long, x: Float, y: Float): FloatArray

    /**
     * The shape of the Shapes tool for a drag from ([x0], [y0]) to ([x1], [y1]) in view pixels, in the layout of
     * [nativeSnap], or empty when the drag draws nothing. [spec] is `[kind, a, b, head, ends]`: see
     * ShapeSettings.spec. Push the pen's samples as the drag goes so the shape takes their pressure.
     */
    @JvmStatic external fun nativeDrawShape(
        handle: Long,
        spec: FloatArray,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ): FloatArray

    /**
     * The shape worth offering for a finished stroke given as packed `[x, y, pressure, timeMs, ...]` samples in
     * view pixels: `[class, n, <n floats packed the same way>]`, the stroke redrawn as that shape, or empty when
     * none fits. `class` is a ShapeGuess code. A stateless call: it needs no stroke handle.
     */
    @JvmStatic external fun nativeSuggest(samples: FloatArray): FloatArray

    /** Ends the stroke, frees the handle and returns its stabilised points as `[x, y, pressure, timeMs, ...]`. */
    @JvmStatic external fun nativeFinish(handle: Long): FloatArray

    /**
     * Makes the stroke follow the nearest edge of [guides] (see [NativeGuide]) to the pen at ([x], [y]) within [reach],
     * all in view pixels. Call it right after [nativeNew]. Returns whether an edge was found; the stroke is then
     * projected onto it, and neither stabilised nor snapped to a shape.
     */
    @JvmStatic external fun nativeGuide(handle: Long, guides: FloatArray, x: Float, y: Float, reach: Float): Boolean
}

/**
 * JNI bridge to grapho-core's ruler and protractor geometry. A guide is packed as six floats, `[kind, x, y, angle,
 * a, b]`: kind 0 is a ruler (a is its length, b its thickness), kind 1 a protractor (a is its radius, b unused).
 * Angles are radians, lengths share the unit of the coordinates.
 */
object NativeGuide {
    init {
        System.loadLibrary("grapho_android")
    }

    /** The index of the first packed protractor in [guides] whose centre mark is within [reach] of ([x], [y]), or -1. */
    @JvmStatic external fun nativeCentreNear(guides: FloatArray, x: Float, y: Float, reach: Float): Int

    /** Degrees from 0 to 180 between a packed protractor's base and the line from its centre to ([x], [y]), or -1. */
    @JvmStatic external fun nativeReading(guide: FloatArray, x: Float, y: Float): Float

    /** [radians] rounded to a whole degree, pulled to a multiple of 15 degrees when within 2 of one. */
    @JvmStatic external fun nativeSnapAngle(radians: Float): Float

    /** The angle a ruler along [radians] makes with the horizontal, anticlockwise, in whole degrees from 0 to 179. */
    @JvmStatic external fun nativeRulerDegrees(radians: Float): Float
}

/**
 * JNI bridge to an open document and its undo history (grapho-core's Editor).
 * A handle is thread-safe (guarded by a mutex in Rust) but must not be used
 * after [nativeFree].
 */
object NativeNotebook {
    init {
        System.loadLibrary("grapho_android")
    }

    /** [paper]: 0 A4, 1 A5, 2 Letter, 3 A3. [pattern]: a [PatternKind] code. Colours are ARGB. */
    @JvmStatic external fun nativeCreate(
        title: String,
        infinite: Boolean,
        paper: Int,
        landscape: Boolean,
        pattern: Int,
        spacingMm: Float,
        background: Int,
        cover: Int,
    ): Long

    @Throws(IOException::class)
    @JvmStatic
    external fun nativeOpen(path: String): Long

    /** document.json of a file, without ink. For the notebook list. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeReadInfo(path: String): String

    /** Saves atomically (temp file, then rename). Safe to call off the UI thread. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeSave(handle: Long, path: String)

    @JvmStatic external fun nativeFree(handle: Long)

    /** document.json plus `can_undo` and `can_redo`. */
    @JvmStatic external fun nativeInfo(handle: Long): String

    @JvmStatic external fun nativeSetTitle(handle: Long, title: String)

    @JvmStatic external fun nativeSetBackground(handle: Long, pattern: Int, spacingMm: Float, color: Int)

    /** Points in document units, packed `[x, y, pressure, timeMs, ...]`. Colour is ARGB. [line] is a line style code. */
    @JvmStatic external fun nativeAddStroke(
        handle: Long,
        page: Int,
        tool: Int,
        color: Int,
        width: Float,
        line: Int,
        startedAtMs: Long,
        points: FloatArray,
    ): Boolean

    /**
     * Bucket fill: fills the area enclosed by ink around ([x], [y]), given in page-local document
     * units, with an ARGB [color]. Stored as a fill stroke beneath the active layer's ink; one undo step.
     * [scale] is the view's screen pixels per document unit and the four edges give the visible
     * rectangle in page-local document units; the core picks its resolution and search area from
     * them. Returns false if the point is on ink, not enclosed, or the page can't take it.
     */
    @JvmStatic external fun nativeFill(
        handle: Long,
        page: Int,
        x: Float,
        y: Float,
        color: Int,
        scale: Float,
        visibleLeft: Float,
        visibleTop: Float,
        visibleRight: Float,
        visibleBottom: Float,
    ): Boolean

    /** Returns the new page's index, or -1 for an infinite canvas. */
    @JvmStatic external fun nativeAddPage(handle: Long): Int

    /**
     * Creates a paged notebook with one page per page of the PDF at [pdfPath]. [title] is the notebook's title and
     * [name] the original file name. Returns a handle, like [nativeCreate]. Throws if the PDF is damaged or
     * protected by a password.
     */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeImportPdf(pdfPath: String, title: String, name: String): Long

    /** Writes the embedded PDF [pdfId] to [outPath]; page backgrounds are rendered from that file. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeWritePdf(handle: Long, pdfId: String, outPath: String)

    /**
     * Inserts a blank page at index [at] (0 to the page count), sized like its neighbour. One undo step.
     * Returns the new page's index, or -1.
     */
    @JvmStatic external fun nativeInsertPage(handle: Long, at: Int): Int

    /**
     * Inserts every page of the PDF at [pdfPath] at index [at], as one undo step. [name] is the original file
     * name. Returns how many pages were inserted. Throws on a bad PDF.
     */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeInsertPdf(handle: Long, at: Int, pdfPath: String, name: String): Int

    /**
     * Writes the notebook as a vector PDF to [outPath]. [pages] are zero-based page indices, or null for all pages.
     * Without [backgrounds] every page is white with ink only. Throws [IOException].
     */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeExportPdf(handle: Long, outPath: String, backgrounds: Boolean, pages: IntArray?)

    /** [area] erases only the part the eraser passes over; otherwise whole strokes go. */
    @JvmStatic external fun nativeBeginErase(handle: Long, page: Int, area: Boolean)

    /**
     * Erases along a polyline in document units, packed `[x, y, ...]`, one segment per consecutive pair
     * of points and all under one lock. Returns how many strokes were removed or cut.
     */
    @JvmStatic external fun nativeErasePath(handle: Long, points: FloatArray, radius: Float): Int

    @JvmStatic external fun nativeEndErase(handle: Long)

    /**
     * Ids of the strokes on visible, unlocked layers of [page] that the closed lasso [polygon] (page-local document
     * units, `[x, y, ...]`) mostly encloses: at least half of a stroke's length lies inside. `[hi, lo, ...]`, in
     * drawing order.
     */
    @JvmStatic external fun nativeSelect(handle: Long, page: Int, polygon: FloatArray): LongArray

    /** `[minX, minY, maxX, maxY]` of the strokes' ink, width included, in page-local document units; empty if none exist. */
    @JvmStatic external fun nativeSelectionBounds(handle: Long, page: Int, ids: LongArray): FloatArray

    /**
     * Moves, scales or otherwise transforms strokes as one undo step. [matrix] is the first six values of an
     * android.graphics.Matrix, `[scaleX, skewX, transX, skewY, scaleY, transY]`, in page-local document units.
     * Widths scale with the square root of the area change. The strokes get new ids, returned in the order asked
     * for; strokes that no longer exist are left out.
     */
    @JvmStatic external fun nativeTransform(handle: Long, page: Int, ids: LongArray, matrix: FloatArray): LongArray

    /** Gives strokes a new ARGB colour as one undo step, keeping each stroke's own alpha. Returns their new ids. */
    @JvmStatic external fun nativeRecolor(handle: Long, page: Int, ids: LongArray, color: Int): LongArray

    /** The whole-point width most of the strokes are drawn at, or 0 when none has one (fills and images). */
    @JvmStatic external fun nativeCommonWidth(handle: Long, page: Int, ids: LongArray): Float

    /** Redraws strokes [width] points wide as one undo step; fills and images are kept. Returns their new ids. */
    @JvmStatic external fun nativeSetWidth(handle: Long, page: Int, ids: LongArray, width: Float): LongArray

    /**
     * Moves strokes and images to the top ([front]) or bottom of their layers as one undo step. The ids stay.
     * Returns false if nothing moved.
     */
    @JvmStatic external fun nativeReorder(handle: Long, page: Int, ids: LongArray, front: Boolean): Boolean

    /** Removes strokes as one undo step. Returns false if none of them existed. */
    @JvmStatic external fun nativeDelete(handle: Long, page: Int, ids: LongArray): Boolean

    /** Copies strokes to the notebook's clipboard, replacing what was there. Returns false if none existed. */
    @JvmStatic external fun nativeCopy(handle: Long, page: Int, ids: LongArray): Boolean

    @JvmStatic external fun nativeHasClipboard(handle: Long): Boolean

    /**
     * Pastes the clipboard onto the active layer of [page], centred on ([x], [y]) in page-local document units, as one
     * undo step. Returns the new strokes' ids; empty if the clipboard is empty or the active layer is locked or hidden.
     */
    @JvmStatic external fun nativePaste(handle: Long, page: Int, x: Float, y: Float): LongArray

    /**
     * Moves strokes from [from] onto the active layer of [to], placed by [matrix] from [from]'s page units to [to]'s,
     * as one undo step. Returns the new ids, empty if nothing moved.
     */
    @JvmStatic
    external fun nativeMoveToPage(handle: Long, from: Int, ids: LongArray, to: Int, matrix: FloatArray): LongArray

    /** Copies strokes onto the active layer of [page], moved by ([dx], [dy]), as one undo step. Returns the copies' ids. */
    @JvmStatic external fun nativeDuplicate(handle: Long, page: Int, ids: LongArray, dx: Float, dy: Float): LongArray

    /** Returns the page index that changed, or -1. */
    @JvmStatic external fun nativeUndo(handle: Long): Int

    @JvmStatic external fun nativeRedo(handle: Long): Int

    /** Every page the last undo or redo changed when it reached more than one, as with a move between pages. */
    @JvmStatic external fun nativeHistoryPages(handle: Long): IntArray

    /**
     * Stroke ids of the visible layers of [page], bottom to top. For each layer: `[alpha, n, <n ids as hi, lo pairs>]`,
     * where alpha is the layer's opacity from 0 to 255. Within a layer the fills come before its other strokes.
     */
    @JvmStatic external fun nativeLayerStrokeIds(handle: Long, page: Int): LongArray

    /**
     * The layers of [page], bottom to top, as JSON:
     * `{"active":1,"layers":[{"name":"Layer 1","visible":true,"locked":false,"opacity":1.0,"strokes":12}]}`.
     * Empty string for an unknown page.
     */
    @JvmStatic external fun nativeLayers(handle: Long, page: Int): String

    /** Makes the layer at [index] the one that drawing, filling, pasting, erasing and lasso selection use. */
    @JvmStatic external fun nativeSetActiveLayer(handle: Long, page: Int, index: Int): Boolean

    /** Adds an empty layer above the active one and makes it active. Returns its index, or -1 at the limit of 10 layers. */
    @JvmStatic external fun nativeAddLayer(handle: Long, page: Int, name: String): Int

    /** Removes a layer and its strokes, as one undo step. Returns false for the last layer of a page. */
    @JvmStatic external fun nativeRemoveLayer(handle: Long, page: Int, index: Int): Boolean

    /** Moves the layer at [from] so it ends up at [to], as one undo step. */
    @JvmStatic external fun nativeMoveLayer(handle: Long, page: Int, from: Int, to: Int): Boolean

    /**
     * Moves a layer's strokes onto the layer below it and removes the layer, as one undo step.
     * Returns false for the bottom layer or when either layer is locked.
     */
    @JvmStatic external fun nativeMergeDown(handle: Long, page: Int, index: Int): Boolean

    /** Layer properties. Not undoable. Each returns false if the layer doesn't exist. */
    @JvmStatic external fun nativeRenameLayer(handle: Long, page: Int, index: Int, name: String): Boolean

    @JvmStatic external fun nativeSetLayerVisible(handle: Long, page: Int, index: Int, visible: Boolean): Boolean

    @JvmStatic external fun nativeSetLayerLocked(handle: Long, page: Int, index: Int, locked: Boolean): Boolean

    /** [opacity] is clamped to 0..1. */
    @JvmStatic external fun nativeSetLayerOpacity(handle: Long, page: Int, index: Int, opacity: Float): Boolean

    /**
     * For each requested id: `[tool, colorHi, colorLo, minX, minY, maxX, maxY, n, <n polygon floats>]`.
     * ARGB = (colorHi.toInt() shl 16) or colorLo.toInt(). Contours are `[count, x0, y0, ...]` repeated, non-overlapping and with holes included;
     * fill them all as one Path (non-zero winding, antialiased). With [quick] the contours of an ink stroke may overlap
     * (same look under non-zero winding, far cheaper to compute for long strokes). Tool -1 means the stroke is gone. A fill (tool 3) has one polygon, its outline.
     */
    @JvmStatic external fun nativeStrokeGeometry(
        handle: Long,
        page: Int,
        ids: LongArray,
        tolerance: Float,
        quick: Boolean,
    ): FloatArray

    /**
     * Embeds a PNG or JPEG and places it on the page's active layer at the rectangle (page points).
     * Returns the image id as [hi, lo], or an empty array if the layer is hidden or locked or the
     * bytes are not a PNG or JPEG.
     */
    @JvmStatic
    external fun nativeInsertImage(
        handle: Long,
        page: Int,
        bytes: ByteArray,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): LongArray

    /** The topmost image under a page point on a visible, unlocked layer, as [hi, lo], or empty. */
    @JvmStatic external fun nativeImageAt(handle: Long, page: Int, x: Float, y: Float): LongArray

    /**
     * The images of a page, per layer, with the same layer list and order as [nativeLayerStrokeIds]. JSON:
     * `[[{"hi":L,"lo":L,"source":"uuid","m":[6 floats],"crop":[4 floats]}, ...], ...]`, one inner array per layer,
     * images in drawing order.
     */
    @JvmStatic external fun nativePageImages(handle: Long, page: Int): String

    /** The encoded bytes (PNG or JPEG) of an embedded image source, or null if unknown. */
    @JvmStatic external fun nativeImageBytes(handle: Long, source: String): ByteArray?

    /** Pixel size of a source as [width, height], or empty if unknown. */
    @JvmStatic external fun nativeImageSize(handle: Long, source: String): IntArray

    /**
     * Changes an image's crop ([left, top, right, bottom] fractions of its source), keeping what is
     * still shown in place. Returns the new id as [hi, lo], or empty if nothing changed.
     */
    @JvmStatic external fun nativeCropImage(handle: Long, page: Int, id: LongArray, crop: FloatArray): LongArray

    /**
     * Places a text box on the page's active layer as one undo step. [frame] is `[x, y, width, height]` in page points,
     * [size] the font size in points, [argb] the colour, [align] 0 start, 1 centre or 2 end, then the [bold], [italic]
     * and [underline] flags. Returns the id as [hi, lo], or an empty array if the frame or size is not valid or the
     * layer is hidden or locked.
     */
    @JvmStatic
    external fun nativeAddText(
        handle: Long,
        page: Int,
        frame: FloatArray,
        text: String,
        size: Float,
        argb: Int,
        align: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
    ): LongArray

    /** The topmost text box under a page point on a visible, unlocked layer, as [hi, lo], or empty. */
    @JvmStatic external fun nativeTextAt(handle: Long, page: Int, x: Float, y: Float): LongArray

    /**
     * The text boxes of a page, per layer, with the same layer list and order as [nativeLayerStrokeIds]. JSON:
     * `[[{"hi":L,"lo":L,"x":F,"y":F,"w":F,"h":F,"rot":F,"text":"s","size":F,"argb":I,"align":I,"bold":B,"italic":B,
     * "underline":B}, ...], ...]`, one inner array per layer, boxes in drawing order. Read it with `TextLayout.parse`.
     */
    @JvmStatic external fun nativePageTexts(handle: Long, page: Int): String

    /**
     * Changes a text box in one undo step, keeping its id. [frame] is `[x, y, width, height, rotation]`, the rest as
     * for [nativeAddText]. Returns true if the box changed.
     */
    @JvmStatic
    external fun nativeUpdateText(
        handle: Long,
        page: Int,
        id: LongArray,
        frame: FloatArray,
        text: String,
        size: Float,
        argb: Int,
        align: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
    ): Boolean

    /** Removes a text box as one undo step. Returns false if it is gone or on a locked layer. */
    @JvmStatic external fun nativeDeleteText(handle: Long, page: Int, id: LongArray): Boolean

    /**
     * Swaps an image for a copy with a transparent background, keeping its place, turn and crop. [argb] is the whole
     * decoded source ([width] by [height], `0xAARRGGBB` not premultiplied, as `Bitmap.getPixels` gives it). Returns
     * the new id as [hi, lo], or empty if the image is gone, locked, or the pixels do not match its source.
     * [strength] is how much counts as background, 0 to 100, where 50 is the usual cut. [outline] is 0 for none, 1
     * for a thin and 2 for a thick white band around what is kept.
     */
    @JvmStatic
    external fun nativeRemoveImageBackground(
        handle: Long,
        page: Int,
        id: LongArray,
        width: Int,
        height: Int,
        argb: IntArray,
        strength: Int,
        outline: Int,
    ): LongArray

    /**
     * Swaps an image for a copy showing [argb] instead, keeping its place, turn and crop, as one undo step. [argb] is
     * as for [nativeRemoveImageBackground] and must be the size of the image's source. Returns the new id as
     * [hi, lo], or empty if the image is gone, locked, or the pixels do not match its source.
     */
    @JvmStatic
    external fun nativeReplaceImagePixels(
        handle: Long,
        page: Int,
        id: LongArray,
        width: Int,
        height: Int,
        argb: IntArray,
    ): LongArray

    /**
     * Swaps an image for pen strokes traced from its drawing, as one undo step. [argb] is as for
     * [nativeRemoveImageBackground] and [startedAtMs] stamps the strokes. Returns the stroke ids flat as
     * [hi, lo, ...], or empty if the image is gone, locked, mismatched, or holds no drawing.
     */
    @JvmStatic
    external fun nativeImageToInk(
        handle: Long,
        page: Int,
        id: LongArray,
        width: Int,
        height: Int,
        argb: IntArray,
        startedAtMs: Long,
    ): LongArray
}

/**
 * JNI bridge to grapho-core's library: folders, items and Trash. A handle is thread-safe (guarded by a mutex in Rust)
 * but must not be used after [nativeFree]. Operations are named calls that take and return JSON; see
 * `grapho_core::library::call` for the names and arguments. A failure throws an [IOException] whose message is the
 * error code (`not_found`, `into_descendant`, ...), a colon and a sentence.
 */
object NativeLibrary {
    init {
        System.loadLibrary("grapho_android")
    }

    /** Makes an empty library and returns its handle. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeNew(): Long

    /** Reads a library from its JSON and returns its handle. Throws if the JSON is damaged or from a newer version. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeFromJson(json: String): Long

    /** The library as JSON, for the shell to write to disk. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeToJson(handle: Long): String

    /** Runs the operation [op] with the JSON object [args] and returns its JSON result. */
    @Throws(IOException::class)
    @JvmStatic
    external fun nativeCall(handle: Long, op: String, args: String): String

    /** Frees the handle. */
    @JvmStatic
    external fun nativeFree(handle: Long)
}

/** Tool codes shared with the file format. */
object Tool {
    const val PEN = 0
    const val PENCIL = 1
    const val HIGHLIGHTER = 2
    const val FILL = 3
}

/** Background pattern codes for [NativeNotebook.nativeCreate] and [NativeNotebook.nativeSetBackground]. */
object PatternKind {
    const val BLANK = 0
    const val LINED = 1
    const val GRID = 2
    const val DOTTED = 3
}
