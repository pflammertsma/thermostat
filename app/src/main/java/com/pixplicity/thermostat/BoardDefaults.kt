package com.pixplicity.thermostat

import android.os.Build

object BoardDefaults {
    private val DEVICE_RPI3 = "rpi3"
    private val DEVICE_IMX6UL_PICO = "imx6ul_pico"
    private val DEVICE_IMX7D_PICO = "imx7d_pico"

    /**
     * Return the preferred I2C port for each board.
     */
    fun getI2CPort(): String {
        when (Build.DEVICE) {
            DEVICE_RPI3 -> return "I2C1"
            DEVICE_IMX6UL_PICO -> return "I2C2"
            DEVICE_IMX7D_PICO -> return "I2C1"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }
    }
}
