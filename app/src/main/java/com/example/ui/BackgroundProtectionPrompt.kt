package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** A dismissible setup reminder; opening settings never counts as granting access. */
@Composable
fun BackgroundProtectionPrompt(canPrompt: Boolean) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var dismissed by remember(prefs) { mutableStateOf(prefs.getBoolean(SEEN_KEY, false)) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRevision++
    }
    key(permissionRevision) {
        val access = rememberBackgroundAccess()
        val needsGuidance = !access.batteryExempt || access.backgroundRestricted ||
            !access.exactAlarms || !access.notifications || !access.countdownChannel
        if (canPrompt && !dismissed && needsGuidance) {
            BackgroundProtectionDialog(
                onDismiss = {
                    dismissed = true
                    prefs.edit().putBoolean(SEEN_KEY, true).apply()
                },
                onRequestPermissions = {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        BackgroundAccess.open(context, BackgroundAccess.notificationsIntent(context))
                    }
                }
            )
        }
    }
}

private const val PREFS = "background_protection"
private const val SEEN_KEY = "background_guidance_v1_seen"
