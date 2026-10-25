package com.example.alarm

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** The common navigation shell overlays its measured island above this screen's scroll list. */
@Composable
fun SunriseAlarmScreen(
    onBack: () -> Unit,
    onAlarmChanged: () -> Unit,
    permissionContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember(context) { SunriseAlarmStore(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var alarms by remember(store) { mutableStateOf(store.list()) }
    var occurrences by remember { mutableStateOf<Map<String, SunriseAlarmOccurrence>>(emptyMap()) }

    fun refresh() {
        val current = store.list()
        val now = System.currentTimeMillis()
        alarms = current
        occurrences = upcomingSunriseAlarmOccurrences(
            current, SunriseAlarmScheduler.pending(context), SunriseAlarmPlanner.allNext(context, current, now), now
        )
    }

    DisposableEffect(context, store) {
        val preferences = context.getSharedPreferences(SunriseAlarmStore.PREFERENCES, Context.MODE_PRIVATE)
        val runtimePreferences = context.getSharedPreferences("sunrise_alarm_runtime", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        runtimePreferences.registerOnSharedPreferenceChangeListener(listener)
        refresh()
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            runtimePreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    LaunchedEffect(lifecycle, store) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                refresh()
                delay(30_000L)
            }
        }
    }

    SunriseAlarmContent(
        alarms = alarms,
        occurrences = occurrences,
        onBack = onBack,
        onSave = { alarm ->
            val saved = runCatching { store.save(alarm) }.isSuccess
            if (saved) {
                refresh()
                onAlarmChanged()
            } else {
                Toast.makeText(context, "Alarm kaydedilemedi. Tekrar deneyin.", Toast.LENGTH_LONG).show()
            }
            saved
        },
        onDelete = { alarm ->
            if (runCatching { store.delete(alarm.id) }.isSuccess) {
                refresh()
                onAlarmChanged()
            } else {
                Toast.makeText(context, "Alarm silinemedi. Tekrar deneyin.", Toast.LENGTH_LONG).show()
            }
        },
        permissionContent = permissionContent
    )
}

/** A snooze can precede tomorrow's regular alarm; display the actual earliest queued delivery. */
internal fun upcomingSunriseAlarmOccurrences(
    alarms: List<SunriseAlarm>,
    pending: List<SunriseAlarmDelivery>,
    planned: List<SunriseAlarmOccurrence>,
    now: Long
): Map<String, SunriseAlarmOccurrence> {
    val enabled = alarms.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val queued = pending.filter { it.alarmId in enabled && it.triggerAtMillis > now }
        .groupBy { it.alarmId }.mapValues { (_, deliveries) ->
            val next = deliveries.minBy { it.triggerAtMillis }
            SunriseAlarmOccurrence(next.alarmId, next.sunriseAtMillis, next.triggerAtMillis, next.sourceDate)
        }
    val preview = planned.filter { it.alarmId in enabled && it.triggerAtMillis > now }.associateBy { it.alarmId }
    return preview + queued
}

@Composable
internal fun SunriseAlarmContent(
    alarms: List<SunriseAlarm>,
    occurrences: Map<String, SunriseAlarmOccurrence>,
    onBack: () -> Unit,
    onSave: (SunriseAlarm) -> Boolean,
    onDelete: (SunriseAlarm) -> Unit,
    permissionContent: @Composable () -> Unit = {}
) {
    var editingAlarm by remember { mutableStateOf<SunriseAlarm?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    if (settingsOpen) {
        SunriseAlarmSettingsScreen(onBack = { settingsOpen = false })
        return
    }
    BackHandler(onBack = onBack)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.fillMaxSize().testTag("sunrise-alarm-screen")) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Sabah Namazı Alarmı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { editingAlarm = SunriseAlarm() },
                    modifier = Modifier.testTag("sunrise-alarm-add")
                ) {
                    Icon(Icons.Default.Add, "Alarm ekle", tint = MaterialTheme.colorScheme.primary)
                }
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("sunrise-alarm-menu")) {
                        Icon(Icons.Default.MoreVert, "Diğer seçenekler", tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Ayarlar") }, onClick = {
                            menuOpen = false
                            settingsOpen = true
                        }, modifier = Modifier.testTag("sunrise-alarm-open-settings"))
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("sunrise-alarm-list"),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 8.dp,
                    bottom = com.example.ui.theme.navBarBottomPadding(extra = 24.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "permissions") { permissionContent() }
                if (alarms.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Henüz alarm yok", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { editingAlarm = SunriseAlarm() }) { Text("Alarm ekle") }
                        }
                    }
                }
                items(alarms, key = { it.id }) { alarm ->
                    SunriseAlarmCard(
                        alarm = alarm,
                        occurrence = occurrences[alarm.id],
                        onEnabledChanged = { enabled -> onSave(alarm.copy(enabled = enabled)) },
                        onEdit = { editingAlarm = alarm },
                        onDelete = { onDelete(alarm) }
                    )
                }
            }
        }
    }

    editingAlarm?.let { alarm ->
        SunriseAlarmEditor(
            alarm = alarm,
            isNew = alarms.none { it.id == alarm.id },
            onDismiss = { editingAlarm = null },
            onSave = { changed -> if (onSave(changed)) editingAlarm = null }
        )
    }
}

@Composable
private fun SunriseAlarmCard(
    alarm: SunriseAlarm,
    occurrence: SunriseAlarmOccurrence?,
    onEnabledChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val locale = remember { Locale.forLanguageTag("tr-TR") }
    val timeZone = TimeZone.getDefault().id
    val clock = remember(timeZone) { SimpleDateFormat("HH:mm", locale) }
    val date = remember(timeZone) { SimpleDateFormat("d MMMM EEEE", locale) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("sunrise-alarm-card-${alarm.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = occurrence?.let { clock.format(Date(it.triggerAtMillis)) } ?: "—:—",
                    fontSize = 36.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).testTag("sunrise-alarm-time-${alarm.id}")
                )
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onEnabledChanged,
                    modifier = Modifier.testTag("sunrise-alarm-enabled-${alarm.id}")
                        .semantics { contentDescription = "${alarm.label}, alarmı aç veya kapat" }
                )
            }
            Text(alarm.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (alarm.minutesBeforeSunrise == 0) "Her gün • Güneş vaktinde"
                    else "Her gün • Güneşten ${alarm.minutesBeforeSunrise} dk önce",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when {
                    !alarm.enabled -> "Kapalı"
                    occurrence == null -> "Vakit bilgisi bekleniyor"
                    else -> "${date.format(Date(occurrence.triggerAtMillis))} • Güneş ${clock.format(Date(occurrence.sunriseAtMillis))}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("sunrise-alarm-status-${alarm.id}")
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit, modifier = Modifier.testTag("sunrise-alarm-edit-${alarm.id}")) { Text("Düzenle") }
                TextButton(onClick = onDelete, modifier = Modifier.testTag("sunrise-alarm-delete-${alarm.id}")) { Text("Sil") }
            }
        }
    }
}

@Composable
private fun SunriseAlarmEditor(
    alarm: SunriseAlarm,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (SunriseAlarm) -> Unit
) {
    var minutes by rememberSaveable(alarm.id) { mutableStateOf(alarm.minutesBeforeSunrise.toString()) }
    var label by rememberSaveable(alarm.id) { mutableStateOf(alarm.label) }
    val offset = minutes.trim().toIntOrNull()?.takeIf { it in 0..SunriseAlarm.MAX_MINUTES_BEFORE_SUNRISE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Alarm ekle" else "Alarmı düzenle") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text("Dakika önce", maxLines = 1) },
                    suffix = { Text("dk") },
                    supportingText = { Text("0–${SunriseAlarm.MAX_MINUTES_BEFORE_SUNRISE} dakika") },
                    isError = offset == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sunrise-alarm-minutes"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(80) },
                    label = { Text("Ad (isteğe bağlı)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sunrise-alarm-label"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = offset != null,
                onClick = {
                    offset?.let { onSave(alarm.copy(minutesBeforeSunrise = it, label = label.trim().ifBlank { SunriseAlarm.DEFAULT_LABEL })) }
                },
                modifier = Modifier.testTag("sunrise-alarm-save")
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
