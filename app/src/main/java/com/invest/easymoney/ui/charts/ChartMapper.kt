package com.invest.easymoney.ui.charts

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Small mapper utilities for charting.
 */
object ChartMapper {
    private val utcFmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun toLabelList(times: List<Long>? = null, size: Int = 0): List<String> {
        if (times == null || times.size != size || times.isEmpty()) return List(size) { it.toString() }
        return times.map { ts ->
            utcFmt.format(Date(ts * 1000))
        }
    }
}
