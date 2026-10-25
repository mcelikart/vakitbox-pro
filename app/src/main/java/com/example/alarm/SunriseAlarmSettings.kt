package com.example.alarm

import android.content.Context
import android.net.Uri
import org.json.JSONObject

enum class AlarmVolumeKeyAction(val title: String) {
    SNOOZE("Ertele"), DISMISS("Kapat"), VOLUME("Ses düzeyini değiştir"), NONE("İşlem yapma")
}

/** Global defaults are snapshotted when an alarm actually rings; editing them never extends a ring. */
data class SunriseAlarmSettings(
    val ringtoneUri: String? = null,
    val ringMinutes: Int = 10,
    val volumePercent: Int? = null,
    val gradualVolume: Boolean = false,
    val preNotificationMinutes: Int = 30,
    val snoozeMinutes: Int = 10,
    val flipToSnooze: Boolean = false,
    val volumeKeyAction: AlarmVolumeKeyAction = AlarmVolumeKeyAction.SNOOZE
) {
    fun sanitized() = copy(
        ringtoneUri = ringtoneUri?.takeIf { it.length <= 2048 && runCatching {
            Uri.parse(it).scheme in setOf("content", "android.resource")
        }.getOrDefault(false) },
        ringMinutes = ringMinutes.takeIf { it in RING_MINUTES } ?: 10,
        volumePercent = volumePercent?.takeIf { it in 1..100 },
        preNotificationMinutes = preNotificationMinutes.takeIf { it in PRE_NOTIFICATION_MINUTES } ?: 30,
        snoozeMinutes = snoozeMinutes.takeIf { it in SNOOZE_MINUTES } ?: 10
    )

    internal fun toJson() = JSONObject().apply {
        put("ringtoneUri", ringtoneUri ?: JSONObject.NULL)
        put("ringMinutes", ringMinutes)
        put("volumePercent", volumePercent ?: JSONObject.NULL)
        put("gradualVolume", gradualVolume)
        put("preNotificationMinutes", preNotificationMinutes)
        put("snoozeMinutes", snoozeMinutes)
        put("flipToSnooze", flipToSnooze)
        put("volumeKeyAction", volumeKeyAction.name)
    }

    companion object {
        val RING_MINUTES = listOf(1, 5, 10, 15, 20, 30)
        val PRE_NOTIFICATION_MINUTES = listOf(0, 5, 10, 15, 30, 60)
        val SNOOZE_MINUTES = listOf(5, 10, 15, 20, 30)

        internal fun fromJson(json: JSONObject) = SunriseAlarmSettings(
            ringtoneUri = json.optString("ringtoneUri").takeUnless { it.isBlank() || it == "null" },
            ringMinutes = json.optInt("ringMinutes", 10),
            volumePercent = if (json.isNull("volumePercent")) null else json.optInt("volumePercent", -1),
            gradualVolume = json.optBoolean("gradualVolume", false),
            preNotificationMinutes = json.optInt("preNotificationMinutes", 30),
            snoozeMinutes = json.optInt("snoozeMinutes", 10),
            flipToSnooze = json.optBoolean("flipToSnooze", false),
            volumeKeyAction = runCatching {
                AlarmVolumeKeyAction.valueOf(json.optString("volumeKeyAction"))
            }.getOrDefault(AlarmVolumeKeyAction.SNOOZE)
        ).sanitized()
    }
}

class SunriseAlarmSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): SunriseAlarmSettings = runCatching {
        SunriseAlarmSettings.fromJson(JSONObject(preferences.getString(SETTINGS, "{}") ?: "{}"))
    }.getOrDefault(SunriseAlarmSettings())

    fun save(settings: SunriseAlarmSettings) {
        check(preferences.edit().putString(SETTINGS, settings.sanitized().toJson().toString()).commit()) {
            "Could not save alarm settings"
        }
    }

    companion object {
        internal const val PREFERENCES = "sunrise_alarm_settings"
        internal const val SETTINGS = "settings_v1"
    }
}
