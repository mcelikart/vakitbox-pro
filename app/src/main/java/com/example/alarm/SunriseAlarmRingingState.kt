package com.example.alarm

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** A delivery stays in this durable queue until it is dismissed, snoozed, or times out. */
data class SunriseAlarmRingRecord(
    val delivery: SunriseAlarmDelivery,
    val label: String,
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
    val settings: SunriseAlarmSettings = SunriseAlarmSettings()
)

class SunriseAlarmRingingState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val settingsStore = SunriseAlarmSettingsStore(context)

    // Expired records remain visible to the service so that their cleanup also releases audio resources.
    fun list(): List<SunriseAlarmRingRecord> = synchronized(lock) { read() }

    fun enqueue(
        delivery: SunriseAlarmDelivery,
        label: String,
        now: Long = System.currentTimeMillis()
    ): SunriseAlarmRingRecord = synchronized(lock) {
        val current = read()
        current.firstOrNull { it.delivery.token == delivery.token }?.let { return@synchronized it }
        val settings = settingsStore.read()
        val record = SunriseAlarmRingRecord(
            delivery = delivery,
            label = label.trim().take(80).ifBlank { SunriseAlarm.DEFAULT_LABEL },
            startedAtMillis = now,
            expiresAtMillis = now + settings.ringMinutes * 60_000L,
            settings = settings
        )
        write(current + record)
        record
    }

    fun remove(token: String) = synchronized(lock) {
        val current = read()
        val next = current.filterNot { it.delivery.token == token }
        if (current.size != next.size) write(next)
    }

    fun removeForAlarm(alarmId: String) = synchronized(lock) {
        val current = read()
        val next = current.filterNot { it.delivery.alarmId == alarmId }
        if (current.size != next.size) write(next)
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        preferences.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        preferences.unregisterOnSharedPreferenceChangeListener(listener)

    private fun read(): List<SunriseAlarmRingRecord> {
        val array = runCatching { JSONArray(preferences.getString(RECORDS, "[]")) }.getOrNull() ?: return emptyList()
        val tokens = mutableSetOf<String>()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val record = runCatching {
                    val alarmId = item.getString("alarmId")
                    val token = item.getString("token")
                    require(alarmId.isNotBlank() && alarmId.length <= 100 && token.isNotBlank() && token.length <= 200)
                    val start = item.getLong("startedAtMillis")
                    val expires = item.getLong("expiresAtMillis")
                    require(start > 0L && expires > start && expires - start <= MAX_RING_DURATION_MILLIS)
                    SunriseAlarmRingRecord(
                        delivery = SunriseAlarmDelivery(
                            alarmId = alarmId,
                            token = token,
                            triggerAtMillis = item.getLong("triggerAtMillis"),
                            sunriseAtMillis = item.getLong("sunriseAtMillis"),
                            sourceDate = item.getString("sourceDate"),
                            fromSnooze = item.optBoolean("fromSnooze", false)
                        ),
                        label = item.optString("label", SunriseAlarm.DEFAULT_LABEL).take(80)
                            .let { if (it == SunriseAlarm.LEGACY_DEFAULT_LABEL) SunriseAlarm.DEFAULT_LABEL else it }
                            .ifBlank { SunriseAlarm.DEFAULT_LABEL },
                        startedAtMillis = start,
                        expiresAtMillis = expires,
                        settings = item.optJSONObject("settings")?.let(SunriseAlarmSettings::fromJson) ?: SunriseAlarmSettings()
                    )
                }.getOrNull() ?: continue
                if (tokens.add(record.delivery.token)) add(record)
            }
        }.sortedWith(compareBy<SunriseAlarmRingRecord> { it.startedAtMillis }.thenBy { it.delivery.token })
    }

    private fun write(records: List<SunriseAlarmRingRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().apply {
                put("alarmId", record.delivery.alarmId)
                put("token", record.delivery.token)
                put("triggerAtMillis", record.delivery.triggerAtMillis)
                put("sunriseAtMillis", record.delivery.sunriseAtMillis)
                put("sourceDate", record.delivery.sourceDate)
                put("fromSnooze", record.delivery.fromSnooze)
                put("label", record.label)
                put("startedAtMillis", record.startedAtMillis)
                put("expiresAtMillis", record.expiresAtMillis)
                put("settings", record.settings.toJson())
            })
        }
        check(preferences.edit().putString(RECORDS, array.toString()).commit()) { "Could not save ringing alarms" }
    }

    companion object {
        const val MAX_RING_DURATION_MILLIS = 30 * 60_000L
        internal const val PREFERENCES = "sunrise_alarm_ringing"
        internal const val RECORDS = "active_v1"
        private val lock = Any()
    }
}
