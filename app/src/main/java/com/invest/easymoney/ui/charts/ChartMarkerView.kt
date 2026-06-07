/*
package com.invest.easymoney.ui.charts

import android.content.Context
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.Utils
import com.invest.easymoney.R

class ChartMarkerView(context: Context) : com.github.mikephil.charting.components.MarkerView(context, R.layout.marker_view) {
    private val priceText: TextView = findViewById(R.id.marker_price)
    private val timeText: TextView = findViewById(R.id.marker_time)
    private val volumeText: TextView = findViewById(R.id.marker_volume)
    private val sparkline: LineChart = findViewById(R.id.marker_sparkline)

    private var pricesSeries: List<Float> = emptyList()
    private var volumesSeries: List<Long>? = null

    init {
        // initialize the small chart
        sparkline.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        sparkline.setTouchEnabled(false)
        sparkline.setDrawGridBackground(false)
        sparkline.axisLeft.isEnabled = false
        sparkline.axisRight.isEnabled = false
        sparkline.xAxis.isEnabled = false
        val d = Description()
        d.text = ""
        sparkline.description = d
        sparkline.legend.isEnabled = false
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            priceText.text = String.format("%.2f", e.y)
            // x is index; time label should be set from outside by caller
            updateSparklineHighlight(e.x.toInt())
        }
        super.refreshContent(e, highlight)
    }

    fun setTimeLabel(label: String) {
        timeText.text = label
    }

    fun setSeries(prices: List<Float>, volumes: List<Long>? = null) {
        this.pricesSeries = prices
        this.volumesSeries = volumes
        // draw sparkline data
        updateSparkline(0)
    }

    private fun updateSparkline(highlightIndex: Int) {
        if (pricesSeries.isEmpty()) {
            sparkline.clear()
            sparkline.invalidate()
            volumeText.text = "Vol: N/A"
            return
        }
        val windowSize = 12 // show last 12 points around highlight when possible
        val size = pricesSeries.size
        val center = highlightIndex.coerceIn(0, size - 1)
        val from = (center - windowSize / 2).coerceAtLeast(0)
        val to = (from + windowSize).coerceAtMost(size)
        val sub = pricesSeries.subList(from, to)
        val entries = sub.mapIndexed { i, p -> Entry(i.toFloat(), p) }
        val set = LineDataSet(entries, "s").apply {
            color = ColorTemplate.getHoloBlue()
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 1.2f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            fillAlpha = 60
            fillColor = ColorTemplate.getHoloBlue()
        }
        sparkline.data = LineData(set)
        sparkline.invalidate()

        // update volume label for the highlighted index if available
        volumesSeries?.let { vols ->
            val volIdx = (from + (center - from)).coerceIn(0, vols.size - 1)
            volumeText.text = "Vol: ${formatVolume(vols[volIdx])}"
        } ?: run { volumeText.text = "Vol: N/A" }
    }

    private fun updateSparklineHighlight(index: Int) {
        // refresh sparkline window around this index
        updateSparkline(index)
    }

    private fun formatVolume(v: Long): String {
        return when {
            v >= 1_000_000_000 -> String.format("%.1fB", v / 1_000_000_000.0)
            v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000.0)
            v >= 1_000 -> String.format("%.1fk", v / 1_000.0)
            else -> v.toString()
        }
    }
}
*/
