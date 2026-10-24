package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.GeminiApiClient
import com.example.data.GenerateContentRequest
import com.example.data.Content
import com.example.data.Part
import com.example.data.QuranAiSearchEngine
import com.example.data.QuranAiSearchResult
import com.example.data.QuranTopicIndex
import com.example.data.QuranIndexVerseItem
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.AppTheme
import com.example.ui.theme.buttonShape
import com.example.ui.theme.cardShape
import com.example.ui.theme.buttonBorder
import com.example.ui.theme.buttonGradient
import com.example.ui.theme.buttonTextColor
import com.example.ui.theme.glassmorphicCard
import com.example.ui.theme.glassSurfaceColor
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.draw.scale
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.esmaUlHusnaList
import com.example.data.NameItem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MyLocation
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.audio.MealSpeechEngine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Slider
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import com.example.data.Timings

import android.widget.Toast
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import kotlinx.coroutines.launch

sealed class DiscoverSubScreen {
    object Main : DiscoverSubScreen()
    object Notes : DiscoverSubScreen()
    object QuranMiracles : DiscoverSubScreen()
    object QazaTracker : DiscoverSubScreen()
    object Surahs : DiscoverSubScreen()
    object EsmaUlHusna : DiscoverSubScreen()
    object AlarmSettings : DiscoverSubScreen()
    object ZakatCalc : DiscoverSubScreen()
    object QuranIndex : DiscoverSubScreen()
    object HadithLibrary : DiscoverSubScreen()
    object QuranReading : DiscoverSubScreen()
    object Steps : DiscoverSubScreen()
    object NearestMosque : DiscoverSubScreen()
    object PrayerLearning : DiscoverSubScreen()
    object AppSettings : DiscoverSubScreen()
}

/** Stable IDs preserve navigation across speech activities and Android recreation. */
internal val DiscoverSubScreenSaver = Saver<DiscoverSubScreen, String>(
    save = { screen ->
        when (screen) {
            DiscoverSubScreen.Main -> "main"
            DiscoverSubScreen.Notes -> "notes"
            DiscoverSubScreen.QuranMiracles -> "quran-miracles"
            DiscoverSubScreen.QazaTracker -> "qaza-tracker"
            DiscoverSubScreen.Surahs -> "surahs"
            DiscoverSubScreen.EsmaUlHusna -> "esma-ul-husna"
            DiscoverSubScreen.AlarmSettings -> "alarm-settings"
            DiscoverSubScreen.ZakatCalc -> "zakat-calculator"
            DiscoverSubScreen.QuranIndex -> "quran-index"
            DiscoverSubScreen.HadithLibrary -> "hadith-library"
            DiscoverSubScreen.QuranReading -> "quran-reading"
            DiscoverSubScreen.Steps -> "steps"
            DiscoverSubScreen.NearestMosque -> "nearest-mosque"
            DiscoverSubScreen.PrayerLearning -> "prayer-learning"
            DiscoverSubScreen.AppSettings -> "app-settings"
        }
    },
    restore = { id ->
        when (id) {
            "notes" -> DiscoverSubScreen.Notes
            "quran-miracles" -> DiscoverSubScreen.QuranMiracles
            "qaza-tracker" -> DiscoverSubScreen.QazaTracker
            "surahs" -> DiscoverSubScreen.Surahs
            "esma-ul-husna" -> DiscoverSubScreen.EsmaUlHusna
            "alarm-settings" -> DiscoverSubScreen.AlarmSettings
            "zakat-calculator" -> DiscoverSubScreen.ZakatCalc
            "quran-index" -> DiscoverSubScreen.QuranIndex
            "hadith-library" -> DiscoverSubScreen.HadithLibrary
            "quran-reading" -> DiscoverSubScreen.QuranReading
            "steps" -> DiscoverSubScreen.Steps
            "nearest-mosque" -> DiscoverSubScreen.NearestMosque
            "prayer-learning" -> DiscoverSubScreen.PrayerLearning
            "app-settings" -> DiscoverSubScreen.AppSettings
            else -> DiscoverSubScreen.Main
        }
    }
)

data class DiscoverItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: DiscoverSubScreen,
    val color: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DiscoverScreen(viewModel: PrayerViewModel, onNavigate: (String) -> Unit) {
    var currentScreen by rememberSaveable(stateSaver = DiscoverSubScreenSaver) {
        mutableStateOf<DiscoverSubScreen>(DiscoverSubScreen.Main)
    }
    val haptic = LocalHapticFeedback.current


    val targetQuranVerse by viewModel.targetQuranVerse.collectAsState()
    LaunchedEffect(targetQuranVerse) {
        if (targetQuranVerse != null) {
            currentScreen = DiscoverSubScreen.QuranReading
        }
    }

    val targetMiracleId by viewModel.targetMiracleId.collectAsState()
    LaunchedEffect(targetMiracleId) {
        if (targetMiracleId != null) {
            currentScreen = DiscoverSubScreen.QuranMiracles
        }
    }

    if (currentScreen != DiscoverSubScreen.Main) {
        androidx.activity.compose.BackHandler {
            currentScreen = DiscoverSubScreen.Main
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val strings = LocalAppStrings.current
    val activeTheme = LocalAppTheme.current
    val isDarkTheme = com.example.ui.theme.LocalThemeIsDark.current
    val isGlassTheme = activeTheme != AppTheme.CLASSIC_EMERALD && activeTheme.spec.isGlass

    val items = listOf(
        DiscoverItem("Not Defteri", "Notlarınız ve kayıtlarınız", Icons.AutoMirrored.Filled.List, DiscoverSubScreen.Notes, primaryColor),
        DiscoverItem("En Yakın Cami", "Konumunuza göre camiler ve yol tarifi", Icons.Default.LocationOn, DiscoverSubScreen.NearestMosque, primaryColor),
        DiscoverItem("Namaz Öğreniyorum", "Hazırlık, adım adım farz namazlar ve dualar", Icons.Default.Book, DiscoverSubScreen.PrayerLearning, primaryColor),
        DiscoverItem("Adımsayar", "Günlük ve aylık yürüyüş kayıtları", Icons.Default.DirectionsWalk, DiscoverSubScreen.Steps, primaryColor),
        DiscoverItem(strings.quranMiraclesTitle, strings.quranMiraclesSubtitle, Icons.Default.AutoAwesome, DiscoverSubScreen.QuranMiracles, primaryColor),
        DiscoverItem(strings.qazaTrackerTitle, "Kaza namazlarınızı kaydedin", Icons.AutoMirrored.Filled.List, DiscoverSubScreen.QazaTracker, primaryColor),
        DiscoverItem(strings.surahsAndPrayersTitle, "Arapça okunuşlar ve mealleri", Icons.Default.Info, DiscoverSubScreen.Surahs, primaryColor),
        DiscoverItem(strings.esmaUlHusnaTitle, "Allah'ın 99 ismi ve anlamları", Icons.Default.Favorite, DiscoverSubScreen.EsmaUlHusna, primaryColor),
        DiscoverItem(strings.azanAlarmsTitle, "Vakit bazlı bildirim ayarları", Icons.Default.Notifications, DiscoverSubScreen.AlarmSettings, primaryColor),
        DiscoverItem(strings.zakatTitle, "Zekatınızı kolayca hesaplayın", Icons.Default.Star, DiscoverSubScreen.ZakatCalc, primaryColor),
        DiscoverItem(strings.quranIndexTitle, "Konularına göre ayetler", Icons.Default.Search, DiscoverSubScreen.QuranIndex, primaryColor),
        DiscoverItem(strings.hadithLibraryTitle, "Arama ve kategorilerle hadisler", Icons.Default.Info, DiscoverSubScreen.HadithLibrary, primaryColor),
        DiscoverItem(strings.quranTitle, "Tefsir karşılaştırma ve meal okuma", Icons.Default.Book, DiscoverSubScreen.QuranReading, primaryColor),
        // Settings lives here as well as on the home screen: the themes that
        // move the home actions into the navigation bar would otherwise leave
        // no way to reach preferences from the home page.
        DiscoverItem(strings.settings, "Tema, dil, hesaplama ve bildirimler", Icons.Default.Settings, DiscoverSubScreen.AppSettings, primaryColor),
    )

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "discover_navigation"
    ) { screen ->
        when (screen) {
            DiscoverSubScreen.Main -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = strings.islamicToolsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gününüzü kolaylaştıran faydalı dini araçlar ve rehberler",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            bottom = com.example.ui.theme.navBarBottomPadding()
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items) { item ->
                            val toolCardShape = if (isGlassTheme) activeTheme.cardShape else RoundedCornerShape(16.dp)
                            val iconBadgeShape = if (isGlassTheme) activeTheme.buttonShape else CircleShape
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGlassTheme)
                                        com.example.ui.theme.glassSurfaceColor(activeTheme, isDarkTheme)
                                    else
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                ),
                                shape = toolCardShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isGlassTheme) {
                                            Modifier.glassmorphicCard(
                                                theme = activeTheme,
                                                isDark = isDarkTheme,
                                                shape = toolCardShape,
                                                border = activeTheme.buttonBorder
                                            )
                                        } else {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), toolCardShape)
                                        }
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentScreen = item.target
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(iconBadgeShape)
                                            .background(item.color.copy(alpha = 0.15f))
                                            .then(
                                                if (isGlassTheme) {
                                                    Modifier.border(activeTheme.buttonBorder, iconBadgeShape)
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            DiscoverSubScreen.Notes -> NotesScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.QuranMiracles -> QuranMiraclesScreen(
                viewModel = viewModel,
                onNavigateToQuranReading = { currentScreen = DiscoverSubScreen.QuranReading },
                onBack = { currentScreen = DiscoverSubScreen.Main }
            )
            DiscoverSubScreen.QazaTracker -> QazaTrackerScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.Surahs -> SurahsScreen(viewModel = viewModel, onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.EsmaUlHusna -> EsmaUlHusnaScreen(viewModel = viewModel, onNavigate = onNavigate, onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.AlarmSettings -> AlarmSettingsScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.ZakatCalc -> ZakatCalcScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.QuranIndex -> QuranIndexScreen(viewModel = viewModel, onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.HadithLibrary -> HadithLibraryScreen(viewModel, onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.QuranReading -> QuranReadingScreen(viewModel = viewModel, onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.Steps -> com.example.steps.StepReportScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.NearestMosque -> NearestMosqueScreen(
                onBack = { currentScreen = DiscoverSubScreen.Main },
                viewModel = viewModel
            )
            DiscoverSubScreen.PrayerLearning -> PrayerLearningScreen(onBack = { currentScreen = DiscoverSubScreen.Main })
            DiscoverSubScreen.AppSettings -> SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = DiscoverSubScreen.Main },
                onNavigateToSearch = { onNavigate("search") }
            )
        }
    }
}

// --- SUB-SCREEN: Qaza Tracker (Kaza Namazı ve Oruç Takibi) ---
@Composable
fun QazaTrackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("qaza_prefs", Context.MODE_PRIVATE) }

    val prayers = remember { listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı", "Vitir") }
    val prayerRakat = remember {
        mapOf(
            "Sabah" to "2 Rekat Farz",
            "Öğle" to "4 Rekat Farz",
            "İkindi" to "4 Rekat Farz",
            "Akşam" to "3 Rekat Farz",
            "Yatsı" to "4 Rekat Farz",
            "Vitir" to "3 Rekat Vacip"
        )
    }

    val counts = remember { mutableStateMapOf<String, Int>() }
    val initialCounts = remember { mutableStateMapOf<String, Int>() }
    var fastingCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var fastingInitialCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var dailyGoal by remember { androidx.compose.runtime.mutableIntStateOf(1) }
    val historyLogs = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) } // 0: Namaz, 1: Oruç, 2: Hesaplayıcı, 3: İstatistik/Geçmiş
    var showGoalDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showResetDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showEditDialogForPrayer by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var editInputValue by remember { androidx.compose.runtime.mutableStateOf("") }

    // Load data from SharedPreferences
    fun loadData() {
        prayers.forEach { prayer ->
            val c = prefs.getInt(prayer, 0)
            val init = prefs.getInt("${prayer}_init", c)
            counts[prayer] = c
            initialCounts[prayer] = init
        }
        fastingCount = prefs.getInt("Oruc", 0)
        fastingInitialCount = prefs.getInt("Oruc_init", fastingCount)
        dailyGoal = prefs.getInt("daily_goal", 1).coerceAtLeast(1)

        val savedLogs = prefs.getString("qaza_history", "") ?: ""
        historyLogs.clear()
        if (savedLogs.isNotBlank()) {
            historyLogs.addAll(savedLogs.split("|||").filter { it.isNotBlank() })
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    fun saveHistoryLog(actionText: String) {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr"))
        val timestamp = sdf.format(java.util.Date())
        val logEntry = "$timestamp • $actionText"
        historyLogs.add(0, logEntry)
        if (historyLogs.size > 50) historyLogs.removeAt(historyLogs.lastIndex)
        prefs.edit().putString("qaza_history", historyLogs.joinToString("|||")).apply()
    }

    fun updateCount(prayer: String, delta: Int, isBatch: Boolean = false) {
        val current = counts[prayer] ?: 0
        val newVal = (current + delta).coerceAtLeast(0)
        counts[prayer] = newVal
        
        // Ensure initial count is updated if user manually increases count beyond original init
        val initVal = initialCounts[prayer] ?: newVal
        if (newVal > initVal) {
            initialCounts[prayer] = newVal
            prefs.edit().putInt("${prayer}_init", newVal).apply()
        }
        
        prefs.edit().putInt(prayer, newVal).apply()

        if (!isBatch && delta != 0) {
            val sign = if (delta > 0) "+$delta" else "$delta"
            saveHistoryLog("$prayer Namazı: $sign kaza ($newVal kaldı)")
        }
    }

    fun updateFastingCount(delta: Int) {
        val newVal = (fastingCount + delta).coerceAtLeast(0)
        fastingCount = newVal
        if (newVal > fastingInitialCount) {
            fastingInitialCount = newVal
            prefs.edit().putInt("Oruc_init", newVal).apply()
        }
        prefs.edit().putInt("Oruc", newVal).apply()
        
        val sign = if (delta > 0) "+$delta" else "$delta"
        saveHistoryLog("Oruç Kazası: $sign gün ($newVal gün kaldı)")
    }

    fun completeOneFullDay() {
        var completedAny = false
        prayers.forEach { prayer ->
            val curr = counts[prayer] ?: 0
            if (curr > 0) {
                updateCount(prayer, -1, isBatch = true)
                completedAny = true
            }
        }
        if (completedAny) {
            saveHistoryLog("⚡ 1 Günlük Tam Kaza Kılındı (6 Vakit)")
            android.widget.Toast.makeText(context, "1 Günlük (6 Vakit) Kaza Namazı Tamamlandı! 🎉", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Kaza edilecek namazınız kalmadı! Maşallah.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val totalRemainingPrayers = counts.values.sum()
    val totalInitialPrayers = initialCounts.values.sum().coerceAtLeast(totalRemainingPrayers)
    val completedPrayers = (totalInitialPrayers - totalRemainingPrayers).coerceAtLeast(0)
    val overallPrayerProgress = if (totalInitialPrayers > 0) completedPrayers.toFloat() / totalInitialPrayers.toFloat() else 1.0f

    val remainingDaysAtGoal = if (dailyGoal > 0) (totalRemainingPrayers.toFloat() / (6 * dailyGoal)).toInt() else 0

    SubScreenScaffold(title = "Kaza Namazı ve Oruç Takibi", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header Card with Summary & Quick Actions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kalan Kaza Namazı",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalRemainingPrayers vakit",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Kalan Oruç",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$fastingCount gün",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tamamlanma Oranı: %${(overallPrayerProgress * 100).toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (remainingDaysAtGoal > 0) {
                            Text(
                                text = "Tahmini: ~$remainingDaysAtGoal gün (Günde $dailyGoal vakit seti)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { overallPrayerProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { completeOneFullDay() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1 Gün Kıldım", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showGoalDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎯 Hedef ($dailyGoal)", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Namaz", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Oruç", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Hesapla", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Geçmiş", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Contents
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> QazaPrayersTab(
                        prayers = prayers,
                        prayerRakat = prayerRakat,
                        counts = counts,
                        initialCounts = initialCounts,
                        onUpdateCount = { prayer, delta -> updateCount(prayer, delta) },
                        onEditCount = { prayer ->
                            showEditDialogForPrayer = prayer
                            editInputValue = (counts[prayer] ?: 0).toString()
                        }
                    )
                    1 -> QazaFastingTab(
                        fastingCount = fastingCount,
                        fastingInitialCount = fastingInitialCount,
                        onUpdateFasting = { delta -> updateFastingCount(delta) }
                    )
                    2 -> QazaCalculatorTab(
                        onApplyCalculation = { calculatedDays ->
                            prayers.forEach { prayer ->
                                counts[prayer] = calculatedDays
                                initialCounts[prayer] = calculatedDays
                                prefs.edit().putInt(prayer, calculatedDays).putInt("${prayer}_init", calculatedDays).apply()
                            }
                            fastingCount = calculatedDays
                            fastingInitialCount = calculatedDays
                            prefs.edit().putInt("Oruc", calculatedDays).putInt("Oruc_init", calculatedDays).apply()

                            saveHistoryLog("🧮 Otomatik Hesaplama: Her vakitten $calculatedDays kaza ve $calculatedDays gün oruç eklendi.")
                            android.widget.Toast.makeText(context, "$calculatedDays günlük kaza listeye tanımlandı!", android.widget.Toast.LENGTH_LONG).show()
                            selectedTab = 0
                        }
                    )
                    3 -> QazaHistoryAndStatsTab(
                        historyLogs = historyLogs,
                        totalRemainingPrayers = totalRemainingPrayers,
                        totalInitialPrayers = totalInitialPrayers,
                        fastingCount = fastingCount,
                        onClearHistory = {
                            historyLogs.clear()
                            prefs.edit().remove("qaza_history").apply()
                            android.widget.Toast.makeText(context, "Geçmiş temizlendi.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onResetAll = { showResetDialog = true }
                    )
                }
            }
        }
    }

    // Dialog for Setting Daily Goal
    if (showGoalDialog) {
        var tempGoal by remember { androidx.compose.runtime.mutableStateOf(dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Günlük Kaza Hedefi") },
            text = {
                Column {
                    Text("Günde kaç günlük kaza namazı kılmayı hedefliyorsunuz?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempGoal,
                        onValueChange = { tempGoal = it.filter { c -> c.isDigit() } },
                        label = { Text("Günlük Set Sayısı") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val g = tempGoal.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        dailyGoal = g
                        prefs.edit().putInt("daily_goal", g).apply()
                        showGoalDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("İptal") }
            }
        )
    }

    // Dialog for Editing Prayer Count directly
    val currentEditPrayer = showEditDialogForPrayer
    if (currentEditPrayer != null) {
        AlertDialog(
            onDismissRequest = { showEditDialogForPrayer = null },
            title = { Text("$currentEditPrayer Kaza Sayısını Düzenle") },
            text = {
                OutlinedTextField(
                    value = editInputValue,
                    onValueChange = { editInputValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Toplam Kaza Adedi") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCount = editInputValue.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        counts[currentEditPrayer] = newCount
                        if (newCount > (initialCounts[currentEditPrayer] ?: 0)) {
                            initialCounts[currentEditPrayer] = newCount
                            prefs.edit().putInt("${currentEditPrayer}_init", newCount).apply()
                        }
                        prefs.edit().putInt(currentEditPrayer, newCount).apply()
                        saveHistoryLog("$currentEditPrayer Kaza adedi $newCount olarak güncellendi.")
                        showEditDialogForPrayer = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogForPrayer = null }) { Text("İptal") }
            }
        )
    }

    // Dialog for Reset All
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Tüm Sayıları Sıfırla?") },
            text = { Text("Bütün kaza namazı ve oruç takibi kayıtlarınız sıfırlanacaktır. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        prayers.forEach { prayer ->
                            counts[prayer] = 0
                            initialCounts[prayer] = 0
                            prefs.edit().remove(prayer).remove("${prayer}_init").apply()
                        }
                        fastingCount = 0
                        fastingInitialCount = 0
                        prefs.edit().remove("Oruc").remove("Oruc_init").apply()
                        historyLogs.clear()
                        prefs.edit().remove("qaza_history").apply()
                        showResetDialog = false
                        android.widget.Toast.makeText(context, "Tüm veriler sıfırlandı.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Evet, Sıfırla", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("İptal") }
            }
        )
    }
}

// --- TAB 0: KAZA NAMAZLARI VAKİT LİSTESİ ---
@Composable
private fun QazaPrayersTab(
    prayers: List<String>,
    prayerRakat: Map<String, String>,
    counts: Map<String, Int>,
    initialCounts: Map<String, Int>,
    onUpdateCount: (String, Int) -> Unit,
    onEditCount: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
            bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(prayers) { prayer ->
            val count = counts[prayer] ?: 0
            val initCount = initialCounts[prayer] ?: count
            val progress = if (initCount > 0) (initCount - count).toFloat() / initCount.toFloat() else 1.0f

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$prayer Namazı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = prayerRakat[prayer] ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Count display & Direct edit click
                        Surface(
                            onClick = { onEditCount(prayer) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Individual Prayer Progress
                    if (initCount > 0 && count < initCount) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Button Action Row (-5, -1, +1, +5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateCount(prayer, -5) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onUpdateCount(prayer, -1) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-1 Kıldım", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateCount(prayer, 1) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onUpdateCount(prayer, 5) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 1: ORUÇ KAZASI ---
@Composable
private fun QazaFastingTab(
    fastingCount: Int,
    fastingInitialCount: Int,
    onUpdateFasting: (Int) -> Unit
) {
    val progress = if (fastingInitialCount > 0) (fastingInitialCount - fastingCount).toFloat() / fastingInitialCount.toFloat() else 1.0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
            bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ramazan Orucu Kaza Takibi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tutamadığınız veya kaza etmeniz gereken oruç günleri", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "$fastingCount",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Kalan Kaza Günü", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (fastingInitialCount > 0 && fastingCount < fastingInitialCount) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "%${(progress * 100).toInt()} Tamamlandı",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { onUpdateFasting(-1) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("1 Gün Tuttum (-1)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onUpdateFasting(-5) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("-5 Gün", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateFasting(1) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 Gün Ekle", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onUpdateFasting(30) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+30 Gün (1 Ay)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Oruç Kazası Hükümleri", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Mazeretsiz veya mazeretli tutulamayan Ramazan oruçlarının gününe gün kaza edilmesi farzdır.\n" +
                                "• Kaza oruçları peş peşe tutulabileceği gibi aralıklı olarak da tutulabilir.\n" +
                                "• İhtiyarlık veya şifa umudu kalmayan hastalık gibi sürekli mazereti olanlar her gün için fidye verir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// --- TAB 2: OTOMATİK HESAPLAMA ASİSTANI ---
@Composable
private fun QazaCalculatorTab(
    onApplyCalculation: (Int) -> Unit
) {
    var ageInput by remember { androidx.compose.runtime.mutableStateOf("25") }
    var pubertyAgeInput by remember { androidx.compose.runtime.mutableStateOf("12") }
    var prayedYearsInput by remember { androidx.compose.runtime.mutableStateOf("0") }
    var femaleExemptionDaysPerYear by remember { androidx.compose.runtime.mutableStateOf("0") }

    val age = ageInput.toIntOrNull() ?: 0
    val pubertyAge = pubertyAgeInput.toIntOrNull() ?: 12
    val prayedYears = prayedYearsInput.toIntOrNull() ?: 0
    val exemptionDays = femaleExemptionDaysPerYear.toIntOrNull() ?: 0

    val unprayedYears = (age - pubertyAge - prayedYears).coerceAtLeast(0)
    val totalCalculatedDays = (unprayedYears * (365 - exemptionDays)).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
            bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "🧮 Kaza Hesaplama Asistanı",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Buluğ çağından itibaren kılınmayan tahmini süreyi girerek toplam kaza miktarınızı otomatik hesaplayabilirsiniz.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Şu Anki Yaşınız") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pubertyAgeInput,
                        onValueChange = { pubertyAgeInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Buluğ (Sorumluluk) Yaşı (Örn: 12)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = prayedYearsInput,
                        onValueChange = { prayedYearsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Düzenli Kılınan/Kaza Edilen Yıl Sayısı") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = femaleExemptionDaysPerYear,
                        onValueChange = { femaleExemptionDaysPerYear = it.filter { c -> c.isDigit() } },
                        label = { Text("Yıllık Muafiyet Gün Sayısı (Kadınlar İçin Özel Durum - İsteğe Bağlı)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hesaplanan Toplam Kaza", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalCalculatedDays Gün",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Her vakitten $totalCalculatedDays kaza namazı ve $totalCalculatedDays gün oruç",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onApplyCalculation(totalCalculatedDays) },
                        enabled = totalCalculatedDays > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hesaplanan Sayıları Kaydet ve Başlat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- TAB 3: İSTATİSTİK VE GEÇMİŞ ---
@Composable
private fun QazaHistoryAndStatsTab(
    historyLogs: List<String>,
    totalRemainingPrayers: Int,
    totalInitialPrayers: Int,
    fastingCount: Int,
    onClearHistory: () -> Unit,
    onResetAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
            bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Genel İstatistikler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val completedCount = (totalInitialPrayers - totalRemainingPrayers).coerceAtLeast(0)
                    Text("• Tamamlanan Kaza Namazı: $completedCount vakit", fontSize = 13.sp)
                    Text("• Kalan Kaza Namazı: $totalRemainingPrayers vakit", fontSize = 13.sp)
                    Text("• Kalan Kaza Orucu: $fastingCount gün", fontSize = 13.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📜 Kaza Kılma Geçmişi Logları", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (historyLogs.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Temizle", fontSize = 12.sp)
                    }
                }
            }
        }

        if (historyLogs.isEmpty()) {
            item {
                Text(
                    text = "Henüz kaydedilmiş kaza işlemi yok. Kaza namazı kıldıkça veya güncelledikçe geçmişiniz burada görünecektir.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(historyLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onResetAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tüm Kaza Takibi Verilerini Sıfırla")
            }
        }
    }
}

// --- SUB-SCREEN: Namaz Sureleri ve Duaları ---
data class SurahItem(val title: String, val arabic: String, val trans: String, val meaning: String)

// Split Helper Functions for Besmele
fun stripArabicBismillah(text: String): String {
    val bismillahPatterns = listOf(
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَنِ ٱلرَّحِيمِ",
        "بِسمِ اللهِ الرَّحمٰنِ الرَّحيمِ"
    )
    var cleaned = text.trim()
    for (b in bismillahPatterns) {
        if (cleaned.startsWith(b)) {
            cleaned = cleaned.substring(b.length).trim()
            break
        }
    }
    return cleaned
}

fun stripTransliterationBismillah(text: String): String {
    val prefixes = listOf(
        "Bismi-llâhi-rrahmâni-rrahîm. ",
        "Bismi-llâhi-rrahmâni-rrahîm.",
        "bismi-llâhi-rrahmâni-rrahîm. ",
        "bismi-llâhi-rrahmâni-rrahîm.",
        "bismi-llâhi-rraḥmâni-rraḥîm. ",
        "bismi-llâhi-rraḥmâni-rraḥîm.",
        "Bismillâhirrahmânirrahîm. ",
        "Bismillâhirrahmânirrahîm.",
        "Bismillâhirrahmânirrahîm ",
        "Bismillâhirrahmânirrahîm",
        "Bismillahir rahmanir rahim. ",
        "Bismillahir rahmanir rahim.",
        "Bismillahir rahmanir rahim ",
        "Bismillahir rahmanir rahim",
        "Bismillahi-r-rahmani-r-rahim. ",
        "Bismillahi-r-rahmani-r-rahim.",
        "Bismillahi-r-rahmani-r-rahim ",
        "Bismillahi-r-rahmani-r-rahim",
        "Bismillah-ir-Rahman-ir-Rahim. ",
        "Bismillah-ir-Rahman-ir-Rahim.",
        "Bismillah-ir-Rahman-ir-Rahim "
    )
    var cleaned = text.trim()
    for (p in prefixes) {
        if (cleaned.startsWith(p)) {
            cleaned = cleaned.substring(p.length).trim()
            break
        }
    }
    return cleaned
}

fun stripMealBismillah(text: String): String {
    val prefixes = listOf(
        "Rahmân ve Rahîm olan Allah'ın adıyla. ",
        "Rahmân ve Rahîm olan Allah'ın adıyla.\n",
        "Rahmân ve Rahîm olan Allah'ın adıyla.",
        "Rahmân ve Rahîm olan Allah'ın adıyla ",
        "Rahmân ve Rahîm olan Allah'ın adıyla",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım). ",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım).",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım) ",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım)",
        "Rahman ve Rahim olan Allah'ın adıyla. ",
        "Rahman ve Rahim olan Allah'ın adıyla.",
        "Rahman ve Rahim olan Allah'ın adıyla ",
        "Rahman ve Rahim olan Allah'ın adıyla",
        "Rahmân ve Rahîm olan Allah'ın Adıyla. ",
        "Rahmân ve Rahîm olan Allah'ın Adıyla."
    )
    var cleaned = text.trim()
    for (p in prefixes) {
        if (cleaned.startsWith(p)) {
            cleaned = cleaned.substring(p.length).trim()
            break
        }
    }
    return cleaned
}

fun splitArabicBesmele(text: String): Pair<String?, String> {
    val besmeleList = listOf(
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\n",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\n",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
    )
    for (b in besmeleList) {
        if (text.startsWith(b)) {
            val rest = text.substring(b.length).trim()
            return Pair("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", rest)
        }
    }
    return Pair(null, text)
}

fun splitTransBesmele(text: String): Pair<String?, String> {
    val prefixes = listOf(
        "Bismillâhirrahmânirrahîm. ",
        "Bismillâhirrahmânirrahîm\n",
        "Bismillâhirrahmânirrahîm.",
        "Bismillâhirrahmânirrahîm"
    )
    for (p in prefixes) {
        if (text.startsWith(p)) {
            val rest = text.substring(p.length).trim()
            return Pair("Bismillâhirrahmânirrahîm", rest)
        }
    }
    return Pair(null, text)
}

fun splitMeaningBesmele(text: String): Pair<String?, String> {
    val prefixes = listOf(
        "Rahmân ve Rahîm olan Allah'ın adıyla. ",
        "Rahmân ve Rahîm olan Allah'ın adıyla.\n",
        "Rahmân ve Rahîm olan Allah'ın adıyla.",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım). ",
        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım)."
    )
    for (p in prefixes) {
        if (text.startsWith(p)) {
            val rest = text.substring(p.length).trim()
            return Pair("Rahmân ve Rahîm olan Allah'ın adıyla", rest)
        }
    }
    return Pair(null, text)
}

@Composable
fun SurahsScreen(viewModel: PrayerViewModel, onBack: () -> Unit) {
    KeepScreenOn()
    val items = listOf(
        SurahItem(
            "Sübhaneke Duası",
            "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ ۞ وَتَبَارَكَ اسْمُكَ ۞ وَتَعَالَى جَدُّكَ ۞ وَلَا إِلَهَ غَيْرُكَ",
            "Sübhânekellâhümme ve bi hamdik ve tebârakesmük ve teâlâ ceddük (ve celle senâük) ve lâ ilâhe ğayrük.",
            "Allah'ım! Sen eksik sıfatlardan pak ve uzaksın. Seni daima böyle tenzih eder ve överim. Senin adın mübarektir. Varlığın her şeyden üstündür. Senden başka ilah yoktur."
        ),
        SurahItem(
            "Fâtiha Suresi", 
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nالْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۞ الرَّحْمَنِ الرَّحِيمِ ۞ مَالِكِ يَوْمِ الدِّينِ ۞ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۞ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۞ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", 
            "Bismillâhirrahmânirrahîm. Elhamdü lillâhi rabbil'alemin. Errahmânirrahîm. Mâliki yevmiddîn. İyyâke na'büdü ve iyyâke neste'în. İhdinessırâtel müstakîm. Sırâtellezîne en'amte aleyhim gayrilmağdûbi aleyhim veleddâllîn.", 
            "Rahmân ve Rahîm olan Allah'ın adıyla. Hamd, âlemlerin Rabbi, Rahmân, Rahîm, hesap ve ceza gününün maliki Allah'a mahsustur. Yalnız sana ibadet eder ve yalnız senden yardım dileriz. Bizi doğru yola ilet."
        ),
        SurahItem(
            "Fil Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nأَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ بِأَصْحَابِ الْفِيلِ ۞ أَلَمْ يَجْعَلْ كَيْدَهُمْ فِي تَضْلِيلٍ ۞ وَأَرْسَلَ عَلَيْهِمْ طَيْرًا أَبَابِيلَ ۞ تَرْمِيهِم بِحِجَارَةٍ مِّن سِجِّيلٍ ۞ فَجَعَلَهُمْ كَعَصْفٍ مَّأْكُولٍ",
            "Bismillâhirrahmânirrahîm. Elem tera keyfe fe'ale rabbüke biashâbilfîl. Elem yec'al keydehüm fî tadlîl. Ve ersele aleyhim tayran ebâbîl. Termîhim bihicâratin min siccîl. Fece'alehüm ke'asfin me'kûl.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Rabbinin, fil sahiplerine ne yaptığını görmedin mi? Onların tuzaklarını boşa çıkarmadı mı? Üzerlerine sürü sürü kuşlar gönderdi. Onlara siccilden sertleşmiş taşlar atıyorlardı. Nihayet onları yenilmiş ekin yaprakları haline getirdi."
        ),
        SurahItem(
            "Kureyş Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nلِإِيلَافِ قُرَيْشٍ ۞ إِيلَافِهِمْ رِحْلَةَ الشِّتَاءِ وَالصَّيْفِ ۞ فَلْيَعْبُدُوا رَبَّ هَذَا الْبَيْتِ ۞ الَّذِي أَطْعَمَهُم مِّن جُوعٍ وَآمَنَهُم مِّنْ خَوْفٍ",
            "Bismillâhirrahmânirrahîm. Li'îlâfi Kurayş'in. Îlâfihim rihleteşşitâi vessayf. Felya'büdû rabbe hâzelbeyt. Ellezî et'amehüm min cû'in ve âmenehüm min havf.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Kureyş'i ısındırıp alıştırdığı; Onları kışın (Yemen'e) ve yazın (Şam'a) yaptıkları yolculuğa ısındırıp alıştırdığı için, Kureyş de, kendilerini besleyip açlıklarını gideren ve onları korkudan emin kılan bu evin (Kâbe'nin) Rabbine kulluk etsin."
        ),
        SurahItem(
            "Kevser Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nإِنَّا أَعْطَيْنَاكَ الْكَوْسَرَ ۞ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۞ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
            "Bismillâhirrahmânirrahîm. İnnâ a'taynâkel kevser. Fesalli lirabbike venhar. İnne şânieke hüvel ebter.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Şüphesiz biz sana Kevser'i verdik. O halde Rabbin için namaz kıl ve kurban kes. Asıl soyu kesik olan, sana buğzedendir."
        ),
        SurahItem(
            "İhlas Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ هُوَ اللَّهُ أَحَدٌ ۞ اللَّهُ الصَّمَدُ ۞ لَمْ يَلِدْ وَلَمْ يُولَدْ ۞ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
            "Bismillâhirrahmânirrahîm. Kul hüvellâhü ehad. Allâhüssamed. Lem yelid ve lem yûled. Ve lem yekün lehû küfüven ehad.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. De ki: O, Allah birdir. Allah sameddir. O, doğurmamış ve doğurulmamıştır. Onun hiçbir dengi yoktur."
        ),
        SurahItem(
            "Felak Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۞ مِن شَرِّ مَا خَلَقَ ۞ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۞ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۞ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            "Bismillâhirrahmânirrahîm. Kul e'ûzü birabbil felak. Min şerri mâ halak. Ve min şerri ğâsikın izâ vekab. Ve min şerrinneffâsâti fil'ukad. Ve min şerri hâsidin izâ hased.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. De ki: Yarattığı şeylerin şerrinden, karanlığı çöktüğü zaman gecenin şerrinden, düğümlere üfleyenlerin şerrinden, haset ettiği zaman hasetçinin şerrinden, sabah aydınlığının Rabbine sığınırım."
        ),
        SurahItem(
            "Nas Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۞ مَلِكِ النَّاسِ ۞ إِلَهِ النَّاسِ ۞ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۞ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۞ مِنَ الْجِنَّةِ وَالنَّاسِ",
            "Bismillâhirrahmânirrahîm. Kul e'ûzü birabbinnâs. Melikinnâs. İlâhinnâs. Min şerrilvesvâsilhannâs. Ellezî yüvesvisü fî sudûrinnâs. Minelcinneti vennâs.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. De ki: İnsanların kalplerine vesvese sokan, (insan Allah'ı andığında) pusuya çekilen cin ve insan şeytanının şerrinden insanların Rabbine, insanların Melikine (mutlak sahip ve hakimine) insanların İlahına sığınırım."
        ),
        SurahItem(
            "Ettehiyyâtü Duası",
            "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ ۞ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ ۞ السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ ۞ أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            "Ettehiyyâtü lillâhi vessalevâtü vettayyibât. Esselâmü aleyke eyyühen-Nebiyyü ve rahmetullâhi ve berakâtüh. Esselâmü aleynâ ve alâ ibâdillâhis-Sâlihîn. Eşhedü en lâ ilâhe illallâh ve eşhedü enne Muhammeden abdühû ve Rasûlüh.",
            "Her türlü hürmet, salavât (dua) ve bütün iyilikler Allah'a mahsustur. Ey Peygamber! Allah'ın rahmeti ve bereketleri senin üzerine olsun. Selâm bizlere ve Allah'ın sâlih kullarına olsun. Ben şahadet ederim ki, Allah'tan başka ilâh yoktur. Yine şahadet ederim ki, Muhammed O'nun kulu ve elçisidir."
        ),
        SurahItem(
            "Salli & Bârik Duaları",
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ ۞ اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
            "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed. Kemâ salleyte alâ İbrahîme ve alâ âli İbrahîm. İnneke hamîdün mecîd. Allâhümme bârik alâ Muhammedin ve alâ âli Muhammed. Kemâ bârakte alâ İbrahîme ve alâ âli İbrahîm. İnneke hamîdün mecîd.",
            "Allah'ım! İbrahim'e ve İbrahim'in âilesine rahmet ettiğin gibi Muhammed'e ve Muhammed'in âilesine de rahmet et. Şüphesiz sen övülmeye lâyık ve yücesin. Allah'ım! İbrahim'e ve İbrahim'in âilesine bereket ihsan ettiğin gibi Muhammed'e ve Muhammed'in âilesine de bereket ihsan et. Şüphesiz sen övülmeye lâyık ve yücesin."
        ),
        SurahItem(
            "Rabbenâ Duaları",
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ ۞ رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ",
            "Rabbenâ âtinâ fid'dünyâ haseneten ve fil'âhirati haseneten ve kınâ azâbennâr. Rabbenâğfirlî ve li-vâlideyye ve lil-Mü'minîne yevme yekûmü'l hisâb.",
            "Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi ateş azabından koru. Rabbimiz! Hesap kurulacağı gün beni, anamı, babamı ve müminleri bağışla."
        ),
        SurahItem(
            "Kunut Duaları",
            "اللَّهُمَّ إِنَّا نَسْتَعِينُكَ وَنَسْتَغْفِرُكَ وَنَسْتَهْدِيكَ وَنُؤْمِنُ بِكَ وَنَتُوبُ إِلَيْكَ... ۞ اللَّهُمَّ إِيَّاكَ نَعْبُدُ وَلَكَ نُصَلِّي وَنَسْجُدُ...",
            "Allâhümme innâ neste'înüke ve nestağfiruke ve nestehdîk. Ve nü'minü bike ve netûbü ileyk. Ve netevekkelü aleyke ve nüsnî aleykel-hayra küllehû neşkürüke ve lâ nekfüruk. Ve nahle'u ve netrükü men yefcüruk. Allâhümme iyyâke na'büdü ve leke nüsallî ve nescüd. Ve ileyke nes'â ve nahfid. Nercû rahmeteke ve nahşâ azâbeke, inne azâbeke bilküffâri mülhık.",
            "Allah'ım! Biz yalnız Senden yardım dileriz, bizi bağışlamanı ve doğru yola iletmeni isteriz. Sana iman ederiz, Sana tövbe ederiz... Allah'ım! Biz yalnız Sana kulluk ederiz. Yalnız Senin için namaz kılar ve Sana secde ederiz..."
        ),
        SurahItem(
            "Ayetü'l-Kürsi",
            "اللَّهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْdِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ وَلَا يَئُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            "Allâhü lâ ilâhe illâ hüvel hayyül kayyûm, lâ te'huzühû sinetün velâ nevm, lehû mâ fis semâvâti vemâ fil ard, men zellezî yeşfeu indehû illâ bi iznih, ya'lemü mâ beyne eydîhim vemâ halfehüm, velâ yuhîtûne bi şey'in min ilmihî illâ bimâ şâe, vesia kürsiyyühüs semâvâti vel arda, velâ yeûdühû hıfzuhümâ ve hüvel aliyyül azîm.",
            "Allah, O'ndan başka hiçbir ilâh yoktur. O hayydir, kayyûmdur. Kendisini ne bir uyuklama ne de bir uyku tutar. Göklerde ve yerde ne varsa hepsi O'nundur. O'nun izni olmadan katında kim şefaat edebilir? O, kullarının önlerinde ve arkalarında ne varsa hepsini bilir. Onlar ise O'nun dilediği kadarından başka ilminden hiçbir şey kavrayamazlar. O'nun kürsüsü bütün gökleri ve yeri kucaklamıştır. Gökleri ve yeri koruyup gözetmek O'na güç gelmez. O yücedir, büyüktür."
        ),
        SurahItem(
            "Kâfirûn Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ يَا أَيُّهَا الْكَافِرُونَ ۞ لَا أَعْبُدُ مَا تَعْبُدُونَ ۞ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُdُ ۞ وَلَا أَنَا عَابِدٌ مَّا عَبَدتُّمْ ۞ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ ۞ لَكُمْ دِينُكُمْ وَلِيَ دِينِ",
            "Bismillâhirrahmânirrahîm. Kul yâ eyyühel kâfirûn. Lâ a'büdü mâ ta'büdûn. Velâ entüm âbidûne mâ a'büd. Velâ ene âbidün mâ abedttüm. Velâ entüm âbidûne mâ a'büd. Leküm dînüküm veliye dîn.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. De ki: Ey kâfirler! Ben sizin tapmakta olduğunuz şeylere tapmam. Siz de benim tapmakta olduğuma tapıcılar değilsiniz. Ben sizin taptıklarınıza tapacak değilim. Siz de benim taptığıma tapacak değilsiniz. Sizin dininiz size, benim dinim banadır."
        ),
        SurahItem(
            "Maûn Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nأَرَأَيْتَ الَّذِي يُكَذِّبُ بِالدِّينِ ۞ فَذَلِكَ الَّذِي يَدُّعُ الْيَتِيمَ ۞ وَلَا يَحُضُّ عَلَى طَعَامِ الْمِسْكِينِ ۞ فَوَيْلٌ لِّلْمُصَلِّينَ ۞ الَّذِينَ هُمْ عَن صَلَاتِهِمْ سَاهُونَ ۞ الَّذِينَ هُمْ يُرَاؤُونَ ۞ وَيَمْنَعُونَ الْمَاعُونَ",
            "Bismillâhirrahmânirrahîm. Era'eytellezî yükezzibü biddîn. Fezâlikellezî yedü'ul yetîm. Velâ yehuddu alâ taâmil miskîn. Feveylün lil musallîn. Ellezîne hüm an salâtihim sâhûn. Ellezîne hüm yürâûn. Ve yemneûnel mâûn.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Gördün mü o dine (hesap ve ceza gününe) yalan diyeni? İşte o, yetimi itip kakan, yoksulu doyurmaya teşvik etmeyen kimsedir. Yazıklar olsun o namaz kılanlara ki, onlar namazlarını ciddiye almazlar. Onlar gösteriş yaparlar ve hayra da mani olurlar."
        ),
        SurahItem(
            "Tebbet Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nتَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ ۞ مَا أَغْنَى عَنْهُ مَالُهُ وَمَا كَسَبَ ۞ سَيَصْلَى نَارًا ذَاتَ لَهَبٍ ۞ وَامْرَأَتُهُ حَمَّالةَ الْحَطَبِ ۞ فِي جِيدِهَا حَبْلٌ mِّن مَّsَدٍ",
            "Bismillâhirrahmânirrahîm. Tebbet yedâ ebî lehebin ve tebbe. Mâ ağnâ anhü mâlühû vemâ keseb. Seyaslâ nâran zâte leheb. Vemraetühû hammâletel hatab. Fî cîdihâ hablün min mesed.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Ebu Leheb'in elleri kurusun; kurudu da! Ona ne malı fayda verdi ne de kazandığı. O, alevli bir ateşe girecektir. Odun taşıyıcı olarak karısı da; gerdanında hurma lifinden bükülmüş bir ip olduğu halde."
        ),
        SurahItem(
            "Nasr Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nإِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ ۞ وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا ۞ فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْfِيرْهُ إِنَّهُ كَانَ تَوَّابًا",
            "Bismillâhirrahmânirrahîm. İzâ câe nasrullâhi velfeth. Ve raeytennâse yedhulûne fî dînillâhi efvâcâ. Fesebbih bihamdi rabbike vestağfirh, innehû kâne tevvâbâ.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Allah'ın yardımı ve fetih geldiği, ve insanların dalga dalga Allah'ın dinine girdiklerini gördüğün zaman, hemen Rabbini hamd ile tesbih et ve O'ndan mağfiret dile. Çünkü O, tövbeleri çok kabul edendir."
        ),
        SurahItem(
            "Asr Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nوَالْعَصْرِ ۞ إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ۞ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ",
            "Bismillâhirrahmânirrahîm. Vel asr. İnnel insâne lefî husr. İllellezîne âmenû ve amilûs sâlihâti ve tevâsav bilhakkı ve tevâsav bissabr.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Asra yemin olsun ki, insan gerçekten ziyan içindedir. Ancak iman edip salih ameller işleyenler, birbirlerine hakkı tavsiye edenler ve birbirlerine sabrı tavsiye edenler müstesnadır."
        ),
        SurahItem(
            "Kadir Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nإِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ ۞ وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ ۞ لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ ۞ تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِم مِّن كُلِّ أَمْرٍ ۞ سَلَامٌ هِيَ حَتَّى مَطْلَعِ الْفَجْرِ",
            "Bismillâhirrahmânirrahîm. İnnâ enzelnâhü fî leyletil kadr. Vemâ edrâke mâ leyletül kadr. Leyletül kadri hayrün min elfi şehr. Tenezzelül melâiketü verrûhu fîhâ biizni rabbihim min külli emr. Selâmün hiye hattâ matla'ıl fecr.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Şüphesiz biz onu (Kur'an'ı) Kadir gecesinde indirdik. Kadir gecesinin ne olduğunu sen bilir misin? Kadir gecesi bin aydan daha hayırlıdır. Melekler ve Ruh (Cebrail) o gecede Rablerinin izniyle her türlü iş için inerler. O gece tan yeri ağarana kadar esenliktir."
        ),
        SurahItem(
            "İnşirah Suresi",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nأَلَمْ نَشْرَحْ لَكَ صَدْرَكَ ۞ وَوَضَعْنَا عَنكَ وِزْرَكَ ۞ الَّذِي أَنقَضَ ظَهْرَكَ ۞ وَرَفَعْنَا لَكَ ذِكْرَكَ ۞ فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۞ إِنَّ مَعَ الْعُسْرِ يُسْرًا ۞ فَإِذَا فَرَغْتَ فَانصَبْ ۞ وَإِلَى رَبِّكَ فَارْغَبْ",
            "Bismillâhirrahmânirrahîm. Elem neşrah leke sadrak. Ve vada'nâ anke vizrak. Ellezî enkada zahrak. Ve rafa'nâ leke zikrak. Feinne me'al usri yusrâ. İnne me'al usri yusrâ. Feizâ ferağte fensab. Ve ilâ rabbike ferğab.",
            "Rahmân ve Rahîm olan Allah'ın adıyla. Biz senin göğsünü açıp genişletmedik mi? Belini büken yükünü üzerinden indirmedik mi? Senin şanını ve ününü yüceltmedik mi? Şüphesiz zorlukla beraber bir kolaylık vardır. Gerçekten zorlukla beraber bir kolaylık vardır. Öyleyse, bir işi bitirince diğerine koyul. Ve yalnızca Rabbine yönel."
        )
    )

    var selectedSurahForDetail by remember { mutableStateOf<SurahItem?>(null) }
    androidx.activity.compose.BackHandler(enabled = selectedSurahForDetail != null) {
        selectedSurahForDetail = null
    }
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) }
    val isForcedDark by viewModel.forceDarkTheme.collectAsState()

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.setForceDarkTheme(false)
        }
    }

    val surah = selectedSurahForDetail
    if (surah != null) {
        val (arabicBesmele, restArabic) = splitArabicBesmele(surah.arabic)
        val (transBesmele, restTrans) = splitTransBesmele(surah.trans)
        val (meaningBesmele, restMeaning) = splitMeaningBesmele(surah.meaning)

        SubScreenScaffold(
            title = surah.title,
            onBack = { selectedSurahForDetail = null },
            actions = {
                IconButton(onClick = { viewModel.setForceDarkTheme(!isForcedDark) }) {
                    Icon(
                        imageVector = if (isForcedDark) androidx.compose.material.icons.Icons.Default.Brightness7 else androidx.compose.material.icons.Icons.Default.Brightness4,
                        contentDescription = "Tema Değiştir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawSimplePaperTexture(isForcedDark)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp), start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Arabic Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (arabicBesmele != null) {
                                Text(
                                    text = arabicBesmele,
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    lineHeight = 44.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = restArabic,
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 44.sp
                            )
                        }
                    }

                    // Pronunciation Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Okunuşu:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (transBesmele != null) {
                                Text(
                                    text = transBesmele,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = restTrans,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Translation Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Meali (Anlamı):",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (meaningBesmele != null) {
                                Text(
                                    text = meaningBesmele,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = restMeaning,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Actions Section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy Arabic
                            androidx.compose.material3.ElevatedButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(surah.arabic))
                                    android.widget.Toast.makeText(context, "Arapça Metin Kopyalandı!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Arapça Kopyala", fontSize = 12.sp)
                            }

                            // Copy Turkish
                            androidx.compose.material3.ElevatedButton(
                                onClick = {
                                    val copyText = "Okunuşu:\n${surah.trans}\n\nMeali:\n${surah.meaning}"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                                    android.widget.Toast.makeText(context, "Meal ve Okunuş Kopyalandı!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Meal Kopyala", fontSize = 12.sp)
                            }

                            // Share
                            androidx.compose.material3.FilledIconButton(
                                onClick = {
                                    val shareText = "✨ ${surah.title} ✨\n\n[Arapça]\n${surah.arabic}\n\n[Okunuşu]\n${surah.trans}\n\n[Anlamı]\n${surah.meaning}"
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    } else {
        SubScreenScaffold(title = "Namaz Sureleri & Duaları", onBack = onBack) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { }
                ) {
                    androidx.compose.material3.Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sureler", fontWeight = FontWeight.Bold) }
                    )
                    androidx.compose.material3.Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Dualar", fontWeight = FontWeight.Bold) }
                    )
                }
                
                val currentItems = if (selectedTab == 0) {
                    items.filter { it.title.contains("Suresi") || it.title.contains("Ayet") }
                } else {
                    items.filter { !it.title.contains("Suresi") && !it.title.contains("Ayet") }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, top = 12.dp, end = 24.dp,
                        bottom = com.example.ui.theme.navBarBottomPadding(extra = 12.dp))
                ) {
                    items(currentItems.size) { index ->
                        val surah = currentItems[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSurahForDetail = surah
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = surah.title, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (index < currentItems.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TesbihIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * 0.38f)
        val radius = size.width * 0.30f
        val beadCount = 8
        val beadRadius = size.width * 0.085f

        for (i in 0 until beadCount) {
            val angle = (i * 2 * Math.PI / beadCount) - (Math.PI / 2)
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            drawCircle(
                color = tint,
                radius = beadRadius,
                center = Offset(x, y)
            )
        }
        val bottomY = center.y + radius
        drawLine(
            color = tint,
            start = Offset(center.x, bottomY),
            end = Offset(center.x, size.height * 0.95f),
            strokeWidth = size.width * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

// --- SUB-SCREEN: Esmaül Hüsna ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EsmaUlHusnaScreen(
    viewModel: PrayerViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredNames = esmaUlHusnaList.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.meaning.contains(searchQuery, ignoreCase = true)
    }

    SubScreenScaffold(title = "Esma-ül Hüsna", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("İsim veya anlam ara...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                singleLine = true
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp,
                    bottom = com.example.ui.theme.navBarBottomPadding(extra = 12.dp)),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNames, key = { it.id }) { name ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Top Row: Number badge + Tesbih Action Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${name.id}",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    onClick = {
                                        viewModel.setDhikrTarget(name.name, name.arabic, name.count)
                                        android.widget.Toast.makeText(
                                            context,
                                            "${name.name} zikirmatik modülüne aktarıldı (${name.count} zikir)",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        onNavigate("dhikr")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TesbihIcon(
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Arabic Name
                            Text(
                                text = name.arabic,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Turkish Name
                            Text(
                                text = name.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Zikir Count badge with Tesbih Icon
                            Surface(
                                onClick = {
                                    viewModel.setDhikrTarget(name.name, name.arabic, name.count)
                                    android.widget.Toast.makeText(
                                        context,
                                        "${name.name} zikirmatik modülüne aktarıldı (${name.count} zikir)",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigate("dhikr")
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TesbihIcon(
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${name.count} Zikir",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Expanded details (meaning & benefit)
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Text(
                                        text = "Anlamı",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = name.meaning,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Text(
                                        text = "Fazileti",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = name.benefit,
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN: Holy Days (Dini Günler) ---
// --- SUB-SCREEN: Alarm Settings (Ezan Alarmları) ---
@Composable
fun AlarmSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE) }
    
    val prayers = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
    
    // Dialog target state: Pair<PrayerName, Is10Min>
    var activeSoundPickerTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var activeVibrationPickerTarget by remember { mutableStateOf<String?>(null) }
    
    val exactToggles = remember { mutableStateMapOf<String, Boolean>() }
    val exactSounds = remember { mutableStateMapOf<String, Int>() }
    val warningToggles = remember { mutableStateMapOf<String, Boolean>() }
    val warningSounds = remember { mutableStateMapOf<String, Int>() }
    val vibrationToggles = remember { mutableStateMapOf<String, Boolean>() }
    val vibrationPatterns = remember { mutableStateMapOf<String, Int>() }

    fun loadPrefs() {
        prayers.forEach { prayer ->
            exactToggles[prayer] = prefs.getBoolean("key_exact_enabled_$prayer", true)
            exactSounds[prayer] = prefs.getInt("key_exact_sound_$prayer", 2)
            warningToggles[prayer] = prefs.getBoolean("key_10min_enabled_$prayer", true)
            warningSounds[prayer] = prefs.getInt("key_10min_sound_$prayer", 5)
            vibrationToggles[prayer] = prefs.getBoolean("key_vibration_enabled_$prayer", true)
            vibrationPatterns[prayer] = prefs.getInt("key_vibration_pattern_$prayer", 0)
        }
    }

    LaunchedEffect(Unit) {
        loadPrefs()
    }

    SubScreenScaffold(title = "Ezan ve Bildirim Sesleri", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp,
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 20.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Her vakit için tam ezan vaktinde ve 10 dakika kala çalınacak bildirim seslerini özel olarak atayabilirsiniz. Ses butonlarına basarak atamadan önce dinleyebilirsiniz.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(prayers) { prayer ->
                val exactEnabled = exactToggles[prayer] ?: true
                val exactSoundId = exactSounds[prayer] ?: 2
                val exactSoundTitle = NotificationSoundManager.SOUND_OPTIONS.find { it.id == exactSoundId }?.title ?: "Manevi Takbir Melodisi"

                val warningEnabled = warningToggles[prayer] ?: true
                val warningSoundId = warningSounds[prayer] ?: 5
                val warningSoundTitle = NotificationSoundManager.SOUND_OPTIONS.find { it.id == warningSoundId }?.title ?: "Kubbeli Cami Zili"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$prayer Vakti Bildirimleri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // 1. Exact Time Notification Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tam Ezan Vaktinde Bildirim",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (exactEnabled) exactSoundTitle else "Sessiz / Kapalı",
                                        fontSize = 12.sp,
                                        color = if (exactEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                                Switch(
                                    checked = exactEnabled,
                                    onCheckedChange = { checked ->
                                        exactToggles[prayer] = checked
                                        prefs.edit().putBoolean("key_exact_enabled_$prayer", checked).apply()
                                        try { PrayerAlarmScheduler.scheduleAllPrayerAlarms(context) } catch (_: Exception) {}
                                    }
                                )
                            }

                            if (exactEnabled) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { activeSoundPickerTarget = Pair(prayer, false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Sesi Değiştir & Dinle ($exactSoundTitle)", fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. 10-Minute Warning Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "10 Dakika Kala Hatırlatıcı",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (warningEnabled) warningSoundTitle else "Sessiz / Kapalı",
                                        fontSize = 12.sp,
                                        color = if (warningEnabled) MaterialTheme.colorScheme.secondary else Color.Gray
                                    )
                                }
                                Switch(
                                    checked = warningEnabled,
                                    onCheckedChange = { checked ->
                                        warningToggles[prayer] = checked
                                        prefs.edit().putBoolean("key_10min_enabled_$prayer", checked).apply()
                                        try { PrayerAlarmScheduler.scheduleAllPrayerAlarms(context) } catch (_: Exception) {}
                                    }
                                )
                            }

                            if (warningEnabled) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { activeSoundPickerTarget = Pair(prayer, true) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Hatırlatma Sesini Seç & Dinle ($warningSoundTitle)", fontSize = 12.sp)
                                }
                            }
                        }

                        // 3. Vibration Settings Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val vibrationEnabled = vibrationToggles[prayer] ?: true
                            val vibrationPatternId = vibrationPatterns[prayer] ?: 0
                            val patternTitle = VibrationPatternManager.getPatternTitleById(vibrationPatternId)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Titreşimli Bildirim Uyarısı",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (vibrationEnabled) patternTitle else "Titreşim Kapalı",
                                        fontSize = 12.sp,
                                        color = if (vibrationEnabled) MaterialTheme.colorScheme.tertiary else Color.Gray
                                    )
                                }
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = { checked ->
                                        vibrationToggles[prayer] = checked
                                        prefs.edit().putBoolean("key_vibration_enabled_$prayer", checked).apply()
                                    }
                                )
                            }

                            if (vibrationEnabled) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { activeVibrationPickerTarget = prayer },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Titreşim Şeklini Seç & Test Et ($patternTitle)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sound Picker Dialog
        activeSoundPickerTarget?.let { (prayerName, is10Min) ->
            val currentSelectedSoundId = if (is10Min) (warningSounds[prayerName] ?: 5) else (exactSounds[prayerName] ?: 2)
            
            SoundSelectionDialog(
                title = if (is10Min) "$prayerName (10 Dk Kala) Sesi" else "$prayerName (Ezan Vakti) Sesi",
                initialSoundId = currentSelectedSoundId,
                onDismiss = { activeSoundPickerTarget = null },
                onSelectSound = { soundId ->
                    if (is10Min) {
                        warningSounds[prayerName] = soundId
                        prefs.edit().putInt("key_10min_sound_$prayerName", soundId).apply()
                    } else {
                        exactSounds[prayerName] = soundId
                        prefs.edit().putInt("key_exact_sound_$prayerName", soundId).apply()
                    }
                    activeSoundPickerTarget = null
                }
            )
        }

        // Vibration Pattern Picker Dialog
        activeVibrationPickerTarget?.let { prayerName ->
            val currentPatternId = vibrationPatterns[prayerName] ?: 0
            VibrationSelectionDialog(
                prayerName = prayerName,
                initialPatternId = currentPatternId,
                onDismiss = { activeVibrationPickerTarget = null },
                onSelectPattern = { patternId ->
                    vibrationPatterns[prayerName] = patternId
                    prefs.edit().putInt("key_vibration_pattern_$prayerName", patternId).apply()
                    activeVibrationPickerTarget = null
                }
            )
        }
    }
}

@Composable
fun SoundSelectionDialog(
    title: String,
    initialSoundId: Int,
    onDismiss: () -> Unit,
    onSelectSound: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(initialSoundId) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Dinlemek için istediğiniz sesin üzerindeki oynat butonuna basın veya ses seçimi yapın:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                NotificationSoundManager.SOUND_OPTIONS.forEach { sound ->
                    val isSelected = (sound.id == selectedId)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedId = sound.id
                                NotificationSoundManager.playSound(context, sound.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedId = sound.id
                                        NotificationSoundManager.playSound(context, sound.id)
                                    }
                                )
                                Column {
                                    Text(
                                        text = sound.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = sound.subtitle,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            androidx.compose.material3.FilledTonalIconButton(
                                onClick = {
                                    NotificationSoundManager.playSound(context, sound.id)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Dinle",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSelectSound(selectedId) }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun VibrationSelectionDialog(
    prayerName: String,
    initialPatternId: Int,
    onDismiss: () -> Unit,
    onSelectPattern: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(initialPatternId) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$prayerName Vakti Titreşim Şekli",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aşağıdaki titreşim ritimlerinden birini seçebilir, titreşim simgesine basarak cihazınızda test edebilirsiniz:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(VibrationPatternManager.PATTERN_OPTIONS) { patternOpt ->
                        val isSelected = patternOpt.id == selectedId
                        Surface(
                            onClick = {
                                selectedId = patternOpt.id
                                VibrationPatternManager.playTestVibration(context, patternOpt.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = patternOpt.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = patternOpt.description,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        VibrationPatternManager.playTestVibration(context, patternOpt.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = "Test Et",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSelectPattern(selectedId) }
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// --- SUB-SCREEN: Nearby Mosques (Yakındaki Camiler) ---

private fun getDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    try {
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    } catch (e: Exception) {
        return Float.MAX_VALUE
    }
}

private fun formatDistanceString(meters: Float): String {
    return if (meters < 1000f) {
        "${meters.toInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1000f)
    }
}

// NearbyMosquesScreen is modularized in NearbyMosquesScreen.kt

// --- SUB-SCREEN: Zakat Calculator (Zekat Hesaplama) ---
@Composable
fun ZakatCalcScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var gold by remember { mutableStateOf("") }
    var cash by remember { mutableStateOf("") }
    var silver by remember { mutableStateOf("") }
    var commercialGoods by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }

    var goldPrice by remember { mutableStateOf("3250") }
    var silverPrice by remember { mutableStateOf("38") }

    var isCalculated by remember { mutableStateOf(false) }
    var isEligible by remember { mutableStateOf(false) }
    var totalAssetsVal by remember { mutableStateOf(0.0) }
    var netAssetsVal by remember { mutableStateOf(0.0) }
    var nisabVal by remember { mutableStateOf(0.0) }
    var zakatAmountVal by remember { mutableStateOf(0.0) }

    var isLoadingPrices by remember { mutableStateOf(false) }
    var priceStatusMessage by remember { mutableStateOf<String?>(null) }

    fun parseAmount(input: String): Double {
        if (input.isBlank()) return 0.0
        val sanitized = input.trim()
            .replace(" ", "")
            .replace(",", ".")
        val parts = sanitized.split(".")
        return if (parts.size > 2) {
            val whole = parts.dropLast(1).joinToString("")
            val dec = parts.last()
            "$whole.$dec".toDoubleOrNull() ?: 0.0
        } else {
            sanitized.toDoubleOrNull() ?: 0.0
        }
    }

    fun fetchLivePrices() {
        isLoadingPrices = true
        priceStatusMessage = null
        scope.launch(Dispatchers.IO) {
            var fetchedGold: Double? = null
            var fetchedSilver: Double? = null

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // Source 1: FawazAhmed Currency API (jsDelivr CDN)
            try {
                val reqGold = okhttp3.Request.Builder()
                    .url("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/xau.json")
                    .header("User-Agent", "NamazVaktiApp/1.0")
                    .build()
                val resGold = client.newCall(reqGold).execute()
                if (resGold.isSuccessful) {
                    val jsonStr = resGold.body?.string() ?: ""
                    val json = org.json.JSONObject(jsonStr)
                    val xauObj = json.optJSONObject("xau")
                    val tryPricePerOz = xauObj?.optDouble("try", 0.0) ?: 0.0
                    if (tryPricePerOz > 0) {
                        fetchedGold = tryPricePerOz / 31.1034768
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                val reqSilver = okhttp3.Request.Builder()
                    .url("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/xag.json")
                    .header("User-Agent", "NamazVaktiApp/1.0")
                    .build()
                val resSilver = client.newCall(reqSilver).execute()
                if (resSilver.isSuccessful) {
                    val jsonStr = resSilver.body?.string() ?: ""
                    val json = org.json.JSONObject(jsonStr)
                    val xagObj = json.optJSONObject("xag")
                    val tryPricePerOz = xagObj?.optDouble("try", 0.0) ?: 0.0
                    if (tryPricePerOz > 0) {
                        fetchedSilver = tryPricePerOz / 31.1034768
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            // Source 2: Truncgil API
            if (fetchedGold == null || fetchedSilver == null) {
                try {
                    val req = okhttp3.Request.Builder()
                        .url("https://finans.truncgil.com/today.json")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val jsonStr = res.body?.string() ?: ""
                        val json = org.json.JSONObject(jsonStr)
                        val gObj = json.optJSONObject("gram-altin") ?: json.optJSONObject("Gram Altın")
                        val sObj = json.optJSONObject("gumus") ?: json.optJSONObject("Gümüş")
                        
                        if (fetchedGold == null && gObj != null) {
                            val s = gObj.optString("Selling", gObj.optString("Sera", "")).replace(".", "").replace(",", ".")
                            fetchedGold = s.toDoubleOrNull()
                        }
                        if (fetchedSilver == null && sObj != null) {
                            val s = sObj.optString("Selling", sObj.optString("Sera", "")).replace(".", "").replace(",", ".")
                            fetchedSilver = s.toDoubleOrNull()
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // Source 3: ER-API USD
            if (fetchedGold == null || fetchedSilver == null) {
                try {
                    val req = okhttp3.Request.Builder()
                        .url("https://open.er-api.com/v6/latest/USD")
                        .build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val jsonStr = res.body?.string() ?: ""
                        val json = org.json.JSONObject(jsonStr)
                        val rates = json.optJSONObject("rates")
                        val usdTry = rates?.optDouble("TRY", 36.5) ?: 36.5
                        if (fetchedGold == null) fetchedGold = 88.5 * usdTry
                        if (fetchedSilver == null) fetchedSilver = 1.05 * usdTry
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            val finalGold = fetchedGold ?: 3250.0
            val finalSilver = fetchedSilver ?: 38.0

            withContext(Dispatchers.Main) {
                goldPrice = String.format(Locale.US, "%.2f", finalGold)
                silverPrice = String.format(Locale.US, "%.2f", finalSilver)
                priceStatusMessage = "Canlı piyasa fiyatları yüklendi"
                isLoadingPrices = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLivePrices()
    }

    fun calculateZakat() {
        val goldGram = parseAmount(gold)
        val silverGram = parseAmount(silver)
        val cashVal = parseAmount(cash)
        val goodsVal = parseAmount(commercialGoods)
        val debtVal = parseAmount(debts)

        val gPrice = parseAmount(goldPrice).let { if (it <= 0) 3250.0 else it }
        val sPrice = parseAmount(silverPrice).let { if (it <= 0) 38.0 else it }

        val goldValue = goldGram * gPrice
        val silverValue = silverGram * sPrice
        val totalAssets = goldValue + silverValue + cashVal + goodsVal
        val netAssets = totalAssets - debtVal
        val nisabLimit = 80.18 * gPrice

        totalAssetsVal = totalAssets
        netAssetsVal = netAssets
        nisabVal = nisabLimit

        if (netAssets >= nisabLimit) {
            isEligible = true
            zakatAmountVal = netAssets * 0.025
        } else {
            isEligible = false
            zakatAmountVal = 0.0
        }
        isCalculated = true
    }

    SubScreenScaffold(title = strings.zakatTitle, onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp,
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 20.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = strings.zakatDesc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
            }

            // Live Prices Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = priceStatusMessage ?: "Piyasa fiyatları güncelleniyor...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Canlı serbest piyasa altın ve gümüş gram kurları",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                        OutlinedButton(
                            onClick = { fetchLivePrices() },
                            enabled = !isLoadingPrices,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoadingPrices) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Yenile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Gram Price Inputs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = goldPrice,
                        onValueChange = { goldPrice = it },
                        label = { Text(strings.goldGramPrice, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = silverPrice,
                        onValueChange = { silverPrice = it },
                        label = { Text(strings.silverGramPrice, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Asset Inputs
            item {
                OutlinedTextField(
                    value = gold,
                    onValueChange = { gold = it },
                    label = { Text("Altın Miktarı (Gram)") },
                    placeholder = { Text("Örn: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = silver,
                    onValueChange = { silver = it },
                    label = { Text("Gümüş Miktarı (Gram)") },
                    placeholder = { Text("Örn: 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = cash,
                    onValueChange = { cash = it },
                    label = { Text("Nakit Para & Banka Birikimi (TL)") },
                    placeholder = { Text("Örn: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = commercialGoods,
                    onValueChange = { commercialGoods = it },
                    label = { Text("Ticari Mal & Alacaklar (TL)") },
                    placeholder = { Text("Örn: 20000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = debts,
                    onValueChange = { debts = it },
                    label = { Text("Borçlar & Düşülecek Giderler (TL)") },
                    placeholder = { Text("Örn: 10000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Button(
                    onClick = { calculateZakat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Zekat Hesapla", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isCalculated) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEligible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isEligible) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Zekat Hesaplama Özeti",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Toplam Brüt Varlık:", fontSize = 13.sp)
                                Text("%.2f TL".format(totalAssetsVal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Düşülen Borçlar:", fontSize = 13.sp)
                                Text("- %.2f TL".format(parseAmount(debts)), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net Zekata Tabi Varlık:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("%.2f TL".format(netAssetsVal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nisab Eşiği (80.18 gr Altın):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text("%.2f TL".format(nisabVal), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                            if (isEligible) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Zekat Vermeniz Gerekmektedir",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ödenmesi Gereken Zekat (%2.5 / 1/40):",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "%.2f TL".format(zakatAmountVal),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Varlığınız Nisab miktarının (%.2f TL) altında kaldığından zekat yükümlülüğünüz bulunmamaktadır.".format(nisabVal),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SHARED SCAFFOLD FOR SUB-SCREENS ---
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    androidx.activity.compose.BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

// --- SUB-SCREEN: 5 - Kuran Fihristi ---

@Composable
fun QuranIndexScreen(viewModel: PrayerViewModel, onBack: () -> Unit) {
    KeepScreenOn()
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { QuranAiSearchEngine.release() }
    }
    QuranIndexSearchContent(
        onBack = onBack,
        onOpenVerse = { surahId, verseNumber -> viewModel.setTargetQuranVerse(surahId, verseNumber) },
        search = { query -> QuranAiSearchEngine.searchQuranWithAi(query = query, context = context) }
    )
}

// --- SUB-SCREEN: 12 - Hadis Kütüphanesi ---

@Composable
fun HadithLibraryScreen(viewModel: PrayerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val favoriteHadiths by viewModel.favoriteHadithsFlow.collectAsState()
    val memorizedHadiths by viewModel.memorizedHadithsFlow.collectAsState()

    val allHadiths = remember { com.example.data.HadithData.allHadiths }
    val categories = remember { com.example.data.HadithData.categories }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Hepsi") }

    // 0: Tüm Hadisler, 1: Favorilerim, 2: Ezberlediklerim
    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Hadith of the Day (deterministic daily seed with manual shuffle option)
    var dailyHadithIndex by remember {
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        androidx.compose.runtime.mutableIntStateOf(if (allHadiths.isNotEmpty()) dayOfYear % allHadiths.size else 0)
    }
    val dailyHadith = allHadiths.getOrElse(dailyHadithIndex) { allHadiths.first() }

    // Expanded Hadith explanations map
    val expandedExplanations = remember { mutableStateMapOf<String, Boolean>() }

    // Card generator state
    var showCardDialogForHadith by remember { mutableStateOf<com.example.data.Hadith?>(null) }

    val activeHadiths = remember(searchQuery, selectedCategory, selectedTab, favoriteHadiths, memorizedHadiths, allHadiths) {
        val q = searchQuery.trim().lowercase(Locale("tr", "TR"))
        allHadiths.filter { hadith ->
            val matchesSearch = q.isEmpty() ||
                hadith.text.lowercase(Locale("tr", "TR")).contains(q) ||
                hadith.source.lowercase(Locale("tr", "TR")).contains(q) ||
                hadith.narrator.lowercase(Locale("tr", "TR")).contains(q) ||
                hadith.explanation.lowercase(Locale("tr", "TR")).contains(q)

            val matchesCategory = when (selectedCategory) {
                "Hepsi", "Tümü" -> true
                "40 Hadis" -> hadith.isArbain
                else -> hadith.category == selectedCategory
            }

            val matchesTab = when (selectedTab) {
                0 -> true
                1 -> favoriteHadiths.contains(hadith.id)
                2 -> memorizedHadiths.contains(hadith.id)
                else -> true
            }

            matchesSearch && matchesCategory && matchesTab
        }
    }

    fun pickRandomHadith() {
        if (allHadiths.size > 1) {
            var nextIdx = kotlin.random.Random.nextInt(allHadiths.size)
            if (nextIdx == dailyHadithIndex) {
                nextIdx = (nextIdx + 1) % allHadiths.size
            }
            dailyHadithIndex = nextIdx
        }
    }

    val memorizedRatio = remember(memorizedHadiths.size, allHadiths.size) {
        if (allHadiths.isEmpty()) 0f else (memorizedHadiths.size.toFloat() / allHadiths.size.toFloat()).coerceIn(0f, 1f)
    }

    SubScreenScaffold(title = "Hadis Külliyatı & 40 Hadis", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Tabs Sticky Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Memorization Progress Strip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hadis Ezber & Okuma İlerlemesi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${memorizedHadiths.size} / ${allHadiths.size} Ezberlendi (%${(memorizedRatio * 100).toInt()})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { memorizedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Hadis metni, râvî, kaynak veya hikmette ara...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Tüm, Favoriler, Ezberlediklerim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("Tüm Hadisler", "Favorilerim", "Ezberlediklerim")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val count = when (index) {
                            0 -> allHadiths.size
                            1 -> favoriteHadiths.size
                            2 -> memorizedHadiths.size
                            else -> 0
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "($count)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Horizontal Scroll
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Hadith List & Custom Top Banner
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
                    bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show "Hadith of the Day" Banner on "All" tab when search is empty
                if (selectedTab == 0 && selectedCategory == "Hepsi" && searchQuery.isEmpty()) {
                    item(key = "daily_hadith_banner") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Günün Hadis-i Şerifi",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (dailyHadith.narrator.isNotBlank()) {
                                                Text(
                                                    text = "Râvî: ${dailyHadith.narrator} • ${dailyHadith.authenticity}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { pickRandomHadith() },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Başka Bir Hadis Getir",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (dailyHadith.arabicText.isNotEmpty()) {
                                    Text(
                                        text = dailyHadith.arabicText,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 28.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text(
                                    text = "“${dailyHadith.text}”",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dailyHadith.source,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                NotesStore.addQuickNote(
                                                    context = context,
                                                    title = "Hadis-i Şerif (${dailyHadith.category})",
                                                    content = "${dailyHadith.arabicText}\n\n“${dailyHadith.text}”\n\n🎙️ Râvî: ${dailyHadith.narrator}\n📚 Kaynak: ${dailyHadith.source}\n\n💡 Hikmeti:\n${dailyHadith.explanation}",
                                                    category = "Ayet & Hadis"
                                                )
                                                Toast.makeText(context, "Hadis not defterine eklendi", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = "Notlara Kaydet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("${dailyHadith.text}\n(${dailyHadith.source})"))
                                                Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                showCardDialogForHadith = dailyHadith
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Görsel Kart", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "daily_hadith_divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }

                if (activeHadiths.isEmpty()) {
                    item(key = "empty_hadiths_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Eşleşen hadis bulunamadı.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Arama kriterlerinizi veya kategori filtrelerinizi değiştirerek tekrar deneyin.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(activeHadiths, key = { it.id }) { hadith ->
                        val isFav = favoriteHadiths.contains(hadith.id)
                        val isMem = memorizedHadiths.contains(hadith.id)
                        val isExpanded = expandedExplanations[hadith.id] ?: false

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Top Meta Info: Category + 40 Hadis Badge + Authenticity Grade
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = hadith.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (hadith.isArbain) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFD97706).copy(alpha = 0.16f))
                                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "40 Hadis #${hadith.arbainNumber}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.14f))
                                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = hadith.authenticity,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Ezberledim (Mark as Memorized) Toggle
                                        IconButton(
                                            onClick = { viewModel.toggleMemorizedHadith(hadith.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMem) Icons.Default.CheckCircle else Icons.Default.Check,
                                                contentDescription = "Ezberledim",
                                                tint = if (isMem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Favorilere Ekle Toggle
                                        IconButton(
                                            onClick = { viewModel.toggleFavoriteHadith(hadith.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorilere Ekle",
                                                tint = if (isFav) Color(0xFFEF4444) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Görsel Kart Tasarla Button
                                        IconButton(
                                            onClick = { showCardDialogForHadith = hadith },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Kart Tasarla",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Arabic Text
                                if (hadith.arabicText.isNotEmpty()) {
                                    Text(
                                        text = hadith.arabicText,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth(),
                                        lineHeight = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Turkish Meaning
                                Text(
                                    text = "“${hadith.text}”",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Narrator & Source Book Row
                                if (hadith.narrator.isNotBlank()) {
                                    Text(
                                        text = "🎙️ Râvî: ${hadith.narrator}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📚 ${hadith.source}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Save to Straw-Paper Notes
                                        IconButton(
                                            modifier = Modifier.size(32.dp),
                                            onClick = {
                                                NotesStore.addQuickNote(
                                                    context = context,
                                                    title = "Hadis: ${hadith.category} (${hadith.narrator.ifBlank { hadith.source }})",
                                                    content = "${hadith.arabicText}\n\n“${hadith.text}”\n\n🎙️ Râvî: ${hadith.narrator}\n📚 Kaynak: ${hadith.source}\n\n💡 Hikmet & Şerh:\n${hadith.explanation}",
                                                    category = "Ayet & Hadis"
                                                )
                                                Toast.makeText(context, "Hadis not defterine kaydedildi", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = "Notlara Kaydet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }

                                        // Quick Copy
                                        IconButton(
                                            modifier = Modifier.size(32.dp),
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("${hadith.arabicText}\n\n“${hadith.text}”\n(${hadith.narrator} - ${hadith.source})"))
                                                Toast.makeText(context, "Hadis panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        }

                                        // Quick Share
                                        IconButton(
                                            modifier = Modifier.size(32.dp),
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "${hadith.arabicText}\n\n“${hadith.text}”\n\n- ${hadith.narrator} (${hadith.source})")
                                                }
                                                runCatching {
                                                    context.startActivity(Intent.createChooser(intent, "Hadis Paylaş"))
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }

                                // Explanation / Şerh & Hikmet section
                                if (hadith.explanation.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedExplanations[hadith.id] = !isExpanded }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💡 Hadis-i Şerif Şerhi & Hikmeti",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .rotate(if (isExpanded) 90f else 0f)
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = hadith.explanation,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                                lineHeight = 19.sp,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Custom Dialog for Hadith Visual Card Designer
    showCardDialogForHadith?.let { hadith ->
        var selectedCardTheme by remember { mutableStateOf(HadithCardTheme.EMERALD_GOLD) }
        var includeArabicText by remember { mutableStateOf(hadith.arabicText != null) }
        var cardTextSizeSp by remember { mutableStateOf(16f) }
        
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCardDialogForHadith = null }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Görsel Kart Tasarımcısı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    
                    // --- LIVE COMPOSE PREVIEW ---
                    Text(
                        text = "Kart Önizleme (Kare Format)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    
                    // Simulate the generated card aspect & design in Compose
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(selectedCardTheme.startColor)),
                                        Color(android.graphics.Color.parseColor(selectedCardTheme.endColor))
                                    )
                                )
                            )
                            .border(
                                width = 3.dp,
                                color = Color(android.graphics.Color.parseColor(selectedCardTheme.frameColor)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        // Inner frame border line
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.dp,
                                    color = Color(android.graphics.Color.parseColor(selectedCardTheme.frameColor)).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // "Hadis-i Şerif" Banner
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(android.graphics.Color.parseColor(selectedCardTheme.frameColor)))
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HADİS-İ ŞERİF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(selectedCardTheme.endColor))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Arabic Text (if requested)
                            if (includeArabicText && !hadith.arabicText.isNullOrEmpty()) {
                                Text(
                                    text = hadith.arabicText,
                                    fontSize = (cardTextSizeSp + 3f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(selectedCardTheme.accentColor)),
                                    textAlign = TextAlign.Center,
                                    lineHeight = (cardTextSizeSp + 8f).sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            
                            // Turkish Text
                            Text(
                                text = "“${hadith.text}”",
                                fontSize = cardTextSizeSp.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(android.graphics.Color.parseColor(selectedCardTheme.textColor)),
                                textAlign = TextAlign.Center,
                                lineHeight = (cardTextSizeSp + 5f).sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Source
                            Text(
                                text = hadith.source,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(android.graphics.Color.parseColor(selectedCardTheme.accentColor)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- Theme Selector ---
                    Text(
                        text = "Arka Plan Teması",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HadithCardTheme.values().forEach { t ->
                            val isSelected = selectedCardTheme == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(t.startColor)))
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCardTheme = t }
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // --- Settings Controls ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arapça Metni Göster", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = includeArabicText && !hadith.arabicText.isNullOrEmpty(),
                            onCheckedChange = { includeArabicText = it },
                            enabled = !hadith.arabicText.isNullOrEmpty()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Yazı Boyutu", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${cardTextSizeSp.toInt()} sp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = cardTextSizeSp,
                            onValueChange = { cardTextSizeSp = it },
                            valueRange = 12f..24f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- Action Buttons ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCardDialogForHadith = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("İptal", fontSize = 14.sp)
                        }
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    HadithCardGenerator.shareHadithCard(
                                        context = context,
                                        hadith = hadith,
                                        theme = selectedCardTheme,
                                        textSizeSp = cardTextSizeSp,
                                        includeArabic = includeArabicText
                                    )
                                    showCardDialogForHadith = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kartı Paylaş", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN: 16 - Kuran Okuma ve Tefsir Karşılaştırma ---
@androidx.compose.runtime.Immutable
data class SurahVerse(
    val number: Int,
    val arabic: String,
    val pronunciation: String,
    val diyanetMeal: String,
    val elmaliliMeal: String,
    val vakfiMeal: String,
    val diyanetTefsir: String,
    val bilmenTefsir: String,
    val selectedMeal: String = "",
    val mealsMap: Map<String, String> = emptyMap(),
    val footnotes: String = "",
    val footnotesMap: Map<String, String> = emptyMap()
)

@androidx.compose.runtime.Immutable
data class Surah(
    val id: Int, // Kuran Sırası
    val name: String,
    val revelationOrder: Int, // İniş Sırası
    val revelationPlace: String, // Mekke / Medine
    val verseCount: Int,
    val verses: List<SurahVerse>
)

@androidx.compose.runtime.Immutable
data class GroupedVerseItem(
    val startVerseNumber: Int,
    val endVerseNumber: Int,
    val verses: List<SurahVerse>,
    val mainVerse: SurahVerse,
    val mealText: String
) {
    val numberLabel: String get() = if (startVerseNumber == endVerseNumber) "$startVerseNumber." else "$startVerseNumber-$endVerseNumber."
}

fun groupSurahVerses(verses: List<SurahVerse>, edKey: String?, disableGrouping: Boolean = false): List<GroupedVerseItem> {
    if (verses.isEmpty()) return emptyList()
    if (disableGrouping) {
        return verses.map { verse ->
            val rawMeal = if (edKey != null) verse.mealsMap[edKey].orEmpty() else verse.selectedMeal.ifBlank { verse.diyanetMeal }
            GroupedVerseItem(
                startVerseNumber = verse.number,
                endVerseNumber = verse.number,
                verses = listOf(verse),
                mainVerse = verse,
                mealText = rawMeal.trim()
            )
        }
    }
    val result = mutableListOf<GroupedVerseItem>()
    var currentGroup = mutableListOf<SurahVerse>()
    var currentMealText: String? = null

    for (verse in verses) {
        val rawMeal = if (edKey != null) {
            verse.mealsMap[edKey].orEmpty()
        } else {
            verse.selectedMeal.ifBlank { verse.diyanetMeal }
        }
        val trimmedMeal = rawMeal.trim()

        if (currentGroup.isEmpty()) {
            currentGroup.add(verse)
            currentMealText = trimmedMeal
        } else if (trimmedMeal.isNotEmpty() && trimmedMeal == currentMealText) {
            currentGroup.add(verse)
        } else {
            result.add(
                GroupedVerseItem(
                    startVerseNumber = currentGroup.first().number,
                    endVerseNumber = currentGroup.last().number,
                    verses = ArrayList(currentGroup),
                    mainVerse = currentGroup.first(),
                    mealText = currentMealText ?: ""
                )
            )
            currentGroup = mutableListOf(verse)
            currentMealText = trimmedMeal
        }
    }
    if (currentGroup.isNotEmpty()) {
        result.add(
            GroupedVerseItem(
                startVerseNumber = currentGroup.first().number,
                endVerseNumber = currentGroup.last().number,
                verses = ArrayList(currentGroup),
                mainVerse = currentGroup.first(),
                mealText = currentMealText ?: ""
            )
        )
    }
    return result
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranReadingScreen(viewModel: PrayerViewModel, onBack: () -> Unit) {
    KeepScreenOn()
    val isForcedDark by viewModel.forceDarkTheme.collectAsState()
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.setForceDarkTheme(false)
        }
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val surahs = remember {
        val predefined = listOf(
            Surah(
                id = 1,
                name = "Fâtiha Suresi",
                revelationOrder = 5,
                revelationPlace = "Mekke",
                verseCount = 7,
                verses = listOf(
                    SurahVerse(
                        1,
                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        "Bismillâhirrahmânirrahîm.",
                        "Rahmân ve Rahîm olan Allah'ın adıyla.",
                        "Rahmân ve Rahîm olan Allah'ın adıyla.",
                        "Rahmân ve Rahîm olan Allah'ın ismiyle (başlarım).",
                        "Besmele, Kuran'ın anahtarıdır. Yaratıcının adıyla başlamak her hayırlı işin başıdır. Kuran tilavetine ve her meşru işe Besmele ile başlanması ilahi bir terbiyedir.",
                        "Her hayırlı amelin başında besmele çekilmesi, o amelin bereketine ve kabulüne vesiledir. Zat-ı Kibriya'nın isimleriyle teberrük etmek her müminin vazifesidir."
                    ),
                    SurahVerse(
                        2,
                        "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                        "Elhamdu lillâhi rabbi-l'âlemîn(e).",
                        "Hamd, âlemlerin Rabbi olan Allah'a mahsustur.",
                        "Hamd, göklerin ve yerin Rabbi olan Allah'adır.",
                        "Bütün hamdler, alemlerin terbiyecisi olan Allah'a mahsustur.",
                        "Hamd, Allah'ın yarattığı nimetlere şükretmek ve O'nun yüceliğini övmektir. O, bütün varlık aleminin tek hakimi, rızıklandırıcısı ve yöneticisidir.",
                        "Hamd, yalnızca bütün kainatın yaratıcısı ve yöneticisi olan Allah'a aittir. Yaratılmışların övgüsü nihayetinde O'nun kemaline ve lütfuna döner."
                    ),
                    SurahVerse(
                        3,
                        "الرَّحْمَٰنِ الرَّحِيمِ",
                        "Errahmâni-rrahîm(i).",
                        "O, Rahmân'dır, Rahîm'dir.",
                        "O, merhametlidir, bağışlayıcıdır.",
                        "O, son derece merhametli ve lütufkardır.",
                        "Rahmân dünyada tüm yaratıklara (inanan inanmayan ayırt etmeksizin) merhamet eden, Rahîm ise ahirette sadece müminlere hususi merhamet edendir.",
                        "Rahmân sıfatı Allah'a has bir isimdir ve mutlak rahmeti ifade eder. Rahîm ise şefkat ve lütfun sürekliliğini ve müminlere yönelik özel ihsanları simgeler."
                    ),
                    SurahVerse(
                        4,
                        "مَالِكِ يَوْمِ الدِّينِ",
                        "Mâliki yevmi-ddîn(i).",
                        "Hesap ve ceza gününün (ahiret gününün) malikidir.",
                        "Din gününün sahibidir.",
                        "Ceza ve mükafat gününün yegane hakimidir.",
                        "Din günü, insanların hesap vermek üzere toplanacağı ahiret günüdür. Malikiyet ise o gün mutlak gücün, hükmün ve egemenliğin sadece Allah'ta olacağını belirtir.",
                        "Ahiret gününün egemenliği sadece Cenab-ı Hakk'a mahsustur. O gün dünyadaki geçici otoriteler son bulur ve adalet-i ilahiye tecelli eder."
                    ),
                    SurahVerse(
                        5,
                        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                        "İyyâke na'budu ve-iyyâke neste'în(u).",
                        "Yalnız sana ibadet eder ve yalnız senden yardım dileriz.",
                        "Ancak sana kulluk eder ve ancak senden yardım dileriz.",
                        "Biz ancak sana kulluk ederiz ve ancak senden yardım isteriz.",
                        "Kulluk ve ibadet sadece Allah'adır. Yardım da sadece O'ndan istenir. Bu tevhidin özüdür. Mümin her türlü aracı ve şirki reddederek doğrudan Rabbine yönelir.",
                        "Şirki reddeden ve yalnızca Yaratıcıya yönelmeyi emreden en temel ayettir. İbadet, boyun eğmenin ve sevginin en yüksek zirvesidir ve sadece Allah'a layıktır."
                    ),
                    SurahVerse(
                        6,
                        "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                        "İhdinâ-ssırâta-lmustekîm(e).",
                        "Bizi doğru yola ilet.",
                        "Bizi doğru yola hidayet eyle.",
                        "Bizi o dosdoğru caddede sabit kıl.",
                        "Sırat-ı Müstakim, İslam dinidir. Doğruluk, adalet ve dürüstlük yoludur. Mümin her namazda bu hidayet üzere kalmayı talep eder.",
                        "Doğru yola ulaştırmak ve o yolda daim kılmak hidayetin kendisidir. Bu yol, ifrat ve tefritten uzak, orta ve dengeli bir yaşamı temsil eder."
                    ),
                    SurahVerse(
                        7,
                        "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
                        "Sırâtallezîne en'amte 'aleyhim gayri-lmağdûbi 'aleyhim veladdâllîn(e).",
                        "Kendilerine nimet verdiklerinin yoluna; gazaba uğramışların ve sapmışların yoluna değil.",
                        "Nimet verdiğin kimselerin yoluna ilet; gazaba uğramışların ve sapmışların yoluna değil.",
                        "O kimselerin yoluna ki, onlara nimet verdin; gazaba uğrayanların ve sapanlarınkine değil.",
                        "Nimet verilenler peygamberler, sıddıklar, şehitler ve salih kullardır. Gazaba uğrayanlar ve sapmışlar ise hakikati bilip de sapanlar veya cehaletle doğrudan uzaklaşanlardır.",
                        "Hidayet nimetini doğru kullananların yolu selamet ve saadet yoludur. Gazap ve dalalet yolları ise kibir, haset ve cehaletle hakikati inkar edenlerin akıbetidir."
                    )
                )
            ),
            Surah(
                id = 94,
                name = "İnşirah Suresi",
                revelationOrder = 12,
                revelationPlace = "Mekke",
                verseCount = 8,
                verses = listOf(
                    SurahVerse(
                        1,
                        "أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ",
                        "Elem neşrah leke sadrak.",
                        "Biz senin göğsünü açıp genişletmedik mi?",
                        "Biz senin göğsünü açıp genişletmedik mi?",
                        "Biz senin için göğsünü genişletmedik mi?",
                        "Gönlün açılması, nübüvvet yükünü taşıyacak manevi bir ferahlık, genişlik ve bilgelik verilmesidir. Sıkıntıların giderilip yerini ferahlığa bırakmasıdır.",
                        "Sadrın inşirahı, ilahi feyizler, marifet ve nurlar ile kalbin huzur bulması, daralmaktan kurtulup tebliğ görevini göğüsleyecek güce erişmesidir."
                    ),
                    SurahVerse(
                        2,
                        "وَوَضَعْنَا عَنْكَ وِزْرَكَ",
                        "Ve veda'nâ 'anke vizrak.",
                        "Belini büken yükünü senden indirmedik mi?",
                        "Sırtından yükünü indirmedik mi?",
                        "Ve senden ağır yükünü indirmedik mi?",
                        "Peygamberlik görevinin getirdiği ağır sorumluluk, beşeriyet yükü ve risaletin ilk dönemlerindeki zorlukların hafifletilmesini ifade eder.",
                        "Sorumlulukların getirdiği manevi ağırlığın ilahi yardım ile kolaylaştırılması, geçmişteki beşeri sıkıntıların affedilip unutturulmasıdır."
                    ),
                    SurahVerse(
                        3,
                        "الَّذِي أَنْقَضَ ظَهْرَكَ",
                        "Ellezî enkada zahrak.",
                        "O yük ki senin belini bükmüştü.",
                        "Belini büken o ağır yükünü.",
                        "O yük ki, senin belini bükmüştü.",
                        "Yükün ağırlığı, ümmetin hidayeti için duyulan derin endişe, tebliğin zorluğu ve inkarcıların baskısıyla omuzlara binen büyük manevi yüktür.",
                        "Beşeri takati aşan ağır tebliğ vazifesinin manevi yükü ve ümmetin geleceğine dair taşınan derin kaygılardır."
                    ),
                    SurahVerse(
                        4,
                        "وَرَفَعْنَا لَكَ ذِكْرَكَ",
                        "Ve rafa'nâ leke zikrak.",
                        "Senin şanını yüceltmedik mi?",
                        "Senin şanını yükseltmedik mi?",
                        "Ve senin için şanını yükseltmedik mi?",
                        "Kelime-i şehadette, ezanda ve dualarda Peygamberimizin isminin Allah'ın ismiyle beraber anılarak dünya ve ahirette en yüce mertebeye çıkarılmasıdır.",
                        "Resulullah'ın adının kıyamete kadar hayırla anılması, melekler katında ve müminler gönlünde şanının en müstesna mertebeye yükseltilmesidir."
                    ),
                    SurahVerse(
                        5,
                        "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                        "Fe-inne me'al 'usri yusrâ.",
                        "Şüphesiz kolaylıkla beraber bir zorluk vardır.",
                        "Demek ki zorlukla beraber bir kolaylık vardır.",
                        "Muhakkak zorlukla beraber bir kolaylık vardır.",
                        "Hayattaki her sıkıntının arkasında veya onunla beraber mutlaka ilahi bir kolaylık mevcuttur. Zorluklar kalıcı değildir, sabırla aşılır.",
                        "Sıkıntıların sonsuza kadar sürmeyeceğini, her dar boğazın arkasından mutlaka genişlik ve ferahlığın geleceğini vaat eden müjdedir."
                    ),
                    SurahVerse(
                        6,
                        "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                        "İnne me'al 'usri yusrâ.",
                        "Şüphesiz, zorlukla beraber bir kolaylık vardır.",
                        "Gerçekten zorlukla beraber bir kolaylık vardır.",
                        "Şüphesiz zorlukla beraber bir kolaylık vardır.",
                        "Zorluğun bir, kolaylığın iki defa zikredilmesi, kolaylığın zorluğa mutlaka galip geleceğini ve her zor anın bir çıkışı olduğunu gösterir.",
                        "Umutsuzluğa asla yer olmadığını teyit eden, müminin azmini tazeleyen mükerrer ve tekitli bir ilahi vaattir."
                    ),
                    SurahVerse(
                        7,
                        "فَإِذَا فَرَغْتَ فَانْصَبْ",
                        "Fe-izâ feragte fensab.",
                        "Boş kaldın mı hemen (başka bir işe) koyul.",
                        "O halde boş kaldığında hemen yorul (başka işe başla).",
                        "O halde boş vakit bulunca hemen ibadete koyul.",
                        "Bir hayırlı işi bitirince durmaksızın diğer bir hayırlı amele, toplumsal faydaya ve ibadete yönelmek müminin hayat düsturudur.",
                        "Müminin hayatında atalete (boş durmaya, vakit öldürmeye) yer olmadığının, her anın hayır ve gayretle değerlendirilmesi gerektiğinin beyanıdır."
                    ),
                    SurahVerse(
                        8,
                        "وَإِلَىٰ رَبِّكَ فَارْغَبْ",
                        "Ve ilâ rabbike fergab.",
                        "Yalnızca Rabbine yönel.",
                        "Ve yalnız Rabbine rağbet et.",
                        "Ve ancak Rabbine yönel.",
                        "Her amelde niyet sadece Allah'ın rızasını kazanmak olmalıdır. Beklentiler insanlardan değil, sadece Yaratıcıdan olmalıdır.",
                        "Manevi teveccühün, arzuların ve taleplerin sadece Cenab-ı Hakk'a yapılması, ihlasın zirvesine uluşulması emredilmektedir."
                    )
                )
            ),
            Surah(
                id = 97,
                name = "Kadir Suresi",
                revelationOrder = 25,
                revelationPlace = "Mekke",
                verseCount = 5,
                verses = listOf(
                    SurahVerse(
                        1,
                        "إِنَّا أَنْزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ",
                        "İnnâ enzelnâhü fî leyletil kadr.",
                        "Şüphesiz biz onu Kadir gecesinde indirdik.",
                        "Biz onu Kadir gecesinde indirdik.",
                        "Şüphesiz biz Kuran'ı Kadir gecesinde indirdik.",
                        "Kuran-ı Kerim'in indiriliş süreci Kadir gecesinde başlamıştır. Bu gece insanlığın hidayet rehberiyle buluştuğu mübarek gecedir.",
                        "Kuran-ı Kerim'in levh-i mahfuzdan dünya semasına (beytü'l-izze) toptan indirildiği şerefli zamandır."
                    ),
                    SurahVerse(
                        2,
                        "وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ",
                        "Ve mâ edrâke mâ leyletül kadr.",
                        "Kadir gecesinin ne olduğunu sen bilir misin?",
                        "Kadir gecesinin ne olduğunu sen nereden bileceksin?",
                        "Kadir gecesinin şerefinin ne olduğunu sana ne şey bildirdi?",
                        "Bu soru, Kadir gecesinin insan idrakini aşan manevi değerine, şerefine ve yüceliğine dikkat çekmek için sorulmuştur.",
                        "Gecenin kadr-ü kıymetinin ve manevi derinliğinin büyüklüğünü tekit etmek amacıyla yöneltilmiş ilahi bir hitaptır."
                    ),
                    SurahVerse(
                        3,
                        "لَيْلَةُ الْقَدْرِ خَيْرٌ مِنْ أَلْفِ شَهْرٍ",
                        "Leyletül kadri hayrün min elfi şehr.",
                        "Kadir gecesi bin aydan daha hayırlıdır.",
                        "Kadir gecesi bin aydan daha hayırlıdır.",
                        "Kadir gecesi bin aydan daha hayırlıdır.",
                        "İçinde Kadir gecesi bulunmayan bin aydan (yaklaşık 83 yıllık bir ömürden) daha hayırlı, bereketli ve sevap doludur.",
                        "Bu kutsal gecede ihlasla yapılan ibadetler, bir insan ömrü boyunca yapılan ibadetlerden daha hayırlı ve sevaba vesiledir."
                    ),
                    SurahVerse(
                        4,
                        "تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِمْ مِنْ كُلِّ أَمْرٍ",
                        "Tenezzelül melâiketü verrûhu fîhâ biizni rabbihim min külli emr.",
                        "Melekler ve Rûh (Cebrail) o gecede, Rablerinin izniyle her türlü iş için iner de iner.",
                        "Melekler ve Ruh o gecede Rablerinin izniyle her iş için iner dururlar.",
                        "Melekler ve Cebrail o gecede Rablerinin izniyle her hayırlı emir için inerler.",
                        "Melekler ve Cebrail (a.s.) yeryüzüne inerek ibadet eden müminlere selam verir, onların bağışlanması için dua ve istiğfarda bulunurlar.",
                        "Yeryüzü meleklerin gelişiyle rahmete ve nura boğulur, semavi esenlik ve feyizler her tarafı kaplar."
                    ),
                    SurahVerse(
                        5,
                        "سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ",
                        "Selâmün hiye hattâ matla'il fecr.",
                        "O gece, tan yerinin ağarmasına kadar bir esenliktir (selamettir).",
                        "O gece tan yeri ağarıncaya kadar esenlik doludur.",
                        "O gece fecrin doğuşuna kadar bir selamettir.",
                        "O gece baştan sona selamettir; şeytanların şerri engellenir, müminler için azaptan eminlik ve esenlik hakimdir.",
                        "Müminlerin manevi huzura erdiği, kötülüklerden emin kılındığı ve fecre kadar süren kesintisiz bir selamet vaktidir."
                    )
                )
            ),
            Surah(
                id = 108,
                name = "Kevser Suresi",
                revelationOrder = 15,
                revelationPlace = "Mekke",
                verseCount = 3,
                verses = listOf(
                    SurahVerse(
                        1,
                        "إِنَّا أَعْطَيْنَاكَ الْكَوْسَرَ",
                        "İnnâ a'taynâkel kevser.",
                        "Şüphesiz biz sana Kevser'i verdik.",
                        "Muhakkak ki biz sana Kevser'i lütfettik.",
                        "Şüphesiz biz sana Kevser'i ihsan ettik.",
                        "Kevser, cennetteki bir nehir, havuz veya bol nimet anlamına gelir. Peygamberimize verilen büyük manevi lütufları ve ümmetinin çokluğunu simgeler.",
                        "Peygamber Efendimize dünya ve ahirette ihsan olunan bitmez tükenmez hayırlar, ilahi marifetler ve yüce makamlardır."
                    ),
                    SurahVerse(
                        2,
                        "فَصَلِّ لِرَبِّكَ وَانْحَرْ",
                        "Fesalli lirabbike venhar.",
                        "O halde Rabbin için namaz kıl ve kurban kes.",
                        "Öyleyse Rabbin için namaz kıl ve kurban kes.",
                        "Artık Rabbin için namaz kıl ve kurban kes.",
                        "Bu büyük nimetlere şükür olarak sadece Allah için ibadet edilmeli, namaz kılınmalı ve yalnızca O'nun adı anılarak kurban kesilmelidir.",
                        "İbadetlerin ve kurban ibadetinin halisane Allah rızası için yapılması gerektiğini, putperest adetlerin reddedilmesini vurgular."
                    ),
                    SurahVerse(
                        3,
                        "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
                        "İnne şânieke hüvel ebter.",
                        "Asıl soyu kesik (ebter) olan, sana buğzedendir.",
                        "Şüphesiz asıl soyu kesik olan sana kin besleyendir.",
                        "Muhakkak ki sana buğzeden, nesli kesilmiş olanın ta kendisidir.",
                        "Müşrikler Peygamberimizin erkek çocukları vefat ettiğinde onun soyunun kesildiğini iddia etmişlerdi. Ayet bu iddiayı çürütmüş, Peygamberimizin adının kıyamete kadar yaşayacağını müjdelemiştir.",
                        "İslam davasına düşmanlık edenlerin, hidayetten yoksun kalarak tarih sahnesinden silineceğini ve unutulup gideceğini beyan eder."
                    )
                )
            ),
            Surah(
                id = 112,
                name = "İhlâs Suresi",
                revelationOrder = 22,
                revelationPlace = "Mekke",
                verseCount = 4,
                verses = listOf(
                    SurahVerse(
                        1,
                        "قُلْ هُوَ اللَّهُ أَحَدٌ",
                        "Kul hüvellâhü ehad.",
                        "De ki: O Allah tektir.",
                        "De ki: O, Allah birdir.",
                        "De ki: O Allah, birdir.",
                        "İhlas suresi İslam'ın tevhid inancını en özlü şekilde özetler. Ehad, Allah'ın zatında, sıfatlarında ve fiillerinde tek, eşsiz ve benzersiz olduğunu ifade eder.",
                        "Tevhid akidesinin esası, Allah'ın zatında ve sıfatlarında ortak kabul etmemektir. Birdir, cüzlerden ve kısımlardan münezzehtir."
                    ),
                    SurahVerse(
                        2,
                        "اللَّهُ الصَّمَدُ",
                        "Allâhüssamed.",
                        "Allah sameddir (her şey O'na muhtaçtır, O hiçbir şeye muhtaç değildir).",
                        "Allah Samed'dir.",
                        "Allah o Samed'dir (hiçbir şeye muhtaç değildir).",
                        "Samed, var olmak ve varlığını sürdürmek için hiçbir şeye ihtiyaç duymayan, aksine her şeyin varlığı ve ihtiyaçları kendisine bağlı olan yüce yaratıcı demektir.",
                        "Zat-ı ilahinin gınâ-yı mutlak (tam ve sınırsız zenginlik) sahibi olması, her mahlukun her an O'nun lütfuna ve yaratmasına muhtaç olması anlamına gelir."
                    ),
                    SurahVerse(
                        3,
                        "لَمْ يَلِدْ وَلَمْ يُولَدْ",
                        "Lem yelid ve lem yûled.",
                        "O, doğurmamış ve doğurulmamıştır.",
                        "Doğurmadı ve doğurulmadı.",
                        "Doğurmamıştır ve doğurulmamıştır.",
                        "Allah için çocuk sahibi olmak veya bir anne-babadan doğmuş olmak kesinlikle söz konusu olamaz. Bu noksanlık sıfatlarından Allah münezzehtir.",
                        "Cenab-ı Hakk ezeli ve ebedidir. Sonradan meydana gelen maddesel ilişkilerden, soy ve nesep bağlarından tamamen uzaktır."
                    ),
                    SurahVerse(
                        4,
                        "وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
                        "Ve lem yekün lehû küfüven ehad.",
                        "O'nun hiçbir dengi yoktur.",
                        "Ve O'na hiçbir şey denk olmamıştır.",
                        "Ve O'nun için hiçbir dengi bulunmamıştır.",
                        "Yaratılmış hiçbir varlık veya kavram Allah'ın benzeri, ortağı, yardımcısı veya dengi olamaz. O her bakımdan benzersizdir.",
                        "Zatında, sıfatlarında ve fiillerinde hiçbir eşi, benzeri ve dengi bulunmamaktadır. Akla gelen her şeyden yücedir."
                    )
                )
            ),
            Surah(
                id = 113,
                name = "Felak Suresi",
                revelationOrder = 20,
                revelationPlace = "Mekke",
                verseCount = 5,
                verses = listOf(
                    SurahVerse(
                        1,
                        "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ",
                        "Kul e'ûzü birabbil felak.",
                        "De ki: Sığınırım o sabahın Rabbine,",
                        "De ki: Sabahın Rabbine sığınırım,",
                        "De ki: Sabahın yaradanına sığınırım,",
                        "Felak, sabah vakti veya yarılma anlamına gelir. Karanlığı yarıp aydınlığı çıkaran, yokluktan varlığı çıkaran Allah'a sığınmak emredilir.",
                        "Sıkıntılardan, karanlıklardan kurtulup feraha ermek için nurların ve sabah aydınlığının yaratıcısına iltica etmektir."
                    ),
                    SurahVerse(
                        2,
                        "مِنْ شَرِّ مَا خَلَقَ",
                        "Min şerri mâ halak.",
                        "Yarattığı şeylerin şerrinden,",
                        "Yarattığı şeylerin kötülüğünden,",
                        "Yarattığı şeylerin şerrinden,",
                        "Tüm canlıların, mahlukatın, insanların, cinlerin ve zararlı varlıkların kendi iradeleri veya tabiatları gereği yapabilecekleri kötülüklerden Allah'a sığınmaktır.",
                        "Kainattaki her türlü yaratılmış zararlı mahlukatın ve şerli odakların kötülüğünden doğrudan yaratıcısına sığınmayı öğretir."
                    ),
                    SurahVerse(
                        3,
                        "وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ",
                        "Ve min şerri gâsıkın izâ vekab.",
                        "Karanlığı çöktüğü zaman gecenin şerrinden,",
                        "Karanlığı çöktüğü zaman gecenin şerrinden,",
                        "Karanlığı her tarafı kapladığı zaman gecenin şerrinden,",
                        "Gece çöktüğünde kötülükler, zararlılar ve sinsi planlar daha kolay yayılır. Gecenin getirdiği belalardan Allah'a sığınma vurgulanır.",
                        "Karanlık, kötülüklerin ve tehlikelerin gizlenip yayıldığı sinsi bir zamandır. Bu yüzden o anlarda sığınma elzem kılınmıştır."
                    ),
                    SurahVerse(
                        4,
                        "وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ",
                        "Ve min şerrin-neffâsâti fîl 'ukad.",
                        "Düğümlere üfleyenlerin şerrinden,",
                        "Düğümlere üfleyen büyücülerin şerrinden,",
                        "Düğümlere üfleyen (büyücülerin/hasetçilerin) şerrinden,",
                        "Büyü, sihir, insanların arasını açmaya çalışan her türlü fesatçı niyet, dedikodu ve psikolojik sinsi telkinler kınanmış ve korunma istenmiştir.",
                        "İnsan iradesini, sağlığını ve huzurunu bozmaya yönelik gizli, karanlık ve sinsi faaliyetlerin etkisinden koruma talep edilir."
                    ),
                    SurahVerse(
                        5,
                        "وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
                        "Ve min şerri hâsidin izâ hased.",
                        "Haset ettiği zaman hasetçinin şerrinden.",
                        "Haset ettiği zaman hasetçinin şerrinden.",
                        "Ve haset ettiği zaman hasetçinin şerrinden.",
                        "Haset, başkasındaki nimetin yok olmasını istemektir. Bu sinsi ve kötü duygu eyleme dönüştüğünde büyük zararlar verebilir, bundan Allah'a sığınılır.",
                        "Kıskançlığın göz veya kötü eyleme dönüştüğü an tehlikelidir. Hasetçinin kem gözünden ve zararından doğrudan Allah'a sığınmak gerekir."
                    )
                )
            ),
            Surah(
                id = 114,
                name = "Nâs Suresi",
                revelationOrder = 21,
                revelationPlace = "Mekke",
                verseCount = 6,
                verses = listOf(
                    SurahVerse(
                        1,
                        "قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
                        "Kul e'ûzü birabbin-nâs.",
                        "De ki: Sığınırım insanların Rabbine,",
                        "De ki: İnsanların Rabbine sığınırım,",
                        "De ki: İnsanların malikine ve rabbine sığınırım,",
                        "İnsanların yaratıcısı, sahibi ve terbiyecisi olan yüce Allah'a her türlü sinsi şerden ve içsel vesveselerden sığınmayı öğreten duadır.",
                        "Rabbü'n-Nas sıfatı, insanın her an ilahi terbiyeye, korumaya ve yönlendirilmeye muhtaç bir varlık olduğunu hatırlatır."
                    ),
                    SurahVerse(
                        2,
                        "مَلِكِ النَّاسِ",
                        "Melikin-nâs.",
                        "İnsanların hükümdarına,",
                        "İnsanların yegane hükümdarına,",
                        "İnsanların yegane hakimine,",
                        "İnsanların gerçek yöneticisi, kanun koyucusu ve egemeni yalnızca Allah'tır. O'ndan başka mutlak egemen yoktur.",
                        "Melik sıfatı, kainattaki tek ve mutlak egemen gücü, mülkün yegane sahibini ve son karar verici makamı işaret eder."
                    ),
                    SurahVerse(
                        3,
                        "إِلَٰهِ النَّاسِ",
                        "İlâhin-nâs.",
                        "İnsanların ilahına;",
                        "İnsanların ilahına;",
                        "İnsanların mabuduna (ilahına);",
                        "Sadece ibadet edilmeye, boyun eğilmeye ve sevilmeye layık olan tek hakiki ilahın Allah olduğu gerçeği vurgulanır.",
                        "Uluhiyetin sadece Allah'a ait olduğunu, insanların başka hiçbir güce, kula veya maddeye tapmaması gerektiğini belirtir."
                    ),
                    SurahVerse(
                        4,
                        "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ",
                        "Min şerril vesvâsil hannâs.",
                        "O sinsi vesvesecinin şerrinden,",
                        "O sinsi vesvesecinin kötülüğünden,",
                        "O sinsi sinsi vesvese veren şeytanın şerrinden,",
                        "Hannas, sinsi ve pusuya yatan demektir. Şeytan insanı kötülüğe çağırır ama insan Allah'ı andığında ve uyanık olduğunda geri çekilir.",
                        "İnsanın zihnine ve kalbine kötü düşünceler, şüpheler, kuruntular fısıldayan ve pusuda bekleyen şeytani güçlerdir."
                    ),
                    SurahVerse(
                        5,
                        "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ",
                        "Ellezî yuvesvisu fî sudûrin-nâs.",
                        "Ki o, insanların göğüslerine vesvese verir.",
                        "İnsanların göğüslerine vesvese fısıldayan,",
                        "O ki, insanların kalplerine vesveseler bırakır,",
                        "Vesvesenin ulaştığı yer insanın kalbi ve göğsüdür. Bu vesvese şüphe, korku, şehvet, ümitsizlik ve günah arzusu şeklinde belirir.",
                        "İnsanın iç dünyasına, karar mekanizmalarına müdahale etmeye çalışan görünmez, fısıltılı dürtüleri ifade eder."
                    ),
                    SurahVerse(
                        6,
                        "مِنَ الْجِنَّةِ وَالنَّاسِ",
                        "Minel cinneti ven-nâs.",
                        "Gerek cinlerden, gerek insanlardan olan (vesvesecilerin).",
                        "Gerek cinlerden gerekse insanlardan olan (tüm sinsi varlıklardan).",
                        "Gerek cinlerden olsun, gerekse insanlardan.",
                        "Sadece görünmeyen şeytanlar değil, insan kılığındaki vesveseciler, kötü arkadaşlar ve yönlendiriciler de kötülük fısıldayabilir. Bu nedenle her iki gruptan da sığınılır.",
                        "Görünen ve görünmeyen, maddi ve manevi tüm sinsi telkin ve yönlendirme sahiplerinden uyanık olunması gerektiğini ihtar eder."
                    )
                )
            )
        )
        QuranData.buildCompleteSurahList(predefined)
    }

    val savedVerses by viewModel.savedVerses.collectAsState()
    val targetQuranVersePair by viewModel.targetQuranVerse.collectAsState()
    var mainTabSelected by remember { mutableStateOf(0) } // 0: Sureler, 1: Kaydedilen Ayetler
    var targetVerseIndexToScroll by remember { mutableStateOf<Int?>(null) }
    var currentSurah by remember { mutableStateOf<Surah?>(null) }
    androidx.activity.compose.BackHandler(enabled = currentSurah != null) {
        currentSurah = null
    }
    var selectedVerseForCopy by remember { mutableStateOf<SurahVerse?>(null) }
    var isLoadingSurah by remember { mutableStateOf(false) }

    LaunchedEffect(targetQuranVersePair) {
        targetQuranVersePair?.let { (surahId, verseNum) ->
            val foundSurah = surahs.find { it.id == surahId }
            if (foundSurah != null) {
                currentSurah = foundSurah
                targetVerseIndexToScroll = verseNum - 1
                mainTabSelected = 0
            }
            viewModel.clearTargetQuranVerse()
        }
    }
    
    val surahsListState = rememberLazyListState()
    var isSurahsFastScrolling by remember { mutableStateOf(false) }
    var surahsFastScrollY by remember { mutableStateOf(0f) }
    var surahsFastScrollPercent by remember { mutableStateOf(0f) }
    var previousSurahTargetIndex by remember { mutableStateOf(-1) }
    val mainCoroutineScope = rememberCoroutineScope()
    val surahView = androidx.compose.ui.platform.LocalView.current
    
    val strings = LocalAppStrings.current
    val activeEditionsSet by viewModel.selectedEditions.collectAsState()
    val activeEditionsList = remember(activeEditionsSet) { QuranReadingEditions.orderedMeals(activeEditionsSet) }
    val offlineReader = remember(context) { OfflineQuranReader(context.applicationContext) }
    val surahCache = rememberQuranSurahCache()
    var surahLoadError by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableStateOf(0) }

    LaunchedEffect(currentSurah?.id, activeEditionsList, loadAttempt) {
        val surah = currentSurah ?: return@LaunchedEffect
        surahLoadError = false
        surahCache.get(surah.id, activeEditionsList)?.let { cached ->
            currentSurah = surah.copy(verses = cached)
            isLoadingSurah = false
            return@LaunchedEffect
        }
        isLoadingSurah = true
        try {
            val verses = offlineReader.loadSurah(surah, activeEditionsList)
            surahCache.put(surah.id, activeEditionsList, verses)
            currentSurah = surah.copy(verses = verses)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            surahLoadError = true
        } finally {
            isLoadingSurah = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentSurah) {
        viewModel.setIsReadingSurah(currentSurah != null)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setIsReadingSurah(false)
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var sortByQuranOrder by remember { mutableStateOf(true) } // true: Kuran Sırası, false: İniş Sırası
    var verseSearchQuery by remember { mutableStateOf("") }
    var detailTabSelected by remember { mutableStateOf(0) } // 0: Ayetler & Mealler, 1: Tefsir Karşılaştırması

    // Ayet Okuma Ayarları
    var fontScale by remember { mutableStateOf(1.0f) }
    var showFontSizeSlider by remember { mutableStateOf(false) }
    var showDiyanetMeal by remember { mutableStateOf(true) }
    var showElmaliliMeal by remember { mutableStateOf(false) }
    var showVakfiMeal by remember { mutableStateOf(false) }

    // Tefsir Karşılaştırma Ayarları
    var selectedVerseIndex by remember { mutableStateOf(0) }

    // Ayet Dinleme & Ses Çalma Ayarları
    var playingVerseNumber by remember { mutableStateOf<Int?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var isAudioBuffering by remember { mutableStateOf(false) }
    // Turkish meal is the default source: the play button used to start Arabic
    // recitation because this defaulted to "arabic". The choice is persisted so
    // it survives leaving the reading screen.
    var audioSourceType by remember {
        mutableStateOf(
            runCatching {
                context.getSharedPreferences("quran_audio_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("source", "meal")
            }.getOrNull() ?: "meal"
        )
    }
    var autoPlayNextVerse by remember { mutableStateOf(true) }
    var activeMediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var visibleMealEdition by remember { mutableStateOf<String?>(null) }

    // Turkish meal narration now lives in a dedicated engine: offline-first
    // voice selection, sentence-level chunking (no more truncation), real
    // error codes, pause/resume, and an automatic pause when backgrounded.
    val mealEngine = remember { com.example.ui.audio.MealSpeechHolder.get(context) }
    val mealStatus by mealEngine.status.collectAsState()

    // Narration is paused when the screen is no longer foreground.
    //
    // The engine is deliberately NOT released here. It used to be, which meant
    // each visit rebuilt a TextToSpeech engine during composition — the service
    // binding blocked the first frame and the screen froze on entry. The holder
    // owns the lifetime instead, and the engine is warmed at app start.
    val mealLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(mealLifecycleOwner, mealEngine) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) mealEngine.onBackgrounded()
        }
        mealLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            mealLifecycleOwner.lifecycle.removeObserver(observer)
            mealEngine.stop()
        }
    }

    // Mirror engine state onto the transport flags the reading UI already uses.
    LaunchedEffect(mealStatus.state) {
        when (mealStatus.state) {
            MealSpeechEngine.State.PREPARING -> { isAudioBuffering = true; isAudioPlaying = false }
            MealSpeechEngine.State.PLAYING -> { isAudioBuffering = false; isAudioPlaying = true }
            else -> { isAudioBuffering = false; isAudioPlaying = false }
        }
    }

    fun stopAudio(clearSelection: Boolean = true) {
        mealEngine.stop()
        try {
            activeMediaPlayer?.apply {
                setOnPreparedListener(null)
                setOnCompletionListener(null)
                setOnErrorListener(null)
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeMediaPlayer = null
        isAudioPlaying = false
        isAudioBuffering = false
        if (clearSelection) playingVerseNumber = null
    }

    /**
     * Switches the narration source without starting playback.
     *
     * The old segmented control called playVerseAudio() on every tap, so choosing
     * a mode immediately started audio the user had not asked for. Selecting is
     * now separate from playing; the play button honours the selection.
     */
    fun selectAudioSource(source: String) {
        if (audioSourceType == source) return
        stopAudio(clearSelection = false)
        audioSourceType = source
        runCatching {
            context.getSharedPreferences("quran_audio_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("source", source).apply()
        }
    }

    DisposableEffect(currentSurah?.id) {
        onDispose {
            stopAudio()
        }
    }

    fun playVerseAudio(surah: Surah, verseNumber: Int, source: String) {
        if (verseNumber < 1 || verseNumber > surah.verses.size) {
            stopAudio()
            return
        }
        stopAudio()

        playingVerseNumber = verseNumber
        audioSourceType = source

        isAudioBuffering = true
        isAudioPlaying = false

        if (source == "meal") {
            val verse = surah.verses.firstOrNull { it.number == verseNumber }
            val meal = verse?.let {
                visibleMealEdition?.let { edition -> QuranAudioSupport.mealSpeechTextForEdition(it, edition) }
                    ?: QuranAudioSupport.mealSpeechText(it, activeEditionsList)
            }.orEmpty()
            if (meal.isBlank()) {
                stopAudio()
                android.widget.Toast.makeText(
                    context, "Bu ayetin meal metni bulunamadı.", android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
            // The engine chunks the verse itself, queues the chunks, and reports
            // failures through its status flow instead of a single silent call.
            mealEngine.speak(meal)
            return
        }

        val audioUrls = QuranAudioSupport.arabicVerseUrls(surah.id, verseNumber)
        lateinit var startAudioSource: (Int) -> Unit
        startAudioSource = { sourceIndex ->
            val mp = android.media.MediaPlayer()
            activeMediaPlayer = mp
            try {
                mp.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mp.setWakeMode(context.applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                mp.setDataSource(audioUrls[sourceIndex])
                mp.setOnPreparedListener { player ->
                    if (activeMediaPlayer === player && playingVerseNumber == verseNumber && audioSourceType == "arabic") {
                        isAudioBuffering = false
                        isAudioPlaying = true
                        player.start()
                    }
                }
                mp.setOnCompletionListener { player ->
                    try {
                        player.setOnPreparedListener(null)
                        player.setOnCompletionListener(null)
                        player.setOnErrorListener(null)
                        player.release()
                    } catch (_: Exception) {}
                    if (activeMediaPlayer === player) activeMediaPlayer = null
                    isAudioPlaying = false
                    if (autoPlayNextVerse && verseNumber < surah.verses.size) {
                        targetVerseIndexToScroll = verseNumber
                        playVerseAudio(surah, verseNumber + 1, "arabic")
                    } else {
                        playingVerseNumber = null
                    }
                }
                mp.setOnErrorListener { failedMp, _, _ ->
                    try {
                        failedMp.setOnPreparedListener(null)
                        failedMp.setOnCompletionListener(null)
                        failedMp.setOnErrorListener(null)
                        failedMp.reset()
                        failedMp.release()
                    } catch (_: Exception) {}
                    if (activeMediaPlayer === failedMp) activeMediaPlayer = null
                    if (sourceIndex + 1 < audioUrls.size && playingVerseNumber == verseNumber) {
                        startAudioSource(sourceIndex + 1)
                    } else {
                        isAudioBuffering = false
                        isAudioPlaying = false
                        playingVerseNumber = null
                        android.widget.Toast.makeText(context, "Tilavet yüklenemedi. İnternet bağlantınızı kontrol edin.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                mp.prepareAsync()
            } catch (_: Exception) {
                try {
                    mp.reset()
                    mp.release()
                } catch (_: Exception) {}
                if (activeMediaPlayer === mp) activeMediaPlayer = null
                if (sourceIndex + 1 < audioUrls.size) {
                    startAudioSource(sourceIndex + 1)
                } else {
                    isAudioBuffering = false
                    isAudioPlaying = false
                    playingVerseNumber = null
                    android.widget.Toast.makeText(context, "Tilavet başlatılamadı.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        startAudioSource(0)
    }

    // Auto-advance and error reporting now key off the engine's status flow.
    LaunchedEffect(mealStatus.state) {
        when (mealStatus.state) {
            MealSpeechEngine.State.FINISHED -> {
                val completedVerse = playingVerseNumber ?: return@LaunchedEffect
                val surah = currentSurah ?: return@LaunchedEffect
                if (autoPlayNextVerse && completedVerse < surah.verses.size) {
                    kotlinx.coroutines.delay(220L)
                    targetVerseIndexToScroll = completedVerse
                    playVerseAudio(surah, completedVerse + 1, "meal")
                } else {
                    playingVerseNumber = null
                }
            }
            MealSpeechEngine.State.ERROR -> {
                playingVerseNumber = null
                mealStatus.message?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            MealSpeechEngine.State.UNAVAILABLE -> {
                playingVerseNumber = null
                mealStatus.message?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            else -> Unit
        }
    }

    fun pauseOrResumeAudio(surah: Surah) {
        if (audioSourceType == "meal") {
            when (mealStatus.state) {
                MealSpeechEngine.State.PLAYING -> mealEngine.pause()
                MealSpeechEngine.State.PAUSED -> mealEngine.resume()
                else -> {
                    val verseNumber = playingVerseNumber ?: return
                    playVerseAudio(surah, verseNumber, "meal")
                }
            }
            return
        }
        activeMediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.pause()
                    isAudioPlaying = false
                } else {
                    mp.start()
                    isAudioPlaying = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    SubScreenScaffold(
        title = currentSurah?.name ?: strings.quranTitle,
        onBack = {
            if (currentSurah != null) {
                currentSurah = null
            } else {
                onBack()
            }
        },
        actions = {}
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentSurah == null) {
            // SÜRE LİSTESİ VEYA KAYDEDİLEN AYETLER GÖRÜNÜMÜ
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                // Main Tab Row: Sureler vs Kaydedilen Ayetler
                TabRow(
                    selectedTabIndex = mainTabSelected,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = mainTabSelected == 0,
                        onClick = { mainTabSelected = 0 },
                        text = { Text("Sureler (${surahs.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = mainTabSelected == 1,
                        onClick = { mainTabSelected = 1 },
                        text = { Text("Kaydedilen Ayetler (${savedVerses.size})", fontWeight = FontWeight.Bold) }
                    )
                }

                if (mainTabSelected == 0) {
                    // Arama Kutusu
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchSurah) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sıralama Seçenekleri (Kuran Sırası vs İniş Sırası)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sortByQuranOrder) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { sortByQuranOrder = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.quranOrder,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sortByQuranOrder) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!sortByQuranOrder) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { sortByQuranOrder = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.revelationOrder,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!sortByQuranOrder) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sıralanmış ve Filtrelenmiş Sure Listesi
                    val filteredSurahs = remember(searchQuery, sortByQuranOrder) {
                        val filtered = surahs.filter {
                            fun norm(s: String) = s.lowercase(java.util.Locale.forLanguageTag("tr"))
                                .replace("â", "a")
                                .replace("î", "i")
                                .replace("û", "u")
                                .replace("ş", "s")
                                .replace("ı", "i")
                                .replace("ö", "o")
                                .replace("ğ", "g")
                                .replace("ü", "u")
                                .replace("ç", "c")
                                .replace("'", "")
                                .replace("-", "")
                                .replace(" ", "")
                            
                            val nameNorm = norm(it.name)
                            val queryNorm = norm(searchQuery)
                            nameNorm.contains(queryNorm)
                        }
                        if (sortByQuranOrder) {
                            filtered.sortedBy { it.id }
                        } else {
                            filtered.sortedBy { it.revelationOrder }
                        }
                    }

                    if (filteredSurahs.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aradığınız kriterlere uygun sure bulunamadı.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = surahsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp))
                            ) {
                                items(filteredSurahs) { surah ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                            .clickable {
                                                currentSurah = surah
                                                selectedVerseIndex = 0
                                                detailTabSelected = 0
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Sıralama Rozeti
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (sortByQuranOrder) surah.id.toString() else surah.revelationOrder.toString(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(16.dp))

                                                Column {
                                                    Text(
                                                        text = surah.name,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${surah.revelationPlace} • ${surah.verseCount} Ayet",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.08f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (sortByQuranOrder) "İniş: ${surah.revelationOrder}" else "Kuran: ${surah.id}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Sağ taraftaki hızlı kaydırma alanı (Sureler)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .pointerInput(filteredSurahs) {
                                        var lastHapticTime = 0L
                                        detectVerticalDragGestures(
                                            onDragStart = { offset ->
                                                isSurahsFastScrolling = true
                                                surahsFastScrollY = offset.y
                                                surahsFastScrollPercent = (surahsFastScrollY / size.height).coerceIn(0f, 1f)
                                                val targetIndex = (surahsFastScrollPercent * (filteredSurahs.size - 1)).toInt().coerceIn(0, filteredSurahs.lastIndex)
                                                mainCoroutineScope.launch { surahsListState.scrollToItem(targetIndex) }
                                                if (targetIndex != previousSurahTargetIndex) {
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - lastHapticTime > 80) {
                                                        surahView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                        lastHapticTime = currentTime
                                                    }
                                                    previousSurahTargetIndex = targetIndex
                                                }
                                            },
                                            onDragEnd = { isSurahsFastScrolling = false },
                                            onDragCancel = { isSurahsFastScrolling = false }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            surahsFastScrollY += dragAmount
                                            surahsFastScrollPercent = (surahsFastScrollY / size.height).coerceIn(0f, 1f)
                                            val targetIndex = (surahsFastScrollPercent * (filteredSurahs.size - 1)).toInt().coerceIn(0, filteredSurahs.lastIndex)
                                            mainCoroutineScope.launch { surahsListState.scrollToItem(targetIndex) }
                                            if (targetIndex != previousSurahTargetIndex) {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastHapticTime > 80) {
                                                    surahView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                    lastHapticTime = currentTime
                                                }
                                                previousSurahTargetIndex = targetIndex
                                            }
                                        }
                                    }
                            )

                            // Sureler için kaydırma baloncukları
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSurahsFastScrolling && filteredSurahs.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                val targetIndex = (surahsFastScrollPercent * (filteredSurahs.size - 1)).toInt().coerceIn(0, filteredSurahs.lastIndex)
                                val currentSurahItem = filteredSurahs[targetIndex]

                                Box(
                                    modifier = Modifier
                                        .padding(end = 36.dp)
                                        .graphicsLayer {
                                            translationY = (surahsFastScrollY - 120.dp.toPx()).coerceIn(-200f, 200f)
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Üstteki küçük baloncuklar
                                        for (i in 2 downTo 1) {
                                            val sIndex = targetIndex - i
                                            if (sIndex >= 0 && sIndex < filteredSurahs.size) {
                                                val prevSurah = filteredSurahs[sIndex]
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f + (2 - i) * 0.2f),
                                                    tonalElevation = 2.dp
                                                ) {
                                                    Text(
                                                        text = "${prevSurah.id}. ${prevSurah.name}",
                                                        color = Color.White,
                                                        fontSize = (11 - (i - 1) * 2).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Ana merkez baloncuk
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)),
                                            shadowElevation = 8.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.25f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${currentSurahItem.id}",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = currentSurahItem.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Text(
                                                        text = "${currentSurahItem.verseCount} Ayet • ${currentSurahItem.revelationPlace}",
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Alttaki küçük baloncuklar
                                        for (i in 1..2) {
                                            val sIndex = targetIndex + i
                                            if (sIndex >= 0 && sIndex < filteredSurahs.size) {
                                                val nextSurah = filteredSurahs[sIndex]
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f - (i - 1) * 0.2f),
                                                    tonalElevation = 2.dp
                                                ) {
                                                    Text(
                                                        text = "${nextSurah.id}. ${nextSurah.name}",
                                                        color = Color.White,
                                                        fontSize = (11 - (i - 1) * 2).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // KAYDEDİLEN AYETLER LİSTESİ
                    if (savedVerses.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Henüz Kaydedilmiş Ayet Yok",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sureleri okurken ayetlere basılı tutarak veya ayet kartındaki kaydet butonuna dokunarak beğendiğiniz ayetleri buraya kaydedebilirsiniz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                bottom = com.example.ui.theme.navBarBottomPadding(extra = 12.dp)
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedVerses) { savedVerse ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Bookmark,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${savedVerse.surahName}, ${savedVerse.verseNumber}. Ayet",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeSavedVerse(savedVerse.surahId, savedVerse.verseNumber)
                                                    android.widget.Toast.makeText(context, "Ayet kaydedilenlerden çıkarıldı.", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Sil",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (savedVerse.arabic.isNotEmpty()) {
                                            Text(
                                                text = savedVerse.arabic,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth(),
                                                lineHeight = 40.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        if (savedVerse.pronunciation.isNotEmpty()) {
                                            Text(
                                                text = savedVerse.pronunciation,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        val savedMeal = savedVerse.selectedMeal.ifBlank { savedVerse.diyanetMeal }
                                        if (savedMeal.isNotEmpty()) {
                                            val mealText = savedMeal.trim()
                                            val formattedMeal = if (mealText.startsWith("“") || mealText.startsWith("\"")) mealText else "“$mealText”"
                                            Text(
                                                text = formattedMeal,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                                lineHeight = 23.sp
                                            )
                                        }

                                        if (savedVerse.footnotes.isNotBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(savedVerse.footnotes, fontSize = 13.sp, lineHeight = 20.sp)
                                        }
                                        if (savedVerse.mealSource.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(savedVerse.mealSource, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val targetSurah = surahs.find { it.id == savedVerse.surahId }
                                                    if (targetSurah != null) {
                                                        currentSurah = targetSurah
                                                        targetVerseIndexToScroll = savedVerse.verseNumber - 1
                                                        mainTabSelected = 0
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Surede Göster", fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (isLoadingSurah) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (surahLoadError) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ayetler yüklenemedi.", color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { loadAttempt++ }) { Text("Yeniden dene") }
            }
        } else {
            // SURE OKUMA & TEFSİR KARŞILAŞTIRMA DETAY GÖRÜNÜMÜ
            val surah = currentSurah
            if (surah != null) {
                val hasPronunciation = surah.verses.isNotEmpty() && surah.verses.all { it.pronunciation.isNotBlank() }
                val tabs = remember(activeEditionsList, surah.id, hasPronunciation) {
                    activeEditionsList.map(QuranReadingEditions::name) + listOf("Arapça") +
                        if (hasPronunciation) listOf("Okunuş") else emptyList()
                }
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(pagerState.currentPage, activeEditionsList) {
                    visibleMealEdition = activeEditionsList.getOrNull(pagerState.currentPage)
                }

                val verse = selectedVerseForCopy
                if (verse != null) {
                    val page = pagerState.currentPage
                    val isSaved = viewModel.isVerseSaved(surah.id, verse.number)
                    val copyText = if (page < activeEditionsList.size) {
                        val edKey = activeEditionsList[page]
                        verse.mealsMap[edKey].orEmpty()
                    } else if (page == activeEditionsList.size) {
                        verse.arabic
                    } else {
                        verse.pronunciation
                    }
                    val clipboardText = "$copyText\n\n(${surah.name}, ${verse.number}. Ayet)"
                    
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { selectedVerseForCopy = null }
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Arapça Tilavet Dinle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedVerseForCopy = null
                                            playVerseAudio(surah, verse.number, "arabic")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Arapça Dinle",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Arapça Tilavet",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Türkçe meal seslendirme
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedVerseForCopy = null
                                            playVerseAudio(surah, verse.number, "meal")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Türkçe Meali Seslendir",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Türkçe Meali Seslendir",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }


                                // Kopyala
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(clipboardText))
                                            selectedVerseForCopy = null
                                            android.widget.Toast.makeText(context, "Ayet kopyalandı.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Kopyala",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Kopyala",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Kaydet
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val selectedEdition = activeEditionsList.getOrNull(page)
                                                ?: activeEditionsList.firstOrNull { !verse.mealsMap[it].isNullOrBlank() }
                                            val sv = SavedVerse(
                                                surahId = surah.id,
                                                surahName = surah.name,
                                                verseNumber = verse.number,
                                                arabic = verse.arabic,
                                                pronunciation = verse.pronunciation,
                                                diyanetMeal = verse.diyanetMeal,
                                                elmaliliMeal = verse.elmaliliMeal,
                                                vakfiMeal = verse.vakfiMeal,
                                                selectedMeal = selectedEdition?.let { verse.mealsMap[it] }.orEmpty(),
                                                mealSource = selectedEdition?.let(QuranReadingEditions::sourceLabel).orEmpty(),
                                                footnotes = selectedEdition?.let { verse.footnotesMap[it] }.orEmpty()
                                            )
                                            viewModel.toggleSaveVerse(sv)
                                            val msg = if (isSaved) "Ayet kaydedilenlerden çıkarıldı." else "Ayet kaydedildi."
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                            selectedVerseForCopy = null
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Kaydet",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isSaved) "Kaydı Sil" else "Kaydet",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Text(
                        text = activeEditionsList.getOrNull(pagerState.currentPage)?.let(QuranReadingEditions::sourceLabel)
                            ?: if (pagerState.currentPage == activeEditionsList.size) "Tanzil · Uthmani 1.1" else "Türkçe Okunuş",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage.coerceAtMost(tabs.size - 1),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            val current = pagerState.currentPage.coerceAtMost(tabPositions.size - 1)
                            if (current in tabPositions.indices) {
                                androidx.compose.material3.TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[current]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }
                    }

                    val listStates = remember(tabs.size, surah.id) { List(tabs.size) { androidx.compose.foundation.lazy.LazyListState() } }
                    val currentPage = pagerState.currentPage.coerceAtMost(tabs.size - 1)
                    val currentListState = listStates.getOrNull(currentPage) ?: rememberLazyListState()
                    val pageIndices = remember(surah.verses, tabs.size, activeEditionsList) {
                        List(tabs.size) { pageIndex ->
                            val isArabic = pageIndex == activeEditionsList.size
                            val edition = activeEditionsList.getOrNull(pageIndex)
                            val available = edition == null || surah.verses.any { !it.mealsMap[edition].isNullOrBlank() }
                            QuranReaderPageIndex(
                                groups = if (available) groupSurahVerses(surah.verses, edition, disableGrouping = isArabic) else emptyList(),
                                hasBesmeleHeader = available && hasQuranBesmeleHeader(surah.id, isArabic)
                            )
                        }
                    }

                    var currentReadingVerseNumber by remember(surah.id) { androidx.compose.runtime.mutableIntStateOf(1) }
                    var currentReadingOffset by remember(surah.id) { androidx.compose.runtime.mutableIntStateOf(0) }
                    var isReadingAtBesmele by remember(surah.id) {
                        androidx.compose.runtime.mutableStateOf(hasQuranBesmeleHeader(surah.id, isArabicPage = false))
                    }
                    var isProgrammaticScroll by remember { androidx.compose.runtime.mutableStateOf(false) }

                    fun getVerseNumberFromItemIndex(itemIndex: Int, pageIndex: Int): Int =
                        pageIndices[pageIndex].verseNumberForItem(itemIndex)

                    fun getItemIndexFromVerseNumber(verseNum: Int, targetPageIndex: Int, isBesmele: Boolean = false): Int =
                        pageIndices[targetPageIndex].itemIndexForVerse(verseNum, atBesmele = isBesmele)

                    // Unified position sync and vertical scroll tracker per active page
                    androidx.compose.runtime.LaunchedEffect(currentPage, surah.id, activeEditionsList) {
                        val activeState = listStates.getOrNull(currentPage) ?: return@LaunchedEffect
                        
                        // 1. Programmatically scroll active page to the stored reading verse position
                        isProgrammaticScroll = true
                        val targetIdx = getItemIndexFromVerseNumber(currentReadingVerseNumber, currentPage, isBesmele = isReadingAtBesmele)
                        activeState.scrollToItem(targetIdx, currentReadingOffset)
                        kotlinx.coroutines.delay(100)
                        isProgrammaticScroll = false

                        // 2. Track user vertical scrolling on the active page
                        androidx.compose.runtime.snapshotFlow {
                            Triple(
                                activeState.firstVisibleItemIndex,
                                activeState.firstVisibleItemScrollOffset,
                                activeState.isScrollInProgress
                            )
                        }.collect { (itemIdx, offset, isScrolling) ->
                            if (!isProgrammaticScroll && !pagerState.isScrollInProgress && isScrolling) {
                                val isBesmele = pageIndices[currentPage].isBesmeleItem(itemIdx)
                                val vNum = if (isBesmele) 1 else getVerseNumberFromItemIndex(itemIdx, currentPage)
                                
                                currentReadingVerseNumber = vNum
                                currentReadingOffset = offset
                                isReadingAtBesmele = isBesmele
                            }
                        }
                    }

                    // Explicit target verse scroll requested from outside (search, bookmarks, cüz, audio jump)
                    LaunchedEffect(targetVerseIndexToScroll, isLoadingSurah, currentSurah?.id) {
                        val verseIdx = targetVerseIndexToScroll
                        val surahObj = currentSurah
                        if (verseIdx != null && surahObj != null && !isLoadingSurah) {
                            val targetVerseNum = verseIdx + 1
                            isProgrammaticScroll = true
                            currentReadingVerseNumber = targetVerseNum
                            currentReadingOffset = 0
                            isReadingAtBesmele = false
                            
                            val activeState = listStates.getOrNull(currentPage)
                            val targetIndex = getItemIndexFromVerseNumber(targetVerseNum, currentPage)
                            activeState?.scrollToItem(targetIndex, 0)
                            
                            targetVerseIndexToScroll = null
                            kotlinx.coroutines.delay(150)
                            isProgrammaticScroll = false
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val listState = listStates.getOrNull(page) ?: rememberLazyListState()
                        var isFastScrolling by remember { androidx.compose.runtime.mutableStateOf(false) }
                        var fastScrollY by remember { androidx.compose.runtime.mutableStateOf(0f) }
                        var fastScrollPercent by remember { androidx.compose.runtime.mutableStateOf(0f) }
                        val view = androidx.compose.ui.platform.LocalView.current
                        var previousTargetIndex by remember { androidx.compose.runtime.mutableStateOf(-1) }

                        val isArabicPage = (page == activeEditionsList.size)
                        val edKey = if (page < activeEditionsList.size) activeEditionsList[page] else null
                        val pageIndex = pageIndices[page]
                        val groupedVerses = pageIndex.groups

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentPadding = PaddingValues(top = 8.dp, bottom = com.example.ui.theme.navBarBottomPadding(extra = 8.dp), start = 8.dp, end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Beautiful Besmele Header item (For all Surahs EXCEPT Fâtihah (1) and Tevbe (9))
                                if (pageIndex.hasBesmeleHeader) {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    if (page == activeEditionsList.size) { // Arabic
                                                        Text(
                                                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                                            fontSize = 26.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    } else if (page == activeEditionsList.size + 1) { // Pronunciation
                                                        Text(
                                                            text = "Bismillâhirrahmânirrahîm",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    } else { // Translations
                                                        Text(
                                                            text = "Rahmân ve Rahîm olan Allah'ın adıyla",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (edKey != null && groupedVerses.isEmpty()) {
                                    item {
                                        Text(
                                            "Bu kaynak bu pakette bulunmuyor.",
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                items(groupedVerses) { groupItem ->
                                    val isGroupPlaying = playingVerseNumber != null && playingVerseNumber in groupItem.startVerseNumber..groupItem.endVerseNumber
                                    val mainVerse = groupItem.mainVerse

                                    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isGroupPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (isGroupPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onLongClick = {
                                                    selectedVerseForCopy = mainVerse
                                                },
                                                onClick = {}
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            // Verse Header Row (Verse Number & Audio Control)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = groupItem.numberLabel,
                                                        fontSize = (18 * fontScale).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isGroupPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isGroupPlaying) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (audioSourceType == "arabic") "🎧 Arapça" else "🎙️ Meal",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                // Play / Pause Button
                                                IconButton(
                                                    onClick = {
                                                        if (isGroupPlaying) {
                                                            pauseOrResumeAudio(surah)
                                                        } else {
                                                            playVerseAudio(surah, mainVerse.number, audioSourceType)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    if (isGroupPlaying && isAudioBuffering) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else if (isGroupPlaying && isAudioPlaying) {
                                                        Icon(
                                                            imageVector = Icons.Default.Pause,
                                                            contentDescription = "Duraklat",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Dinle",
                                                            tint = if (isGroupPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Verse Content
                                            if (page < activeEditionsList.size) {
                                                Text(
                                                    text = groupItem.mealText,
                                                    fontSize = (17 * fontScale).sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = (26 * fontScale).sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                                )
                                                val editionNotes = groupItem.verses.mapNotNull { it.footnotesMap[edKey] }
                                                    .filter { it.isNotBlank() }.distinct().joinToString("\n\n")
                                                if (editionNotes.isNotBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = editionNotes,
                                                        fontSize = (13 * fontScale).sp,
                                                        lineHeight = (20 * fontScale).sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            } else if (page == activeEditionsList.size) {
                                                Text(
                                                    text = mainVerse.arabic,
                                                    fontSize = (28 * fontScale).sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    lineHeight = (48 * fontScale).sp
                                                )
                                            } else {
                                                Column {
                                                    val fullPronunciation = groupItem.verses.joinToString(" ") { v ->
                                                        if (groupItem.verses.size > 1) "${v.pronunciation} (${v.number})" else v.pronunciation
                                                    }
                                                    Text(
                                                        text = fullPronunciation,
                                                        fontSize = (18 * fontScale).sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        lineHeight = (27 * fontScale).sp,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    val mealText = groupItem.mealText.trim()
                                                    if (mealText.isNotBlank()) {
                                                        val primaryMealEdition = activeEditionsList.firstOrNull {
                                                            !mainVerse.mealsMap[it].isNullOrBlank()
                                                        }
                                                        val formattedMeal = if (mealText.startsWith("“") || mealText.startsWith("\"")) mealText else "“$mealText”"
                                                        Text(
                                                            text = formattedMeal,
                                                            fontSize = (16 * fontScale).sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                                            lineHeight = (24 * fontScale).sp
                                                        )
                                                        if (primaryMealEdition != null) {
                                                            Spacer(Modifier.height(6.dp))
                                                            Text(
                                                                text = QuranReadingEditions.sourceLabel(primaryMealEdition),
                                                                fontSize = (11 * fontScale).sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            val editionNotes = groupItem.verses.mapNotNull { it.footnotesMap[primaryMealEdition] }
                                                                .filter { it.isNotBlank() }.distinct().joinToString("\n\n")
                                                            if (editionNotes.isNotBlank()) {
                                                                Spacer(Modifier.height(8.dp))
                                                                Text(
                                                                    text = editionNotes,
                                                                    fontSize = (13 * fontScale).sp,
                                                                    lineHeight = (20 * fontScale).sp,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        // Custom Fast Scroller on the right edge (Ayetler)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(28.dp)
                                .pointerInput(groupedVerses) {
                                    if (groupedVerses.isEmpty()) return@pointerInput
                                    var lastHapticTime = 0L
                                    detectVerticalDragGestures(
                                        onDragStart = { offset ->
                                            isFastScrolling = true
                                            fastScrollY = offset.y
                                            fastScrollPercent = (fastScrollY / size.height).coerceIn(0f, 1f)
                                            val targetIndex = (fastScrollPercent * (groupedVerses.size - 1)).toInt().coerceIn(0, groupedVerses.lastIndex)
                                            coroutineScope.launch { listState.scrollToItem(pageIndex.itemIndexForGroup(targetIndex)) }
                                            if (targetIndex != previousTargetIndex) {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastHapticTime > 80) {
                                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                    lastHapticTime = currentTime
                                                }
                                                previousTargetIndex = targetIndex
                                            }
                                        },
                                        onDragEnd = { isFastScrolling = false },
                                        onDragCancel = { isFastScrolling = false }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        fastScrollY += dragAmount
                                        fastScrollPercent = (fastScrollY / size.height).coerceIn(0f, 1f)
                                        val targetIndex = (fastScrollPercent * (groupedVerses.size - 1)).toInt().coerceIn(0, groupedVerses.lastIndex)
                                        coroutineScope.launch { listState.scrollToItem(pageIndex.itemIndexForGroup(targetIndex)) }
                                        if (targetIndex != previousTargetIndex) {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastHapticTime > 80) {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                lastHapticTime = currentTime
                                            }
                                            previousTargetIndex = targetIndex
                                        }
                                    }
                                }
                        )

                        // Scrolling indicator bubble (Ayetler)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isFastScrolling && groupedVerses.isNotEmpty(),
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            val targetIndex = (fastScrollPercent * (groupedVerses.size - 1)).toInt().coerceIn(0, groupedVerses.lastIndex)
                            val currentVisibleLabel = groupedVerses[targetIndex].numberLabel
                            
                            Box(
                                modifier = Modifier
                                    .padding(end = 36.dp)
                                    .graphicsLayer {
                                        translationY = (fastScrollY - 140.dp.toPx()).coerceIn(-200f, 200f)
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Üstteki küçük baloncuklar
                                    for (i in 3 downTo 1) {
                                        val vIndex = targetIndex - i
                                        val bubbleSize = (32 + (3-i) * 6).dp
                                        val fontSize = (10 + (3-i) * 2).sp
                                        if (vIndex >= 0 && vIndex < groupedVerses.size) {
                                            Box(
                                                modifier = Modifier
                                                    .size(bubbleSize)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f + (3-i)*0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = groupedVerses[vIndex].numberLabel, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(bubbleSize))
                                        }
                                    }
                                    
                                    // Ana merkez baloncuk
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)),
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$currentVisibleLabel Ayet",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    
                                    // Alttaki küçük baloncuklar
                                    for (i in 1..3) {
                                        val vIndex = targetIndex + i
                                        val bubbleSize = (44 - (i-1) * 6).dp
                                        val fontSize = (14 - (i-1) * 2).sp
                                        if (vIndex >= 0 && vIndex < groupedVerses.size) {
                                            Box(
                                                modifier = Modifier
                                                    .size(bubbleSize)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f - (i-1)*0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = groupedVerses[vIndex].numberLabel, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(bubbleSize))
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Audio Player Bar
                        androidx.compose.animation.AnimatedVisibility(
                            visible = playingVerseNumber != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            val surahObj = currentSurah
                            val playingNum = playingVerseNumber ?: 1
                            if (surahObj != null) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "${surahObj.name} • ${playingNum}. Ayet",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (audioSourceType == "arabic") "Mişari el-Afasi • Ayet tilaveti" else "Türkçe meal • Cihaz seslendirmesi",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                                    )
                                                }
                                            }

                                            // Player Controls
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                // Previous Verse
                                                IconButton(
                                                    onClick = {
                                                        if (playingNum > 1) {
                                                            targetVerseIndexToScroll = playingNum - 2
                                                            playVerseAudio(surahObj, playingNum - 1, audioSourceType)
                                                        }
                                                    },
                                                    enabled = playingNum > 1,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipPrevious,
                                                        contentDescription = "Önceki Ayet",
                                                        tint = if (playingNum > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                // Play / Pause
                                                IconButton(
                                                    onClick = { pauseOrResumeAudio(surahObj) },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    if (isAudioBuffering) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(22.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                            contentDescription = if (isAudioPlaying) "Duraklat" else "Oynat",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                }

                                                // Next Verse
                                                IconButton(
                                                    onClick = {
                                                        if (playingNum < surahObj.verses.size) {
                                                            targetVerseIndexToScroll = playingNum
                                                            playVerseAudio(surahObj, playingNum + 1, audioSourceType)
                                                        }
                                                    },
                                                    enabled = playingNum < surahObj.verses.size,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipNext,
                                                        contentDescription = "Sonraki Ayet",
                                                        tint = if (playingNum < surahObj.verses.size) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                // Close
                                                IconButton(
                                                    onClick = { stopAudio() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Kapat",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Mode & Autoplay Controls Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Mode switch (Arapça / Meal)
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    .padding(2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .background(if (audioSourceType == "arabic") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                        .clickable { selectAudioSource("arabic") }
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "🎧 Arapça",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (audioSourceType == "arabic") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .background(if (audioSourceType == "meal") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                        .clickable { selectAudioSource("meal") }
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "🎙️ Türkçe Meal",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (audioSourceType == "meal") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }

                                            // Auto next verse toggle
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { autoPlayNextVerse = !autoPlayNextVerse }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Otomatik Geçiş",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Switch(
                                                    checked = autoPlayNextVerse,
                                                    onCheckedChange = { autoPlayNextVerse = it },
                                                    modifier = Modifier.scale(0.65f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Bottom Bar for Quran Reading Screen
            if (currentSurah != null) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showFontSizeSlider) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "A",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = fontScale,
                                        onValueChange = { fontScale = it },
                                        valueRange = 0.75f..1.5f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = "A",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${(fontScale * 100).toInt()}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // 1. Sure Listesi / Ana Sayfa
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (currentSurah != null) {
                                        currentSurah = null
                                    } else {
                                        onBack()
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (currentSurah != null) Icons.AutoMirrored.Filled.List else Icons.Default.Home,
                                contentDescription = if (currentSurah != null) "Sure Listesi" else strings.navHome,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentSurah != null) "Sure Listesi" else strings.navHome,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 2. Sureyi Oynat / Durdur
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val s = currentSurah ?: return@clickable
                                    if (playingVerseNumber != null) {
                                        pauseOrResumeAudio(s)
                                    } else {
                                        playVerseAudio(s, 1, audioSourceType)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Oynat",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAudioPlaying) "Duraklat" else "Oynat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Yazı Boyutu (Font size toggle)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFontSizeSlider = !showFontSizeSlider }
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = strings.fontSizeLabel,
                                tint = if (showFontSizeSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Yazı Boyutu",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showFontSizeSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 4. Gece / Gündüz Modu (Dark mode toggle)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setForceDarkTheme(!isForcedDark) }
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isForcedDark) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Koyu Tema",
                                tint = if (isForcedDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isForcedDark) "Gündüz" else "Gece",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}
}
}

fun cleanTransliteration(text: String): String {
    if (text.isBlank()) return text
    var t = text
        .replace("ḥ", "h").replace("Ḥ", "H")
        .replace("ṭ", "t").replace("Ṭ", "T")
        .replace("ḍ", "d").replace("Ḍ", "D")
        .replace("ẕ", "z").replace("Ẕ", "Z")
        .replace("ṣ", "s").replace("Ṣ", "S")
        .replace("ḳ", "k").replace("Ḳ", "K")
        .replace("ḫ", "h").replace("Ḫ", "H")
        .replace("ẓ", "z").replace("Ẓ", "Z")
        .replace("ṯ", "s").replace("Ṯ", "S")
        .replace("ŝ", "s").replace("Ŝ", "S")
        .replace("ġ", "ğ").replace("Ġ", "Ğ")

    t = t.replace(Regex("(?i)sh"), "ş")
        .replace(Regex("(?i)kh"), "h")
        .replace(Regex("(?i)dh"), "z")
        .replace(Regex("(?i)th"), "s")
        .replace(Regex("(?i)gh"), "ğ")
        .replace(Regex("(?i)ch"), "ç")
        .replace(Regex("(?i)zh"), "j")

    t = t.replace("aa", "â").replace("Aa", "Â").replace("AA", "Â")
        .replace("ee", "î").replace("Ee", "Î").replace("EE", "Î")
        .replace("uu", "û").replace("Uu", "Û").replace("UU", "Û")
        .replace("oo", "û")

    t = t.replace(Regex("([a-zA-ZâîûÂÎÛçğıöşüÇĞİÖŞÜ])w([a-zA-ZâîûÂÎÛçğıöşüÇĞİÖŞÜ])"), "$1v$2")
        .replace("W", "V").replace("w", "v")

    t = t.replace("`", "")
        .replace("ʿ", "")
        .replace("ʾ", "")
        .replace("’", "'")

    if (t.isNotEmpty() && t[0].isLowerCase()) {
        t = t[0].uppercaseChar() + t.substring(1)
    }
    return t
}

fun Modifier.drawSimplePaperTexture(isDark: Boolean): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height
    
    // 1. Draw a base background gradient for warm paper aging / vignette effect
    val basePaperColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF1F1714) else androidx.compose.ui.graphics.Color(0xFFFAF4EA)
    drawRect(color = basePaperColor)

    // 2. 3D Spine and page-fold shadows (Soft Book Page effect)
    val spineShadow = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = if (isDark) listOf(
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
            androidx.compose.ui.graphics.Color.Transparent
        ) else listOf(
            androidx.compose.ui.graphics.Color(0xFF8B7355).copy(alpha = 0.18f),
            androidx.compose.ui.graphics.Color.Transparent
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(40.dp.toPx(), 0f)
    )
    drawRect(brush = spineShadow, size = androidx.compose.ui.geometry.Size(40.dp.toPx(), height))

    val edgeShadow = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = if (isDark) listOf(
            androidx.compose.ui.graphics.Color.Transparent,
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f)
        ) else listOf(
            androidx.compose.ui.graphics.Color.Transparent,
            androidx.compose.ui.graphics.Color(0xFF8B7355).copy(alpha = 0.08f)
        ),
        start = androidx.compose.ui.geometry.Offset(width - 24.dp.toPx(), 0f),
        end = androidx.compose.ui.geometry.Offset(width, 0f)
    )
    drawRect(
        brush = edgeShadow,
        topLeft = androidx.compose.ui.geometry.Offset(width - 24.dp.toPx(), 0f),
        size = androidx.compose.ui.geometry.Size(24.dp.toPx(), height)
    )

    // 3. Organic paper grains & fibers for antique look
    val grainColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF000000).copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color(0xFF8B7355).copy(alpha = 0.05f)
    val numGrains = 250
    val random = java.util.Random(42) // Fixed seed
    for (i in 0 until numGrains) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        val isDot = random.nextBoolean()
        
        if (isDot) {
            drawCircle(
                color = grainColor,
                radius = random.nextFloat() * 1.5f + 0.5f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        } else {
            val length = 3f + random.nextFloat() * 6f
            val angle = random.nextFloat() * kotlin.math.PI.toFloat()
            drawLine(
                color = grainColor,
                start = androidx.compose.ui.geometry.Offset(x, y),
                end = androidx.compose.ui.geometry.Offset(
                    x + length * kotlin.math.cos(angle.toDouble()).toFloat(),
                    y + length * kotlin.math.sin(angle.toDouble()).toFloat()
                ),
                strokeWidth = random.nextFloat() * 0.8f + 0.4f
            )
        }
    }
}
