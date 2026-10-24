package com.example.steps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalPatternIntensity
import com.example.ui.theme.LocalSelectedBackground
import com.example.ui.theme.drawCustomBackground
import com.example.ui.theme.navBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Turkish = Locale.forLanguageTag("tr-TR")
private fun integer(value: Long) = NumberFormat.getIntegerInstance(Turkish).format(value)
private fun decimal(value: Double) = String.format(Turkish, "%.1f", value)

private data class WalkingReport(
    val days: List<DailySteps> = emptyList(),
    val chart: List<DailySteps> = emptyList(),
    val profile: WalkingProfile? = null,
    val todaySteps: Long = 0L,
    val sessions: List<WalkingSessionRecord> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { StepStore.get(context) }
    val scope = rememberCoroutineScope()
    val available = remember { StepCounterService.sensor(context) != null }
    var enabled by remember { mutableStateOf(StepCounterService.enabled(context)) }
    var permission by remember { mutableStateOf(StepCounterService.hasPermission(context)) }
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var goal by remember { mutableIntStateOf(StepCounterService.settings(context).getInt("goal", 6000)) }
    var today by remember { mutableStateOf(StepStore.dayKey()) }
    var selected by rememberSaveable { mutableStateOf(StepStore.dayKey()) }
    var period by rememberSaveable { mutableStateOf(ReportPeriod.DAY) }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var selectedPace by rememberSaveable { mutableStateOf(WalkingPacePreset.MODERATE) }
    var sessionRunning by rememberSaveable { mutableStateOf(false) }
    var sessionPaused by rememberSaveable { mutableStateOf(false) }
    var sessionStartMillis by rememberSaveable { mutableLongStateOf(0L) }
    var sessionBaseSteps by rememberSaveable { mutableLongStateOf(0L) }
    var sessionElapsedSec by rememberSaveable { mutableIntStateOf(0) }

    val revision by store.changes.collectAsStateWithLifecycle()
    val range = remember(selected, period) { WalkingDates.range(selected, period) }
    val report by produceState(WalkingReport(), range, today, selected, revision) {
        value = withContext(Dispatchers.IO) {
            val rows = store.range(range.start, range.end).associateBy { it.date }
            val days = range.days.filter { it <= today }.map { rows[it] ?: DailySteps(it, 0, store.profile(it)) }
            val chartRange = if (period == ReportPeriod.DAY) {
                (6 downTo 0).map { WalkingDates.shift(selected, ReportPeriod.DAY, -it) }
            } else range.days
            val chartRows = store.range(chartRange.first(), chartRange.last()).associateBy { it.date }
            WalkingReport(
                days = days,
                chart = chartRange.map { chartRows[it] ?: DailySteps(it, 0, store.profile(it)) },
                profile = store.profile(),
                todaySteps = store.day(today),
                sessions = store.sessions(8)
            )
        }
    }
    val total = report.days.sumOf { it.steps }
    val estimates = report.days.mapNotNull { it.estimate }
    val distance = estimates.sumOf { it.meters }
    val calories = estimates.sumOf { it.activeKcal }
    val activeMinutes = WalkingMetrics.activeMinutes(total, selectedPace.cadence)
    val periodGoal = when (period) {
        ReportPeriod.DAY -> goal.toLong()
        ReportPeriod.WEEK -> goal.toLong() * 7L
        ReportPeriod.MONTH -> goal.toLong() * report.days.size.coerceAtLeast(1)
    }.coerceAtLeast(1L)
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val selectedBg = LocalSelectedBackground.current
    val patternIntensity = LocalPatternIntensity.current
    val isDark = colors.background.luminance() < 0.5f

    LaunchedEffect(sessionRunning, sessionPaused) {
        while (isActive && sessionRunning && !sessionPaused) {
            delay(1_000L)
            sessionElapsedSec += 1
        }
    }
    fun activate() {
        scope.launch {
            if (!StepCounterService.enabled(context)) withContext(Dispatchers.IO) { store.resetBaseline() }
            StepCounterService.settings(context).edit().putBoolean("enabled", true).apply()
            enabled = true
            StepCounterService.startIfEnabled(context)
        }
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = it
        if (it) activate()
    }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = StepCounterService.hasPermission(context)
                enabled = StepCounterService.enabled(context)
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            val nextToday = StepStore.dayKey()
            if (selected == today) selected = nextToday
            today = nextToday
            delay(30_000)
        }
    }
    if (editProfile) {
        ProfileEditor(
            profile = report.profile,
            error = profileError,
            onDismiss = { editProfile = false; profileError = null },
            onSave = { profile ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { store.saveProfile(profile) }
                        editProfile = false
                        profileError = null
                    } catch (_: Exception) {
                        profileError = "Profil kaydedilemedi. Yeniden deneyin."
                    }
                }
            }
        )
        return
    }

    Box(Modifier.fillMaxSize().drawCustomBackground(selectedBg, appTheme, isDark, patternIntensity)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Yürüyüş", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                    },
                    actions = {
                        IconButton(onClick = { editProfile = true }) { Icon(Icons.Default.PersonOutline, "Yürüyüş profili") }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("step-report-list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBarBottomPadding(20.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StepSection {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    !available -> "Adım sensörü bulunamadı"
                                    !permission -> "Fiziksel aktivite izni gerekli"
                                    enabled -> "Adım sayımı açık"
                                    else -> "Adım sayımı duraklatıldı"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = enabled && permission && available,
                                enabled = available,
                                onCheckedChange = {
                                    if (it) {
                                        if (permission) activate() else permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    } else {
                                        StepCounterService.settings(context).edit().putBoolean("enabled", false).apply()
                                        enabled = false
                                        context.stopService(Intent(context, StepCounterService::class.java))
                                    }
                                }
                            )
                        }
                        if (!permission && available) {
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                            }) { Text("Uygulama izinleri") }
                        }
                        if (!notifications) {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                            }) { Text("Bildirim izni") }
                        }
                    }
                }
                item {
                    TabRow(selectedTabIndex = period.ordinal, containerColor = Color.Transparent) {
                        ReportPeriod.entries.forEach { item ->
                            Tab(selected = item == period, onClick = { period = item }, text = { Text(item.title) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selected = WalkingDates.shift(selected, period, -1) }) {
                            Icon(Icons.Default.ChevronLeft, "Önceki dönem")
                        }
                        val label = when (period) {
                            ReportPeriod.DAY -> SimpleDateFormat("d MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                            ReportPeriod.WEEK -> {
                                val format = SimpleDateFormat("d MMM", Turkish)
                                "${format.format(WalkingDates.calendar(range.start).time)} – ${format.format(WalkingDates.calendar(range.end).time)}"
                            }
                            ReportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                        }
                        Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { selected = WalkingDates.shift(selected, period, 1).coerceAtMost(today) }, enabled = range.end < today) {
                            Icon(Icons.Default.ChevronRight, "Sonraki dönem")
                        }
                    }
                }
                item {
                    StepSection {
                        Text(integer(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("adım", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (total.toFloat() / periodGoal).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Text(
                            "Hedef ${integer(periodGoal)} · %${(total * 100 / periodGoal).coerceAtMost(100)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        val metrics = listOf(
                            "Tahmini mesafe" to if (report.profile != null) "${decimal(distance / 1000)} km" else "—",
                            "Tahmini aktif kcal" to if (report.profile != null) integer(kotlin.math.round(calories).toLong()) else "—",
                            "Tahmini süre" to "$activeMinutes dk"
                        )
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    metrics.forEach { (label, value) ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    metrics.forEach { (label, value) ->
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                        if (period != ReportPeriod.DAY) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Aktif gün: ${report.days.count { it.steps > 0 }} / ${report.days.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Günlük ortalama: ${integer(if (report.days.isEmpty()) 0 else total / report.days.size)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (report.profile == null) {
                            TextButton(onClick = { editProfile = true }) { Text("Boy ve kilo ekle") }
                        }
                    }
                }
                if (period == ReportPeriod.DAY) {
                    item {
                        StepSection {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Günlük hedef", style = MaterialTheme.typography.bodyMedium)
                                    Text("${integer(goal.toLong())} adım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = {
                                    goal = (goal - 500).coerceAtLeast(1000)
                                    StepCounterService.settings(context).edit().putInt("goal", goal).apply()
                                }, enabled = goal > 1000) { Icon(Icons.Default.Remove, "Hedefi azalt") }
                                IconButton(onClick = {
                                    goal = (goal + 500).coerceAtMost(30000)
                                    StepCounterService.settings(context).edit().putInt("goal", goal).apply()
                                }, enabled = goal < 30000) { Icon(Icons.Default.Add, "Hedefi artır") }
                            }
                            Text("Tempo", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WalkingPacePreset.entries.forEach { pace ->
                                    FilterChip(
                                        selected = selectedPace == pace,
                                        onClick = { selectedPace = pace },
                                        label = { Text(when (pace) { WalkingPacePreset.GENTLE -> "Rahat"; WalkingPacePreset.MODERATE -> "Normal"; WalkingPacePreset.BRISK -> "Tempolu" }) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    StepSection {
                        Text(if (period == ReportPeriod.DAY) "Son 7 gün" else "Günlük dağılım", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        StepChart(report.chart, today, goal) { clicked ->
                            if (clicked <= today) {
                                selected = clicked
                                period = ReportPeriod.DAY
                            }
                        }
                    }
                }
                if (period != ReportPeriod.DAY) {
                    item { Text("Geçmiş", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                    items(report.days.asReversed(), key = { it.date }) { day ->
                        Column(Modifier.fillMaxWidth().clickable { selected = day.date; period = ReportPeriod.DAY }.padding(vertical = 8.dp)) {
                            Text(SimpleDateFormat("d MMMM, EEE", Turkish).format(WalkingDates.calendar(day.date).time), style = MaterialTheme.typography.bodyMedium)
                            Text("${integer(day.steps)} adım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            day.estimate?.let { estimate ->
                                Text("${decimal(estimate.meters / 1000)} km · ${integer(kotlin.math.round(estimate.activeKcal).toLong())} kcal", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                } else if (selected == today) {
                    item {
                        WalkingSessionCard(
                            canStart = available && permission && enabled,
                            running = sessionRunning,
                            paused = sessionPaused,
                            elapsedSec = sessionElapsedSec,
                            sessionSteps = (report.todaySteps - sessionBaseSteps).coerceAtLeast(0),
                            sessions = report.sessions,
                            onStart = {
                                sessionStartMillis = System.currentTimeMillis()
                                sessionBaseSteps = report.todaySteps
                                sessionElapsedSec = 0
                                sessionPaused = false
                                sessionRunning = true
                            },
                            onTogglePause = { sessionPaused = !sessionPaused },
                            onSave = {
                                val steps = (report.todaySteps - sessionBaseSteps).coerceAtLeast(0)
                                val estimate = WalkingMetrics.estimate(steps, report.profile ?: WalkingProfile(172.0, 72.0))
                                val meters = estimate?.meters ?: steps * 0.72
                                val kcal = estimate?.activeKcal ?: meters * 72.0 * 0.0005
                                val started = sessionStartMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
                                val duration = sessionElapsedSec.coerceAtLeast(1)
                                scope.launch(Dispatchers.IO) { store.saveSession("Yürüyüş", started, duration, steps, meters, kcal) }
                                sessionRunning = false
                                sessionPaused = false
                                sessionElapsedSec = 0
                            },
                            onDelete = { id -> scope.launch(Dispatchers.IO) { store.deleteSession(id) } }
                        )
                    }
                }
                item { Spacer(Modifier.height(1.dp).testTag("step-report-end")) }
            }
        }
    }
}

@Composable
private fun StepSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) { Column(Modifier.fillMaxWidth().padding(16.dp), content = content) }
}

@Composable
private fun StepChart(days: List<DailySteps>, today: String, goal: Int, onSelectDay: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Canvas(
        Modifier.fillMaxWidth().height(136.dp)
            .pointerInput(days) {
                detectTapGestures { position ->
                    if (days.isNotEmpty() && size.width > 0) {
                        val index = (position.x / (size.width.toFloat() / days.size)).toInt().coerceIn(0, days.lastIndex)
                        if (days[index].date <= today) onSelectDay(days[index].date)
                    }
                }
            }
            .semantics { contentDescription = days.joinToString { "${it.date}: ${it.steps} adım" } }
    ) {
        if (days.isEmpty()) return@Canvas
        val maximum = maxOf(days.maxOf { it.steps }, goal.toLong(), 1L)
        val slot = size.width / days.size
        val goalY = size.height * (1f - goal.toFloat() / maximum)
        drawLine(colors.outlineVariant, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        days.forEachIndexed { index, day ->
            val height = (size.height * (day.steps.toFloat() / maximum)).coerceAtLeast(2.dp.toPx())
            val width = (slot * 0.55f).coerceAtMost(28.dp.toPx())
            drawRoundRect(
                color = if (day.date > today || day.steps == 0L) colors.outlineVariant else colors.primary,
                topLeft = Offset(index * slot + (slot - width) / 2, size.height - height),
                size = Size(width, height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOfNotNull(days.firstOrNull(), days.lastOrNull()).distinctBy { it.date }.forEach { day ->
            Text(SimpleDateFormat("d MMM", Turkish).format(WalkingDates.calendar(day.date).time), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun WalkingSessionCard(
    canStart: Boolean,
    running: Boolean,
    paused: Boolean,
    elapsedSec: Int,
    sessionSteps: Long,
    sessions: List<WalkingSessionRecord>,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    StepSection {
        Text("Yürüyüş kaydı", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (running) {
            Text("%02d:%02d".format(elapsedSec / 60, elapsedSec % 60), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("${integer(sessionSteps)} adım", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            // Separate full-width controls keep large text readable on a narrow phone.
            OutlinedButton(onClick = onTogglePause, modifier = Modifier.fillMaxWidth()) { Text(if (paused) "Devam et" else "Duraklat") }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
        } else {
            FilledTonalButton(onClick = onStart, enabled = canStart, modifier = Modifier.fillMaxWidth().testTag("walking-session-start")) { Text("Yürüyüşü başlat") }
        }
        if (sessions.isNotEmpty()) {
            TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Kayıtları gizle" else "Kayıtlar (${sessions.size})") }
            if (showHistory) sessions.forEach { record ->
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(SimpleDateFormat("d MMM HH:mm", Turkish).format(Date(record.startTimeMillis)), style = MaterialTheme.typography.bodyMedium)
                        Text("${integer(record.steps)} adım · ${decimal(record.meters / 1000)} km", style = MaterialTheme.typography.bodySmall)
                        Text("%02d:%02d · %s kcal".format(record.durationSec / 60, record.durationSec % 60, decimal(record.kcal)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(record.id) }) { Icon(Icons.Default.DeleteOutline, "Yürüyüş kaydını sil") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditor(
    profile: WalkingProfile?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (WalkingProfile) -> Unit
) {
    var height by rememberSaveable { mutableStateOf(profile?.heightCm?.let(::decimal) ?: "") }
    var weight by rememberSaveable { mutableStateOf(profile?.weightKg?.let(::decimal) ?: "") }
    var stride by rememberSaveable { mutableStateOf(profile?.calibratedStepCm?.let(::decimal) ?: "") }
    var distance by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    fun parse(value: String) = value.trim().replace(',', '.').toDoubleOrNull()
    val entered = WalkingProfile(
        parse(height) ?: Double.NaN,
        parse(weight) ?: Double.NaN,
        if (stride.isBlank()) null else parse(stride) ?: Double.NaN
    )
    val calibration = WalkingMetrics.calibratedStepCm(parse(distance) ?: Double.NaN, steps.toIntOrNull() ?: 0)
    val colors = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Vazgeç")
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Kaydet") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { onSave(entered) }, enabled = entered.valid) {
                            Icon(Icons.Default.Check, "Kaydet")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .testTag("walking-profile-scroll")
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = navBarBottomPadding(36.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Boy (cm)") },
                singleLine = true,
                isError = height.isNotBlank() && (parse(height)?.let { it in 80.0..250.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Kilo (kg)") },
                singleLine = true,
                isError = weight.isNotBlank() && (parse(weight)?.let { it in 20.0..350.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = stride,
                onValueChange = { stride = it },
                label = { Text("Adım uzunluğu (cm)") },
                singleLine = true,
                placeholder = { Text("İsteğe bağlı") },
                isError = stride.isNotBlank() && (parse(stride)?.let { it in 20.0..150.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            Text("Mesafe kalibrasyonu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it },
                label = { Text("Yürünen mesafe (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text("Sayılan adım") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(enabled = calibration != null, onClick = { stride = decimal(calibration!!) }) {
                Text(if (calibration != null) "${decimal(calibration)} cm uygula" else "Kalibrasyonu uygula")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
