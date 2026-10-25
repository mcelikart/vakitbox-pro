package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SunriseAlarmSchedulerTest {
    private lateinit var context: Context
    private lateinit var manager: AlarmManager
    private lateinit var originalZone: TimeZone
    private lateinit var store: SunriseAlarmStore
    private val now = at("2027-01-31T05:00:00Z")
    private val trigger = at("2027-01-31T06:45:00Z")
    private fun at(value: String) = Instant.parse(value).toEpochMilli()

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(AlarmManager::class.java)
        listOf(SunriseAlarmStore.PREFERENCES, SunriseAlarmRingingState.PREFERENCES, SunriseAlarmSettingsStore.PREFERENCES,
            "sunrise_alarm_runtime", "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
        // These tests isolate exact ringing deliveries; upcoming notices are covered separately.
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(preNotificationMinutes = 0))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= 33) shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        cache("2027-01-31", "Güneş,07:15")
        cache("2027-02-01", "Güneş,07:16")
        store = SunriseAlarmStore(context)
    }

    @After fun restoreZone() {
        TimeZone.setDefault(originalZone)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun cache(date: String, times: String) {
        context.getSharedPreferences("prayer_daily_times", 0).edit().putString(date, times).commit()
    }

    private fun save(id: String = "first", minutes: Int = 30): SunriseAlarm {
        return SunriseAlarm(id = id, minutesBeforeSunrise = minutes).also { store.save(it) }
    }

    private fun pending() = SunriseAlarmScheduler.pending(context)
    private fun refresh(time: Long = now) = SunriseAlarmScheduler.refresh(context, time)
    private fun fire(delivery: SunriseAlarmDelivery, time: Long = delivery.triggerAtMillis) =
        SunriseAlarmScheduler.receive(context, SunriseAlarmScheduler.fireIntent(context, delivery), time)
    private fun ringing() = SunriseAlarmRingingState(context).list()

    @Test fun defaultIsAnExactAlarmClockWithAStableTokenAndAlarmScreenShowIntent() {
        save()
        refresh()
        val delivery = pending().single()
        assertEquals(trigger, delivery.triggerAtMillis)
        assertFalse(delivery.fromSnooze)
        val scheduled = shadowOf(manager).scheduledAlarms.single()
        assertEquals(trigger, scheduled.alarmClockInfo?.triggerTime)
        assertEquals(SunriseAlarmScheduler.ACTION_OPEN_ALARMS,
            shadowOf(requireNotNull(scheduled.showIntent)).savedIntent.action)
        refresh()
        assertEquals(delivery, pending().single())
        assertEquals(1, shadowOf(manager).scheduledAlarms.size)
    }

    @Test @Config(sdk = [24]) fun exactAlarmClockAlsoArmsOnMinimumSupportedAndroid() {
        save()
        refresh()
        assertEquals(trigger, shadowOf(manager).scheduledAlarms.single().alarmClockInfo?.triggerTime)
    }

    @Test fun multipleAlarmsCanHaveIndependentTokensAndTheSameTrigger() {
        save("first")
        save("second")
        refresh()
        assertEquals(2, pending().size)
        assertEquals(2, pending().map { it.token }.distinct().size)
        assertEquals(listOf(trigger, trigger), shadowOf(manager).scheduledAlarms.map { it.triggerAtTime })
        pending().forEach { assertEquals(it, fire(it)) }
        assertEquals(setOf("first", "second"), ringing().map { it.delivery.alarmId }.toSet())
    }

    @Test fun refreshAtExactDueTimePreservesDueTokenAndAlsoArmsNextDatedDay() {
        save()
        refresh()
        val due = pending().single()
        val token = shadowOf(manager).scheduledAlarms.single().operation
        refresh(trigger)
        assertTrue(pending().contains(due))
        assertEquals(2, pending().size)
        assertEquals(token, shadowOf(manager).scheduledAlarms.single { it.triggerAtTime == trigger }.operation)
        assertEquals(due, fire(due))
        assertEquals(1, ringing().size)
        assertEquals("2027-02-01", pending().single().sourceDate)
    }

    @Test fun tokenIsConsumedOnlyOnceAndRingStateIsDurableBeforeReceiverReturns() {
        save()
        refresh()
        val due = pending().single()
        assertEquals(due, fire(due))
        assertTrue(pending().isEmpty())
        assertEquals(due, SunriseAlarmRingingState(context).list().single().delivery)
        assertNull(fire(due))
        assertEquals(1, ringing().size)
    }

    @Test fun processRecoveryAfterEnqueueBeforeConsumeDoesNotDuplicateRingRecord() {
        val alarm = save()
        refresh()
        val due = pending().single()
        SunriseAlarmRingingState(context).enqueue(due, alarm.label, trigger)
        assertEquals(due, fire(due, trigger + 1_000L))
        assertEquals(1, ringing().size)
        assertEquals(trigger, ringing().single().startedAtMillis)
        assertTrue(pending().isEmpty())
    }

    @Test fun earlyBroadcastDoesNotConsumeFutureAlarmButLateBroadcastNeverRings() {
        save()
        refresh()
        val due = pending().single()
        assertNull(fire(due, trigger - 1L))
        assertEquals(listOf(due), pending())
        assertNull(fire(due, trigger + SunriseAlarmScheduler.DELIVERY_GRACE_MILLIS + 1L))
        assertTrue(ringing().isEmpty())
        assertTrue(pending().isEmpty())
    }

    @Test fun endOfGraceWindowCanStillRingButRefreshAfterItRemovesMissedToken() {
        save()
        refresh()
        val due = pending().single()
        refresh(trigger + SunriseAlarmScheduler.DELIVERY_GRACE_MILLIS)
        assertTrue(pending().contains(due))
        refresh(trigger + SunriseAlarmScheduler.DELIVERY_GRACE_MILLIS + 1L)
        assertFalse(pending().contains(due))
        assertNull(fire(due, trigger + SunriseAlarmScheduler.DELIVERY_GRACE_MILLIS + 1L))
    }

    @Test fun changedOffsetLabelOrDeletedAlarmRejectsAlreadyQueuedOldDelivery() {
        val alarm = save()
        refresh()
        val old = pending().single()
        store.save(alarm.copy(minutesBeforeSunrise = 45))
        assertNull(fire(old))
        refresh()
        val edited = pending().single()
        assertNotEquals(old.token, edited.token)
        assertEquals(trigger - 15 * 60_000L, edited.triggerAtMillis)
        store.save(alarm.copy(minutesBeforeSunrise = 45, label = "Yeni ad"))
        assertNull(fire(edited))
        refresh()
        val renamed = pending().single()
        store.delete(alarm.id)
        assertNull(fire(renamed))
        refresh()
        assertTrue(pending().isEmpty())
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
    }

    @Test fun disablingOneAlarmCancelsItsPendingSnoozesAndRingingButPreservesOthers() {
        val first = save("first")
        save("second")
        refresh()
        val due = pending().toList()
        due.forEach { fire(it) }
        assertTrue(SunriseAlarmScheduler.snooze(context, due.first { it.alarmId == "first" }.token, 10, trigger + 1_000L))
        store.save(first.copy(enabled = false))
        refresh(trigger + 2_000L)
        assertTrue(pending().none { it.alarmId == "first" })
        assertEquals(listOf("second"), ringing().map { it.delivery.alarmId })
        assertEquals(listOf("second"), pending().map { it.alarmId })
    }

    @Test fun locationAndTimezoneChangesInvalidateQueuedOriginalData() {
        save()
        refresh()
        val old = pending().single()
        cache("2027-01-31", "Güneş,07:20")
        assertNull(fire(old))
        refresh()
        val moved = pending().single()
        assertEquals(trigger + 5 * 60_000L, moved.triggerAtMillis)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        assertNull(fire(moved))
        refresh()
        assertEquals("2027-02-01", pending().single().sourceDate)
    }

    @Test fun missingCacheAndDisabledAllLeaveNoInventedAlarm() {
        val alarm = save()
        context.getSharedPreferences("prayer_daily_times", 0).edit().clear().commit()
        refresh()
        assertTrue(pending().isEmpty())
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
        cache("2027-01-31", "Güneş,07:15")
        store.save(alarm.copy(enabled = false))
        refresh()
        assertTrue(pending().isEmpty())
    }

    @Test fun revokedExactPermissionCancelsExistingAlarmsAndDoesNotUseInexactFallback() {
        save()
        refresh()
        val old = pending().single()
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(SunriseAlarmScheduler.canSchedule(context))
        assertNull(fire(old))
        refresh()
        assertTrue(pending().isEmpty())
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        refresh()
        assertEquals(1, pending().size)
        assertNotEquals(old.token, pending().single().token)
    }

    @Test fun disabledNotificationsPreventArmingAndLeaveUserSettingsIntact() {
        val alarm = save()
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        refresh()
        assertFalse(SunriseAlarmScheduler.canSchedule(context))
        assertTrue(pending().isEmpty())
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
        assertEquals(alarm, store.find(alarm.id))
    }

    @Test fun rebootCanRearmPersistentPendingDeliveryWithoutChangingItsToken() {
        save()
        refresh()
        val before = pending().single()
        shadowOf(manager).scheduledAlarms.forEach { manager.cancel(requireNotNull(it.operation)) }
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
        refresh()
        assertEquals(listOf(before), pending())
        assertEquals(trigger, shadowOf(manager).scheduledAlarms.single().triggerAtTime)
    }

    @Test fun dismissRemovesOnlyChosenRingAndKeepsTomorrowsRegularAlarm() {
        save("first")
        save("second")
        refresh()
        val due = pending().toList()
        due.forEach { fire(it) }
        refresh(trigger)
        SunriseAlarmRingingService.postStartFailureNotification(context)
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        SunriseAlarmScheduler.dismiss(context, due.first { it.alarmId == "first" }.token)
        assertEquals(listOf("second"), ringing().map { it.delivery.alarmId })
        assertNotNull(notifications.getNotification(SunriseAlarmRingingService.NOTIFICATION_ID))
        assertEquals(2, pending().size)
        assertTrue(pending().all { it.sourceDate == "2027-02-01" })
        SunriseAlarmScheduler.dismiss(context, due.first { it.alarmId == "second" }.token)
        assertNull(notifications.getNotification(SunriseAlarmRingingService.NOTIFICATION_ID))
    }

    @Test fun snoozeArmsTenMinutesFromActionAndDoesNotReplaceNextRegularAlarm() {
        save()
        refresh()
        val due = pending().single()
        fire(due)
        refresh(trigger)
        val regular = pending().single()
        val snoozeAt = trigger + 20_000L
        SunriseAlarmRingingService.postStartFailureNotification(context)
        assertTrue(SunriseAlarmScheduler.snooze(context, due.token, now = snoozeAt))
        assertTrue(ringing().isEmpty())
        assertNull(shadowOf(context.getSystemService(NotificationManager::class.java))
            .getNotification(SunriseAlarmRingingService.NOTIFICATION_ID))
        val snoozed = pending().single { it.fromSnooze }
        assertEquals(snoozeAt + 600_000L, snoozed.triggerAtMillis)
        assertNotEquals(due.token, snoozed.token)
        assertTrue(pending().contains(regular))
        assertFalse(SunriseAlarmScheduler.snooze(context, due.token, now = snoozeAt))
        assertEquals(snoozed, fire(snoozed))
        assertEquals(snoozed, ringing().single().delivery)
    }

    @Test fun failedPermissionAndExpiredRingSnoozesKeepNoFalseScheduledClaim() {
        save()
        refresh()
        val due = pending().single()
        fire(due)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(SunriseAlarmScheduler.snooze(context, due.token, now = trigger + 1_000L))
        assertEquals(1, ringing().size)
        assertTrue(pending().isEmpty())
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertFalse(SunriseAlarmScheduler.snooze(context, due.token, now = trigger + 600_000L))
        assertTrue(pending().isEmpty())
        assertTrue(runCatching { SunriseAlarmScheduler.snooze(context, due.token, 0) }.isFailure)
    }

    @Test fun changedOffsetCancelsActiveRingAndSnoozedTokenBeforeReplacement() {
        val alarm = save()
        refresh()
        val original = pending().single()
        fire(original)
        assertTrue(SunriseAlarmScheduler.snooze(context, original.token, now = trigger + 1_000L))
        val snoozed = pending().single()
        fire(snoozed)
        store.save(alarm.copy(minutesBeforeSunrise = 10))
        refresh(snoozed.triggerAtMillis)
        assertTrue(ringing().isEmpty())
        assertEquals(1, pending().size)
        assertFalse(pending().single().fromSnooze)
        assertEquals(at("2027-01-31T07:05:00Z"), pending().single().triggerAtMillis)
        assertNull(fire(snoozed))
    }

    @Test fun alteredExtrasAndUnknownTokenDoNotConsumeAValidDelivery() {
        save()
        refresh()
        val due = pending().single()
        assertNull(fire(due.copy(triggerAtMillis = due.triggerAtMillis - 1L)))
        assertNull(fire(due.copy(token = "not-armed")))
        assertEquals(listOf(due), pending())
        assertTrue(ringing().isEmpty())
    }
}
