package com.pixplicity.thermostat

import android.animation.Animator
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import java.io.IOException

private val TAG = MainActivity::class.java.simpleName

class MainActivity : Activity() {
    companion object {
        private const val TEMP_MIN = 10f
        private const val TEMP_MAX = 28f
    }

    private val TAG = MainActivity::class.java.simpleName

    private val handler = Handler()

    private var furnaceOn = false

    private val reader = object : Runnable {
        override fun run() {
            readData()
            if (tempCurrent < tempTarget) {
                if (!furnaceOn) {
                    // Turn on furnace once
                    furnaceOn = true
                }
            } else {
                if (furnaceOn) {
                    // Turn off furnace once
                    furnaceOn = false
                }
            }
            if (furnaceOn) {
                ledRed?.value = !(ledRed?.value ?: false)
                ledGreen?.value = false
                ivFlame.visibility = View.VISIBLE
            } else {
                ledRed?.value = false
                ledGreen?.value = true
                ivFlame.visibility = View.GONE
            }
            handler.postDelayed(this, 100)
        }
    }
    private val fader1 = Runnable {
        setBrightness(0f)
    }
    private val fader2 = Runnable {
        vDim.animate().alpha(1f).start()
    }

    private var mBmp180: Bmp180? = null
    private val mI2cPort = BoardDefaults.getI2CPort()

    private var ledRed: Gpio? = null
    private var ledGreen: Gpio? = null

    private var tempTarget = 20f
    private var tempCurrent = 20f

    private lateinit var tvTempCurrent: TextView
    private lateinit var tvTempTarget: TextView
    private lateinit var ivFlame: ImageView
    private lateinit var vDim: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTempCurrent = findViewById(R.id.tv_temp_current)
        tvTempTarget = findViewById(R.id.tv_temp_target)
        ivFlame = findViewById(R.id.iv_flame)
        vDim = findViewById(R.id.v_dim)

        findViewById<ImageButton>(R.id.bt_decrease).setOnClickListener {
            wakeUp()
            tempTarget = Math.max(TEMP_MIN, Math.min(tempTarget - 0.5f, TEMP_MAX))
            updateTarget()
        }
        findViewById<ImageButton>(R.id.bt_increase).setOnClickListener {
            wakeUp()
            tempTarget = Math.max(TEMP_MIN, Math.min(tempTarget + 0.5f, TEMP_MAX))
            updateTarget()
        }
        vDim.setOnClickListener {
            wakeUp()
        }
    }

    private fun wakeUp() {
        setBrightness(1f)
        vDim.animate().alpha(0f).start()
        handler.removeCallbacks(fader1)
        handler.removeCallbacks(fader2)
        handler.postDelayed(fader1, 5000)
        handler.postDelayed(fader2, 10000)
    }

    private fun setBrightness(brightness: Float) {
        val attr = window.attributes
        attr.screenBrightness = brightness
        window.attributes = attr
    }

    override fun onStart() {
        super.onStart()

        initSensor()
        openLeds()

        updateTarget()
        handler.post(reader)

        wakeUp()
    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacks(reader)
        closeSensor()
        closeLeds()
    }

    private fun initSensor() {
        mBmp180 = Bmp180(mI2cPort)
    }

    private fun readData() {
        try {
            tempCurrent = mBmp180!!.readTemperature()
            val press = mBmp180!!.readPressure().toFloat()
            val alt = mBmp180!!.readAltitude().toDouble()
//            Log.d(TAG, "loop: temp $tempCurrent alt: $alt press: $press")
            tvTempCurrent.text = String.format("%.1f", tempCurrent)
        } catch (e: IOException) {
            Log.e(TAG, "Sensor loop  error : ", e)
            tvTempCurrent.text = "ERROR"
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

    private fun openLeds() {
        val service = PeripheralManagerService()
        try {
            ledRed = service.openGpio("GPIO2_IO03")
        } catch (e: IOException) {
            Log.e(TAG, "gpio failure", e)
        }
        ledRed?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        try {
            ledGreen = service.openGpio("GPIO1_IO10")
        } catch (e: IOException) {
            Log.e(TAG, "gpio failure", e)
        }
        ledGreen?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
    }

    private fun closeLeds() {
        ledRed?.close()
        ledGreen?.close()
    }

    private fun updateTarget() {
        tvTempTarget.text = String.format("%.1f", tempTarget)
    }

}
