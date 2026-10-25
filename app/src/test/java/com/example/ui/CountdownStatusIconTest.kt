package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "xhdpi")
class CountdownStatusIconTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun hoursAndMinutesRemainUnambiguousAtMinuteAndHourBoundaries() {
        assertEquals("0:00", CountdownStatusIcon.formatRemaining(-1L))
        assertEquals("0:00", CountdownStatusIcon.formatRemaining(0L))
        assertEquals("0:01", CountdownStatusIcon.formatRemaining(1L))
        assertEquals("0:01", CountdownStatusIcon.formatRemaining(60_000L))
        assertEquals("0:02", CountdownStatusIcon.formatRemaining(60_001L))
        assertEquals("1:00", CountdownStatusIcon.formatRemaining(3_600_000L))
        assertEquals("9:59", CountdownStatusIcon.formatRemaining(35_940_000L))
        assertEquals("10:00", CountdownStatusIcon.formatRemaining(35_940_001L))
        assertEquals("99:59", CountdownStatusIcon.formatRemaining(Long.MAX_VALUE))
    }

    @Test fun nextRefreshCoincidesWithVisibleMinuteChange() {
        assertEquals(1L, CountdownStatusIcon.millisUntilNextMinuteChange(60_001L))
        assertEquals(15_250L, CountdownStatusIcon.millisUntilNextMinuteChange(75_250L))
        assertEquals(60_000L, CountdownStatusIcon.millisUntilNextMinuteChange(60_000L))
        assertEquals(60_000L, CountdownStatusIcon.millisUntilNextMinuteChange(0L))
        assertEquals(60_000L, CountdownStatusIcon.millisUntilNextMinuteChange(-1L))
    }

    @Test fun sameDisplayedMinuteReusesIconAndNewMinuteRefreshesIt() {
        val icon = CountdownStatusIcon.create(context, 75_000L)
        assertSame(icon, CountdownStatusIcon.create(context, 60_001L))
        val nextMinute = CountdownStatusIcon.create(context, 60_000L)
        assertNotSame(icon, nextMinute)
        assertEquals(IconCompat.TYPE_BITMAP, nextMinute.type)
    }

    @Test fun clockIsAWhiteTextMaskThatFillsWidthWithoutAFrame() {
        val bitmap = requireNotNull(CountdownStatusIcon.create(context, 8 * 3_600_000L + 8 * 60_000L).bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(48, bitmap.height)
        var visiblePixels = 0
        var left = bitmap.width
        var right = 0
        var top = bitmap.height
        var bottom = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) > 0) {
                    assertEquals(255, Color.red(pixel))
                    assertEquals(255, Color.green(pixel))
                    assertEquals(255, Color.blue(pixel))
                    visiblePixels++
                    left = minOf(left, x)
                    right = maxOf(right, x)
                    top = minOf(top, y)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        assertTrue("Bold numerals must remain visible", visiblePixels > 250)
        assertTrue("The time must fill the icon width", right - left >= 44)
        assertTrue("Numerals must stay large in a wider proportion", bottom - top >= 35)
        assertTrue("Numerals must be shorter than the previous stretched treatment", bottom - top < 42)
        assertTrue("Small margins must prevent clipping", top >= 1 && bottom < 47)
        assertTrue("Text must be vertically centered", kotlin.math.abs(top + bottom - 47) <= 2)
        for (x in 0 until bitmap.width) {
            assertEquals(0, Color.alpha(bitmap.getPixel(x, 0)))
            assertEquals(0, Color.alpha(bitmap.getPixel(x, bitmap.height - 1)))
        }
    }

    @Test fun singleAndDoubleDigitHoursKeepLargeNumerals() {
        for (millis in listOf(1L, 600_000L, 4_980_000L, 29_280_000L, 36_000_000L)) {
            val bitmap = requireNotNull(CountdownStatusIcon.create(context, millis).bitmap)
            val visibleRows = (0 until bitmap.height).filter { y ->
                (0 until bitmap.width).any { x -> Color.alpha(bitmap.getPixel(x, y)) > 127 }
            }
            assertTrue("${CountdownStatusIcon.formatRemaining(millis)} must retain large numerals", visibleRows.last() - visibleRows.first() >= 35)
        }
    }

    /** Actual 24 dp and common 18 dp SystemUI sizes, with enlarged copies for inspection. */
    @Test fun renderStatusBarPreview() {
        val labels = listOf("0:01", "0:10", "1:23", "8:08", "10:00")
        val remaining = listOf(1L, 600_000L, 4_980_000L, 29_280_000L, 36_000_000L)
        val sheet = Bitmap.createBitmap(620, 290, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(24, 27, 31))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
        }
        remaining.forEachIndexed { index, millis ->
            val x = 24f + index * 120f
            val bitmap = requireNotNull(CountdownStatusIcon.create(context, millis).bitmap)
            canvas.drawText(labels[index], x, 28f, paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 48f, x + 24, 72f), paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 92f, x + 18, 110f), paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 140f, x + 96, 236f), paint)
        }
        val file = File("build/status-icon-preview.png").apply { parentFile.mkdirs() }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    @Test fun renderOldAndLargerClockComparison() {
        val labels = listOf("0:01", "0:10", "1:23", "8:08", "10:00")
        val remaining = listOf(1L, 600_000L, 4_980_000L, 29_280_000L, 36_000_000L)
        val sheet = Bitmap.createBitmap(670, 380, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(24, 27, 31))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
        }
        canvas.drawText("24 dp / önce", 12f, 63f, paint)
        canvas.drawText("24 dp / yeni", 12f, 105f, paint)
        canvas.drawText("18 dp / önce", 12f, 147f, paint)
        canvas.drawText("18 dp / yeni", 12f, 189f, paint)
        canvas.drawText("4× görünüm", 12f, 290f, paint)
        remaining.forEachIndexed { index, millis ->
            val x = 140f + index * 102f
            val bitmap = requireNotNull(CountdownStatusIcon.create(context, millis).bitmap)
            // Reproduce 5.4.0 for a real-size comparison with the bolder clock.
            val old = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
            val oldCanvas = Canvas(old)
            val oldPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textSize = 100f
            }
            val label = labels[index]
            val bounds = Rect()
            oldPaint.getTextBounds(label, 0, label.length, bounds)
            oldCanvas.translate(0.5f, 5.5f)
            oldCanvas.scale(47f / bounds.width(), 37f / bounds.height())
            oldCanvas.drawText(label, -bounds.left.toFloat(), -bounds.top.toFloat(), oldPaint)
            canvas.drawText(label, x, 25f, paint)
            canvas.drawBitmap(old, null, RectF(x, 44f, x + 24, 68f), paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 86f, x + 24, 110f), paint)
            canvas.drawBitmap(old, null, RectF(x, 132f, x + 18, 150f), paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 174f, x + 18, 192f), paint)
            canvas.drawBitmap(bitmap, null, RectF(x, 225f, x + 96, 321f), paint)
        }
        val file = File("build/status-icon-comparison.png").apply { parentFile.mkdirs() }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }
}
