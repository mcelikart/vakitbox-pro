package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.*

data class ThemeInfo(
    val theme: AppTheme,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    val city by viewModel.cityFlow.collectAsState()
    val strings = LocalAppStrings.current

    val context = LocalContext.current

    val currentTheme by viewModel.currentTheme.collectAsState()

    when (activeSubScreen) {
        "background_running" -> {
            androidx.activity.compose.BackHandler { activeSubScreen = null }
            BackgroundSettingsScreen(onBack = { activeSubScreen = null })
            return
        }
        "theme" -> {
            ThemeGalleryScreen(
                currentTheme = currentTheme,
                onThemeSelect = { viewModel.selectTheme(it) },
                onBack = { activeSubScreen = null },
                viewModel = viewModel
            )
            return
        }
        "alarm_settings" -> {
            AlarmSettingsScreen(onBack = { activeSubScreen = null })
            return
        }
        "hijri" -> {
            HijriSettingsSubScreen(
                context = context,
                onBack = { activeSubScreen = null }
            )
            return
        }
        "calc" -> {
            CalculationMethodSubScreen(
                context = context,
                onBack = { activeSubScreen = null }
            )
            return
        }
        "silent" -> {
            AutoSilentSubScreen(
                context = context,
                onBack = { activeSubScreen = null }
            )
            return
        }
        "language" -> {
            LanguageSettingsSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = null }
            )
            return
        }
        "quran_meal" -> {
            val activeEditions by viewModel.selectedEditions.collectAsState()
            QuranMealSettingsSubScreen(
                activeEditions = activeEditions,
                onToggleEdition = viewModel::toggleEdition,
                onBack = { activeSubScreen = null }
            )
            return
        }
        "about" -> {
            AboutSubScreen(
                onBack = { activeSubScreen = null }
            )
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Extra bottom inset so the last settings row can always be scrolled
            // clear of the floating navigation bar.
            contentPadding = PaddingValues(
                top = 16.dp,
                // Reserve the floating bar so the final row can scroll clear of it.
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)
            )
        ) {
            item {
                SettingCategoryRow(
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Tema Galerisi",
                    subtitle = "Kubbeli Zümrüt, 10 Glassmorphic tema ve renk seçenekleri",
                    onClick = { activeSubScreen = "theme" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Color(0xFFE65100),
                    title = "Ezan ve Bildirim Sesleri",
                    subtitle = "Ezan, vakit hatılatıcı ve bildirim tonları",
                    onClick = { activeSubScreen = "alarm_settings" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.Settings,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Arka Planda Çalışma",
                    subtitle = "Pil, alarm ve bildirim izinleri",
                    onClick = { activeSubScreen = "background_running" }
                )
            }
            item {
                SettingCategoryRow(
                    icon = Icons.Default.CalendarToday,
                    iconTint = Color(0xFF0277BD),
                    title = "Hicri Takvim Düzeltmesi",
                    subtitle = "Hicri gün kaydırmasını manuel ayarlayın",
                    onClick = { activeSubScreen = "hijri" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.Calculate,
                    iconTint = Color(0xFF6A1B9A),
                    title = "Hesaplama Yöntemi",
                    subtitle = "Namaz vakitleri hesaplama metodunu değiştirin",
                    onClick = { activeSubScreen = "calc" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.VolumeOff,
                    iconTint = Color(0xFFD32F2F),
                    title = "Sessize Alma",
                    subtitle = "Namaz vakitlerinde telefonu otomatik sessize al",
                    onClick = { activeSubScreen = "silent" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF00838F),
                    title = "Uygulama Dili / Language",
                    subtitle = "Türkçe, English, العربية",
                    onClick = { activeSubScreen = "language" }
                )
            }

            item {
                val activeEditions by viewModel.selectedEditions.collectAsState()
                SettingCategoryRow(
                    icon = Icons.Default.MenuBook,
                    iconTint = Color(0xFF2E7D32),
                    title = strings.quranMealSettingsTitle,
                    subtitle = "${activeEditions.size} kaynak seçili",
                    onClick = { activeSubScreen = "quran_meal" }
                )
            }

            item {
                SettingCategoryRow(
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Uygulama Hakkında",
                    subtitle = "Sürüm ${BuildConfig.VERSION_NAME} • Geliştirici Bilgileri",
                    onClick = { activeSubScreen = "about" }
                )
            }
        }
    }
}

@Composable
private fun SettingCategoryRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HijriSettingsSubScreen(
    context: Context,
    onBack: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE) }
    var offset by remember { mutableStateOf(prefs.getInt("hijri_offset", 0)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hicri Takvim Düzeltmesi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ayın görünürlüğüne göre Hicri gün kaydırmasını ayarlayın (-2 ile +2 gün):", fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (offset > -2) {
                        offset--
                        prefs.edit().putInt("hijri_offset", offset).apply()
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Azalt")
                }
                Text("$offset Gün", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (offset < 2) {
                        offset++
                        prefs.edit().putInt("hijri_offset", offset).apply()
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Artır")
                }
            }
            Spacer(Modifier.height(navBarBottomPadding()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationMethodSubScreen(
    context: Context,
    onBack: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE) }
    var method by remember { mutableStateOf(prefs.getString("calc_method", "Diyanet") ?: "Diyanet") }
    val methods = listOf("Diyanet İşleri Başkanlığı (Türkiye)", "Muslim World League (MWL)", "ISNA (North America)", "Egyptian General Authority", "Umm Al-Qura (Makkah)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hesaplama Yöntemi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = navBarBottomPadding())
        ) {
            items(methods.size) { idx ->
                val m = methods[idx]
                val isSelected = m.contains(method) || method == m
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            method = m
                            prefs.edit().putString("calc_method", m).apply()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(m, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        RadioButton(selected = isSelected, onClick = {
                            method = m
                            prefs.edit().putString("calc_method", m).apply()
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSilentSubScreen(
    context: Context,
    onBack: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("auto_silent", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Otomatik Sessize Alma", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ezan vakitlerinde telefonu otomatik sessize al", modifier = Modifier.weight(1f).padding(end = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        prefs.edit().putBoolean("auto_silent", it).apply()
                    }
                )
            }
            Spacer(Modifier.height(navBarBottomPadding()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettingsSubScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val languages = listOf("tr" to "Türkçe", "en" to "English", "ar" to "العربية")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dil Seçimi / Language", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { (code, name) ->
                val isSelected = currentLang == code
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLanguage(code) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        RadioButton(selected = isSelected, onClick = { viewModel.setLanguage(code) })
                    }
                }
            }
            Spacer(Modifier.height(navBarBottomPadding()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuranMealSettingsSubScreen(
    activeEditions: Set<String>,
    onToggleEdition: (String) -> Unit,
    onBack: () -> Unit
) {
    val availableEditions = QuranReadingEditions.choices
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kur'an Meal Seçimi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("quran-meal-choices")
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // Reserve the floating bar's measured height so the last row can be
            // scrolled fully clear of it.
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = com.example.ui.theme.navBarBottomPadding()
            )
        ) {
            item {
                Text(
                    text = "Okumak istediğiniz kaynakları seçin. Seçili mealler çevrimdışı açılır.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(availableEditions.size) { idx ->
                val (key, label) = availableEditions[idx]
                val isSelected = activeEditions.contains(key)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(Modifier.testTag("quran-edition-$key"))
                        .clickable { onToggleEdition(key) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleEdition(key) }
                        )
                    }
                }
            }
            item {
                TextButton(onClick = { uriHandler.openUri("https://tanzil.net/trans/") }) {
                    Text("Tanzil kaynakları ve güncellemeler")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutSubScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    val notices = remember(context) {
        listOf("quran_search/NOTICE.txt", "quran_search/LICENSE-CC-BY-SA-4.0.txt", "quran_search/MODEL-NOTICE.txt", "quran_reading/NOTICE.txt")
            .mapNotNull { assetPath ->
                runCatching {
                    context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "${assetPath.substringAfterLast('/')}\n\n$it" }
            }.joinToString("\n\n──────────\n\n")
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Kaynaklar ve lisanslar") },
            text = {
                SelectionContainer {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            notices.ifBlank { "Kaynak ve lisans bilgileri bu pakette bulunamadı." },
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Kapat") } }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uygulama Hakkında", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("about-scroll-content"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }

            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Sürüm ${BuildConfig.VERSION_NAME} • Yapı ${BuildConfig.VERSION_CODE}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Uygulama Hakkında", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Muhammet ÇELİK tarafından günlük ibadetlere yardımcı olmak için geliştirilmiştir. Namaz vakitleri, Kur’an ve meal, çevrimdışı ayet arama, kıble pusulası ve zikirmatik sunar.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    )
                }
            }
            Text(
                "Kur’an metni: Tanzil · Seçilebilir mealler ve Türkçe okunuş: çevrimdışı",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { showLicenses = true }) { Text("Kaynaklar ve lisanslar") }
            Spacer(Modifier.height(navBarBottomPadding()))
        }
    }
}
