package com.example

import com.example.ui.theme.drawCustomBackground
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.CalendarScreen
import com.example.ui.CompassScreen
import com.example.ui.DhikrScreen
import com.example.ui.DiscoverScreen
import com.example.ui.HomeScreen
import com.example.ui.FloatingNavigationScaffold
import com.example.ui.PrayerViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppTheme
import com.example.ui.theme.drawPaperTexture
import com.example.ui.theme.frostedGlassBackdrop
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    private val viewModel: PrayerViewModel by viewModels()
    private var alarmOpenRequest by androidx.compose.runtime.mutableIntStateOf(0)
    private var stepOpenRequest by androidx.compose.runtime.mutableIntStateOf(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == com.example.steps.StepCounterService.ACTION_OPEN_STEPS) stepOpenRequest++
        if (intent.action == com.example.alarm.SunriseAlarmScheduler.ACTION_OPEN_ALARMS) alarmOpenRequest++
    }

    override fun onResume() {
        super.onResume()
        com.example.ui.BackgroundServices.startCountdown(this, userInitiated = true)
        viewModel.onAppResumed()
        com.example.alarm.SunriseAlarmScheduler.refresh(this)
        com.example.alarm.SunriseAlarmReceiver.armRefresh(this)
        com.example.alarm.SunriseAlarmAudio.recoverVolumeIfIdle(this)
        com.example.alarm.SunriseAlarmDataRefresh.request(this)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            if (viewModel.currentRoute.value == "dhikr" && viewModel.volumeKeyDhikrEnabled.value) {
                viewModel.triggerVolumeDown()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            if (viewModel.currentRoute.value == "dhikr" && viewModel.volumeKeyDhikrEnabled.value) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == com.example.steps.StepCounterService.ACTION_OPEN_STEPS) stepOpenRequest++
        if (intent.action == com.example.alarm.SunriseAlarmScheduler.ACTION_OPEN_ALARMS) alarmOpenRequest++
        enableEdgeToEdge()
        // MapLibre must be initialised before any MapView exists, so this has to
        // happen before setContent, not lazily from a composable.
        runCatching { org.maplibre.android.MapLibre.getInstance(applicationContext) }
        setContent {
            val appTheme by viewModel.currentTheme.collectAsState()
            val selectedBgId by viewModel.selectedBackground.collectAsState()
            val patternIntensity by viewModel.patternIntensity.collectAsState()
            val themeColorVariant by viewModel.themeColorVariant.collectAsState()
            val isForcedDark by viewModel.forceDarkTheme.collectAsState()
            val selectedLang by viewModel.selectedLanguage.collectAsState()
            val strings = com.example.ui.getAppStrings(selectedLang)

            androidx.compose.runtime.CompositionLocalProvider(com.example.ui.LocalAppStrings provides strings) {
                MyApplicationTheme(
                    darkTheme = isForcedDark,
                    appTheme = appTheme,
                    selectedBgId = selectedBgId,
                    patternIntensity = patternIntensity,
                    colorVariant = themeColorVariant
                ) {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions)
                    val hasAnyLocationPermission = permissionsState.permissions.any {
                        (it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                            it.permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            it.status == com.google.accompanist.permissions.PermissionStatus.Granted
                    }

                    LaunchedEffect(Unit) {
                        com.example.ui.NotificationSoundManager.initDhikrSounds(this@MainActivity)
                        com.example.ui.audio.MealSpeechHolder.warmUp(this@MainActivity)
                        if (!hasAnyLocationPermission) {
                            // Allow the initial window surface frame to finish rendering before
                            // launching the system permission dialog overlay.
                            kotlinx.coroutines.delay(400L)
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    }

                    LaunchedEffect(hasAnyLocationPermission) {
                        if (hasAnyLocationPermission) {
                            viewModel.startLiveLocationTracking()
                            viewModel.checkLocationChanged()
                        }
                    }

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .drawCustomBackground(
                                bgId = selectedBgId,
                                theme = appTheme,
                                isDark = isForcedDark,
                                intensity = patternIntensity,
                                colorVariant = themeColorVariant
                            )
                    ) {
                        MainApp(viewModel, stepOpenRequest, alarmOpenRequest)
                    }
                    com.example.ui.BackgroundProtectionPrompt(canPrompt = hasAnyLocationPermission)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: PrayerViewModel, stepOpenRequest: Int = 0, alarmOpenRequest: Int = 0) {
    val navController = rememberNavController()
    val alarmContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(alarmOpenRequest) {
        if (alarmOpenRequest > 0) navController.navigate("sunrise_alarms") { launchSingleTop = true }
    }
    LaunchedEffect(stepOpenRequest) {
        if (stepOpenRequest > 0) navController.navigate("steps") { launchSingleTop = true }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val isReadingSurah by viewModel.isReadingSurah.collectAsState()


    LaunchedEffect(currentRoute) {
        viewModel.setCurrentRoute(currentRoute)
    }

    FloatingNavigationScaffold(
        showBottomBar = currentRoute != "quran_reading" || !isReadingSurah,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute != "quran_reading" || !isReadingSurah) {
                val strings = com.example.ui.LocalAppStrings.current
                val navSpec = com.example.ui.theme.LocalThemeSpec.current
                // NAV_DESTINATION themes push Tools into the navigation bar
                // itself, so the home page carries nothing but prayer data.
                val actionsInNav = navSpec.actionPlacement ==
                    com.example.ui.theme.ActionPlacement.NAV_DESTINATION
                val navItems = buildList {
                    add(Triple("home", strings.navHome, Icons.Default.Home))
                    add(Triple("quran_reading", strings.navQuran, Icons.Default.Book))
                    add(Triple("compass", strings.navCompass, Icons.Default.Explore))
                    add(Triple("dhikr", strings.navDhikr, Icons.Default.Star))
                    add(Triple("calendar", strings.navCalendar, Icons.Default.DateRange))
                    if (actionsInNav) add(Triple("discover", strings.tools, Icons.Default.GridView))
                }

                // The navigation bar is part of the theme's architecture, not a
                // fixed widget. Each form has a different silhouette and a
                // different relationship to the content behind it.
                val navStyle = com.example.ui.theme.LocalThemeSpec.current.navStyle

                // NAV_DESTINATION themes push the tools/settings pair into the
                // navigation bar itself rather than onto the home content, so the
                // home page carries nothing but prayer data.

                fun goTo(route: String) {
                    if (currentRoute == route) return
                    navController.navigate(route) {
                        if (route == "home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        } else {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            restoreState = true
                        }
                        launchSingleTop = true
                    }
                }

                // Declared as a RowScope extension so the per-style branches
                // can use Modifier.weight(), which is what lets the active item
                // grow in the PILL style and stay fixed in the others.
                @Composable
                fun androidx.compose.foundation.layout.RowScope.navItem(
                    route: String,
                    label: String,
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                    modifier: Modifier
                ) {
                    val isSelected = currentRoute == route
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    when (navStyle) {
                        com.example.ui.theme.NavStyle.PILL -> {
                            val animWeight by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected) 1.25f else 1.0f,
                                label = "navAnim"
                            )
                            Column(
                                modifier = modifier
                                    .weight(animWeight)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.12f)
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { goTo(route) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, label, tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier.size(if (isSelected) 23.dp else 21.dp))
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    label,
                                    fontSize = if (isSelected) 10.5.sp else 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else inactiveColor,
                                    maxLines = 1
                                )
                            }
                        }
                        // Full-width flat bar: a thin indicator under the active
                        // tab, no container behind it at all.
                        com.example.ui.theme.NavStyle.UNDERLINE -> Column(
                            modifier = modifier
                                .weight(1f)
                                .clickable { goTo(route) }
                                .padding(top = 10.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, label, tint = if (isSelected) activeColor else inactiveColor,
                                modifier = Modifier.size(21.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activeColor else inactiveColor,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .width(if (isSelected) 20.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(activeColor)
                            )
                        }
                        // Docked rail: taller, always elevated, icon above label.
                        com.example.ui.theme.NavStyle.RAIL_DOCK -> Column(
                            modifier = modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) activeColor.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { goTo(route) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, label, tint = if (isSelected) activeColor else inactiveColor,
                                modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) activeColor else inactiveColor,
                                maxLines = 1
                            )
                        }
                        // Tabs: classic filled pill behind the active item only.
                        com.example.ui.theme.NavStyle.TABS -> Column(
                            modifier = modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) activeColor else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { goTo(route) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else inactiveColor,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(
                                label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else inactiveColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                val outerPadding = when (navStyle) {
                    com.example.ui.theme.NavStyle.UNDERLINE -> 0.dp
                    else -> 14.dp
                }
                val isDarkNav = com.example.ui.theme.LocalThemeIsDark.current
                val themeColors = com.example.ui.theme.LocalThemeColors.current
                val blurredBg = com.example.ui.theme.LocalBlurredBackgroundBitmap.current
                val barShape = when (navStyle) {
                    com.example.ui.theme.NavStyle.UNDERLINE -> RoundedCornerShape(0.dp)
                    com.example.ui.theme.NavStyle.RAIL_DOCK -> RoundedCornerShape(30.dp)
                    com.example.ui.theme.NavStyle.TABS -> RoundedCornerShape(22.dp)
                    else -> RoundedCornerShape(26.dp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = outerPadding, vertical = if (navStyle == com.example.ui.theme.NavStyle.UNDERLINE) 0.dp else 8.dp)
                ) {
                    Surface(
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        shape = barShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkNav) 0.46f else 0.80f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    Color.White.copy(alpha = if (isDarkNav) 0.18f else 0.48f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlassBackdrop(
                                shape = barShape,
                                tintColor = MaterialTheme.colorScheme.primary,
                                tintAlpha = if (isDarkNav) 0.16f else 0.12f,
                                surfaceVeilAlpha = if (isDarkNav) 0.30f else 0.36f,
                                blurAlpha = 1f
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (navStyle == com.example.ui.theme.NavStyle.UNDERLINE) 8.dp else 6.dp,
                                    vertical = 6.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { (route, label, icon) ->
                                navItem(route, label, icon, Modifier)
                            }
                        }
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") { 
                HomeScreen(
                    viewModel = viewModel, 
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToQuranReading = { navController.navigate("quran_reading") },
                    onNavigateToAssistant = { navController.navigate("ai_assistant") },
                    onNavigateToDiscover = { navController.navigate("discover") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToSteps = { navController.navigate("steps") },
                    onNavigateToAlarms = { navController.navigate("sunrise_alarms") }
                ) 
            }
            composable("sunrise_alarms") {
                fun refreshAlarms() {
                    com.example.alarm.SunriseAlarmScheduler.refresh(alarmContext)
                    com.example.alarm.SunriseAlarmReceiver.armRefresh(alarmContext)
                    com.example.alarm.SunriseAlarmDataRefresh.request(alarmContext)
                }
                LaunchedEffect(Unit) { refreshAlarms() }
                com.example.alarm.SunriseAlarmScreen(
                    onBack = { navController.popBackStack() },
                    onAlarmChanged = ::refreshAlarms,
                    permissionContent = {
                        com.example.alarm.SunriseAlarmPermissionPanel(onPermissionsChanged = ::refreshAlarms)
                    }
                )
            }
            composable("compass") { CompassScreen(viewModel) }
            composable("dhikr") { DhikrScreen(viewModel) }
            composable("calendar") { CalendarScreen(viewModel) }
            composable("ai_assistant") { com.example.ui.AiAssistantScreen(onBack = { navController.popBackStack() }) }
            composable("quran_reading") { com.example.ui.QuranReadingScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("discover") { DiscoverScreen(viewModel) { route -> navController.navigate(route) } }
            composable("steps") { com.example.steps.StepReportScreen(onBack = { navController.popBackStack() }) }
            composable("settings") {
                com.example.ui.SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate("search") }
                )
            }
            composable("search") {
                com.example.ui.LocationSearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
