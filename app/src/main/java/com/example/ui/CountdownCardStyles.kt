package com.example.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriDate
import com.example.data.Timings
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Persists and coordinates the 6 Countdown Hero Card Styles:
 * Page 0: Original Gökkubbe (Current app design - 100% intact)
 * Page 1: 24-Hour Prayer & Kerahat Timeline Ruler Card (24-Saat Vakit & Kerahat Cetveli - birebir uyarlama)
 * Page 2: Arched Gauge & Needle Dial (Yarım Kadran & İbre)
 * Page 3: Big Digital Segmented Clock (Büyük Dijital Sayaç & Rakam Blokları)
 * Page 4: Celestial Sun Orbit Arc & 6 Day Milestones (Güneş Yörüngesi & 6 Vakit)
 * Page 5: Minimalist Giant Clock + Dual Date Capsule (Minimalist Dev Saat & Çift Tarih Kapsülü)
 */
@Composable
fun CountdownCardPager(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    todayHijri: HijriDate? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val initialPage = remember {
        prefs.getInt("pref_home_countdown_card_page", 0).coerceIn(0, 5)
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 6 })

    // Save page preference when changed
    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putInt("pref_home_countdown_card_page", pagerState.currentPage).apply()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> HeroGokKubbeCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings
                )
                1 -> EzanVaktiTimelineRulerCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings,
                    todayHijri = todayHijri
                )
                2 -> EzanVaktiDialCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings
                )
                3 -> EzanVaktiDigitalClockCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings,
                    todayHijri = todayHijri
                )
                4 -> EzanVaktiSunOrbitCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings
                )
                5 -> EzanVaktiMinimalistPillCard(
                    timingsObj = timingsObj,
                    remainingTime = remainingTime,
                    currentAndNext = currentAndNext,
                    progress = progress,
                    strings = strings,
                    todayHijri = todayHijri
                )
            }
        }

        // Pager Indicator Dots (6 dots)
        CountdownPagerIndicator(
            pagerState = pagerState,
            pageCount = 6,
            onDotClick = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp, bottom = 6.dp)
        )
    }
}

/**
 * 5-dot animated indicator bar
 */
@Composable
fun CountdownPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (isSelected) 22.dp else 7.dp,
                animationSpec = tween(durationMillis = 280),
                label = "dotWidth"
            )
            val dotColor = if (isSelected) {
                themeColors.primary
            } else {
                themeColors.primary.copy(alpha = 0.28f)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onDotClick(index) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

/**
 * Style 1 (Page 0): Original Gökkubbe Hero Card (Current app design - 100% intact)
 */
@Composable
fun HeroGokKubbeCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val sunriseMinutes = parseToMinutes(timingsObj.Sunrise)
    val sunsetMinutes = parseToMinutes(timingsObj.Maghrib)
    val dhuhrMinutes = parseToMinutes(timingsObj.Dhuhr)
    val asrMinutes = parseToMinutes(timingsObj.Asr)
    val imsakMinutes = parseToMinutes(timingsObj.Imsak)
    val ishaMinutes = parseToMinutes(timingsObj.Isha)

    val isDay = nowMinutes in sunriseMinutes..sunsetMinutes
    val totalDayMinutes = (sunsetMinutes - sunriseMinutes).coerceAtLeast(1)
    val totalNightMinutes = (if (sunriseMinutes > sunsetMinutes) (1440 - sunsetMinutes) + sunriseMinutes else sunriseMinutes + (1440 - sunsetMinutes)).coerceAtLeast(1)

    val celProgress = if (isDay) {
        if (totalDayMinutes > 0) ((nowMinutes - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0f, 1f) else 0f
    } else {
        val currentNightMinutes = if (nowMinutes >= sunsetMinutes) nowMinutes - sunsetMinutes else (1440 - sunsetMinutes) + nowMinutes
        if (totalNightMinutes > 0) (currentNightMinutes.toFloat() / totalNightMinutes).coerceIn(0f, 1f) else 0f
    }

    val animatedCelProgress = remember { Animatable(0f) }
    LaunchedEffect(celProgress) {
        animatedCelProgress.animateTo(celProgress, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }

    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDarkArc = LocalThemeIsDark.current

    val standardArcColor = if (isDay) Color(0xFFFFB300) else themeColors.primary
    val keraatRedColor = Color(0xFFE53935)
    val nodeGlowColor = if (isDay) Color(0xFFFFD54F).copy(alpha = 0.45f) else themeColors.glowColor.copy(alpha = 0.45f)
    val nodeCoreColor = if (isDay) Color(0xFFFF8F00) else themeColors.goldMotifColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDarkArc)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val onBgColor = MaterialTheme.colorScheme.onSurface
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    val primaryColor = MaterialTheme.colorScheme.primary

                    val k1End = (45f / totalDayMinutes).coerceIn(0.01f, 0.35f)
                    val k2Start = ((dhuhrMinutes - 45 - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0.05f, 0.95f)
                    val k2End = ((dhuhrMinutes - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0.05f, 0.95f)
                    val k3Start = ((sunsetMinutes - 45 - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0.65f, 0.99f)
                    val k3End = 1.0f

                    data class PrayerMarker(val letter: String, val t: Float, val name: String, val isKeraat: Boolean)

                    val prayerMarkers = if (isDay) {
                        val dhuhrT = ((dhuhrMinutes - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0f, 1f)
                        val asrT = ((asrMinutes - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0f, 1f)
                        listOf(
                            PrayerMarker("G", 0f, "Güneş", true),
                            PrayerMarker("Ö", dhuhrT, "Öğle", false),
                            PrayerMarker("İ", asrT, "İkindi", false),
                            PrayerMarker("A", 1f, "Akşam", true)
                        )
                    } else {
                        val ishaT = (((ishaMinutes - sunsetMinutes + 1440) % 1440).toFloat() / totalNightMinutes).coerceIn(0f, 1f)
                        val imsakT = (((imsakMinutes - sunsetMinutes + 1440) % 1440).toFloat() / totalNightMinutes).coerceIn(0f, 1f)
                        listOf(
                            PrayerMarker("A", 0f, "Akşam", false),
                            PrayerMarker("Y", ishaT, "Yatsı", false),
                            PrayerMarker("İ", imsakT, "İmsak", false),
                            PrayerMarker("G", 1f, "Güneş", false)
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val margin = 36f
                        val radiusX = (w - margin * 2) / 2
                        val radiusY = h - 30f
                        val centerX = w / 2
                        val centerY = h - 6f

                        drawLine(
                            color = onBgColor.copy(alpha = 0.15f),
                            start = Offset(6f, centerY),
                            end = Offset(w - 6f, centerY),
                            strokeWidth = 2f
                        )

                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

                        if (isDay) {
                            data class ArcSegment(val startT: Float, val endT: Float, val isKeraat: Boolean)
                            val segments = listOf(
                                ArcSegment(0f, k1End, true),
                                ArcSegment(k1End, k2Start, false),
                                ArcSegment(k2Start, k2End, true),
                                ArcSegment(k2End, k3Start, false),
                                ArcSegment(k3Start, k3End, true)
                            )

                            for (seg in segments) {
                                if (seg.endT <= seg.startT) continue
                                val segStartAngle = 180f + (seg.startT * 180f)
                                val segSweep = (seg.endT - seg.startT) * 180f
                                val segColor = if (seg.isKeraat) keraatRedColor else standardArcColor
                                val segStrokeWidth = if (seg.isKeraat) 4.5f else 3.5f

                                drawArc(
                                    color = segColor,
                                    startAngle = segStartAngle,
                                    sweepAngle = segSweep,
                                    useCenter = false,
                                    topLeft = Offset(centerX - radiusX, centerY - radiusY),
                                    size = Size(radiusX * 2, radiusY * 2),
                                    style = Stroke(width = segStrokeWidth, pathEffect = pathEffect, cap = StrokeCap.Round)
                                )
                            }
                        } else {
                            drawArc(
                                color = standardArcColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(centerX - radiusX, centerY - radiusY),
                                size = Size(radiusX * 2, radiusY * 2),
                                style = Stroke(width = 3.5f, pathEffect = pathEffect, cap = StrokeCap.Round)
                            )
                        }

                        val textPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            textSize = 24f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        for (marker in prayerMarkers) {
                            val angleRad = Math.PI - (marker.t * Math.PI)
                            val dotX = centerX + radiusX * cos(angleRad).toFloat()
                            val dotY = centerY - radiusY * sin(angleRad).toFloat()

                            val isMarkerInKeraat = isDay && (
                                marker.t <= k1End + 0.005f ||
                                (marker.t >= k2Start - 0.005f && marker.t <= k2End + 0.005f) ||
                                marker.t >= k3Start - 0.005f
                            )
                            val markerColor = if (isMarkerInKeraat) keraatRedColor else standardArcColor

                            drawCircle(
                                color = surfaceColor,
                                radius = 14f,
                                center = Offset(dotX, dotY)
                            )
                            drawCircle(
                                color = markerColor,
                                radius = 13.5f,
                                center = Offset(dotX, dotY),
                                style = Stroke(width = 2.2f)
                            )

                            textPaint.color = markerColor.toArgb()
                            val textCenterY = dotY - ((textPaint.descent() + textPaint.ascent()) / 2)
                            drawContext.canvas.nativeCanvas.drawText(
                                marker.letter,
                                dotX,
                                textCenterY,
                                textPaint
                            )
                        }

                        val celAngleRad = Math.PI - (animatedCelProgress.value * Math.PI)
                        val bodyX = centerX + radiusX * cos(celAngleRad).toFloat()
                        val bodyY = centerY - radiusY * sin(celAngleRad).toFloat()

                        drawCircle(nodeGlowColor, radius = 18f, center = Offset(bodyX, bodyY))
                        drawCircle(nodeCoreColor, radius = 7f, center = Offset(bodyX, bodyY))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 6.dp).align(Alignment.BottomCenter)
                    ) {
                        val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)
                        Text(
                            text = "$nextPrayerLocalized VAKTİNE KALAN SÜRE".uppercase(),
                            fontSize = 12.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val countdownStyle = LocalTextStyle.current.copy(
                                fontSize = 44.sp,
                                lineHeight = 50.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                                fontFeatureSettings = "tnum"
                            )
                            val textMeasurer = rememberTextMeasurer()
                            val naturalWidth = textMeasurer.measure(
                                text = remainingTime,
                                style = countdownStyle,
                                maxLines = 1,
                                softWrap = false
                            ).size.width.coerceAtLeast(1)
                            val availableWidth = with(LocalDensity.current) { maxWidth.toPx() }
                            val fitScale = (availableWidth / naturalWidth).coerceAtMost(1f)
                            Text(
                                text = remainingTime,
                                modifier = Modifier.fillMaxWidth(),
                                style = countdownStyle.copy(
                                    fontSize = countdownStyle.fontSize * fitScale,
                                    lineHeight = countdownStyle.lineHeight * fitScale
                                ),
                                color = primaryColor,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDay) "Doğuş ${timingsObj.Sunrise}" else "Batış ${timingsObj.Maghrib}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = standardArcColor
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .height(5.dp)
                            .clip(CircleShape),
                        color = standardArcColor,
                        trackColor = standardArcColor.copy(alpha = 0.18f)
                    )
                    Text(
                        text = if (isDay) "Batış ${timingsObj.Maghrib}" else "Doğuş ${timingsObj.Imsak}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = standardArcColor
                    )
                }
            }
        }
    }
}

/**
 * Style 2 (Page 1): 24-Hour Prayer & Kerahat Timeline Ruler Card (EVP 24-Saat Vakit & Kerahat Cetveli)
 * Üstte 7 İslami Ara Vakit (İşrak, Duha, Evvabin, Gece 1/3, Gece Yarısı, Gece 2/3, Seher),
 * Ortada 24 Saatlik Zaman Cetveli, Kerahat ve Namaz Blokları, Kırmızı Canlı Zaman İbresi ve Aktif Dönem Rozeti,
 * Altta Miladi ve Hicri Çift Tarih Gösterimi.
 */
@Composable
fun EzanVaktiTimelineRulerCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings,
    todayHijri: HijriDate? = null
) {
    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current

    val sunriseMinutes = parseToMinutes(timingsObj.Sunrise)
    val dhuhrMinutes = parseToMinutes(timingsObj.Dhuhr)
    val asrMinutes = parseToMinutes(timingsObj.Asr)
    val maghribMinutes = parseToMinutes(timingsObj.Maghrib)
    val ishaMinutes = parseToMinutes(timingsObj.Isha)
    val imsakMinutes = parseToMinutes(timingsObj.Imsak)

    val nightDuration = if (imsakMinutes >= maghribMinutes) {
        imsakMinutes - maghribMinutes
    } else {
        (1440 - maghribMinutes) + imsakMinutes
    }.coerceAtLeast(1)

    // 7 Milestones matching Islamic astronomy & EVP
    val israkMinutes = sunriseMinutes + 45
    val duhaMinutes = sunriseMinutes + 65
    val evvabinMinutes = maghribMinutes
    val gece1_3Minutes = (maghribMinutes + (nightDuration / 3)) % 1440
    val geceYarisiMinutes = (maghribMinutes + (nightDuration / 2)) % 1440
    val gece2_3Minutes = (maghribMinutes + (nightDuration * 2 / 3)) % 1440
    val seherMinutes = (imsakMinutes - (nightDuration / 6) + 1440) % 1440

    val milestones = remember(timingsObj) {
        listOf(
            "İşrak" to formatMinutesToTime(israkMinutes),
            "Duha" to formatMinutesToTime(duhaMinutes),
            "Evvabin" to formatMinutesToTime(evvabinMinutes),
            "Gece 1/3" to formatMinutesToTime(gece1_3Minutes),
            "Gece Yar..." to formatMinutesToTime(geceYarisiMinutes),
            "Gece 2/3" to formatMinutesToTime(gece2_3Minutes),
            "Seher" to formatMinutesToTime(seherMinutes)
        )
    }

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val nowSeconds = now.get(Calendar.SECOND)
    val exactNowMinutes = nowMinutes + (nowSeconds / 60f)

    // Current period classification
    val (activePeriodName, isKerahatActive) = when {
        nowMinutes in sunriseMinutes until israkMinutes -> "KERAHAT VAKTİ" to true
        nowMinutes in israkMinutes until (dhuhrMinutes - 45) -> "DUHA" to false
        nowMinutes in (dhuhrMinutes - 45) until dhuhrMinutes -> "KERAHAT VAKTİ" to true
        nowMinutes in dhuhrMinutes until asrMinutes -> "ÖĞLE" to false
        nowMinutes in asrMinutes until (maghribMinutes - 45) -> "İKİNDİ" to false
        nowMinutes in (maghribMinutes - 45) until maghribMinutes -> "KERAHAT VAKTİ" to true
        nowMinutes in maghribMinutes until ishaMinutes -> "AKŞAM" to false
        nowMinutes in seherMinutes until imsakMinutes -> "SEHER" to false
        nowMinutes in imsakMinutes until sunriseMinutes -> "SABAH" to false
        else -> "YATSI" to false
    }

    val hijriDateStr = remember(todayHijri) {
        if (todayHijri != null && !todayHijri.day.isNullOrEmpty()) {
            val monthNames = listOf(
                "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
                "Cemazi-el Evvel", "Cemazi-el Ahir", "Recep", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            )
            val monthNum = todayHijri.month?.number ?: 0
            val mName = if (monthNum in 1..12) {
                monthNames[monthNum - 1]
            } else if (todayHijri.month?.en != null) {
                getTurkishHijriMonth(todayHijri.month.en)
            } else {
                ""
            }
            "${todayHijri.day} $mName ${todayHijri.year}"
        } else {
            getFallbackHijriDate()
        }
    }

    // Fraction mapping for 24h timeline window starting at 05:00 (300m)
    fun timeToFraction(min: Float): Float {
        val norm = if (min >= 300f) min - 300f else min + 1440f - 300f
        return (norm / 1440f).coerceIn(0f, 1f)
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDark)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Row: 7 Milestones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                milestones.forEach { (name, time) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White.copy(alpha = 0.70f) else onSurfaceVariantColor.copy(alpha = 0.80f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = time,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else onSurfaceColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Center: 24-Hour Timeline Ruler & Blocks Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            ) {
                val width = size.width
                val rulerH = 18.dp.toPx()
                val blocksH = 62.dp.toPx()
                val blocksTop = rulerH
                val blocksBottom = rulerH + blocksH
                val bottomTrackH = 20.dp.toPx()
                val totalContainerH = blocksBottom + bottomTrackH

                // Outer timeline box background (covering ruler, blocks, and bottom tabs)
                drawRoundRect(
                    color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.22f else 0.12f),
                    size = Size(width, totalContainerH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Outer box border
                drawRoundRect(
                    color = themeColors.primary.copy(alpha = 0.20f),
                    size = Size(width, totalContainerH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Ruler background
                drawRoundRect(
                    color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.22f),
                    size = Size(width, rulerH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Hour labels & tick marks on ruler
                val majorHours = listOf(6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 2, 4)
                val rulerTextColor = if (isDark) Color.White.copy(alpha = 0.85f) else onSurfaceColor.copy(alpha = 0.75f)
                val rulerTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = rulerTextColor.toArgb()
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (h in 0..24) {
                    val hourMin = ((h % 24) * 60).toFloat()
                    val x = timeToFraction(hourMin) * width
                    val displayHour = if (h == 0) 24 else h
                    val isMajor = majorHours.contains(displayHour)

                    val tickH = if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()
                    val tickAlpha = if (isMajor) 0.45f else 0.22f

                    drawLine(
                        color = themeColors.primary.copy(alpha = tickAlpha),
                        start = Offset(x, rulerH - tickH),
                        end = Offset(x, rulerH),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (isMajor) {
                        val label = String.format(Locale.ROOT, "%02d", displayHour)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            rulerH - tickH - 2.dp.toPx(),
                            rulerTextPaint
                        )
                    }

                    // Half-hour tick
                    val halfX = timeToFraction(hourMin + 30f) * width
                    drawLine(
                        color = themeColors.primary.copy(alpha = 0.18f),
                        start = Offset(halfX, rulerH - 2.5.dp.toPx()),
                        end = Offset(halfX, rulerH),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Horizontal line separating ruler from blocks
                drawLine(
                    color = themeColors.primary.copy(alpha = 0.25f),
                    start = Offset(0f, rulerH),
                    end = Offset(width, rulerH),
                    strokeWidth = 1.dp.toPx()
                )

                // Daytime and prayer blocks with Kerahat-Duha intermediate box
                data class BlockSegment(
                    val name: String,
                    val startMin: Float,
                    val endMin: Float,
                    val isKerahat: Boolean
                )

                val daytimeBlocks = listOf(
                    BlockSegment("", 300f, imsakMinutes.toFloat(), false),
                    BlockSegment("Sabah", imsakMinutes.toFloat(), sunriseMinutes.toFloat(), false),
                    BlockSegment("Kerahat Vakti", sunriseMinutes.toFloat(), israkMinutes.toFloat(), true),
                    // Distinct box between Kerahat (07:47) and Duha (08:07)
                    BlockSegment("", israkMinutes.toFloat(), duhaMinutes.toFloat(), false),
                    BlockSegment("Duha", duhaMinutes.toFloat(), (dhuhrMinutes - 45).toFloat(), false),
                    BlockSegment("Kerahat Vakti", (dhuhrMinutes - 45).toFloat(), dhuhrMinutes.toFloat(), true),
                    BlockSegment("Öğle", dhuhrMinutes.toFloat(), asrMinutes.toFloat(), false),
                    BlockSegment("İkindi", asrMinutes.toFloat(), (maghribMinutes - 45).toFloat(), false),
                    BlockSegment("Kerahat Vakti", (maghribMinutes - 45).toFloat(), maghribMinutes.toFloat(), true),
                    BlockSegment("Akşam", maghribMinutes.toFloat(), ishaMinutes.toFloat(), false)
                )

                val onBlockTextColor = if (isDark) Color.White.copy(alpha = 0.90f) else onSurfaceColor.copy(alpha = 0.85f)
                val vertTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = onBlockTextColor.toArgb()
                    textSize = 8.5.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val kerahatTextColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                val kerahatTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = kerahatTextColor.toArgb()
                    textSize = 7.5.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (b in daytimeBlocks) {
                    val xStart = timeToFraction(b.startMin) * width
                    val xEnd = timeToFraction(b.endMin) * width
                    val blockW = xEnd - xStart
                    if (blockW <= 0f) continue

                    val bgCol = if (b.isKerahat) {
                        Color(0xFFE53935).copy(alpha = if (isDark) 0.28f else 0.18f)
                    } else if (b.name.isEmpty()) {
                        // Intermediate box between Kerahat & Duha
                        themeColors.primaryContainer.copy(alpha = if (isDark) 0.10f else 0.05f)
                    } else {
                        themeColors.primary.copy(alpha = if (isDark) 0.16f else 0.08f)
                    }

                    drawRect(
                        color = bgCol,
                        topLeft = Offset(xStart, blocksTop),
                        size = Size(blockW, blocksH)
                    )

                    // Vertical divider
                    drawLine(
                        color = themeColors.primary.copy(alpha = 0.20f),
                        start = Offset(xEnd, blocksTop),
                        end = Offset(xEnd, blocksBottom),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Rotated vertical text
                    if (b.name.isNotEmpty() && blockW > 9.dp.toPx()) {
                        val centerX = xStart + blockW / 2f
                        val centerY = blocksTop + blocksH / 2f
                        val p = if (b.isKerahat) kerahatTextPaint else vertTextPaint

                        drawContext.canvas.nativeCanvas.save()
                        drawContext.canvas.nativeCanvas.rotate(-90f, centerX, centerY)
                        drawContext.canvas.nativeCanvas.drawText(
                            b.name,
                            centerX,
                            centerY - ((p.descent() + p.ascent()) / 2f),
                            p
                        )
                        drawContext.canvas.nativeCanvas.restore()
                    }
                }

                // Night Zone: Gece & Yatsı
                val nightStartX = timeToFraction(ishaMinutes.toFloat()) * width
                val nightWidth = width - nightStartX
                val halfBlocksH = blocksH / 2f

                if (nightWidth > 0f) {
                    // Upper half: Gece
                    drawRect(
                        color = themeColors.onSurface.copy(alpha = if (isDark) 0.14f else 0.08f),
                        topLeft = Offset(nightStartX, blocksTop),
                        size = Size(nightWidth, halfBlocksH)
                    )

                    val gecePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = onBlockTextColor.toArgb()
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "Gece",
                        nightStartX + nightWidth / 2f,
                        blocksTop + 13.dp.toPx(),
                        gecePaint
                    )

                    // Gece 1/3, 1/2, 2/3 ticks
                    val subGecePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = rulerTextColor.toArgb()
                        textSize = 8.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val gTicks = listOf(
                        "1/3" to timeToFraction(gece1_3Minutes.toFloat()) * width,
                        "1/2" to timeToFraction(geceYarisiMinutes.toFloat()) * width,
                        "2/3" to timeToFraction(gece2_3Minutes.toFloat()) * width
                    )

                    for ((gLabel, gx) in gTicks) {
                        if (gx in nightStartX..width) {
                            drawLine(
                                color = themeColors.primary.copy(alpha = 0.25f),
                                start = Offset(gx, blocksTop + halfBlocksH - 7.dp.toPx()),
                                end = Offset(gx, blocksTop + halfBlocksH),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                gLabel,
                                gx,
                                blocksTop + halfBlocksH - 9.dp.toPx(),
                                subGecePaint
                            )
                        }
                    }

                    // Horizontal divider between Gece and Yatsı
                    drawLine(
                        color = themeColors.primary.copy(alpha = 0.20f),
                        start = Offset(nightStartX, blocksTop + halfBlocksH),
                        end = Offset(width, blocksTop + halfBlocksH),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Lower half: Yatsı
                    drawRect(
                        color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.18f else 0.10f),
                        topLeft = Offset(nightStartX, blocksTop + halfBlocksH),
                        size = Size(nightWidth, halfBlocksH)
                    )

                    val yatsiPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = onBlockTextColor.toArgb()
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val yCenter = blocksTop + halfBlocksH + halfBlocksH / 2f - ((yatsiPaint.descent() + yatsiPaint.ascent()) / 2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "Yatsı",
                        nightStartX + nightWidth / 2f,
                        yCenter,
                        yatsiPaint
                    )
                }

                // Bottom baseline below blocks
                drawLine(
                    color = themeColors.primary.copy(alpha = 0.28f),
                    start = Offset(0f, blocksBottom),
                    end = Offset(width, blocksBottom),
                    strokeWidth = 1.dp.toPx()
                )

                // Bottom ticks along the lower ruler edge
                for (h in 0..24) {
                    val hourMin = ((h % 24) * 60).toFloat()
                    val x = timeToFraction(hourMin) * width
                    drawLine(
                        color = themeColors.primary.copy(alpha = 0.22f),
                        start = Offset(x, totalContainerH - 4.dp.toPx()),
                        end = Offset(x, totalContainerH - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Bottom milestone box tabs: [ İşrak ] and [ Evvabin ]
                val tabTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = (if (isDark) Color.White.copy(alpha = 0.90f) else onSurfaceColor).toArgb()
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }

                val tabBoxH = 15.dp.toPx()
                val tabBoxTop = blocksBottom + 2.dp.toPx()
                val tabBgColor = themeColors.primaryContainer.copy(alpha = if (isDark) 0.75f else 0.45f)
                val tabBorderColor = themeColors.primary.copy(alpha = 0.35f)

                // 1. [ İşrak ] Box Tab
                // Anchored at israkMinutes (when morning Kerahat ends and İşrak period begins)
                val israkStartX = timeToFraction(israkMinutes.toFloat()) * width
                val israkTextW = tabTextPaint.measureText("İşrak")
                val israkBoxW = maxOf(48.dp.toPx(), israkTextW + 12.dp.toPx())
                val israkBoxLeft = israkStartX.coerceIn(2.dp.toPx(), width - israkBoxW - 2.dp.toPx())

                drawRoundRect(
                    color = tabBgColor,
                    topLeft = Offset(israkBoxLeft, tabBoxTop),
                    size = Size(israkBoxW, tabBoxH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
                drawRoundRect(
                    color = tabBorderColor,
                    topLeft = Offset(israkBoxLeft, tabBoxTop),
                    size = Size(israkBoxW, tabBoxH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                val israkTextY = tabBoxTop + tabBoxH / 2f - ((tabTextPaint.descent() + tabTextPaint.ascent()) / 2f)
                drawContext.canvas.nativeCanvas.drawText("İşrak", israkBoxLeft + israkBoxW / 2f, israkTextY, tabTextPaint)

                // 2. [ Evvabin ] Box Tab
                // Anchored at maghribMinutes (when Maghrib begins and Evvabin period begins)
                val evvabinStartX = timeToFraction(evvabinMinutes.toFloat()) * width
                val evvabinTextW = tabTextPaint.measureText("Evvabin")
                val evvabinBoxW = maxOf(54.dp.toPx(), evvabinTextW + 12.dp.toPx())
                val evvabinBoxLeft = evvabinStartX.coerceIn(2.dp.toPx(), width - evvabinBoxW - 2.dp.toPx())

                drawRoundRect(
                    color = tabBgColor,
                    topLeft = Offset(evvabinBoxLeft, tabBoxTop),
                    size = Size(evvabinBoxW, tabBoxH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
                drawRoundRect(
                    color = tabBorderColor,
                    topLeft = Offset(evvabinBoxLeft, tabBoxTop),
                    size = Size(evvabinBoxW, tabBoxH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                val evvabinTextY = tabBoxTop + tabBoxH / 2f - ((tabTextPaint.descent() + tabTextPaint.ascent()) / 2f)
                drawContext.canvas.nativeCanvas.drawText("Evvabin", evvabinBoxLeft + evvabinBoxW / 2f, evvabinTextY, tabTextPaint)

                // Current time vertical cursor line & active badge
                val curX = (timeToFraction(exactNowMinutes) * width).coerceIn(0f, width)

                val needleColor = if (isKerahatActive) Color(0xFFE53935) else themeColors.primary

                // Needle cursor
                drawLine(
                    color = needleColor,
                    start = Offset(curX, rulerH),
                    end = Offset(curX, blocksBottom),
                    strokeWidth = 2.dp.toPx()
                )

                // Active banner badge at bottom of cursor
                val badgeTextColor = if (isKerahatActive) Color.White else onPrimaryColor
                val badgePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = badgeTextColor.toArgb()
                    textSize = 9.5.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }

                val textW = badgePaint.measureText(activePeriodName)
                val badgeW = (textW + 16.dp.toPx()).coerceAtLeast(46.dp.toPx())
                val badgeH = 17.5.dp.toPx()
                val badgeLeft = (curX - badgeW / 2f).coerceIn(4.dp.toPx(), width - badgeW - 4.dp.toPx())
                val badgeTop = blocksBottom + 0.5.dp.toPx()

                val badgeBgColor = if (isKerahatActive) Color(0xFFD32F2F) else primaryColor

                drawRoundRect(
                    color = badgeBgColor,
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeW, badgeH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                val badgeTextY = badgeTop + badgeH / 2f - ((badgePaint.descent() + badgePaint.ascent()) / 2f)
                drawContext.canvas.nativeCanvas.drawText(
                    activePeriodName,
                    badgeLeft + badgeW / 2f,
                    badgeTextY,
                    badgePaint
                )
            }

            // 3. Bottom Row: Turkish Hijri Date (Right-aligned, Gregorian date removed per user request)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.22f),
                    border = BorderStroke(0.8.dp, themeColors.primary.copy(alpha = 0.28f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = themeColors.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hijriDateStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Style 3 (Page 2): Arched Gauge & Needle Dial (EVP Orta Pager Item 0 Adaptasyonu)
 * 180° Yarım Kadran, İbre, Kademeli Çentikler, Kalan Süre ve Vakit Durumu
 */
@Composable
fun EzanVaktiDialCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)
    val currentPrayerLocalized = getLocalizedPrayerName(currentAndNext?.first, strings)

    val animatedProgress = remember { Animatable(progress) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing))
    }

    val parts = remainingTime.split(":")
    val hours = parts.getOrNull(0) ?: "00"
    val minutes = parts.getOrNull(1) ?: "00"
    val seconds = parts.getOrNull(2) ?: "00"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDark)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
        ) {
            Text(
                text = "VAKTİN ÇIKMASINA".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Gauge Canvas with rotating needle & graduation ticks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerX = w / 2f
                    val centerY = h - 10f
                    val radius = ((w - 56f) / 2f).coerceAtMost(h - 18f)

                    // Baseline horizon chord
                    drawLine(
                        color = primaryColor.copy(alpha = 0.16f),
                        start = Offset(centerX - radius - 10f, centerY),
                        end = Offset(centerX + radius + 10f, centerY),
                        strokeWidth = 1.5f
                    )

                    // Background Track Arc (180 to 360 deg)
                    drawArc(
                        color = primaryColor.copy(alpha = 0.14f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Graduation Ticks (every 7.5 degrees)
                    for (i in 0..24) {
                        val tickAngleDeg = 180f + (i * 7.5f)
                        val tickRad = Math.toRadians(tickAngleDeg.toDouble())
                        val isMajor = i % 4 == 0
                        val tickLen = if (isMajor) 9.dp.toPx() else 4.5.dp.toPx()
                        val tickWidth = if (isMajor) 2.2f else 1.2f
                        val tickColor = if (isMajor) primaryColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.35f)

                        val startX = centerX + (radius + 4.dp.toPx()) * cos(tickRad).toFloat()
                        val startY = centerY + (radius + 4.dp.toPx()) * sin(tickRad).toFloat()
                        val endX = centerX + (radius + 4.dp.toPx() + tickLen) * cos(tickRad).toFloat()
                        val endY = centerY + (radius + 4.dp.toPx() + tickLen) * sin(tickRad).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = tickWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Active Progress Arc
                    val sweepProgressAngle = (animatedProgress.value * 180f).coerceIn(0f, 180f)
                    if (sweepProgressAngle > 0.5f) {
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.7f),
                                    themeColors.goldMotifColor,
                                    primaryColor
                                )
                            ),
                            startAngle = 180f,
                            sweepAngle = sweepProgressAngle,
                            useCenter = false,
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Rotating Needle (İbre)
                    val needleAngleDeg = 180f + sweepProgressAngle
                    val needleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val needleLen = radius - 14.dp.toPx()
                    val tipX = centerX + needleLen * cos(needleRad).toFloat()
                    val tipY = centerY + needleLen * sin(needleRad).toFloat()

                    // Needle path (tapered pointer)
                    val perpRad = needleRad + (Math.PI / 2.0)
                    val baseHalfWidth = 4.5.dp.toPx()
                    val p1X = centerX + baseHalfWidth * cos(perpRad).toFloat()
                    val p1Y = centerY + baseHalfWidth * sin(perpRad).toFloat()
                    val p2X = centerX - baseHalfWidth * cos(perpRad).toFloat()
                    val p2Y = centerY - baseHalfWidth * sin(perpRad).toFloat()

                    val needlePath = Path().apply {
                        moveTo(p1X, p1Y)
                        lineTo(tipX, tipY)
                        lineTo(p2X, p2Y)
                        close()
                    }
                    drawPath(
                        path = needlePath,
                        brush = Brush.linearGradient(
                            listOf(themeColors.goldMotifColor, primaryColor),
                            start = Offset(centerX, centerY),
                            end = Offset(tipX, tipY)
                        )
                    )

                    // Center Hub Pivot Cap
                    drawCircle(
                        color = themeColors.primary,
                        radius = 8.5.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = themeColors.goldMotifColor,
                        radius = 4.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                }

                // Centered Digital Countdown inside the Dial with frosted container
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.82f))
                        .border(
                            BorderStroke(1.dp, primaryColor.copy(alpha = 0.16f)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$hours:$minutes",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 0.5.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = ":$seconds",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 2.dp),
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                    Text(
                        text = "$nextPrayerLocalized Vaktine",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Status & Prayer Interval Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.primaryContainer.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .border(
                        BorderStroke(1.dp, primaryColor.copy(alpha = 0.18f)),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Current Prayer
                Text(
                    text = currentPrayerLocalized.ifEmpty { "Giriş" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Center: Dynamic Interval / Kerahat badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% Tamamlandı",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Right: Target Prayer
                Text(
                    text = nextPrayerLocalized.ifEmpty { "Sonraki" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

/**
 * Style 3 (Page 2): Big Digital Segmented Clock (EVP Orta Pager Item 1 Adaptasyonu)
 * Büyük Rakam Blokları (HH : MM : SS), İlerleme Çubuğu ve Çift Tarih Kapsülü
 */
@Composable
fun EzanVaktiDigitalClockCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings,
    todayHijri: HijriDate? = null
) {
    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    val parts = remainingTime.split(":")
    val hours = parts.getOrNull(0) ?: "00"
    val minutes = parts.getOrNull(1) ?: "00"
    val seconds = parts.getOrNull(2) ?: "00"

    val gregorianDateStr = remember {
        SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR")).format(Date())
    }
    val hijriDateStr = remember(todayHijri) {
        if (todayHijri != null && !todayHijri.day.isNullOrEmpty()) {
            val monthNames = listOf(
                "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
                "Cemazi-el Evvel", "Cemazi-el Ahir", "Recep", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            )
            val monthNum = todayHijri.month?.number ?: 0
            val mName = if (monthNum in 1..12) {
                monthNames[monthNum - 1]
            } else if (todayHijri.month?.en != null) {
                getTurkishHijriMonth(todayHijri.month.en)
            } else {
                ""
            }
            "${todayHijri.day} $mName ${todayHijri.year}"
        } else {
            getFallbackHijriDate()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDark)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            Text(
                text = "$nextPrayerLocalized VAKTİNE KALAN SÜRE".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.3.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Large Digital Clock Digit Blocks
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                // Hour Card
                DigitalBlockBox(digits = hours, primaryColor = primaryColor, isDark = isDark)

                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )

                // Minute Card
                DigitalBlockBox(digits = minutes, primaryColor = primaryColor, isDark = isDark)

                Text(
                    text = ":",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor.copy(alpha = 0.65f),
                    modifier = Modifier.padding(start = 3.dp, end = 3.dp, bottom = 6.dp)
                )

                // Second Card (subscripted / smaller)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.30f else 0.18f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = seconds,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor.copy(alpha = 0.95f),
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Track
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(5.dp)
                    .clip(CircleShape),
                color = primaryColor,
                trackColor = primaryColor.copy(alpha = 0.16f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dual Date Capsule Badge (gunLayout)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = themeColors.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = gregorianDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .background(primaryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hijriDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalBlockBox(
    digits: String,
    primaryColor: Color,
    isDark: Boolean
) {
    val themeColors = LocalThemeColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = themeColors.primaryContainer.copy(alpha = if (isDark) 0.38f else 0.22f),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.28f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = digits,
            fontSize = 46.sp,
            fontWeight = FontWeight.Black,
            color = primaryColor,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Style 4 (Page 3): Celestial Sun Orbit Arc & 6 Day Milestones (EVP Orta Pager Item 2 Adaptasyonu)
 * 6 Gün Vakti (İşrak, Duha, Evvabin, Gece, Geceyarısı, Seher) ve Güneş Ufuk Yörünge Grafiği
 */
@Composable
fun EzanVaktiSunOrbitCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBgColor = MaterialTheme.colorScheme.onSurface
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    val sunriseMinutes = parseToMinutes(timingsObj.Sunrise)
    val dhuhrMinutes = parseToMinutes(timingsObj.Dhuhr)
    val maghribMinutes = parseToMinutes(timingsObj.Maghrib)
    val imsakMinutes = parseToMinutes(timingsObj.Imsak)

    val nightDuration = if (imsakMinutes >= maghribMinutes) {
        imsakMinutes - maghribMinutes
    } else {
        (1440 - maghribMinutes) + imsakMinutes
    }.coerceAtLeast(1)

    // Calculate 6 Milestones from prayer astronomy
    val israkTime = formatMinutesToTime(sunriseMinutes + 45)
    val duhaTime = formatMinutesToTime(sunriseMinutes + ((dhuhrMinutes - sunriseMinutes) / 2))
    val evvabinTime = formatMinutesToTime(maghribMinutes + 20)
    val geceTime = formatMinutesToTime((maghribMinutes + (nightDuration * 2 / 3)) % 1440)
    val geceyarisiTime = formatMinutesToTime((maghribMinutes + (nightDuration / 2)) % 1440)
    val seherTime = formatMinutesToTime((imsakMinutes - 45 + 1440) % 1440)

    val milestones = remember(timingsObj) {
        listOf(
            "İşrak" to israkTime,
            "Duha" to duhaTime,
            "Evvabin" to evvabinTime,
            "Gece" to geceTime,
            "Geceyarısı" to geceyarisiTime,
            "Seher" to seherTime
        )
    }

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val totalDayMinutes = (maghribMinutes - sunriseMinutes).coerceAtLeast(1)
    val isDay = nowMinutes in sunriseMinutes..maghribMinutes

    val orbitProgress = if (isDay) {
        ((nowMinutes - sunriseMinutes).toFloat() / totalDayMinutes).coerceIn(0f, 1f)
    } else {
        val curNight = if (nowMinutes >= maghribMinutes) nowMinutes - maghribMinutes else (1440 - maghribMinutes) + nowMinutes
        ((curNight.toFloat() / nightDuration)).coerceIn(0f, 1f)
    }

    val animatedOrbit = remember { Animatable(0f) }
    LaunchedEffect(orbitProgress) {
        animatedOrbit.animateTo(orbitProgress, tween(900, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDark)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            // Top Row: 6 Islamic Milestones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(themeColors.primaryContainer.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                milestones.forEach { (name, time) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = time,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = onBgColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Parabolic Sun Orbit Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val margin = 28f
                    val horizonY = h - 16f
                    val arcHeight = h - 28f

                    // Dashed Horizon Line
                    drawLine(
                        color = primaryColor.copy(alpha = 0.22f),
                        start = Offset(6f, horizonY),
                        end = Offset(w - 6f, horizonY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // Sun Orbit Parabolic Track
                    val orbitPath = Path().apply {
                        moveTo(margin, horizonY)
                        quadraticTo(
                            w / 2f,
                            horizonY - (arcHeight * 1.8f),
                            w - margin,
                            horizonY
                        )
                    }
                    drawPath(
                        path = orbitPath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFB300).copy(alpha = 0.35f),
                                Color(0xFFFFD54F),
                                Color(0xFFFF8F00).copy(alpha = 0.35f)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Moving Celestial Sun Orb
                    val t = animatedOrbit.value.coerceIn(0f, 1f)
                    // Quadratic Bezier Formula: B(t) = (1-t)^2 * P0 + 2*(1-t)*t * P1 + t^2 * P2
                    val p0x = margin
                    val p0y = horizonY
                    val p1x = w / 2f
                    val p1y = horizonY - (arcHeight * 1.8f)
                    val p2x = w - margin
                    val p2y = horizonY

                    val oneMinusT = 1f - t
                    val sunX = (oneMinusT * oneMinusT * p0x) + (2f * oneMinusT * t * p1x) + (t * t * p2x)
                    val sunY = (oneMinusT * oneMinusT * p0y) + (2f * oneMinusT * t * p1y) + (t * t * p2y)

                    // Glowing outer ring
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.40f),
                        radius = 16.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    // Sun core
                    drawCircle(
                        color = if (isDay) Color(0xFFFF8F00) else themeColors.goldMotifColor,
                        radius = 7.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                }

                // Centered Countdown Overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "$nextPrayerLocalized Vaktine",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = remainingTime,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

/**
 * Style 5 (Page 4): Minimalist Giant Clock + Dual Date Pill Capsule (EVP Orta Pager Item 3 Adaptasyonu)
 * Minimalist Dev Saat (HH:MM), Saniye Göstergesi ve Çift Takvim Kapsülü
 */
@Composable
fun EzanVaktiMinimalistPillCard(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings,
    todayHijri: HijriDate? = null
) {
    val arcTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    val parts = remainingTime.split(":")
    val hours = parts.getOrNull(0) ?: "00"
    val minutes = parts.getOrNull(1) ?: "00"
    val seconds = parts.getOrNull(2) ?: "00"

    val gregorianDateStr = remember {
        SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")).format(Date())
    }
    val hijriDateStr = remember(todayHijri) {
        if (todayHijri != null && !todayHijri.day.isNullOrEmpty()) {
            val monthNames = listOf(
                "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
                "Cemazi-el Evvel", "Cemazi-el Ahir", "Recep", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            )
            val monthNum = todayHijri.month?.number ?: 0
            val mName = if (monthNum in 1..12) {
                monthNames[monthNum - 1]
            } else if (todayHijri.month?.en != null) {
                getTurkishHijriMonth(todayHijri.month.en)
            } else {
                ""
            }
            "${todayHijri.day} $mName ${todayHijri.year}"
        } else {
            getFallbackHijriDate()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphicCard(theme = arcTheme, isDark = isDark)
            .frostedGlassBackdrop(shape = arcTheme.cardShape),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = arcTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "$nextPrayerLocalized VAKTİNE KALAN".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Giant Minimalist Clock HH:MM with Auto-Fit
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val giantStyle = LocalTextStyle.current.copy(
                    fontSize = 72.sp,
                    lineHeight = 78.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    fontFeatureSettings = "tnum"
                )
                val textMeasurer = rememberTextMeasurer()
                val textToMeasure = "$hours:$minutes"
                val measuredWidth = textMeasurer.measure(
                    text = textToMeasure,
                    style = giantStyle,
                    maxLines = 1,
                    softWrap = false
                ).size.width.coerceAtLeast(1)
                val availablePx = with(LocalDensity.current) { maxWidth.toPx() }
                val fitFactor = (availablePx / measuredWidth).coerceAtMost(1f)

                Text(
                    text = textToMeasure,
                    modifier = Modifier.fillMaxWidth(),
                    style = giantStyle.copy(
                        fontSize = giantStyle.fontSize * fitFactor,
                        lineHeight = giantStyle.lineHeight * fitFactor
                    ),
                    color = primaryColor,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            // Seconds Pill Ticker
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f)),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = ":$seconds SANİYE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Dual Calendar Capsule Pill (gunLayout)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = themeColors.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.25f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = gregorianDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .background(primaryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hijriDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

/**
 * Helper to compute a localized fallback Hijri date on API 26+ devices
 */
fun getFallbackHijriDate(): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        try {
            val hijrahDate = java.time.chrono.HijrahDate.now()
            val day = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
            val monthNames = listOf(
                "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
                "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            )
            val monthName = monthNames.getOrElse(month - 1) { "" }
            "$day $monthName $year"
        } catch (e: Exception) {
            ""
        }
    } else {
        ""
    }
}

fun formatMinutesToTime(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return String.format(Locale.ROOT, "%02d:%02d", h, m)
}
