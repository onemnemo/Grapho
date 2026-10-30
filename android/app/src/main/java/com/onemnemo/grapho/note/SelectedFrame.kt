package com.onemnemo.grapho.note

import android.graphics.RectF

/** The one selected image's page, id and frame (a box in page-local units), for swapping it for another picture. */
class SelectedFrame(val page: Int, val id: StrokeId, val bounds: RectF)
