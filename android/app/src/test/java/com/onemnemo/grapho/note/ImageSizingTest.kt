package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {
    @Test
    fun aPictureThatFitsIsNotShrunk() {
        assertEquals(1, ImageSizing.sampleSize(4096, 4096))
        assertEquals(1, ImageSizing.sampleSize(800, 2048))
    }

    @Test
    fun aLargePictureIsHalvedUntilItFits() {
        assertEquals(2, ImageSizing.sampleSize(4097, 4096))
        assertEquals(2, ImageSizing.sampleSize(4000, 2048))
        assertEquals(4, ImageSizing.sampleSize(9000, 4096))
    }

    @Test
    fun aWidePictureFillsSixtyPercentOfTheWidth() {
        val p = ImageSizing.place(2, 0f, 0f, 500f, 800f, 2000, 1000)
        assertEquals(300f, p.width, 0.01f)
        assertEquals(150f, p.height, 0.01f)
        assertEquals(2, p.page)
    }

    @Test
    fun aTallPictureFillsSixtyPercentOfTheHeight() {
        val p = ImageSizing.place(0, 0f, 0f, 500f, 800f, 1000, 2000)
        assertEquals(240f, p.width, 0.01f)
        assertEquals(480f, p.height, 0.01f)
    }

    @Test
    fun thePictureIsCentredInTheVisibleArea() {
        val p = ImageSizing.place(0, 100f, 200f, 400f, 400f, 100, 100)
        assertEquals(100f + (400f - p.width) / 2f, p.x, 0.01f)
        assertEquals(200f + (400f - p.height) / 2f, p.y, 0.01f)
    }
}
