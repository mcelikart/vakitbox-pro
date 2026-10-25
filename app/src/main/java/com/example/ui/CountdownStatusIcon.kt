package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale
import kotlin.math.roundToInt

/** A monochrome H:MM clock drawn at the full size Android permits for a small icon. */
internal object CountdownStatusIcon {
    private const val MINUTE_MILLIS = 60_000L
    private const val SIZE_DP = 24f

    private data class CachedIcon(val label: String, val densityDpi: Int, val icon: IconCompat)
    private var cachedIcon: CachedIcon? = null

    /**
     * SystemUI applies its own foreground tint, so only white and transparency
     * are used. Bold, wider numerals fill the slot without a frame taking
     * space from the time. Android still controls the small-icon
     * slot size; app text-size settings cannot enlarge that slot.
     */
    @Synchronized
    fun create(context: Context, remainingMillis: Long): IconCompat {
        val label = formatRemaining(remainingMillis)
        val metrics = context.resources.displayMetrics
        cachedIcon?.let {
            if (it.label == label && it.densityDpi == metrics.densityDpi) return it.icon
        }
        val size = (SIZE_DP * metrics.density).roundToInt().coerceAtLeast(24)
        val bitmap = render(label, size).apply { density = metrics.densityDpi }
        return IconCompat.createWithBitmap(bitmap).also {
            cachedIcon = CachedIcon(label, metrics.densityDpi, it)
        }
    }

    /** Minutes round up; a positive countdown never prematurely reads 0:00. */
    internal fun formatRemaining(remainingMillis: Long): String {
        val positive = remainingMillis.coerceAtLeast(0L)
        val minutes = positive / MINUTE_MILLIS + if (positive % MINUTE_MILLIS == 0L) 0 else 1
        // A next prayer is normally less than a day away. Keep corrupt or
        // unusually distant targets legible rather than shrinking an arbitrary
        // number of digits into the status bar.
        val displayedMinutes = minutes.coerceAtMost(99 * 60L + 59)
        return String.format(Locale.US, "%d:%02d", displayedMinutes / 60, displayedMinutes % 60)
    }

    /** Wake only when the displayed minute changes, including an hour rollover. */
    fun millisUntilNextMinuteChange(remainingMillis: Long): Long {
        if (remainingMillis <= 0L) return MINUTE_MILLIS
        return (remainingMillis % MINUTE_MILLIS).takeIf { it != 0L } ?: MINUTE_MILLIS
    }

    private fun render(label: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(size / SIZE_DP, size / SIZE_DP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2.4f
            textSize = 100f
        }
        // A compact bold face keeps the counters open at status-bar size.
        // The subtle extra stroke survives SystemUI downsampling without
        // adding a frame or spending any of the available width on padding.
        val bounds = Rect()
        paint.getTextBounds(label, 0, label.length, bounds)
        val width = SIZE_DP - 0.5f
        val height = 18.5f
        val scaleX = width / bounds.width()
        val scaleY = height / bounds.height()
        canvas.translate(
            (SIZE_DP - width) / 2f,
            (SIZE_DP - height) / 2f
        )
        canvas.scale(scaleX, scaleY)
        canvas.drawText(label, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
        return bitmap
    }
}
