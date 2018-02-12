package com.pixplicity.thermostat

import android.util.Log
import java.util.*

class Rolling(private val size: Int) {
    private var total = 0.0
    private var index = 0
    private val samples: DoubleArray
    private var initialSize = 0

    val average: Double
        get() = total / size()

    init {
        samples = DoubleArray(size)
        for (i in 0 until size) {
            samples[i] = 0.0
        }
    }

    fun add(x: Double) {
        initialSize++
        total -= samples[index]
        samples[index] = x
        total += x
        if (++index == size) {
            index = 0 // cheaper than modulus
        }
    }

    fun size(): Int {
        return Math.min(initialSize, size)
    }
}