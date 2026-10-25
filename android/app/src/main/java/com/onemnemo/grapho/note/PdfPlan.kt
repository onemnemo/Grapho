package com.onemnemo.grapho.note

import kotlin.math.max
import kotlin.math.min

/** A rectangle in page points. */
class PdfArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(o: PdfArea): Boolean = left <= o.left && top <= o.top && right >= o.right && bottom >= o.bottom
}

/** A bitmap the background thread should have. [scale] is bitmap pixels per page point. */
sealed class PdfWant(val ref: PdfRef, val width: Float, val height: Float, val scale: Float)

/** The whole page at roughly the resolution of a fitted view. */
class BaseWant(ref: PdfRef, width: Float, height: Float, scale: Float) : PdfWant(ref, width, height, scale)

/**
 * The part of a page that is on screen ([visible]) plus some margin around it ([area]), at the scale the view
 * shows it, for when zoomed in beyond what the whole-page bitmap resolves.
 */
class DetailWant(ref: PdfRef, width: Float, height: Float, scale: Float, val visible: PdfArea, val area: PdfArea) :
    PdfWant(ref, width, height, scale)

/**
 * What the PDF thread should render for one viewport: whole pages that are on screen first, then the detail of
 * the zoomed-in ones (only once the view has [settled]), then the pages just off screen.
 */
class PdfPlan(
    val visible: List<BaseWant>,
    val near: List<BaseWant>,
    val details: List<DetailWant>,
    val settled: Boolean,
)

/** Decides which PDF bitmaps a viewport needs. Pure arithmetic, so it can be tested without a device. */
object PdfPlanner {
    /** Longest side of a whole-page bitmap, in pixels. */
    const val MAX_BASE_SIDE = 2048f

    /** A detail is only worth rendering when the view shows the page this much larger than the base bitmap. */
    private const val DETAIL_RATIO = 1.05f

    /** Margin of a detail around the visible part, as a fraction of the view, so a small pan stays sharp. */
    private const val DETAIL_MARGIN = 0.125f

    /** Pixels per point of the whole-page bitmap of a page of this size. */
    fun baseScale(vp: Viewport, width: Float, height: Float): Float {
        val longest = max(width, height)
        val cap = if (longest > 0f) MAX_BASE_SIDE / longest else vp.baseScale
        return min(vp.baseScale, cap)
    }

    /** Null when the notebook has no PDF pages or the view has no size yet. */
    fun plan(vp: Viewport, info: NotebookInfo, settled: Boolean): PdfPlan? {
        if (vp.infinite || !info.hasPdf || vp.viewWidth < 1f || vp.viewHeight < 1f) return null
        val shown = vp.visiblePages()
        if (shown.isEmpty()) return null
        val visible = ArrayList<BaseWant>()
        val near = ArrayList<BaseWant>()
        val details = ArrayList<DetailWant>()
        for (page in max(0, shown.first - 1)..min(min(info.pdfRefs.size, vp.pageCount) - 1, shown.last + 1)) {
            val ref = info.pdfRefs[page] ?: continue
            val w = vp.pages.width(page)
            val h = vp.pages.height(page)
            val base = BaseWant(ref, w, h, baseScale(vp, w, h))
            if (page !in shown) {
                near.add(base)
                continue
            }
            visible.add(base)
            if (settled && vp.scale > base.scale * DETAIL_RATIO) details.add(detail(vp, page, ref, w, h))
        }
        return PdfPlan(visible, near, details, settled)
    }

    private fun detail(vp: Viewport, page: Int, ref: PdfRef, w: Float, h: Float): DetailWant {
        val left = max(0f, vp.localX(page, 0f))
        val top = max(0f, vp.localY(page, 0f))
        val right = min(w, vp.localX(page, vp.viewWidth))
        val bottom = min(h, vp.localY(page, vp.viewHeight))
        val visible = PdfArea(left, top, right, bottom)
        val mx = vp.viewWidth * DETAIL_MARGIN / vp.scale
        val my = vp.viewHeight * DETAIL_MARGIN / vp.scale
        val area = PdfArea(max(0f, left - mx), max(0f, top - my), min(w, right + mx), min(h, bottom + my))
        return DetailWant(ref, w, h, vp.scale, visible, area)
    }
}
