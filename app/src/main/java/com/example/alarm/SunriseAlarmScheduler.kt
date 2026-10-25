package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Exact alarm-clock scheduling, isolated from the prayer notification scheduler. */
object SunriseAlarmScheduler {
    const val ACTION_FIRE = "com.example.alarm.ACTION_FIRE_SUNRISE"
    const val ACTION_OPEN_ALARMS = "com.example.OPEN_SUNRISE_ALARMS"
    private const val PREFERENCES = "sunrise_alarm_runtime"
    private const val STATE = "state_v1"
    private const val EXTRA_DELIVERY = "sunrise_delivery_v1"
    private const val REQUEST_FIRE = 73_000
    private const val REQUEST_SHOW = 73_001
    internal const val DELIVERY_GRACE_MILLIS = 5 * 60_000L

    private data class RuntimeState(
        val pending: List<SunriseAlarmDelivery> = emptyList(),
        val configurations: Map<String, String> = emptyMap()
    )

    /** No inexact fallback: the screen explains the permissions needed to arm an alarm. */
    fun canSchedule(context: Context): Boolean = runCatching {
        SunriseAlarmPermissions.canScheduleExact(context) && SunriseAlarmPermissions.canNotify(context)
    }.getOrDefault(false)

    @Synchronized
    fun refresh(context: Context, now: Long = System.currentTimeMillis()) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val previous = read(context)
        val alarms = SunriseAlarmStore(context).list()
        val byId = alarms.associateBy { it.id }
        val configurations = alarms.associate { it.id to signature(it) }
        val changed = (previous.configurations.keys + configurations.keys).filter {
            previous.configurations[it] != configurations[it]
        }.toSet()
        val ringing = SunriseAlarmRingingState(context)
        changed.forEach { ringing.removeForAlarm(it) }
        SunriseAlarmRingingService.clearNotificationIfIdle(context)
        SunriseAlarmAudio.recoverVolumeIfIdle(context, now)

        if (!canSchedule(context)) {
            write(context, RuntimeState(configurations = configurations))
            previous.pending.forEach { cancel(context, manager, it) }
            return
        }

        val desired = SunriseAlarmPlanner.allNext(context, alarms, now).associateBy { it.alarmId }
        val keep = previous.pending.filter { delivery ->
            val alarm = byId[delivery.alarmId]
            alarm != null && alarm.id !in changed && alarm.enabled &&
                previous.configurations[alarm.id] == signature(alarm) &&
                isCurrent(context, alarm, delivery) && isTimelyOrFuture(delivery, now) &&
                (delivery.fromSnooze || delivery.triggerAtMillis <= now || matches(delivery, desired[alarm.id]))
        }.distinctBy { it.token }.toMutableList()

        for ((id, occurrence) in desired) {
            if (keep.none { !it.fromSnooze && it.alarmId == id && matches(it, occurrence) }) {
                keep += SunriseAlarmDelivery(id, UUID.randomUUID().toString(), occurrence.triggerAtMillis,
                    occurrence.sunriseAtMillis, occurrence.sourceDate)
            }
        }

        val armed = keep.filter { delivery ->
            // A due broadcast may already be in flight. Its token must survive T+0 refresh.
            if (delivery.triggerAtMillis <= now) true else runCatching {
                arm(context, manager, delivery)
            }.onFailure { Log.w("SunriseAlarmScheduler", "Could not arm sunrise alarm", it) }.isSuccess
        }
        write(context, RuntimeState(armed, configurations))
        SunriseAlarmPreNotification.sync(context, armed, now)
        val keptTokens = armed.mapTo(mutableSetOf()) { it.token }
        previous.pending.filterNot { it.token in keptTokens }.forEach { cancel(context, manager, it) }
    }

    /** A durable ring record is committed before consuming its one-shot delivery token. */
    @Synchronized
    fun receive(context: Context, intent: Intent, now: Long = System.currentTimeMillis()): SunriseAlarmDelivery? {
        if (intent.action != ACTION_FIRE) return null
        val delivery = decodeDelivery(intent) ?: return null
        val state = read(context)
        if (state.pending.none { it == delivery }) return null
        val alarm = SunriseAlarmStore(context).find(delivery.alarmId)
        val valid = canSchedule(context) && alarm != null && alarm.enabled &&
            state.configurations[alarm.id] == signature(alarm) && isCurrent(context, alarm, delivery) &&
            now >= delivery.triggerAtMillis && now - delivery.triggerAtMillis <= DELIVERY_GRACE_MILLIS
        if (!valid) {
            // An early or altered broadcast must not consume a correctly armed future delivery.
            if (now >= delivery.triggerAtMillis) {
                write(context, state.copy(pending = state.pending.filterNot { it.token == delivery.token }))
                (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.let { cancel(context, it, delivery) }
            }
            return null
        }
        SunriseAlarmRingingState(context).enqueue(delivery, requireNotNull(alarm).label, now)
        write(context, state.copy(pending = state.pending.filterNot { it.token == delivery.token }))
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.let { cancel(context, it, delivery) }
        return delivery
    }

    @Synchronized
    fun dismiss(context: Context, token: String) {
        SunriseAlarmRingingState(context).remove(token)
        SunriseAlarmRingingService.clearNotificationIfIdle(context)
    }

    @Synchronized
    fun snooze(
        context: Context,
        token: String,
        minutes: Int? = null,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        require(minutes == null || minutes in 1..30) { "Snooze duration is out of range" }
        val ringing = SunriseAlarmRingingState(context)
        val record = ringing.list().firstOrNull {
            it.delivery.token == token && now >= it.startedAtMillis && it.expiresAtMillis > now
        } ?: return false
        val snoozeMinutes = minutes ?: record.settings.snoozeMinutes
        val alarm = SunriseAlarmStore(context).find(record.delivery.alarmId) ?: return false
        val state = read(context)
        if (!canSchedule(context) || state.configurations[alarm.id] != signature(alarm) ||
            !isCurrent(context, alarm, record.delivery)) return false
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val delivery = record.delivery.copy(token = UUID.randomUUID().toString(),
            triggerAtMillis = now + snoozeMinutes * 60_000L, fromSnooze = true)
        return try {
            arm(context, manager, delivery)
            write(context, state.copy(pending = state.pending + delivery))
            ringing.remove(token)
            SunriseAlarmRingingService.clearNotificationIfIdle(context)
            true
        } catch (error: Exception) {
            cancel(context, manager, delivery)
            runCatching { write(context, state) }
            Log.w("SunriseAlarmScheduler", "Could not snooze sunrise alarm", error)
            false
        }
    }

    @Synchronized
    fun pending(context: Context): List<SunriseAlarmDelivery> = read(context).pending

    @Synchronized
    internal fun canShowPreNotification(context: Context, delivery: SunriseAlarmDelivery): Boolean {
        val state = read(context)
        val alarm = SunriseAlarmStore(context).find(delivery.alarmId) ?: return false
        return canSchedule(context) && alarm.enabled && state.pending.any { it == delivery } &&
            state.configurations[alarm.id] == signature(alarm) && isCurrent(context, alarm, delivery)
    }

    fun fireIntent(context: Context, delivery: SunriseAlarmDelivery): Intent =
        Intent(context, SunriseAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("ezanvakti://sunrise-alarm/v1/${Uri.encode(delivery.token)}")
            putExtra(EXTRA_DELIVERY, deliveryJson(delivery).toString())
        }

    fun decodeDelivery(intent: Intent): SunriseAlarmDelivery? = runCatching {
        parseDelivery(JSONObject(requireNotNull(intent.getStringExtra(EXTRA_DELIVERY))))
    }.getOrNull()

    private fun matches(delivery: SunriseAlarmDelivery, occurrence: SunriseAlarmOccurrence?) =
        occurrence != null && delivery.alarmId == occurrence.alarmId &&
            delivery.triggerAtMillis == occurrence.triggerAtMillis && delivery.sunriseAtMillis == occurrence.sunriseAtMillis &&
            delivery.sourceDate == occurrence.sourceDate

    private fun isCurrent(context: Context, alarm: SunriseAlarm, delivery: SunriseAlarmDelivery): Boolean {
        val originalTrigger = if (delivery.fromSnooze) delivery.sunriseAtMillis - alarm.minutesBeforeSunrise * 60_000L
            else delivery.triggerAtMillis
        return SunriseAlarmPlanner.isCurrentOccurrence(context, alarm,
            SunriseAlarmOccurrence(delivery.alarmId, delivery.sunriseAtMillis, originalTrigger, delivery.sourceDate))
    }

    private fun isTimelyOrFuture(delivery: SunriseAlarmDelivery, now: Long) =
        delivery.triggerAtMillis > now || now - delivery.triggerAtMillis <= DELIVERY_GRACE_MILLIS

    private fun arm(context: Context, manager: AlarmManager, delivery: SunriseAlarmDelivery) {
        val show = PendingIntent.getActivity(context, REQUEST_SHOW,
            Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_ALARMS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pending = PendingIntent.getBroadcast(context, REQUEST_FIRE, fireIntent(context, delivery),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(delivery.triggerAtMillis, show), pending)
    }

    private fun cancel(context: Context, manager: AlarmManager, delivery: SunriseAlarmDelivery) {
        SunriseAlarmPreNotification.cancel(context, delivery)
        runCatching {
            val pending = PendingIntent.getBroadcast(context, REQUEST_FIRE, fireIntent(context, delivery),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
            manager.cancel(pending)
            // Never invalidate a PendingIntent whose broadcast may already have been queued.
        }.onFailure { Log.w("SunriseAlarmScheduler", "Could not cancel sunrise alarm", it) }
    }

    private fun signature(alarm: SunriseAlarm) = JSONArray().put(alarm.minutesBeforeSunrise).put(alarm.enabled).put(alarm.label).toString()

    private fun read(context: Context): RuntimeState = runCatching {
        val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val root = JSONObject(prefs.getString(STATE, "{}") ?: "{}")
        val array = root.optJSONArray("pending") ?: JSONArray()
        val pending = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                runCatching { parseDelivery(item) }.getOrNull()?.let { add(it) }
            }
        }.distinctBy { it.token }
        val config = root.optJSONObject("configurations") ?: JSONObject()
        RuntimeState(pending, config.keys().asSequence().associateWith { config.getString(it) })
    }.getOrDefault(RuntimeState())

    private fun write(context: Context, state: RuntimeState) {
        val root = JSONObject().put("pending", JSONArray().apply { state.pending.forEach { put(deliveryJson(it)) } })
            .put("configurations", JSONObject(state.configurations))
        check(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(STATE, root.toString()).commit()) { "Could not save pending sunrise alarms" }
    }

    private fun deliveryJson(delivery: SunriseAlarmDelivery) = JSONObject().apply {
        put("alarmId", delivery.alarmId)
        put("token", delivery.token)
        put("triggerAtMillis", delivery.triggerAtMillis)
        put("sunriseAtMillis", delivery.sunriseAtMillis)
        put("sourceDate", delivery.sourceDate)
        put("fromSnooze", delivery.fromSnooze)
    }

    private fun parseDelivery(item: JSONObject): SunriseAlarmDelivery {
        val delivery = SunriseAlarmDelivery(item.getString("alarmId"), item.getString("token"),
            item.getLong("triggerAtMillis"), item.getLong("sunriseAtMillis"), item.getString("sourceDate"),
            item.optBoolean("fromSnooze", false))
        require(delivery.alarmId.isNotBlank() && delivery.alarmId.length <= 100)
        require(delivery.token.isNotBlank() && delivery.token.length <= 200)
        require(delivery.triggerAtMillis > 0 && delivery.sunriseAtMillis > 0)
        require(delivery.sourceDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        return delivery
    }
}
