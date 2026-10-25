package com.example.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Requires a deliberate face-up to stable face-down transition after ringing has begun. */
internal class SunriseAlarmFlipDetector {
    private var seenFaceUp = false
    private var faceDownSince: Long? = null
    private var fired = false

    fun update(z: Float, elapsedMillis: Long): Boolean {
        if (fired) return false
        if (z > 6f) {
            seenFaceUp = true
            faceDownSince = null
        } else if (z < -6f && seenFaceUp) {
            val start = faceDownSince ?: elapsedMillis.also { faceDownSince = it }
            if (elapsedMillis - start >= 600L) {
                fired = true
                return true
            }
        } else faceDownSince = null
        return false
    }
}

/** Registered only while a ring record with flip enabled is actively sounding. */
internal class SunriseAlarmFlipGesture(context: Context, private val onFlip: () -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private var detector = SunriseAlarmFlipDetector()
    private var registered = false

    fun start() {
        if (registered) return
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        detector = SunriseAlarmFlipDetector()
        registered = manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL) == true
    }

    fun stop() {
        if (registered) manager?.unregisterListener(this)
        registered = false
        detector = SunriseAlarmFlipDetector()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (registered && event.values.size >= 3 && detector.update(event.values[2], event.timestamp / 1_000_000L)) onFlip()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
