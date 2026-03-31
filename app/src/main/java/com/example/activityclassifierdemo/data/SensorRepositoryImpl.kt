package com.example.activityclassifierdemo.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.activityclassifierdemo.domain.SensorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val SENSOR_RATE_HZ = 50
private const val SENSOR_DELAY_US = 1_000_000 / SENSOR_RATE_HZ // 20,000 μs = 20 ms

/**
 * Reads linear acceleration and gyroscope sensors at 50Hz.
 * Emits 8-element float arrays: [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq]
 */
@Singleton
class SensorRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SensorRepository {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscopeSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // ── Public flows ────────────────────────────────────────────────────────

    private val _sensorData = MutableSharedFlow<FloatArray>(
        replay = 0,
        extraBufferCapacity = 10
    )
    override val sensorData: SharedFlow<FloatArray> = _sensorData.asSharedFlow()

    private val _isRecording = MutableSharedFlow<Boolean>(
        replay = 1
    )
    override val isRecording: SharedFlow<Boolean> = _isRecording.asSharedFlow()

    // ── Internal state ──────────────────────────────────────────────────────

    /** Latest accelerometer readings [x, y, z] in m/s² */
    private val latestAccel = FloatArray(3)
    
    /** Latest gyroscope readings [x, y, z] in rad/s */
    private val latestGyro = FloatArray(3)

    private var accelReady = false
    private var gyroReady = false

    // ── Sensor listener ─────────────────────────────────────────────────────

    private val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            if (_isRecording.replayCache.lastOrNull() != true) return

            when (event.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    latestAccel[0] = event.values[0]
                    latestAccel[1] = event.values[1]
                    latestAccel[2] = event.values[2]
                    accelReady = true
                }

                Sensor.TYPE_GYROSCOPE -> {
                    latestGyro[0] = event.values[0]
                    latestGyro[1] = event.values[1]
                    latestGyro[2] = event.values[2]
                    gyroReady = true
                    emitSensorData()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Emits combined sensor data when both sensors have delivered at least one reading.
     * Array format: [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq]
     */
    private fun emitSensorData() {
        if (!accelReady || !gyroReady) return

        // Calculate sum of squares (magnitude squared)
        val accMagSq = latestAccel[0] * latestAccel[0] + 
                       latestAccel[1] * latestAccel[1] + 
                       latestAccel[2] * latestAccel[2]

        val gyroMagSq = latestGyro[0] * latestGyro[0] + 
                        latestGyro[1] * latestGyro[1] + 
                        latestGyro[2] * latestGyro[2]

        val data = floatArrayOf(
            latestAccel[0], latestAccel[1], latestAccel[2],
            latestGyro[0], latestGyro[1], latestGyro[2],
            accMagSq, gyroMagSq
        )
        _sensorData.tryEmit(data)
    }

    // ── Repository API ──────────────────────────────────────────────────────

    override fun startRecording() {
        accelReady = false
        gyroReady = false
        _isRecording.tryEmit(true)

        accelerometerSensor?.let { 
            sensorManager.registerListener(listener, it, SENSOR_DELAY_US) 
        }
        gyroscopeSensor?.let { 
            sensorManager.registerListener(listener, it, SENSOR_DELAY_US) 
        }
    }

    override fun stopRecording() {
        _isRecording.tryEmit(false)
        sensorManager.unregisterListener(listener)
    }
}
