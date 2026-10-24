package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CurrentWeather
import com.example.data.WeatherResponse
import com.example.data.Timings
import com.example.data.TimingsData
import com.example.data.MiracleItem
import com.example.data.QuranMiraclesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun LiveClockHeader(locale: Locale) {
    var currentDateTimeState by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTimeState = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val sdfTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val sdfDate = remember(locale) { SimpleDateFormat("d MMMM yyyy, EEEE", locale) }
    val timeString = sdfTime.format(currentDateTimeState)
    val dateString = sdfDate.format(currentDateTimeState)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = timeString,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/** One tappable destination in the home screen's quick-access area. */
private data class QuickEntry(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: PrayerViewModel, 
    onNavigateToSearch: () -> Unit,
    onNavigateToQuranReading: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSteps: () -> Unit = {},
    onNavigateToAlarms: () -> Unit = {}
) {
    val timings by viewModel.timings.collectAsState()
    val city by viewModel.cityFlow.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val currentAndNext by viewModel.currentAndNextPrayer.collectAsState()
    val progress by viewModel.prayerProgress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val strings = LocalAppStrings.current
    val currentTheme by viewModel.currentTheme.collectAsState()
    val selectedBgId by viewModel.selectedBackground.collectAsState()
    
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val locale = remember(selectedLang) {
        when (selectedLang.lowercase()) {
            "en" -> Locale.ENGLISH
            "ar" -> Locale("ar")
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "ru" -> Locale("ru")
            else -> Locale("tr", "TR")
        }
    }

    val flipAnimatable = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(currentAndNext?.first) {
        if (currentAndNext?.first != null) {
            flipAnimatable.snapTo(180f)
            flipAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
    }

    var showActionSheet by remember { mutableStateOf(false) }

    // Where the global actions live is a property of the theme, not a constant.
    val spec = LocalThemeSpec.current
    val actionPlacement = spec.actionPlacement
    val heroScale = spec.heroScale
    val quickActions = rememberQuickActions(
        askLabel = strings.askQuestion,
        toolsLabel = strings.tools,
        settingsLabel = strings.settings,
        onAsk = onNavigateToAssistant,
        onTools = onNavigateToDiscover,
        onSettings = onNavigateToSettings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The floating bar overlays this list, so the last card reserves its
            // height as scroll padding. That lets content pass behind the bar
            // while still being fully reachable.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = com.example.ui.theme.navBarBottomPadding()
            )
        ) {
        // Modern Architectural Top Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (spec.headerStyle != com.example.ui.theme.HomeHeaderStyle.CLASSIC) {
                    HeritageHomeHeader(city ?: "Konum Seç...", locale, onNavigateToSearch)
                } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeCardSurface(
                        modifier = Modifier.clickable { onNavigateToSearch() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Konum Değiştir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = city ?: "Konum Seç...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date & Live Clock Row (Isolated Recomposition for 120 FPS Fluidity)
                LiveClockHeader(locale = locale)
                }

                // HEADER_CHIPS themes put the global actions directly under the
                // location, so the header block is the navigation.
                if (actionPlacement == ActionPlacement.HEADER_CHIPS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickActionChips(
                        actions = quickActions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // HEADER_ACTION_SHEET themes keep the header to a single control.
                if (actionPlacement == ActionPlacement.HEADER_ACTION_SHEET) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .testTag("home_actions_button")
                                .clickable { showActionSheet = true }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "Araçlar ve ayarlar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Araçlar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isLoading && timings == null) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (timings == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HomeCardSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Namaz Vakitleri Yüklenemedi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lütfen internet bağlantınızı kontrol edin ve tekrar deneyin veya başka bir konum seçin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            androidx.compose.material3.Button(
                                onClick = { viewModel.updateLocation() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Yeniden Dene",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Konumu Güncelle ve Dene", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = { onNavigateToSearch() },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Konum Ara",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Manuel Konum Ara", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // INLINE_TILES: the actions sit above the hero, the way a poster
            // puts its utility strip under the masthead.
            if (actionPlacement == ActionPlacement.INLINE_TILES) {
                item {
                    QuickActionTiles(
                        actions = quickActions,
                        asGrid = spec.layout == ThemeLayout.DASHBOARD || spec.layout == ThemeLayout.LEDGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = LocalThemeMetrics.current.screenGutter,
                                vertical = 6.dp
                            )
                    )
                }
            }

            // Hero countdown. The composition is chosen by the active theme's
            // layout family rather than by a swipeable carousel: if the user can
            // pick any arrangement at will, the theme stops being a design and
            // becomes a colour filter. Each layout has a distinct silhouette.
            item {
                val timingsObj = timings?.timings
                if (timingsObj != null) {
                    ThemePrayerScene(timingsObj, remainingTime, currentAndNext, progress, todayHijri = timings?.date?.hijri)
                }
            }

            // Compact Pedometer (Adımsayar) Mini Window on Home Screen
            item(key = "home_steps_alarm_shortcuts") {
                com.example.alarm.HomeStepsAndAlarmRow(onAlarmClick = onNavigateToAlarms) { modifier ->
                    HomeStepsMiniWidget(
                        onNavigateToSteps = onNavigateToSteps,
                        modifier = modifier
                    )
                }
            }

            val weatherSectionIndex = spec.sections.indexOf(com.example.ui.theme.HomeSection.WEATHER)
            spec.sections.forEachIndexed { sectionIndex, section ->
                item(key = section.name) {
                    val goldFrame = weatherSectionIndex >= 0 && sectionIndex > weatherSectionIndex
                    when (section) {
                        com.example.ui.theme.HomeSection.WEATHER -> WeatherSection(weather = weather, city = city ?: "")
                        com.example.ui.theme.HomeSection.MIRACLE -> HomeRandomMiracleCard(
                            goldFrame = goldFrame,
                            onNavigateToMiracle = { miracleId ->
                                viewModel.setTargetMiracleId(miracleId)
                                onNavigateToDiscover()
                            }
                        )
                        com.example.ui.theme.HomeSection.QUOTE -> DailyQuoteCard(goldFrame = goldFrame)
                        com.example.ui.theme.HomeSection.DETAILS -> timings?.timings?.let { DetailedTimingsCard(timings = it, goldFrame = goldFrame) }
                    }
                }
            }

            // FOOTER_TILES: the actions are the last thing on the page, the way a
            // colophon sits at the end of a chapter.
            if (actionPlacement == ActionPlacement.FOOTER_TILES) {
                item {
                    QuickActionTiles(
                        actions = quickActions,
                        asGrid = spec.layout == ThemeLayout.DASHBOARD || spec.layout == ThemeLayout.LEDGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = LocalThemeMetrics.current.screenGutter,
                                vertical = 10.dp
                            )
                    )
                }
            }

        }
    }
    }

    if (showActionSheet) {
        QuickActionSheet(
            actions = quickActions,
            onDismiss = { showActionSheet = false }
        )
    }

}

/** Bottom sheet used by the HEADER_ACTION_SHEET themes. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionSheet(actions: List<QuickAction>, onDismiss: () -> Unit) {
    val spec = LocalThemeSpec.current
    val colors = LocalThemeColors.current
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(
            topStart = 28.dp, topEnd = 28.dp
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Araçlar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            actions.forEach { action ->
                Surface(
                    shape = spec.shapes.buttonShape,
                    color = action.tint.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            action.onClick()
                            onDismiss()
                        }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(action.icon, null, tint = action.tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            action.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeRandomMiracleCard(
    goldFrame: Boolean = true,
    onNavigateToMiracle: (String) -> Unit
) {
    val allMiracles = remember { QuranMiraclesRepository.miraclesList }
    if (allMiracles.isEmpty()) return

    // Track current index and shuffle generation for timer reset
    var currentMiracleIndex by remember {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        mutableIntStateOf(kotlin.math.abs(dayOfYear * 43 + 7) % allMiracles.size)
    }
    var manualShuffleCount by remember { mutableIntStateOf(0) }

    val currentMiracle = remember(currentMiracleIndex, allMiracles) {
        allMiracles.getOrNull(currentMiracleIndex) ?: allMiracles.first()
    }

    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "shuffle_rotation"
    )

    fun shuffleMiracle(isManual: Boolean = false) {
        if (isManual) {
            rotationDegrees += 360f
            manualShuffleCount++
        }
        var newIndex: Int
        do {
            newIndex = kotlin.random.Random.nextInt(allMiracles.size)
        } while (newIndex == currentMiracleIndex && allMiracles.size > 1)
        currentMiracleIndex = newIndex
    }

    // Auto-rotate miracle periodically every 8 seconds
    LaunchedEffect(manualShuffleCount) {
        while (true) {
            kotlinx.coroutines.delay(8000L)
            shuffleMiracle(isManual = false)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    HomeCardSurface(
        goldFrame = goldFrame,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("home_random_miracle_card")
            .clickable { onNavigateToMiracle(currentMiracle.id) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Sparkle Badge + Live Dot + Shuffle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "KUR'AN MUCİZESİ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = currentMiracle.category.displayName,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(250))
                        },
                        label = "category_transition"
                    ) { categoryName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = categoryName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Shuffle / Randomize Button
                IconButton(
                    onClick = { shuffleMiracle(isManual = true) },
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer(rotationZ = animatedRotation)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Farklı Mucize Göster",
                        tint = primaryColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated content body transitioning between miracles
            AnimatedContent(
                targetState = currentMiracle,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 4 })
                        .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -it / 4 })
                },
                label = "miracle_content_transition"
            ) { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stack source lines so a long reference never squeezes into a narrow column.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.surahName}, ${item.verseNumber}. Ayet",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                        if (item.discoveryCentury.isNotBlank()) {
                            Text(
                                text = item.discoveryCentury,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teaser / Hook Text
                    val teaserText = remember(item) {
                        if (item.subtitle.isNotBlank() && item.subtitle != item.title) {
                            item.subtitle
                        } else if (item.scientificDiscoveryTitle.isNotBlank()) {
                            item.scientificDiscoveryTitle
                        } else {
                            item.scientificExplanation.take(150) + "..."
                        }
                    }

                    Text(
                        text = teaserText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asırlar önce haber verilen ilmi hakikat",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Keşfi İncele",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyQuoteCard(goldFrame: Boolean = true) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Ayet, 1: Hadis, 2: Dua
    
    val ayetText = "“Rabbiniz şöyle buyurdu: Bana dua edin, duanızı kabul edeyim.”"
    val ayetSource = "Mü'min Suresi, 60. Ayet"
    
    val hadisText = "“Kolaylaştırınız, zorlaştırmayınız; müjdeleyiniz, nefret ettirmeyiniz.”"
    val hadisSource = "Buhârî, İlim, 11"
    
    val duaText = "“Allah'ım! Faydasız ilimden, ürpermeyen kalpten ve kabul olunmayan duadan sana sığınırım.”"
    val duaSource = "Müslim, Zikir, 73"

    val (currentText, currentSource) = when (selectedTab) {
        0 -> Pair(ayetText, ayetSource)
        1 -> Pair(hadisText, hadisSource)
        else -> Pair(duaText, duaSource)
    }

    val theme = LocalAppTheme.current
    val isGlassTheme = theme != AppTheme.CLASSIC_EMERALD && theme.spec.isGlass
    val chipShape = if (isGlassTheme) theme.buttonShape else RoundedCornerShape(16.dp)
    HomeCardSurface(
        goldFrame = goldFrame,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chips / Tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Günün Ayeti", "Günün Hadisi", "Günün Duası").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val tc = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    val borderModifier = if (isSelected) {
                        if (isGlassTheme) Modifier.border(theme.buttonBorder, chipShape) else Modifier
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), chipShape)
                    }

                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(bg)
                            .then(borderModifier)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tc,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Text Content
            Text(
                text = currentText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Source Text
            Text(
                text = currentSource,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PrayerTimeRow(name: String, time: String, isCurrent: Boolean) {
    val bgColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val textColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun clean(time: String) = time.take(5)

private fun addMinutesTime(timeStr: String, minutes: Int): String {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val cleanStr = timeStr.replace(Regex("[^0-9:]"), "")
        val date = format.parse(cleanStr) ?: return ""
        val cal = Calendar.getInstance().apply { time = date }
        cal.add(Calendar.MINUTE, minutes)
        format.format(cal.time)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun DetailedTimingsCard(timings: Timings, modifier: Modifier = Modifier, goldFrame: Boolean = true) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    fun addMinutes(timeStr: String, minutes: Int): String {
        return try {
            val cleanStr = timeStr.replace(Regex("[^0-9:]"), "")
            val date = format.parse(cleanStr) ?: return ""
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MINUTE, minutes)
            format.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }

    val israk = addMinutes(timings.Sunrise, 45)
    val duha = "$israk - ${addMinutes(timings.Dhuhr, -45)}"
    val kerahatMorning = "${clean(timings.Sunrise)} - $israk"
    val kerahatNoon = "${addMinutes(timings.Dhuhr, -45)} - ${clean(timings.Dhuhr)}"
    val kerahatEvening = "${addMinutes(timings.Maghrib, -45)} - ${clean(timings.Maghrib)}"
    val teheccud = "01:30 - ${clean(timings.Fajr)}" // typical tahajjud range in TR

    HomeCardSurface(
        goldFrame = goldFrame,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detaylı Vakitler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.LocationOn, // using dropdown styles
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        if (!isExpanded) rotationZ = -90f
                    }
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                DetailedRow("İşrak Vakti", israk)
                DetailedRow("Duhâ Vakti (Kuşluk)", duha)
                DetailedRow("Teheccüd Vakti", teheccud)
                DetailedRow("Sabah Keraheti", kerahatMorning)
                DetailedRow("Öğle Keraheti", kerahatNoon)
                DetailedRow("Akşam Keraheti", kerahatEvening)
            }
        }
    }
}

@Composable
fun DetailedRow(title: String, timeRange: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = timeRange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
    }
}

@Composable
fun QuranHeroCard(onNavigate: () -> Unit) {
    HomeCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "القرآن الكريم",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Kur’an-ı Kerim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "114 sureyi Arapça metni ve Rowwad Türkçe mealiyle çevrimdışı okuyun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Şimdi Okumaya Başla",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Oku",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer { rotationZ = -90f }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "Kuran-ı Kerim",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

fun getLocalizedPrayerName(key: String?, strings: AppStrings): String {
    if (key == null) return ""
    return when (key) {
        "İmsak", "Fajr" -> strings.fajr
        "Sabah" -> strings.fajr
        "Güneş", "Sunrise" -> strings.sunrise
        "Öğle", "Dhuhr" -> strings.dhuhr
        "İkindi", "Asr" -> strings.asr
        "Akşam", "Maghrib" -> strings.maghrib
        "Yatsı", "Isha" -> strings.isha
        else -> key
    }
}

@Composable
fun PrayerTimesGrid(timings: com.example.data.Timings, currentAndNext: Pair<String?, String?>?) {
    val currentPrayerName = currentAndNext?.first
    val strings = LocalAppStrings.current
    
    fun clean(t: String) = t.take(5)
    
    val list = listOf(
        Triple(strings.fajr, clean(timings.Fajr), "İmsak" == currentPrayerName),
        Triple(strings.fajr + " (Namaz)", calculateSabah(clean(timings.Sunrise)), "Sabah" == currentPrayerName),
        Triple(strings.sunrise, clean(timings.Sunrise), "Güneş" == currentPrayerName),
        Triple(strings.dhuhr, clean(timings.Dhuhr), "Öğle" == currentPrayerName),
        Triple(strings.asr, clean(timings.Asr), "İkindi" == currentPrayerName),
        Triple(strings.maghrib, clean(timings.Maghrib), "Akşam" == currentPrayerName),
        Triple(strings.isha, clean(timings.Isha), "Yatsı" == currentPrayerName)
    )

    val currentIndex = remember(list) { list.indexOfFirst { it.third }.takeIf { it >= 0 } ?: 0 }
    val initialTargetIndex = remember(currentIndex) { maxOf(0, currentIndex - 1) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(initialTargetIndex) {
        val targetPx = with(density) { (initialTargetIndex * 102).dp.roundToPx() }
        scrollState.scrollTo(targetPx)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        list.forEach { item ->
            PrayerTimeTile(item.first, item.second, item.third)
        }
    }
}
@Composable
fun PrayerTimeTile(name: String, time: String, isCurrent: Boolean) {
    val theme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val isDarkTile = LocalThemeIsDark.current
    val tileShape = RoundedCornerShape(22.dp)
    val tileBorder = if (isCurrent) {
        BorderStroke(2.dp, themeColors.goldMotifColor)
    } else {
        BorderStroke(
            1.4.dp,
            Brush.linearGradient(
                colors = listOf(
                    themeColors.goldMotifColor.copy(alpha = if (isDarkTile) 0.62f else 0.72f),
                    themeColors.primary.copy(alpha = if (isDarkTile) 0.52f else 0.58f),
                    Color.White.copy(alpha = if (isDarkTile) 0.32f else 0.65f)
                )
            )
        )
    }

    val textColor = if (isCurrent) theme.buttonTextColor else themeColors.primary
    val secondaryTextColor = if (isCurrent) theme.buttonTextColor.copy(alpha = 0.96f) else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .frostedGlassBackdrop(
                shape = tileShape,
                tintColor = if (isCurrent) theme.buttonGradient.first() else themeColors.primary,
                tintAlpha = if (isCurrent) 0.78f else (if (isDarkTile) 0.22f else 0.16f),
                surfaceVeilAlpha = if (isCurrent) 0.26f else (if (isDarkTile) 0.36f else 0.44f)
            )
            .then(
                if (isCurrent) {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                theme.buttonGradient.first().copy(alpha = 0.72f),
                                theme.buttonGradient.last().copy(alpha = 0.82f)
                            )
                        ),
                        shape = tileShape
                    )
                } else {
                    Modifier
                }
            )
            .border(tileBorder, tileShape)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryTextColor,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = time,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            if (isCurrent) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(textColor.copy(alpha = 0.22f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ŞU AN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = " ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


private fun calculateSabah(sunrise: String): String {
    try {
        val parts = sunrise.split(":")
        if (parts.size == 2) {
            var h = parts[0].toInt()
            val m = parts[1].toInt()
            h -= 1
            if (h < 0) h += 24
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sunrise
}



@Composable
fun WeatherSection(weather: WeatherResponse?, city: String) {
    if (weather == null || weather.current == null || weather.current.temperature_2m == null) return
    
    val weatherPrimaryColor = MaterialTheme.colorScheme.primary
    var expanded by remember { mutableStateOf(false) }
    var expandedDayIndex by remember { mutableStateOf<Int?>(null) }
    
    val temp = "${weather.current.temperature_2m}°C"
    val isSunny = weather.current.weather_code == 0 || weather.current.weather_code == 1 || weather.current.weather_code == 2
    val icon = if (isSunny) Icons.Default.WbSunny else Icons.Default.Cloud
    val desc = if (isSunny) "Açık/Parçalı Bulutlu" else "Bulutlu/Yağışlı"
    
    // Extra Current details
    val humidity = weather.current.relative_humidity_2m?.let { "%$it" } ?: "-"
    val wind = weather.current.wind_speed_10m?.let { "${it.toInt()} km/s" } ?: "-"
    val feelsLike = weather.current.apparent_temperature?.let { "${it.toInt()}°C" } ?: "-"

    HomeCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(homeCardShape())
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Hava Durumu",
                    tint = if (isSunny) Color(0xFFFFD54F) else Color.LightGray,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = city,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = temp,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Extra details
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeatherDetailItem("Hissedilen", feelsLike)
                    WeatherDetailItem("Nem", humidity)
                    WeatherDetailItem("Rüzgar", wind)
                    val maxUv = weather.daily?.uv_index_max?.firstOrNull()?.let { "%.1f".format(it) } ?: "-"
                    WeatherDetailItem("UV İndeksi", maxUv)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (expanded && weather.daily != null && weather.daily.time != null) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Haftalık Beklenti",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                    )
                    
                    val daysCount = minOf(weather.daily.time.size, 7)
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(daysCount) { i ->
                            val dateStr = weather.daily.time[i]
                            val minTemp = weather.daily.temperature_2m_min?.getOrNull(i) ?: 0.0
                            val maxTemp = weather.daily.temperature_2m_max?.getOrNull(i) ?: 0.0
                            val dcode = weather.daily.weather_code?.getOrNull(i) ?: 0
                            val dIsSunny = dcode == 0 || dcode == 1 || dcode == 2
                            val dIcon = if (dIsSunny) Icons.Default.WbSunny else Icons.Default.Cloud
                            val dColor = if (dIsSunny) Color(0xFFFFD54F) else Color.LightGray
                            
                            // parse date to show only day e.g. "Pzt" or "12 Oca"
                            val parsedDate = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val parsed = sdf.parse(dateStr)
                                val outSdf = java.text.SimpleDateFormat("d MMM", java.util.Locale.forLanguageTag("tr"))
                                parsed?.let { outSdf.format(it) } ?: dateStr
                            } catch (e: Exception) {
                                dateStr
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (expandedDayIndex == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.width(80.dp).clickable { 
                                    expandedDayIndex = if (expandedDayIndex == i) null else i 
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = parsedDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (expandedDayIndex == i) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        imageVector = dIcon,
                                        contentDescription = null,
                                        tint = if (expandedDayIndex == i) MaterialTheme.colorScheme.primary else dColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${maxTemp.toInt()}°",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (expandedDayIndex == i) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${minTemp.toInt()}°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (expandedDayIndex == i) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    
                                    val rainProb = weather.daily.precipitation_probability_max?.getOrNull(i) ?: 0
                                    if (rainProb > 0) {
                                        Text(
                                            text = "%$rainProb",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Hourly section
                    val dayIdx = expandedDayIndex
                    if (dayIdx != null && weather.hourly?.time != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Saatlik Beklenti",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                        )
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val startHour = dayIdx * 24
                            val currentHour = if (dayIdx == 0) {
                                val cal = java.util.Calendar.getInstance()
                                cal.get(java.util.Calendar.HOUR_OF_DAY)
                            } else {
                                0
                            }
                            
                            val endHour = startHour + 24
                            
                            for (h in startHour until endHour) {
                                if (dayIdx == 0 && h % 24 < currentHour) continue // Skip past hours for today
                                
                                val hTime = weather.hourly.time.getOrNull(h) ?: continue
                                val hTemp = weather.hourly.temperature_2m?.getOrNull(h) ?: continue
                                val hCode = weather.hourly.weather_code?.getOrNull(h) ?: 0
                                
                                val hIsSunny = hCode == 0 || hCode == 1 || hCode == 2
                                val hIcon = if (hIsSunny) Icons.Default.WbSunny else Icons.Default.Cloud
                                val hColor = if (hIsSunny) Color(0xFFFFD54F) else Color.LightGray
                                
                                val parsedHour = try {
                                    hTime.substring(11, 16) // extract "HH:mm" from "yyyy-MM-ddTHH:mm"
                                } catch (e: Exception) {
                                    hTime
                                }
                                
                                item {
                                    Column(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = parsedHour,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(
                                            imageVector = hIcon,
                                            contentDescription = null,
                                            tint = hColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${hTemp.toInt()}°",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
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

@androidx.compose.runtime.Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ==========================================
// 4 ALTERNATIVE HERO PRAYER SECTION STYLES
// ==========================================

fun parseToMinutes(timeStr: String): Int {
    return try {
        val clean = timeStr.replace(Regex("[^0-9:]"), "")
        val parts = clean.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) {
        0
    }
}

@Composable
fun HeroStyleGokKubbe(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings,
    showPrayerTimes: Boolean = true,
    todayHijri: com.example.data.HijriDate? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CountdownCardPager(
            timingsObj = timingsObj,
            remainingTime = remainingTime,
            currentAndNext = currentAndNext,
            progress = progress,
            strings = strings,
            todayHijri = todayHijri
        )

        if (showPrayerTimes) PrayerTimesGrid(timings = timingsObj, currentAndNext = currentAndNext)
    }
}

@Composable
fun HeroStyleSarayMihrab(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val theme = LocalAppTheme.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val sunriseMinutes = parseToMinutes(timingsObj.Sunrise)
    val sunsetMinutes = parseToMinutes(timingsObj.Maghrib)
    val isDay = nowMinutes in sunriseMinutes..sunsetMinutes
    val dayNightColor = if (isDay) Color(0xFFFFB300) else Color(0xFF2196F3)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paperLiftShadow(theme = theme, isDarkTheme = isDark)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🕌 $nextPrayerLocalized VAKTİNE KALAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = remainingTime,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = dayNightColor,
                    trackColor = dayNightColor.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val prayerList = listOf(
            "İmsak" to timingsObj.Imsak,
            "Güneş" to timingsObj.Sunrise,
            "Öğle" to timingsObj.Dhuhr,
            "İkindi" to timingsObj.Asr,
            "Akşam" to timingsObj.Maghrib,
            "Yatsı" to timingsObj.Isha
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(prayerList) { (name, time) ->
                val isNext = currentAndNext?.second?.equals(name, ignoreCase = true) == true
                val isCurrent = currentAndNext?.first?.equals(name, ignoreCase = true) == true

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isNext -> MaterialTheme.colorScheme.primary
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = when {
                        isNext -> MaterialTheme.colorScheme.onPrimary
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    border = BorderStroke(1.dp, if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.width(95.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(text = getLocalizedPrayerName(name, strings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = time.take(5), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        if (isNext) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Sıradaki", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStyleMinimalCircle(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val theme = LocalAppTheme.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val sunriseMinutes = parseToMinutes(timingsObj.Sunrise)
    val sunsetMinutes = parseToMinutes(timingsObj.Maghrib)
    val isDay = nowMinutes in sunriseMinutes..sunsetMinutes
    val circleColor = if (isDay) Color(0xFFFFB300) else Color(0xFF2196F3)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paperLiftShadow(theme = theme, isDarkTheme = isDark)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(theme.buttonBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = circleColor.copy(alpha = 0.18f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = trackColor, style = Stroke(width = 12f))
                        drawArc(
                            color = circleColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 12f, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${(progress * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = circleColor)
                        Text(text = "Geçen", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(text = "SIRADAKİ VAKİT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Text(text = nextPrayerLocalized, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = remainingTime, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = circleColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val prayerList = listOf(
            "İmsak" to timingsObj.Imsak,
            "Güneş" to timingsObj.Sunrise,
            "Öğle" to timingsObj.Dhuhr,
            "İkindi" to timingsObj.Asr,
            "Akşam" to timingsObj.Maghrib,
            "Yatsı" to timingsObj.Isha
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 6 step 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    for (j in 0..1) {
                        val (pName, pTime) = prayerList[i + j]
                        val isNext = currentAndNext?.second?.equals(pName, ignoreCase = true) == true
                        val isCurrent = currentAndNext?.first?.equals(pName, ignoreCase = true) == true

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(text = getLocalizedPrayerName(pName, strings), fontSize = 13.sp, fontWeight = if (isNext || isCurrent) FontWeight.Bold else FontWeight.Medium)
                                Text(text = pTime.take(5), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStyleKubbeList(
    timingsObj: Timings,
    remainingTime: String,
    currentAndNext: Pair<String?, String?>?,
    progress: Float,
    strings: AppStrings
) {
    val theme = LocalAppTheme.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val nextPrayerLocalized = getLocalizedPrayerName(currentAndNext?.second, strings)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$nextPrayerLocalized Vaktine", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = remainingTime, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                }

                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val prayerList = listOf(
            "İmsak" to timingsObj.Imsak,
            "Güneş" to timingsObj.Sunrise,
            "Öğle" to timingsObj.Dhuhr,
            "İkindi" to timingsObj.Asr,
            "Akşam" to timingsObj.Maghrib,
            "Yatsı" to timingsObj.Isha
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                prayerList.forEach { (pName, pTime) ->
                    val isNext = currentAndNext?.second?.equals(pName, ignoreCase = true) == true
                    val isCurrent = currentAndNext?.first?.equals(pName, ignoreCase = true) == true

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNext) MaterialTheme.colorScheme.primaryContainer else if (isCurrent) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isNext) MaterialTheme.colorScheme.primary else if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = getLocalizedPrayerName(pName, strings), fontSize = 14.sp, fontWeight = if (isNext || isCurrent) FontWeight.Bold else FontWeight.Normal)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isNext) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$remainingTime kaldı",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = pTime.take(5), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}


/* ============================================================================
   THEME-PLACED QUICK ACTIONS
   The three global actions (ask, tools, settings) move around depending on the
   active theme: inline tiles, a header chip strip, a footer block, a navigation
   destination, or a single header icon that opens a sheet. Two themes sharing a
   palette therefore still produce different screens.
   ========================================================================== */

private data class QuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun rememberQuickActions(
    askLabel: String,
    toolsLabel: String,
    settingsLabel: String,
    onAsk: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit
): List<QuickAction> {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    return listOf(
        QuickAction(askLabel, Icons.Default.ChatBubbleOutline, primary, onAsk),
        QuickAction(toolsLabel, Icons.Default.GridView, secondary, onTools),
        QuickAction(settingsLabel, Icons.Default.Settings, tertiary, onSettings)
    )
}

/** Full-width or grid tiles, used by INLINE_TILES and FOOTER_TILES. */
@Composable
private fun QuickActionTiles(
    actions: List<QuickAction>,
    asGrid: Boolean,
    modifier: Modifier = Modifier
) {
    if (asGrid) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionTile(actions[0], Modifier.weight(1f))
                QuickActionTile(actions[1], Modifier.weight(1f))
            }
            QuickActionTile(actions[2], Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            actions.forEach { QuickActionTile(it, Modifier.weight(1f)) }
        }
    }
}

/** Compact chips for the header strip used by HEADER_CHIPS themes. */
@Composable
private fun QuickActionChips(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .homeCardSurface()
                    .clickable { action.onClick() }
                    .heightIn(min = 42.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        action.icon,
                        action.label,
                        tint = action.tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = action.tint,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .homeCardSurface()
            .clickable { action.onClick() }
            .heightIn(min = 48.dp)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                action.icon,
                action.label,
                tint = action.tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                action.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = action.tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeStepsMiniWidget(
    onNavigateToSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { com.example.steps.StepStore.get(context) }
    val revision by store.changes.collectAsState()
    val todayKey = remember { com.example.steps.StepStore.dayKey() }

    val todaySteps = remember(revision, todayKey) {
        store.day(todayKey).toInt()
    }
    val dailyGoal = remember(revision) {
        com.example.steps.StepCounterService.settings(context).getInt("goal", 6000)
    }
    val safeGoal = dailyGoal.coerceAtLeast(1000)
    val progress = (todaySteps.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "home_steps_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentEmerald = Color(0xFF10B981)

    HomeCardSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onNavigateToSteps() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Text(
                text = "ADIMSAYAR",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                color = primaryColor,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Left: Circular Progress Ring + Step Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    drawArc(
                        color = primaryColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, accentEmerald, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Adımsayar",
                        tint = primaryColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "%${(progress * 100).toInt()}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Keep the essentials readable in the narrower side-by-side card.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale("tr", "TR"), "%,d", todaySteps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "Hedef ${String.format(Locale("tr", "TR"), "%,d", safeGoal)}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
            }
        }
    }
}

