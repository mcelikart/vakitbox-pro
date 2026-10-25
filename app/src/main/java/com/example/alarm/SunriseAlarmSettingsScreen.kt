package com.example.alarm

import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.navBarBottomPadding
import kotlin.math.roundToInt

@Composable
internal fun SunriseAlarmSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SunriseAlarmSettingsStore(context) }
    var settings by remember(store) { mutableStateOf(store.read()) }
    fun save(next: SunriseAlarmSettings) {
        runCatching { store.save(next) }.onSuccess {
            settings = store.read()
            SunriseAlarmScheduler.refresh(context)
        }.onFailure { Toast.makeText(context, "Ayar kaydedilemedi.", Toast.LENGTH_SHORT).show() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                save(settings.copy(ringtoneUri = if (RingtoneManager.isDefault(uri)) null else uri.toString()))
            }
        }
    }
    val toneTitle = remember(settings.ringtoneUri) {
        val uri = settings.ringtoneUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val title = runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
        title?.takeIf { it.isNotBlank() } ?: "Telefonun varsayılan alarmı"
    }
    val manager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val systemPercent = remember { (manager.getStreamVolume(AudioManager.STREAM_ALARM) * 100f /
        manager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)).roundToInt().takeIf { it > 0 } ?: 60 }
    val canFlip = remember(context) {
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
    SunriseAlarmSettingsContent(
        settings = settings,
        toneTitle = toneTitle,
        systemVolumePercent = systemPercent,
        canFlip = canFlip,
        onBack = onBack,
        onChange = ::save,
        onRingtone = {
            runCatching {
                picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, settings.ringtoneUri?.let(Uri::parse)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sesi")
                })
            }.onFailure { Toast.makeText(context, "Telefonun ses seçicisi açılamadı.", Toast.LENGTH_SHORT).show() }
        }
    )
}

@Composable
internal fun SunriseAlarmSettingsContent(
    settings: SunriseAlarmSettings,
    toneTitle: String,
    systemVolumePercent: Int,
    canFlip: Boolean,
    onBack: () -> Unit,
    onChange: (SunriseAlarmSettings) -> Unit,
    onRingtone: () -> Unit
) {
    var selection by remember { mutableStateOf<String?>(null) }
    var volume by remember(settings.volumePercent, systemVolumePercent) {
        mutableFloatStateOf((settings.volumePercent ?: systemVolumePercent).toFloat())
    }
    BackHandler(onBack = onBack)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().testTag("sunrise-alarm-settings")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                Text("Alarm ayarları", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("sunrise-settings-list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = navBarBottomPadding(24.dp))
            ) {
                item { AlarmSettingRow("Alarm sesi", toneTitle, "alarm-setting-tone", onRingtone) }
                item { AlarmSettingRow("Çalma süresi", "${settings.ringMinutes} dk", "alarm-setting-duration") { selection = "duration" } }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Alarm ses düzeyi", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text("%${volume.roundToInt()}", color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = volume, onValueChange = { volume = it }, valueRange = 1f..100f,
                            onValueChangeFinished = { onChange(settings.copy(volumePercent = volume.roundToInt())) },
                            modifier = Modifier.fillMaxWidth().testTag("alarm-setting-volume"))
                        if (settings.volumePercent != null) {
                            TextButton(onClick = { onChange(settings.copy(volumePercent = null)) }) { Text("Telefonun ses düzeyini kullan") }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item { AlarmSettingToggle("Ses düzeyini kademeli artır", settings.gradualVolume, "alarm-setting-gradual") {
                    onChange(settings.copy(gradualVolume = it))
                } }
                item { AlarmSettingRow("Ön çalma bildirimi", if (settings.preNotificationMinutes == 0) "Kapalı"
                    else "${settings.preNotificationMinutes} dk önce", "alarm-setting-pre") { selection = "pre" } }
                item { AlarmSettingRow("Erteleme süresi", "${settings.snoozeMinutes} dk", "alarm-setting-snooze") { selection = "snooze" } }
                if (canFlip) item { AlarmSettingToggle("Ertelemek için telefonu çevir", settings.flipToSnooze, "alarm-setting-flip") {
                    onChange(settings.copy(flipToSnooze = it))
                } }
                item { AlarmSettingRow("Ses tuşlarının işlevi", settings.volumeKeyAction.title, "alarm-setting-keys") { selection = "keys" } }
                item {
                    Text("Telefon tamamen kapalıyken alarm çalma özelliği yalnızca telefonun Saat uygulamasında kullanılabilir.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("alarm-power-off-note"))
                }
            }
        }
    }
    selection?.let { key ->
        val title = when (key) { "duration" -> "Çalma süresi"; "pre" -> "Ön çalma bildirimi"; "snooze" -> "Erteleme süresi"; else -> "Ses tuşlarının işlevi" }
        val options: List<Pair<String, Boolean>> = when (key) {
            "duration" -> SunriseAlarmSettings.RING_MINUTES.map { "$it dk" to (it == settings.ringMinutes) }
            "pre" -> SunriseAlarmSettings.PRE_NOTIFICATION_MINUTES.map { (if (it == 0) "Kapalı" else "$it dk önce") to (it == settings.preNotificationMinutes) }
            "snooze" -> SunriseAlarmSettings.SNOOZE_MINUTES.map { "$it dk" to (it == settings.snoozeMinutes) }
            else -> AlarmVolumeKeyAction.entries.map { it.title to (it == settings.volumeKeyAction) }
        }
        AlertDialog(onDismissRequest = { selection = null }, title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEachIndexed { index, (text, checked) ->
                        Row(Modifier.fillMaxWidth().clickable {
                            onChange(when (key) {
                                "duration" -> settings.copy(ringMinutes = SunriseAlarmSettings.RING_MINUTES[index])
                                "pre" -> settings.copy(preNotificationMinutes = SunriseAlarmSettings.PRE_NOTIFICATION_MINUTES[index])
                                "snooze" -> settings.copy(snoozeMinutes = SunriseAlarmSettings.SNOOZE_MINUTES[index])
                                else -> settings.copy(volumeKeyAction = AlarmVolumeKeyAction.entries[index])
                            })
                            selection = null
                        }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = checked, onClick = null)
                            Text(text, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { selection = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun AlarmSettingRow(title: String, value: String, tag: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AlarmSettingToggle(title: String, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(value, onChange, Modifier.testTag(tag))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
