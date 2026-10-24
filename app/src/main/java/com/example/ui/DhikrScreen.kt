package com.example.ui
import android.view.HapticFeedbackConstants

import android.content.Context
import androidx.compose.ui.platform.LocalView
import android.view.SoundEffectConstants
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog

data class DhikrItem(val name: String, val arabic: String, val target: Int, val isCustom: Boolean = false, val meaning: String = "")
data class HistoryItem(val date: String, val dhikrName: String, val count: Int)

@Composable
fun DhikrScreen(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // SharedPreferences setup for robust, lightweight, crash-free local persistence
    val prefs = remember { context.getSharedPreferences("dhikr_prefs", Context.MODE_PRIVATE) }

    // Load settings
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var soundType by remember { mutableStateOf(prefs.getInt("sound_type", 0)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var interactionMode by remember { mutableStateOf(prefs.getInt("interaction_mode", 1)) }
    var selectedTesbihMaterial by remember { mutableStateOf(prefs.getInt("tesbih_material", 0)) }

    LaunchedEffect(context) {
        NotificationSoundManager.initDhikrSounds(context)
    }

    // Load custom dhikrs list
    // Format: Name|Arabic|Target;Name2|Arabic2|Target2
    var customDhikrsString by remember { mutableStateOf(prefs.getString("custom_dhikrs", "") ?: "") }

    val customDhikrs = remember(customDhikrsString) {
        if (customDhikrsString.isEmpty()) {
            emptyList<DhikrItem>()
        } else {
            customDhikrsString.split(";").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 3) {
                    DhikrItem(
                        name = parts[0],
                        arabic = parts[1],
                        target = parts[2].toIntOrNull() ?: 33,
                        isCustom = true
                    )
                } else null
            }
        }
    }

    // Default presets
    val presets = listOf(
        DhikrItem("Sübhanallah", "سُبْحَانَ ٱللَّٰهِ", 33, false, "Allah'ı tüm noksanlıklardan tenzih ederim."),
        DhikrItem("Elhamdülillah", "ٱلْحَمْدُ لِلَّٰهِ", 33, false, "Hamd ve övgü yalnızca Allah'adır."),
        DhikrItem("Allahu Ekber", "ٱللَّٰهُ أَكْبَرُ", 33, false, "Allah en büyüktür."),
        DhikrItem("Lâ ilâhe illallah", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", 99, false, "Allah'tan başka ilah yoktur."),
        DhikrItem("Estağfirullah", "أَسْتَغْفِرُ ٱللَّٰهَ", 100, false, "Allah'tan bağışlanma ve af dilerim."),
        DhikrItem("Salavat-ı Şerife", "اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", 100, false, "Peygamber Efendimiz'e (s.a.v.) selam ve salat olsun.")
    )

    // Combine presets and custom ones
    val allDhikrs = remember(customDhikrs) { presets + customDhikrs }

    val activeDhikrTarget by viewModel.selectedDhikrTarget.collectAsState()

    var selectedIndex by remember { 
        mutableStateOf(
            prefs.getInt("selected_index", 0).coerceIn(0, allDhikrs.lastIndex.coerceAtLeast(0))
        ) 
    }

    LaunchedEffect(activeDhikrTarget) {
        val targetObj = activeDhikrTarget
        if (targetObj != null) {
            var idx = allDhikrs.indexOfFirst { it.name == targetObj.name }
            if (idx < 0) {
                // Add to custom dhikrs list
                val newCustom = DhikrItem(
                    name = targetObj.name,
                    arabic = targetObj.arabic,
                    target = targetObj.target,
                    isCustom = true
                )
                val currentCustomList = if (customDhikrsString.isEmpty()) {
                    emptyList()
                } else {
                    customDhikrsString.split(";").mapNotNull {
                        val parts = it.split("|")
                        if (parts.size >= 3) DhikrItem(parts[0], parts[1], parts[2].toIntOrNull() ?: 33, true) else null
                    }
                }
                val updatedList = currentCustomList.filter { it.name != targetObj.name } + newCustom
                val updatedString = updatedList.joinToString(";") { "${it.name}|${it.arabic}|${it.target}" }
                prefs.edit().putString("custom_dhikrs", updatedString).apply()
                customDhikrsString = updatedString
                val combined = presets + updatedList
                idx = combined.indexOfFirst { it.name == targetObj.name }
            }
            if (idx >= 0) {
                selectedIndex = idx
                prefs.edit().putInt("selected_index", idx).apply()
                prefs.edit().putInt("target_${targetObj.name}", targetObj.target).apply()
            }
            viewModel.clearDhikrTarget()
        }
    }
    
    // Ensure index remains valid if custom dhikrs are deleted
    if (selectedIndex >= allDhikrs.size) {
        selectedIndex = 0
    }

    val currentDhikr = allDhikrs[selectedIndex]

    // Count persistent per-dhikr progress
    var count by remember(currentDhikr.name) {
        mutableStateOf(prefs.getInt("count_${currentDhikr.name}", 0))
    }

    // Dynamic target choice (presets or customized target per dhikr)
    var currentTarget by remember(currentDhikr.name, currentDhikr.target) {
        mutableStateOf(prefs.getInt("target_${currentDhikr.name}", currentDhikr.target))
    }

    // Total counts ever clicked
    var totalCount by remember {
        mutableStateOf(prefs.getInt("total_count", 0))
    }

    val volumeKeyDhikrEnabled by viewModel.volumeKeyDhikrEnabled.collectAsState()

    // Completion log history
    var historyString by remember { mutableStateOf(prefs.getString("history", "") ?: "") }
    val historyList = remember(historyString) {
        if (historyString.isEmpty()) {
            emptyList<HistoryItem>()
        } else {
            historyString.split(";").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 3) {
                    HistoryItem(parts[0], parts[1], parts[2].toIntOrNull() ?: 0)
                } else null
            }
        }
    }

    // UI state states
    var showAddDhikrDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var showDhikrListDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var buttonPressed by remember { mutableStateOf(false) }

    val view = LocalView.current

    val vibrator = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator 
        }
    }

    fun vibrateShort() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(16, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(16)
            }
        } catch (e: Exception) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (e2: Exception) {}
        }
    }

    // Two-stage realistic tesbih haptic: finger release + bead-to-bead impact click ("tık-tak")
    fun vibrateTesbihPull(isDurakOrImame: Boolean = false) {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = if (isDurakOrImame) longArrayOf(0, 14, 38, 24) else longArrayOf(0, 10, 34, 16)
                val amplitudes = if (isDurakOrImame) intArrayOf(0, 150, 0, 255) else intArrayOf(0, 110, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(22)
            }
        } catch (e: Exception) {
            vibrateShort()
        }
    }

    fun vibrateLong() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {}
    }

    fun playBeep() {
        if (soundEnabled) {
            try {
                NotificationSoundManager.playDhikrClick(context, soundType)
            } catch (e: Exception) {}
        }
    }

    // SharedPreferences State Save Functions
    fun saveCount(newCount: Int) {
        count = newCount
        prefs.edit().putInt("count_${currentDhikr.name}", newCount).apply()
    }

    fun saveTotalCount(newTotal: Int) {
        totalCount = newTotal
        prefs.edit().putInt("total_count", newTotal).apply()
    }

    fun saveTarget(newTarget: Int) {
        currentTarget = newTarget
        prefs.edit().putInt("target_${currentDhikr.name}", newTarget).apply()
    }

    fun toggleSound() {
        val nextVal = !soundEnabled
        soundEnabled = nextVal
        prefs.edit().putBoolean("sound_enabled", nextVal).apply()
    }

    fun cycleSoundType() {
        val nextVal = (soundType + 1) % 4
        soundType = nextVal
        prefs.edit().putInt("sound_type", nextVal).apply()
        // Play it once to preview
        if (soundEnabled) {
            playBeep()
        }
    }

    fun toggleVibration() {
        val nextVal = !vibrationEnabled
        vibrationEnabled = nextVal
        prefs.edit().putBoolean("vibration_enabled", nextVal).apply()
    }

    fun addCustomDhikr(name: String, arabic: String, target: Int) {
        val cleanName = name.replace("|", "").replace(";", "")
        val cleanArabic = arabic.replace("|", "").replace(";", "")
        val newItemStr = "$cleanName|$cleanArabic|$target"
        val newString = if (customDhikrsString.isEmpty()) {
            newItemStr
        } else {
            "$customDhikrsString;$newItemStr"
        }
        customDhikrsString = newString
        prefs.edit().putString("custom_dhikrs", newString).apply()
    }

    fun deleteCustomDhikr(indexInCustom: Int) {
        val itemToDelete = customDhikrs[indexInCustom]
        val updatedList = customDhikrs.filterIndexed { idx, _ -> idx != indexInCustom }
        val newString = updatedList.joinToString(";") { "${it.name}|${it.arabic}|${it.target}" }
        customDhikrsString = newString
        prefs.edit().putString("custom_dhikrs", newString).apply()

        // Clean up stored metrics
        prefs.edit()
            .remove("count_${itemToDelete.name}")
            .remove("target_${itemToDelete.name}")
            .apply()

        selectedIndex = 0
    }

    fun addHistoryEntry(name: String, reachedCount: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date())
        val entry = "$dateStr|$name|$reachedCount"
        val newHistory = if (historyString.isEmpty()) {
            entry
        } else {
            "$entry;$historyString"
        }


        val limited = newHistory.split(";").take(30).joinToString(";")
        historyString = limited
        prefs.edit().putString("history", limited).apply()
    }

    fun clearHistory() {
        historyString = ""
        prefs.edit().remove("history").apply()
    }

    // Observe volume down events for zikir
    LaunchedEffect(Unit) {
        viewModel.volumeDownEvent.collect {
            buttonPressed = true
            val nextCount = count + 1
            saveCount(nextCount)
            saveTotalCount(totalCount + 1)
            // Check target threshold reached (multiples)
            if (currentTarget > 0 && nextCount > 0 && nextCount % currentTarget == 0) {
                vibrateLong()
                playBeep()
                addHistoryEntry(currentDhikr.name, currentTarget)
            } else {
                vibrateShort()
                playBeep()
            }
        }
    }

    // Animation scale for big central button click
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.94f else 1.0f,
        animationSpec = tween(75),
        finishedListener = { buttonPressed = false },
        label = "button_scale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                // Reserve the floating bar so the last card can scroll clear.
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER (Title and Sound/Vibration settings)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zikirmatik",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dualar & Zikirler",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (count > 0) {
                                    addHistoryEntry(currentDhikr.name, count)
                                }
                                saveCount(0)
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sıfırla",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { toggleSound() },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Ses Aç/Kapat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { toggleVibration() },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Titreşim Aç/Kapat",
                                tint = if (vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            if (soundEnabled) {
                item {
                    val soundTypeNames = listOf("🪵 Ahşap Tesbih", "🪨 Taş Tesbih", "🌾 Kuka & Kehribar", "🦴 Kemik & Fildişi")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(soundTypeNames) { idx, name ->
                            val isSel = (soundType == idx)
                            val bg = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            val tc = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val bc = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .border(1.dp, bc, RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundType = idx
                                        prefs.edit().putInt("sound_type", idx).apply()
                                        NotificationSoundManager.playDhikrClick(context, idx)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = tc
                                )
                            }
                        }
                    }
                }
            }

            // 3. MINIMAL TITLE & MODE SWITCHER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val targetText = if (currentTarget > 0) "Hedef: $currentTarget" else "Hedef: Sınırsız"
                        val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                        val tourText = if (completedCycles > 0) " • Tur: ${completedCycles + 1}" else ""
                        Text(
                            text = "$targetText$tourText",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (interactionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    interactionMode = 0
                                    prefs.edit().putInt("interaction_mode", 0).apply()
                                    if (vibrationEnabled) vibrateShort()
                                    playBeep()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Klasik",
                                color = if (interactionMode == 0) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (interactionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    interactionMode = 1
                                    prefs.edit().putInt("interaction_mode", 1).apply()
                                    if (vibrationEnabled) vibrateShort()
                                    playBeep()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Tesbih",
                                color = if (interactionMode == 1) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 4. BIG TACTILE GLOWING DIGI-DIAL / TESBİH SWIPER
            item {
                Spacer(modifier = Modifier.height(14.dp))
                if (interactionMode == 0) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                buttonPressed = true
                                val nextCount = count + 1
                                saveCount(nextCount)
                                saveTotalCount(totalCount + 1)

                                // Check target threshold reached (multiples)
                                if (currentTarget > 0 && nextCount > 0 && nextCount % currentTarget == 0) {
                                    vibrateLong()
                                    playBeep()
                                    addHistoryEntry(currentDhikr.name, currentTarget)
                                } else {
                                    vibrateShort()
                                    playBeep()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Circular Progress Canvas rim drawing
                        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                            val currentProgress = if (currentTarget > 0) (count % currentTarget).toFloat() / currentTarget else 0f
                            val sweep = if (currentTarget > 0) currentProgress * 360f else 360f
                            
                            val cycleColors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green (Primary)
                                androidx.compose.ui.graphics.Color(0xFF26A69A), // Teal
                                androidx.compose.ui.graphics.Color(0xFFFFA726), // Orange
                                androidx.compose.ui.graphics.Color(0xFFAB47BC), // Purple
                                androidx.compose.ui.graphics.Color(0xFFEC407A), // Pink
                                androidx.compose.ui.graphics.Color(0xFF29B6F6)  // Light Blue
                            )
                            
                            val strokeWidth = 11.dp.toPx()
                            val gap = 4.dp.toPx()
                            
                            // Draw completed inner rings
                            val maxInnerRings = 4
                            val visibleCompletedRings = completedCycles.coerceAtMost(maxInnerRings)
                            
                            for (i in 0 until visibleCompletedRings) {
                                val cycleIndex = (completedCycles - 1 - i)
                                val ringColor = cycleColors[cycleIndex % cycleColors.size]
                                
                                val inset = (i + 1) * (strokeWidth + gap)
                                
                                drawArc(
                                    color = ringColor.copy(alpha = 0.4f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth - 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                                )
                            }
                            
                            // Current outer ring
                            val currentRingColor = cycleColors[completedCycles % cycleColors.size]
                            val trailColor = currentRingColor.copy(alpha = 0.08f)
                            
                            // Draw trail for current ring
                            drawArc(
                                color = trailColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            
                            // Draw progress for current ring
                            drawArc(
                                color = currentRingColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        // Numeric reading core
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                            val isExactMultiple = currentTarget > 0 && count > 0 && count % currentTarget == 0
                            val displayStatus = if (isExactMultiple) "TAMAMLANDI" else if (completedCycles > 0) "${completedCycles + 1}. TUR" else "ZİKİR"
                            val statusColor = if (isExactMultiple) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            
                            Text(
                                text = displayStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = count.toString(),
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.displayMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (currentTarget > 0) "Hedef: $currentTarget" else "Hedef: Sınırsız",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    TesbihBeadSwiper(
                        count = count,
                        currentTarget = currentTarget,
                        currentDhikrName = currentDhikr.name,
                        selectedMaterialIndex = selectedTesbihMaterial,
                        onSelectMaterial = { idx ->
                            selectedTesbihMaterial = idx
                            prefs.edit().putInt("tesbih_material", idx).apply()
                            if (vibrationEnabled) vibrateShort()
                        },
                        onIncrement = {
                            val nextCount = count + 1
                            saveCount(nextCount)
                            saveTotalCount(totalCount + 1)
                            val activeTarget = if (currentTarget > 0) currentTarget else 33
                            val isSpecialBead = nextCount > 0 && nextCount % activeTarget == 0
                            if (isSpecialBead) {
                                vibrateLong()
                                vibrateTesbihPull(isDurakOrImame = true)
                                playBeep()
                                if (currentTarget > 0) {
                                    addHistoryEntry(currentDhikr.name, currentTarget)
                                }
                            } else {
                                vibrateTesbihPull(isDurakOrImame = false)
                                playBeep()
                            }
                        },
                        onDecrement = {
                            if (count > 0) {
                                saveCount(count - 1)
                                vibrateTesbihPull(isDurakOrImame = false)
                                playBeep()
                            }
                        }
                    )
                }
            }

            // 5. TARGET SELECTOR CHIPS & CONTROL BUTTONS
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Target Chips + Manual Custom Target Button ("Özel")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hedef Sayı:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presetTargets = listOf(33, 99, 100, 0)
                            val isCustomTarget = currentTarget !in presetTargets

                            for (targetVal in presetTargets) {
                                val isSel = (currentTarget == targetVal)
                                val textLabel = if (targetVal == 0) "Sınırsız" else "$targetVal"

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            saveTarget(targetVal)
                                            if (vibrationEnabled) vibrateShort()
                                            playBeep()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = textLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Custom Target Button ("Özel" or custom value e.g. 500)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCustomTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        showCustomTargetDialog = true
                                        if (vibrationEnabled) vibrateShort()
                                        playBeep()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "El ile Hedef Belirle",
                                        tint = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isCustomTarget) "$currentTarget" else "Özel",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomTarget) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Aligned Control & Action Buttons (Sıfırla + Ses Tuşları Zikir)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sıfırla (Reset) Button
                        OutlinedButton(
                            onClick = {
                                if (count > 0) {
                                    addHistoryEntry(currentDhikr.name, count)
                                }
                                saveCount(0)
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sıfırla",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sıfırla",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Ses Tuşları Zikir Toggle Button
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleVolumeKeyDhikr()
                                val msg = if (!volumeKeyDhikrEnabled) "Ses tuşları zikir çekmek için aktif." else "Ses tuşları varsayılan işlevine döndürüldü."
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = "Ses Tuşları ile Zikir",
                                    tint = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (volumeKeyDhikrEnabled) "Ses Tuşları: Açık" else "Ses Tuşları",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 7. STATS AND DETAILED COMPLETION LOG
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zikir Geçmişi & Analiz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Genel Toplam:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = totalCount.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Henüz tamamlanmış zikir kaydı bulunmuyor.\nZikri sıfırladığınızda veya tamamladığınızda buraya eklenir.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.take(5).forEach { hist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = hist.dhikrName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = hist.date,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                            )
                                        }
                                        Text(
                                            text = "${hist.count} Adet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (historyList.size > 5) {
                                    var showAllHistoryDialog by remember { mutableStateOf(false) }

                                    Text(
                                        text = "Tüm Geçmişi Gör (${historyList.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable { showAllHistoryDialog = true }
                                            .padding(vertical = 6.dp)
                                    )

                                    if (showAllHistoryDialog) {
                                        Dialog(onDismissRequest = { showAllHistoryDialog = false }) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(20.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Tüm Zikir Geçmişi",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        IconButton(onClick = { showAllHistoryDialog = false }) {
                                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    LazyColumn(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.height(300.dp)
                                                    ) {
                                                        items(historyList) { hist ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = hist.dhikrName,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Text(
                                                                        text = hist.date,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "${hist.count} Adet",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Button(
                                                        onClick = {
                                                            clearHistory()
                                                            showAllHistoryDialog = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text(text = "Geçmişi Temizle", color = Color.White)
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
        }

        // 8. CUSTOM DHIKR CREATION DIALOG
        if (showAddDhikrDialog) {
            var newName by remember { mutableStateOf("") }
            var newArabic by remember { mutableStateOf("") }
            var newTargetStr by remember { mutableStateOf("33") }
            var isError by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showAddDhikrDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Özel Zikir Ekle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Zikir Adı *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = { Text("Örn: Lâ havle ve lâ kuvvete...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Arapça Okunuşu (İsteğe bağlı)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newArabic,
                                onValueChange = { newArabic = it },
                                placeholder = { Text("Arapça metin veya açıklama") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hedef Sayı (Limitsiz için 0 girin)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newTargetStr,
                                onValueChange = { newTargetStr = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (isError) {
                            Text(
                                text = "Lütfen geçerli bir zikir adı girin.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDhikrDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "İptal")
                            }

                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        val target = newTargetStr.toIntOrNull() ?: 33
                                        addCustomDhikr(newName, newArabic, target)
                                        showAddDhikrDialog = false
                                        // Auto-select the newly added custom dhikr
                                        selectedIndex = allDhikrs.size
                                        count = 0
                                        currentTarget = target
                                    } else {
                                        isError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Kaydet", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 9. CUSTOM TARGET DIALOG
        if (showCustomTargetDialog) {
            var newTargetStr by remember { mutableStateOf("") }
            var isError by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showCustomTargetDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Özel Hedef Belirle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hedef Sayı (Limitsiz için 0 girin)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newTargetStr,
                                onValueChange = { newTargetStr = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                placeholder = { Text("Örn: 500") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (isError) {
                            Text(
                                text = "Lütfen geçerli bir sayı girin.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomTargetDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "İptal")
                            }

                            Button(
                                onClick = {
                                    val target = newTargetStr.toIntOrNull()
                                    if (target != null && target >= 0) {
                                        saveTarget(target)
                                        showCustomTargetDialog = false
                                    } else {
                                        isError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Kaydet", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCubicPoint(
    p0: androidx.compose.ui.geometry.Offset,
    p1: androidx.compose.ui.geometry.Offset,
    p2: androidx.compose.ui.geometry.Offset,
    p3: androidx.compose.ui.geometry.Offset,
    t: Float
): androidx.compose.ui.geometry.Offset {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val tt = clampedT * clampedT
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * clampedT

    val x = uuu * p0.x + 3 * uu * clampedT * p1.x + 3 * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3 * uu * clampedT * p1.y + 3 * u * tt * p2.y + ttt * p3.y
    return androidx.compose.ui.geometry.Offset(x, y)
}

/**
 * First derivative of the same cubic Bézier.
 *
 * Used to orient each bead along the strand: a bead travelling around a real
 * loop turns to follow the cord, so its highlight and its printed numeral
 * rotate with the path tangent instead of staying screen-aligned.
 */
private fun getCubicTangent(
    p0: androidx.compose.ui.geometry.Offset,
    p1: androidx.compose.ui.geometry.Offset,
    p2: androidx.compose.ui.geometry.Offset,
    p3: androidx.compose.ui.geometry.Offset,
    t: Float
): androidx.compose.ui.geometry.Offset {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val dx = 3 * u * u * (p1.x - p0.x) + 6 * u * clampedT * (p2.x - p1.x) + 3 * clampedT * clampedT * (p3.x - p2.x)
    val dy = 3 * u * u * (p1.y - p0.y) + 6 * u * clampedT * (p2.y - p1.y) + 3 * clampedT * clampedT * (p3.y - p2.y)
    return androidx.compose.ui.geometry.Offset(dx, dy)
}

private data class TesbihMaterialSpec(
    val name: String,
    val subtitle: String,
    val lightCore: Color,
    val midBody: Color,
    val deepEdge: Color,
    val rimGlow: Color,
    val accentMetal: Color,
    val cordColor: Color,
    val hasWoodGrain: Boolean = false,
    val hasAmberInclusions: Boolean = false,
    val hasSilverInlay: Boolean = false
)

private val TESBIH_MATERIALS = listOf(
    TesbihMaterialSpec(
        name = "Zümrüt Yeşim",
        subtitle = "Kubbeli Zümrüt",
        lightCore = Color(0xFF43C687),
        midBody = Color(0xFF0E6B45),
        deepEdge = Color(0xFF042919),
        rimGlow = Color(0xFF80F5B9),
        accentMetal = Color(0xFFD4AF37),
        cordColor = Color(0xFFC9A64E)
    ),
    TesbihMaterialSpec(
        name = "Oltu Taşı",
        subtitle = "Erzurum Gümüş Kakma",
        lightCore = Color(0xFF545B66),
        midBody = Color(0xFF1D2026),
        deepEdge = Color(0xFF07080A),
        rimGlow = Color(0xFFB0BEC5),
        accentMetal = Color(0xFFE0E6ED),
        cordColor = Color(0xFF9EA7B3),
        hasSilverInlay = true
    ),
    TesbihMaterialSpec(
        name = "Damla Kehribar",
        subtitle = "Doğal Bal Kehribarı",
        lightCore = Color(0xFFFFE082),
        midBody = Color(0xFFD97706),
        deepEdge = Color(0xFF4A1D03),
        rimGlow = Color(0xFFFFF59D),
        accentMetal = Color(0xFFD4AF37),
        cordColor = Color(0xFFB8860B),
        hasAmberInclusions = true
    ),
    TesbihMaterialSpec(
        name = "Kuka Ağacı",
        subtitle = "Torna Çekim Kuka",
        lightCore = Color(0xFFA6683C),
        midBody = Color(0xFF5C2E14),
        deepEdge = Color(0xFF240F05),
        rimGlow = Color(0xFFD79968),
        accentMetal = Color(0xFFC5A059),
        cordColor = Color(0xFF8D5B3C),
        hasWoodGrain = true
    ),
    TesbihMaterialSpec(
        name = "Ateş Kehribar",
        subtitle = "Osmanlı Sıkma Yakut",
        lightCore = Color(0xFFFF6E7F),
        midBody = Color(0xFF9E1128),
        deepEdge = Color(0xFF33030B),
        rimGlow = Color(0xFFFF9E9E),
        accentMetal = Color(0xFFD4AF37),
        cordColor = Color(0xFFC5A059),
        hasAmberInclusions = true
    ),
    TesbihMaterialSpec(
        name = "Deve Kemiği",
        subtitle = "Sedefli Fildişi",
        lightCore = Color(0xFFFFFDF7),
        midBody = Color(0xFFE6D7B8),
        deepEdge = Color(0xFF7D6949),
        rimGlow = Color(0xFFFFFFFF),
        accentMetal = Color(0xFFD4AF37),
        cordColor = Color(0xFFB89958)
    )
)

@Composable
fun TesbihBeadSwiper(
    count: Int,
    currentTarget: Int,
    currentDhikrName: String,
    selectedMaterialIndex: Int = 0,
    onSelectMaterial: (Int) -> Unit = {},
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val material = TESBIH_MATERIALS[selectedMaterialIndex.coerceIn(0, TESBIH_MATERIALS.lastIndex)]
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val activeTarget = if (currentTarget > 0) currentTarget else 33

    // Continuous finger drag state: 1.0 full unit corresponds to 1 bead spacing (dt)
    var dragDistancePx by remember { mutableStateOf(0f) }
    var lastTickQuarter by remember { mutableStateOf(0) }

    // Visual offset of the bead stream in bead-spacing units (-1f..1f)
    val beadStreamOffset = remember { Animatable(0f) }
    // Cord & tassel inertia energy (-1f..1f)
    val flingEnergy = remember { Animatable(0f) }
    // Impact shockwave when the pulled bead strikes the lower stack (0f..1f)
    val collisionShock = remember { Animatable(0f) }
    // Pendulum swing angle of the imame tassel
    val tasselSwing = remember { Animatable(0f) }
    // Thumb pressure indicator (0f..1f)
    val thumbPressure = remember { Animatable(0f) }
    // Celebratory golden/emerald aura pulse when the target bead is reached (0f..1f)
    val targetPulse = remember { Animatable(0f) }

    // Track external count increments (e.g. hardware volume keys) so they also animate
    var previousCount by remember { mutableStateOf(count) }

    fun triggerBeadSnapAnimation(
        direction: Float,
        startOffsetFraction: Float,
        hitTargetStone: Boolean = false
    ) {
        // Use UNDISPATCHED so snapTo runs synchronously on the current frame before Compose redraws
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            beadStreamOffset.snapTo(startOffsetFraction)
            beadStreamOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = 540f
                )
            )
        }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            flingEnergy.snapTo(direction)
            flingEnergy.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.46f, stiffness = 220f)
            )
        }
        scope.launch {
            kotlinx.coroutines.delay(28)
            collisionShock.snapTo(if (hitTargetStone) 1.35f else 1f)
            collisionShock.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 460f)
            )
        }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            tasselSwing.snapTo(direction * (if (hitTargetStone) 20f else 13f))
            tasselSwing.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.34f, stiffness = 130f)
            )
        }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            thumbPressure.snapTo(1f)
            thumbPressure.animateTo(0f, tween(240))
        }
        if (hitTargetStone) {
            scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                targetPulse.snapTo(1f)
                targetPulse.animateTo(0f, tween(900))
            }
        }
    }

    LaunchedEffect(count) {
        if (count != previousCount) {
            val delta = count - previousCount
            previousCount = count
            val hitTarget = count > 0 && count % activeTarget == 0
            // If triggered externally (e.g. volume key) when not actively dragging
            if (kotlin.math.abs(dragDistancePx) < 4f && !beadStreamOffset.isRunning) {
                triggerBeadSnapAnimation(
                    direction = if (delta > 0) -1f else 1f,
                    startOffsetFraction = if (delta > 0) -1.0f else 1.0f,
                    hitTargetStone = hitTarget
                )
            }
        }
    }

    val currentCountState = rememberUpdatedState(count)
    val currentTargetState = rememberUpdatedState(activeTarget)
    val incrementCallback = rememberUpdatedState(onIncrement)
    val decrementCallback = rememberUpdatedState(onDecrement)
    val density = LocalDensity.current
    val stepThresholdPx = with(density) { 68.dp.toPx() }

    fun finishDrag() {
        dragDistancePx = 0f
        lastTickQuarter = 0
        scope.launch {
            thumbPressure.animateTo(0f, tween(180))
            beadStreamOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 620f)
            )
        }
    }

    fun handleVerticalDrag(dragAmount: Float) {
        val currentCount = currentCountState.value
        val target = currentTargetState.value
        val dragPullRatio = 0.72f
        dragDistancePx += dragAmount
        val normalized = (dragDistancePx / stepThresholdPx).coerceIn(-1.15f, 1.15f)

        val quarter = (normalized * 3f).toInt()
        if (quarter != lastTickQuarter && kotlin.math.abs(quarter) in 1..2) {
            lastTickQuarter = quarter
            runCatching { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        }

        if (dragDistancePx >= stepThresholdPx) {
            dragDistancePx -= stepThresholdPx
            lastTickQuarter = 0
            val nextCount = currentCount + 1
            previousCount = nextCount
            val hitTarget = nextCount > 0 && nextCount % target == 0
            triggerBeadSnapAnimation(
                direction = -1f,
                startOffsetFraction = -(1f - dragPullRatio),
                hitTargetStone = hitTarget
            )
            incrementCallback.value()
        } else if (dragDistancePx <= -stepThresholdPx && currentCount > 0) {
            dragDistancePx += stepThresholdPx
            lastTickQuarter = 0
            previousCount = (currentCount - 1).coerceAtLeast(0)
            triggerBeadSnapAnimation(
                direction = 1f,
                startOffsetFraction = 1f - dragPullRatio,
                hitTargetStone = false
            )
            decrementCallback.value()
        } else {
            scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                beadStreamOffset.snapTo(normalized * dragPullRatio)
                flingEnergy.snapTo(normalized * 0.45f)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Material selector strip for authentic Turkish tesbih feel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TESBIH_MATERIALS.forEachIndexed { idx, mat ->
                val isSelected = idx == selectedMaterialIndex
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) material.accentMetal else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectMaterial(idx) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(mat.lightCore, mat.midBody, mat.deepEdge),
                                center = Offset(size.width * 0.35f, size.height * 0.35f),
                                radius = size.width * 0.65f
                            )
                        )
                    }
                    Text(
                        text = mat.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .padding(horizontal = 8.dp)
                .testTag("tesbih-pull-area")
                .pointerInput(activeTarget, stepThresholdPx) {
                    val pageScrollGutterPx = 72.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.position.x < pageScrollGutterPx) {
                            // Keep a full-height, easy-to-reach strip available to the parent LazyColumn.
                            return@awaitEachGesture
                        }

                        var dragStarted = false
                        val firstDrag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            dragStarted = true
                            dragDistancePx = 0f
                            lastTickQuarter = 0
                            scope.launch { thumbPressure.animateTo(0.85f, tween(90)) }
                            change.consume()
                            handleVerticalDrag(overSlop)
                        }

                        if (firstDrag == null) {
                            if (dragStarted) finishDrag()
                        } else {
                            var pointerId = firstDrag.id
                            while (true) {
                                val change = awaitVerticalDragOrCancellation(pointerId)
                                if (change == null || !change.pressed) {
                                    finishDrag()
                                    break
                                }
                                val dragAmount = change.positionChange().y
                                if (dragAmount != 0f) {
                                    change.consume()
                                    handleVerticalDrag(dragAmount)
                                }
                                pointerId = change.id
                            }
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val nextCount = count + 1
                    previousCount = nextCount
                    val hitTarget = nextCount > 0 && nextCount % activeTarget == 0
                    // On tap, bead (count + 1) starts from its full upper position (continuousIndex = +1.0f -> startOffsetFraction = -1.0f)
                    triggerBeadSnapAnimation(
                        direction = -1f,
                        startOffsetFraction = -1.0f,
                        hitTargetStone = hitTarget
                    )
                    onIncrement()
                },
            contentAlignment = Alignment.Center
        ) {
            val dragShift = beadStreamOffset.value
            val energy = flingEnergy.value
            val shock = collisionShock.value
            val swingDeg = tasselSwing.value
            val pressure = thumbPressure.value
            val celebration = targetPulse.value

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Natural S-curve / draped loop of a hand-held tesbih
                val p0 = Offset(w * 0.24f, h * 0.03f)
                val p1 = Offset(w * 0.89f, h * 0.08f)
                val p2 = Offset(w * 0.93f, h * 0.63f)
                val p3 = Offset(w * 0.34f, h * 0.96f)

                // Cord flexes slightly with finger pull & inertia
                val cordFlex = Offset(
                    x = energy * 4.5.dp.toPx() - pressure * 3.dp.toPx(),
                    y = energy * 7.5.dp.toPx() + pressure * 4.dp.toPx()
                )

                val stringShadowPath = Path().apply {
                    moveTo(p0.x + cordFlex.x + 3.dp.toPx(), p0.y + cordFlex.y + 5.dp.toPx())
                    cubicTo(
                        p1.x + cordFlex.x + 3.dp.toPx(), p1.y + cordFlex.y + 5.dp.toPx(),
                        p2.x + cordFlex.x + 3.dp.toPx(), p2.y + cordFlex.y + 5.dp.toPx(),
                        p3.x + cordFlex.x + 3.dp.toPx(), p3.y + cordFlex.y + 5.dp.toPx()
                    )
                }

                val stringPath = Path().apply {
                    moveTo(p0.x + cordFlex.x * 0.3f, p0.y + cordFlex.y * 0.3f)
                    cubicTo(
                        p1.x + cordFlex.x, p1.y + cordFlex.y,
                        p2.x + cordFlex.x, p2.y + cordFlex.y,
                        p3.x + cordFlex.x * 0.4f, p3.y + cordFlex.y * 0.4f
                    )
                }

                // 1. Braided Tesbih Cord (İpek & Sırma İp)
                drawPath(
                    path = stringShadowPath,
                    color = Color.Black.copy(alpha = 0.32f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = stringPath,
                    color = Color(0xFF3E2723),
                    style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = stringPath,
                    color = material.cordColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = stringPath,
                    color = Color.White.copy(alpha = 0.42f),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                            phase = (count * 9f + dragShift * 9f)
                        )
                    )
                )

                // 2. Thumb Contact Zone (Çekim Boğumu / Başparmak Nişanesi) at t = 0.49f
                val tFocal = 0.49f
                val focalPos = getCubicPoint(p0, p1, p2, p3, tFocal)
                val focalRadius = (31.dp.toPx()) * (1f + 0.08f * pressure + 0.06f * shock)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            material.accentMetal.copy(alpha = 0.24f + 0.14f * pressure),
                            material.lightCore.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = focalPos,
                        radius = focalRadius * 1.35f
                    ),
                    radius = focalRadius * 1.35f,
                    center = focalPos
                )
                drawCircle(
                    color = material.accentMetal.copy(alpha = 0.48f + 0.25f * pressure),
                    radius = focalRadius * 0.84f,
                    center = focalPos,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            0f
                        )
                    )
                )

                // Celebratory expanding ring when reaching the target stone
                if (celebration > 0.01f) {
                    val ringR = focalRadius * (0.9f + (1f - celebration) * 1.6f)
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = (celebration * 0.75f).coerceIn(0f, 1f)),
                        radius = ringR,
                        center = focalPos,
                        style = Stroke(width = (3.5f * celebration).dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = (celebration * 0.55f).coerceIn(0f, 1f)),
                        radius = ringR * 0.78f,
                        center = focalPos,
                        style = Stroke(width = (2.2f * celebration).dp.toPx())
                    )
                }

                // 3. Render Continuous 3D Beads with Smooth Cubic Thumb Gap & Stack Impact
                val dt = 0.078f
                val range = -6..6

                for (i in range) {
                    val beadIndex = count + i
                    if (beadIndex < 0) continue

                    // Continuous index of this bead along the stream:
                    // Smooth cubic S-curve across the thumb gap so there is zero velocity kink
                    val continuousIndex = i - dragShift
                    val gapNorm = (continuousIndex * 1.85f).coerceIn(-1f, 1f)
                    val smoothGap = gapNorm * (1.5f - 0.5f * gapNorm * gapNorm)
                    val gapAdjustment = 0.023f * smoothGap

                    // Damped cascade compression on the lower stack (i < 0) when a bead snaps down
                    val stackImpactShift = if (continuousIndex < -0.1f) {
                        val distBelow = (-continuousIndex).coerceAtLeast(0.5f)
                        val attenuation = (1f / (distBelow * distBelow)).coerceIn(0f, 1f)
                        shock * 0.018f * attenuation
                    } else 0f

                    val t = tFocal - continuousIndex * dt - gapAdjustment + stackImpactShift
                    if (t < 0.03f || t > 0.97f) continue

                    val beadPos = getCubicPoint(p0, p1, p2, p3, t)
                    val isImame = (beadIndex == 0)
                    // ONLY the user's target count (or its multiples) has the special stone
                    val isTargetBead = (!isImame && beadIndex > 0 && beadIndex % activeTarget == 0)

                    val depth = ((t - tFocal) / 0.50f).coerceIn(-1f, 1f)
                    val depthAbs = kotlin.math.abs(depth)
                    val depthScale = 1f - 0.20f * depthAbs
                    val depthAlpha = (1f - 0.24f * depthAbs).coerceIn(0.45f, 1f)

                    // Smooth tactile zoom as the active bead passes right under the thumb
                    val focalProximity = (1f - kotlin.math.abs(continuousIndex).coerceIn(0f, 1f))
                    val activeFocusBoost = 1f + 0.10f * focalProximity + 0.05f * pressure * focalProximity
                    // Receiving bead in the lower stack compresses slightly on impact
                    val impactSquash = if (i == 0 || i == -1) (1f - 0.07f * shock * focalProximity) else 1f

                    val tangent = getCubicTangent(p0, p1, p2, p3, t)
                    val tangentAngle = Math.toDegrees(
                        kotlin.math.atan2(tangent.y.toDouble(), tangent.x.toDouble())
                    ).toFloat()

                    // Ambient + Directional Contact Shadow
                    val shadowOffset = (4.dp.toPx() + (1f - depthAbs) * 2.5.dp.toPx()) * depthScale
                    val shadowBaseRadius = when {
                        isImame -> 24.dp.toPx()
                        isTargetBead -> 23.dp.toPx()
                        else -> 16.5.dp.toPx()
                    } * depthScale * activeFocusBoost

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (0.46f * depthAlpha).coerceIn(0f, 0.55f)),
                                Color.Black.copy(alpha = 0.14f * depthAlpha),
                                Color.Transparent
                            ),
                            center = Offset(beadPos.x + shadowOffset * 0.75f, beadPos.y + shadowOffset * 1.15f),
                            radius = shadowBaseRadius * 1.32f
                        ),
                        radius = shadowBaseRadius * 1.32f,
                        center = Offset(beadPos.x + shadowOffset * 0.75f, beadPos.y + shadowOffset * 1.15f)
                    )

                    if (isImame) {
                        // --- Ornate Ottoman Lathe-Turned İmame & Swinging Kamçı Püskül ---
                        val imameLength = 50.dp.toPx() * depthScale
                        val imameMaxW = 20.dp.toPx() * depthScale

                        rotate(degrees = tangentAngle - 90f, pivot = beadPos) {
                            val imameBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    material.deepEdge,
                                    material.midBody,
                                    material.lightCore,
                                    material.midBody,
                                    material.deepEdge
                                ),
                                startX = beadPos.x - imameMaxW / 2,
                                endX = beadPos.x + imameMaxW / 2
                            )

                            // Bulbous lower body
                            drawRoundRect(
                                brush = imameBrush,
                                topLeft = Offset(beadPos.x - imameMaxW / 2, beadPos.y - imameLength * 0.45f),
                                size = androidx.compose.ui.geometry.Size(imameMaxW, imameLength * 0.55f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(imameMaxW / 2, imameMaxW / 2)
                            )
                            // Slender upper neck
                            val neckW = imameMaxW * 0.58f
                            drawRoundRect(
                                brush = imameBrush,
                                topLeft = Offset(beadPos.x - neckW / 2, beadPos.y - imameLength * 0.10f),
                                size = androidx.compose.ui.geometry.Size(neckW, imameLength * 0.56f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(neckW / 2, neckW / 2)
                            )

                            // Gold/Silver Ottoman Filigree Collars (Boğum Halkaları)
                            val ringPositions = listOf(-0.36f, -0.02f, 0.24f, 0.42f)
                            val ringWidths = listOf(imameMaxW * 0.86f, imameMaxW * 0.78f, neckW * 1.08f, neckW * 0.92f)
                            ringPositions.forEachIndexed { rIdx, ry ->
                                val rw = ringWidths[rIdx]
                                val yPos = beadPos.y + imameLength * ry
                                drawRoundRect(
                                    color = material.accentMetal,
                                    topLeft = Offset(beadPos.x - rw / 2, yPos - 1.5.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(rw, 3.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }

                            // Specular highlight strip along the İmame
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.38f),
                                topLeft = Offset(beadPos.x - imameMaxW * 0.22f, beadPos.y - imameLength * 0.38f),
                                size = androidx.compose.ui.geometry.Size(2.6.dp.toPx() * depthScale, imameLength * 0.74f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )

                            // Swinging Silk Tassel (Kamçı Püskül) attached to the tip of the İmame
                            val tasselPivot = Offset(beadPos.x, beadPos.y + imameLength * 0.48f)
                            rotate(degrees = swingDeg, pivot = tasselPivot) {
                                drawCircle(
                                    color = material.accentMetal,
                                    radius = 4.5.dp.toPx() * depthScale,
                                    center = tasselPivot
                                )
                                val tasselLen = 22.dp.toPx() * depthScale
                                for (s in -3..3) {
                                    val spread = s * 1.8.dp.toPx() * depthScale
                                    drawLine(
                                        color = if (s % 2 == 0) material.accentMetal.copy(alpha = 0.85f) else material.cordColor,
                                        start = Offset(tasselPivot.x + spread * 0.25f, tasselPivot.y + 2.dp.toPx()),
                                        end = Offset(tasselPivot.x + spread, tasselPivot.y + tasselLen),
                                        strokeWidth = 1.4.dp.toPx() * depthScale,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    } else if (isTargetBead) {
                        // --- Distinct Ornate Target Stone (Hedef / Müezzin Taşı) at exact target multiple ---
                        val durakR = 23.5.dp.toPx() * depthScale * activeFocusBoost * (1f + 0.08f * celebration)
                        val durakLen = durakR * 1.24f * impactSquash
                        val durakThick = durakR * 0.98f

                        // Outer golden/emerald halo so the target stone immediately stands out on the string
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = (0.38f + 0.32f * celebration) * depthAlpha),
                                    Color(0xFF00E676).copy(alpha = 0.16f * depthAlpha),
                                    Color.Transparent
                                ),
                                center = beadPos,
                                radius = durakR * 1.45f
                            ),
                            radius = durakR * 1.45f,
                            center = beadPos
                        )

                        rotate(degrees = tangentAngle, pivot = beadPos) {
                            // Sculpted Gold Crown End Caps (Habbe Çivisi)
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = 5.2.dp.toPx() * depthScale,
                                center = Offset(beadPos.x - durakLen * 0.52f, beadPos.y)
                            )
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = 5.2.dp.toPx() * depthScale,
                                center = Offset(beadPos.x + durakLen * 0.52f, beadPos.y)
                            )

                            // Distinctive Imperial Gold & Emerald-Flanked Target Body
                            val durakBrush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFDE7),
                                    Color(0xFFFFD54F),
                                    Color(0xFFD4AF37),
                                    Color(0xFF795516),
                                    Color(0xFF3E2723)
                                ),
                                center = Offset(beadPos.x - durakLen * 0.18f, beadPos.y - durakThick * 0.22f),
                                radius = durakLen * 0.88f
                            )

                            drawOval(
                                brush = durakBrush,
                                topLeft = Offset(beadPos.x - durakLen / 2, beadPos.y - durakThick / 2),
                                size = androidx.compose.ui.geometry.Size(durakLen, durakThick)
                            )

                            // Twin Filigree Rings + Emerald Inlay Band
                            drawLine(
                                color = Color(0xFF00C853).copy(alpha = 0.92f * depthAlpha),
                                start = Offset(beadPos.x, beadPos.y - durakThick * 0.48f),
                                end = Offset(beadPos.x, beadPos.y + durakThick * 0.48f),
                                strokeWidth = 3.2.dp.toPx() * depthScale,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFFFF8E1).copy(alpha = 0.85f * depthAlpha),
                                start = Offset(beadPos.x - 3.dp.toPx() * depthScale, beadPos.y - durakThick * 0.45f),
                                end = Offset(beadPos.x - 3.dp.toPx() * depthScale, beadPos.y + durakThick * 0.45f),
                                strokeWidth = 1.2.dp.toPx() * depthScale,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFFFF8E1).copy(alpha = 0.85f * depthAlpha),
                                start = Offset(beadPos.x + 3.dp.toPx() * depthScale, beadPos.y - durakThick * 0.45f),
                                end = Offset(beadPos.x + 3.dp.toPx() * depthScale, beadPos.y + durakThick * 0.45f),
                                strokeWidth = 1.2.dp.toPx() * depthScale,
                                cap = StrokeCap.Round
                            )

                            // Glossy dome highlight
                            drawOval(
                                color = Color.White.copy(alpha = 0.62f * depthAlpha),
                                topLeft = Offset(beadPos.x - durakLen * 0.28f, beadPos.y - durakThick * 0.36f),
                                size = androidx.compose.ui.geometry.Size(durakLen * 0.34f, durakThick * 0.24f)
                            )

                            // Outer crisp gold rim
                            drawOval(
                                color = Color(0xFFFFECB3).copy(alpha = 0.78f * depthAlpha),
                                topLeft = Offset(beadPos.x - durakLen / 2, beadPos.y - durakThick / 2),
                                size = androidx.compose.ui.geometry.Size(durakLen, durakThick),
                                style = Stroke(width = 1.4.dp.toPx() * depthScale)
                            )
                        }
                    } else {
                        // --- Authentic Beyzi / Sıkma Turned Ellipsoid Tesbih Bead (Habbe) ---
                        val baseR = 16.dp.toPx() * depthScale * activeFocusBoost
                        val rx = baseR * 1.12f * impactSquash
                        val ry = baseR * 0.98f * (2f - impactSquash)
                        // Subtle 3D rolling shift as the bead slides along the cord
                        val rollPhase = (continuousIndex * 0.35f).coerceIn(-0.35f, 0.35f)

                        rotate(degrees = tangentAngle, pivot = beadPos) {
                            // 1. Metallic/Drilled Cord Collar at both poles (Delik Ağızlığı)
                            val poleOffset = rx * 0.94f
                            drawCircle(
                                color = Color(0xFF1A120B).copy(alpha = 0.85f * depthAlpha),
                                radius = 3.4.dp.toPx() * depthScale,
                                center = Offset(beadPos.x - poleOffset, beadPos.y)
                            )
                            drawCircle(
                                color = Color(0xFF1A120B).copy(alpha = 0.85f * depthAlpha),
                                radius = 3.4.dp.toPx() * depthScale,
                                center = Offset(beadPos.x + poleOffset, beadPos.y)
                            )
                            drawCircle(
                                color = material.accentMetal.copy(alpha = 0.82f * depthAlpha),
                                radius = 2.4.dp.toPx() * depthScale,
                                center = Offset(beadPos.x - poleOffset, beadPos.y)
                            )
                            drawCircle(
                                color = material.accentMetal.copy(alpha = 0.82f * depthAlpha),
                                radius = 2.4.dp.toPx() * depthScale,
                                center = Offset(beadPos.x + poleOffset, beadPos.y)
                            )

                            // 2. 3D Subsurface & Specular Shaded Ellipsoid Body
                            val lightCenter = Offset(
                                beadPos.x - rx * (0.28f - rollPhase * 0.22f),
                                beadPos.y - ry * 0.30f
                            )
                            val beadBrush = Brush.radialGradient(
                                colors = listOf(
                                    material.lightCore,
                                    material.midBody,
                                    material.deepEdge
                                ),
                                center = lightCenter,
                                radius = rx * 1.48f
                            )

                            drawOval(
                                brush = beadBrush,
                                topLeft = Offset(beadPos.x - rx, beadPos.y - ry),
                                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f)
                            )

                            // 3. Material-Specific Authentic Surface Details (rolling with bead motion)
                            if (material.hasWoodGrain) {
                                for (g in -1..1) {
                                    val gx = beadPos.x + (g * 0.36f + rollPhase * 0.18f) * rx
                                    drawArc(
                                        color = Color(0xFF240F05).copy(alpha = 0.34f * depthAlpha),
                                        startAngle = -65f,
                                        sweepAngle = 130f,
                                        useCenter = false,
                                        topLeft = Offset(gx - rx * 0.35f, beadPos.y - ry * 0.78f),
                                        size = androidx.compose.ui.geometry.Size(rx * 0.70f, ry * 1.56f),
                                        style = Stroke(width = 1.2.dp.toPx() * depthScale)
                                    )
                                }
                            } else if (material.hasAmberInclusions) {
                                val seed = (beadIndex * 37) % 5
                                drawOval(
                                    color = material.rimGlow.copy(alpha = 0.28f * depthAlpha),
                                    topLeft = Offset(
                                        beadPos.x - rx * 0.35f + (seed - 2) * 1.5.dp.toPx() + rollPhase * rx * 0.25f,
                                        beadPos.y - ry * 0.15f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(rx * 0.65f, ry * 0.55f)
                                )
                            } else if (material.hasSilverInlay) {
                                val inlayShift = rollPhase * rx * 0.22f
                                drawLine(
                                    color = material.accentMetal.copy(alpha = 0.65f * depthAlpha),
                                    start = Offset(beadPos.x + inlayShift, beadPos.y - ry * 0.85f),
                                    end = Offset(beadPos.x + inlayShift, beadPos.y + ry * 0.85f),
                                    strokeWidth = 1.3.dp.toPx() * depthScale
                                )
                                drawCircle(
                                    color = material.accentMetal.copy(alpha = 0.75f * depthAlpha),
                                    radius = 1.6.dp.toPx() * depthScale,
                                    center = Offset(beadPos.x - rx * 0.38f + inlayShift, beadPos.y)
                                )
                                drawCircle(
                                    color = material.accentMetal.copy(alpha = 0.75f * depthAlpha),
                                    radius = 1.6.dp.toPx() * depthScale,
                                    center = Offset(beadPos.x + rx * 0.38f + inlayShift, beadPos.y)
                                )
                            }

                            // 4. Lower-Right Subsurface Rim Bounce Light
                            drawArc(
                                color = material.rimGlow.copy(alpha = 0.32f * depthAlpha),
                                startAngle = 15f,
                                sweepAngle = 110f,
                                useCenter = false,
                                topLeft = Offset(beadPos.x - rx * 0.88f, beadPos.y - ry * 0.88f),
                                size = androidx.compose.ui.geometry.Size(rx * 1.76f, ry * 1.76f),
                                style = Stroke(width = 1.8.dp.toPx() * depthScale, cap = StrokeCap.Round)
                            )

                            // 5. Crisp Curved Specular Window Highlight on Upper-Left Dome
                            drawOval(
                                color = Color.White.copy(alpha = 0.62f * depthAlpha),
                                topLeft = Offset(beadPos.x - rx * 0.58f, beadPos.y - ry * 0.64f),
                                size = androidx.compose.ui.geometry.Size(rx * 0.52f, ry * 0.32f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.82f * depthAlpha),
                                radius = 1.9.dp.toPx() * depthScale,
                                center = Offset(beadPos.x - rx * 0.42f, beadPos.y - ry * 0.48f)
                            )

                            // Subtle outer contour for crisp separation
                            drawOval(
                                color = Color.Black.copy(alpha = 0.25f * depthAlpha),
                                topLeft = Offset(beadPos.x - rx, beadPos.y - ry),
                                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                                style = Stroke(width = 0.9.dp.toPx())
                            )
                        }
                    }

                    // Draw subtle bead number on the active focal bead and on the target bead
                    if (i == 0 || isTargetBead) {
                        drawIntoCanvas { canvas ->
                            val textSizeSp = if (isTargetBead) 11.5.sp.toPx() else 11.sp.toPx()
                            val paint = android.graphics.Paint().apply {
                                color = if (isTargetBead) {
                                    android.graphics.Color.parseColor("#1B1002")
                                } else {
                                    android.graphics.Color.WHITE
                                }
                                alpha = (248 * depthAlpha).toInt().coerceIn(0, 255)
                                textSize = textSizeSp * depthScale
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                isAntiAlias = true
                                if (!isTargetBead) {
                                    setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
                                }
                            }
                            val textToDraw = if (isImame) "0" else beadIndex.toString()
                            val fm = paint.fontMetrics
                            val textOffset = (fm.descent - fm.ascent) / 2 - fm.descent
                            canvas.nativeCanvas.drawText(
                                textToDraw,
                                beadPos.x,
                                beadPos.y + textOffset,
                                paint
                            )
                        }
                    }
                }
            }

            // Left-side HUD Readout Card (Count, Cycle, Target Stone & Pull Hint)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                val completedCycles = count / activeTarget
                val isExactMultiple = count > 0 && count % activeTarget == 0
                val statusLabel = when {
                    isExactMultiple -> "★ HEDEF TAŞI"
                    completedCycles > 0 -> "${completedCycles + 1}. DEVİR"
                    else -> material.name.uppercase()
                }
                val statusColor = if (isExactMultiple) Color(0xFF00E676) else material.accentMetal

                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    letterSpacing = 1.6.sp
                )

                Text(
                    text = count.toString(),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 80.sp
                )

                val beadInTarget = if (count == 0) 0 else ((count - 1) % activeTarget) + 1
                Surface(
                    color = if (isExactMultiple) Color(0xFF00E676).copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isExactMultiple) Color(0xFF00E676).copy(alpha = 0.55f) else material.accentMetal.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "Tur ${completedCycles + 1} • Habbe $beadInTarget / $activeTarget",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExactMultiple) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tesbihte çekin / dokunun\nSayfa için sol kenarı kaydırın",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

