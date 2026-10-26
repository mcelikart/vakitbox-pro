package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val access = rememberBackgroundAccess()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Arka Planda Çalışma") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .testTag("background-settings-scroll")
        ) {
            BackgroundSettingRow("Pil kullanımı", access.batteryStatus, "Pil ayarları") {
                requestIgnoreBatteryOptimizations(context)
            }
            BackgroundSettingRow("Tam zamanında alarmlar", if (access.exactAlarms) "İzin verildi" else "İzin gerekli", "Alarm izinleri") {
                BackgroundAccess.open(context, BackgroundAccess.exactAlarmIntent(context))
            }
            BackgroundSettingRow("Otomatik başlatma", "Telefon ayarlarından kontrol edin", "Otomatik başlatma ayarları") {
                openAutoStartSettings(context)
            }
            Text(
                "Telefonunuzda varsa otomatik başlatmayı açın ve pil kullanımını kısıtlamayın. Zorla durdurursanız uygulamayı yeniden açın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            BackgroundSettingRow("Geri sayım bildirimi", access.countdownStatus, "Geri sayım bildirim ayarları") {
                BackgroundAccess.open(context, BackgroundAccess.countdownIntent(context))
            }
            BackgroundSettingRow("Bildirimler", if (access.notifications) "Açık" else "Kapalı", "Bildirim ayarları") {
                BackgroundAccess.open(context, BackgroundAccess.notificationsIntent(context))
            }
            Spacer(Modifier.height(com.example.ui.theme.navBarBottomPadding()))
        }
    }
}

@Composable
private fun BackgroundSettingRow(title: String, status: String, actionLabel: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(status) },
        trailingContent = { IconButton(onClick = onClick) { Icon(Icons.Default.Settings, actionLabel) } }
    )
}
