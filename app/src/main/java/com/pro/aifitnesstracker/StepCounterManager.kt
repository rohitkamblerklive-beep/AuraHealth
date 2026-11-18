package com.pro.aifitnesstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepCounterManager(
    context: Context,
    private val prefsManager: SharedPrefsManager,
    private val onStepCountUpdated: (Int) -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    fun startListening() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            val lastValue = prefsManager.getLastSensorValue()
            val isNewDay = prefsManager.checkIfNewDay()
            
            if (lastValue <= 0 || isNewDay) {
                if (isNewDay) prefsManager.resetDay()
                prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                onStepCountUpdated(prefsManager.getStepsToday())
                return
            }

            if (totalStepsSinceBoot < lastValue) {
                // Phone rebooted, sensor reset
                prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                return
            }

            val delta = totalStepsSinceBoot - lastValue
            if (delta > 50) {
                // Anti-spike anomaly filter
                prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                return
            }

            if (delta > 0) {
                val currentStepsToday = prefsManager.getStepsToday()
                val updatedSteps = currentStepsToday + delta
                
                prefsManager.saveStepsToday(updatedSteps)
                prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                onStepCountUpdated(updatedSteps)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
