package com.pixplicity.thermostat

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import com.google.android.things.device.DeviceManager
import com.google.android.things.device.ScreenManager
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

class MainActivity : Activity() {
    companion object {
        private const val TEMP_MIN = 10f
        private const val TEMP_MAX = 28f
        private const val SENSOR_READ_PER_SECOND = 10
        private const val SENSOR_READ_DELAY = 1000L / SENSOR_READ_PER_SECOND
        private const val AVERAGE_OVER_SECONDS = 3
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
                iv_flame.visibility = View.VISIBLE
            } else {
                ledRed?.value = false
                ledGreen?.value = true
                iv_flame.visibility = View.GONE
            }
            handler.postDelayed(this, SENSOR_READ_DELAY)
        }
    }
    private val fader1 = Runnable {
        setBrightness(0f)
    }
    private val fader2 = Runnable {
        v_dim.animate().alpha(1f).start()
    }

    private var mBmp180: Bmp180? = null
    private val mI2cPort = BoardDefaults.getI2CPort()

    private var ledRed: Gpio? = null
    private var ledGreen: Gpio? = null

    private var tempTarget = 20f
    private var tempCurrent = 20f
    private var tempRecent = Rolling(SENSOR_READ_PER_SECOND * AVERAGE_OVER_SECONDS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ScreenManager(Display.DEFAULT_DISPLAY).setDisplayDensity(180)

        setContentView(R.layout.activity_main)

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
        vg_root.interceptTouchListener = View.OnTouchListener { v, event ->
            wakeUp()
            false
        }
        bt_restart.setOnClickListener {
            val menu = PopupMenu(this, bt_restart)
            menu.inflate(R.menu.power)
            menu.setOnMenuItemClickListener { listener ->
                when (listener.itemId) {
                    R.id.action_reset -> System.exit(0)
                    R.id.action_reboot -> {
                        DeviceManager().reboot()
                    }
                }
                true
            }
            menu.show()
        }
    }

    private fun wakeUp() {
        setBrightness(1f)
        v_dim.animate().alpha(0f).start()
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
        mBmp180?.let {
            try {
                tempCurrent = it.readTemperature()
                tempRecent.add(tempCurrent.toDouble())
                val average = tempRecent.average
                tv_temp_current.text = getString(R.string.state_temp, average)
                vg_temp_current.visibility = View.VISIBLE
                vg_error.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Sensor loop error", e)
                tv_error.text = getString(R.string.state_error)
                tv_error_detail.text = e.message
                vg_temp_current.visibility = View.GONE
                vg_error.visibility = View.VISIBLE
            }
        }
    }

    private fun closeSensor() {
        try {
            mBmp180?.close()
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
        tv_temp_target.text = getString(R.string.state_temp, tempTarget)
    }

}
