package com.pixplicity.thermostat

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import java.io.IOException


private val TAG = MainActivity::class.java.simpleName

class MainActivity : Activity() {
    companion object {
        private const val TEMP_MIN = 10f
        private const val TEMP_MAX = 28f
    }

    private val TAG = MainActivity::class.java!!.getSimpleName()

    private val handler = Handler()
    private val reader = object : Runnable {
        override fun run() {
            readData()
            handler.postDelayed(this, 500)
        }
    }

    private var tempTarget = 20f

    private lateinit var tvTempCurrent: TextView
    private lateinit var tvTempTarget: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTempCurrent = findViewById(R.id.tv_temp_current)
        tvTempTarget = findViewById(R.id.tv_temp_target)

        findViewById<ImageButton>(R.id.bt_decrease).setOnClickListener {
            tempTarget = Math.max(TEMP_MIN, Math.min(tempTarget - 0.5f, TEMP_MAX))
            updateTarget()
        }
        findViewById<ImageButton>(R.id.bt_increase).setOnClickListener {
            tempTarget = Math.max(TEMP_MIN, Math.min(tempTarget + 0.5f, TEMP_MAX))
            updateTarget()
        }
    }

    override fun onStart() {
        super.onStart()

        initSensor()
        updateTarget()
        handler.post(reader)
    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacks(reader)
        closeSensor()
    }

    private var mBmp180: Bmp180? = null
    private val mI2cPort = BoardDefaults.getI2CPort()

    private fun initSensor() {
        mBmp180 = Bmp180(mI2cPort)
    }

    private fun readData() {
        try {
            val temp = mBmp180!!.readTemperature()
            val press = mBmp180!!.readPressure().toFloat()
            val alt = mBmp180!!.readAltitude().toDouble()
            Log.d(TAG, "loop: temp $temp alt: $alt press: $press")
            tvTempCurrent.text = String.format("%.1f", temp)
        } catch (e: IOException) {
            Log.e(TAG, "Sensor loop  error : ", e)
        }
    }

    private fun closeSensor() {
        try {
            mBmp180!!.close()
        } catch (e: IOException) {
            Log.e(TAG, "closeSensor  error: ", e)
        }

        mBmp180 = null
    }

    private fun updateTarget() {
        tvTempTarget.text = String.format("%.1f", tempTarget)
    }

}
