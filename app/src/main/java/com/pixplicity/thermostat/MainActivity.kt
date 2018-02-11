package com.pixplicity.thermostat

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import java.io.IOException


private val TAG = MainActivity::class.java.simpleName

class MainActivity : Activity() {

    private val TAG = MainActivity::class.java!!.getSimpleName()

    private val handler = Handler()
    private val reader = object: Runnable {
        override fun run() {
            readData()
            handler.postDelayed(this, 500)
        }
    }

    private lateinit var tvTempCurrent: TextView
    private lateinit var tvTempTarget: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTempCurrent = findViewById(R.id.tv_temp_current)
        tvTempTarget = findViewById(R.id.tv_temp_target)
    }

    override fun onStart() {
        super.onStart()

        initSensor()
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

}

