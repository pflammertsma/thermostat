package com.pixplicity.thermostat

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.DynamicSensorCallback
import android.os.Bundle
import android.util.Log
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver
import java.io.IOException


private val TAG = MainActivity::class.java.simpleName

class MainActivity : Activity(), SensorEventListener {

    private val TAG = MainActivity::class.java!!.getSimpleName()

    private var mTemperatureSensorDriver: Bmx280SensorDriver? = null
    private lateinit var mSensorManager: SensorManager

    private val mDynamicSensorCallback = object : DynamicSensorCallback() {
        override fun onDynamicSensorConnected(sensor: Sensor) {
            if (sensor.getType() === Sensor.TYPE_AMBIENT_TEMPERATURE) {
                Log.i(TAG, "Temperature sensor connected")
                mSensorManager!!.registerListener(this@MainActivity,
                        sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Starting TemperatureActivity")

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback)

        try {
            mTemperatureSensorDriver = Bmx280SensorDriver(BoardDefaults.getI2CPort())
            mTemperatureSensorDriver!!.registerTemperatureSensor()
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring sensor", e)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Closing sensor")
        if (mTemperatureSensorDriver != null) {
            mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback)
            mSensorManager.unregisterListener(this)
            mTemperatureSensorDriver!!.unregisterTemperatureSensor()
            try {
                mTemperatureSensorDriver!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing sensor", e)
            } finally {
                mTemperatureSensorDriver = null
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        Log.i(TAG, "sensor changed: " + event.values[0])
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy)
    }

}

