package com.example.alarm

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.KeyEvent
import android.media.AudioManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Date

/** A normal alarm control screen that may show over, but never unlock, the keyguard. */
class SunriseAlarmRingingActivity : ComponentActivity() {
    private val handledVolumeKeys = mutableSetOf<Int>()
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return super.onKeyDown(keyCode, event)
        val current = SunriseAlarmRingingState(this).list().firstOrNull { it.expiresAtMillis > System.currentTimeMillis() }
            ?: return if (keyCode in handledVolumeKeys) true else super.onKeyDown(keyCode, event)
        handledVolumeKeys += keyCode
        when (current.settings.volumeKeyAction) {
            AlarmVolumeKeyAction.SNOOZE -> if (event.repeatCount == 0 && !SunriseAlarmScheduler.snooze(this, current.delivery.token)) {
                Toast.makeText(this, "Alarm ertelenemedi. Alarm iznini kontrol edin.", Toast.LENGTH_LONG).show()
            }
            AlarmVolumeKeyAction.DISMISS -> if (event.repeatCount == 0) SunriseAlarmScheduler.dismiss(this, current.delivery.token)
            AlarmVolumeKeyAction.VOLUME -> getSystemService(AudioManager::class.java).adjustStreamVolume(
                AudioManager.STREAM_ALARM,
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            AlarmVolumeKeyAction.NONE -> Unit
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (handledVolumeKeys.remove(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }
    override fun onResume() {
        super.onResume()
        val now = System.currentTimeMillis()
        val hasAlarm = SunriseAlarmRingingState(this).list().any {
            it.expiresAtMillis > now && SunriseAlarmStore(this).find(it.delivery.alarmId)?.enabled == true
        }
        if (hasAlarm) runCatching {
            ContextCompat.startForegroundService(this, SunriseAlarmRingingService.syncIntent(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val state = SunriseAlarmRingingState(this)
        setContent {
            var records by remember { mutableStateOf(state.list()) }
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            DisposableEffect(state) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> records = state.list() }
                state.registerListener(listener)
                records = state.list()
                onDispose { state.unregisterListener(listener) }
            }
            LaunchedEffect(Unit) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1_000L)
                }
            }
            val active = records.filter { it.expiresAtMillis > now }
            LaunchedEffect(active.isEmpty()) { if (active.isEmpty()) finish() }
            BackHandler { moveTaskToBack(true) }
            MaterialTheme {
                SunriseAlarmRingingContent(
                    records = active,
                    now = now,
                    onSnooze = { token ->
                        if (!SunriseAlarmScheduler.snooze(this, token)) {
                            Toast.makeText(this, "Alarm ertelenemedi. Alarm iznini kontrol edin.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onDismiss = { token -> SunriseAlarmScheduler.dismiss(this, token) }
                )
            }
        }
    }

    companion object {
        fun openIntent(context: Context): Intent = Intent(context, SunriseAlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
    }
}

@Composable
internal fun SunriseAlarmRingingContent(
    records: List<SunriseAlarmRingRecord>,
    now: Long,
    onSnooze: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = records.firstOrNull()
    val background = Color(0xFF0D1C18)
    val foreground = Color(0xFFF6F3E8)
    val secondary = Color(0xFFB7C6BD)
    val accent = Color(0xFFE4CC99)
    val context = LocalContext.current
    val clockFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    Surface(modifier = modifier.fillMaxSize(), color = background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = accent, modifier = Modifier.height(36.dp))
            Text("SABAH NAMAZI ALARMI", color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val time = clockFormat.format(Date(now))
                val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
                val size = (maxWidth.value / (time.length.coerceAtLeast(4) * .63f)).coerceAtMost(92f) / fontScale
                Text(
                    time, color = foreground, fontFamily = FontFamily.SansSerif,
                    fontSize = size.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("ringing_clock")
                )
            }
            if (current != null) {
                Text(current.label, color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                val sunrise = clockFormat.format(Date(current.delivery.sunriseAtMillis))
                Text("Güneş $sunrise", color = secondary, fontSize = 18.sp, textAlign = TextAlign.Center)
                if (records.size > 1) {
                    Text("${records.size} alarm çalıyor", color = accent, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color(0xFF21392E), RoundedCornerShape(16.dp)).padding(12.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSnooze(current.delivery.token) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("ringing_snooze"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF294537), contentColor = foreground)
                ) {
                    Text("${current.settings.snoozeMinutes} dk ertele", fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = { onDismiss(current.delivery.token) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("ringing_dismiss"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = background)
                ) {
                    Text("Kapat", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
