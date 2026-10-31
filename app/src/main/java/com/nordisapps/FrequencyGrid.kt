package com.nordisapps

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object FrequencyGrid {
    fun start(minFreq: Double, step: Double): Double {
        val firstWhole = ceil(minFreq - 1e-9)
        val below = floor((firstWhole - minFreq) / step + 1e-9)
        return firstWhole - below * step
    }

    fun snap(freq: Double, minFreq: Double, maxFreq: Double, step: Double): Double {
        val s = start(minFreq, step)
        val maxN = floor((maxFreq - s) / step + 1e-9).toInt()
        val n = ((freq - s) / step).roundToInt().coerceIn(0, maxN)
        return ((s + n * step) * 1000).roundToInt() / 1000.0
    }
}