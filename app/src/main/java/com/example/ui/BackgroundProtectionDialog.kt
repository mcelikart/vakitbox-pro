package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BackgroundProtectionDialog(onDismiss: () -> Unit, onRequestPermissions: () -> Unit) {
    val context = LocalContext.current
    val access = rememberBackgroundAccess()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp), tonalElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()).testTag("background-protection-scroll")) {
                Text("Arka planda çalışma", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Vakitleri ve geri sayımı takip etmek için bu ayarları kontrol edin.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                ProtectionSetting("Pil kullanımı", access.batteryStatus, "Pil ayarları") {
                    requestIgnoreBatteryOptimizations(context)
                }
                ProtectionSetting("Tam zamanında alarmlar", if (access.exactAlarms) "İzin verildi" else "İzin gerekli", "Alarm izinleri") {
                    BackgroundAccess.open(context, BackgroundAccess.exactAlarmIntent(context))
                }
                ProtectionSetting("Bildirimler", if (access.notifications) access.countdownStatus else "Kapalı", "Bildirim ayarları") {
                    if (!access.notifications) onRequestPermissions()
                    else BackgroundAccess.open(context, BackgroundAccess.countdownIntent(context))
                }
                // Settings remains available after a permanent runtime denial.
                if (!access.notifications) TextButton(onClick = {
                    BackgroundAccess.open(context, BackgroundAccess.notificationsIntent(context))
                }) { Text("Uygulama bildirim ayarlarını aç") }
                ProtectionSetting("Otomatik başlatma", "Telefon ayarlarından kontrol edin", "Cihaz ayarları") {
                    openAutoStartSettings(context)
                }
                Text("Zorla durdurursanız uygulamayı yeniden açın.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("background-protection-done")) {
                    Text("Tamam")
                }
            }
        }
    }
}

@Composable
private fun ProtectionSetting(title: String, status: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(action) }
    }
}
