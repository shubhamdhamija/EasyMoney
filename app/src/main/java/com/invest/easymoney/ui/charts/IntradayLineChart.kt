package com.invest.easymoney.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive intraday line chart with gradient fill/stroke and touch tooltip.
 * - prices: list of Float
 * - times: optional list of epoch seconds
 */
@Composable
fun IntradayLineChart(
    prices: List<Float>,
    times: List<Long>? = null,
    modifier: Modifier = Modifier
) {
    if (prices.isEmpty()) {
        Box(
            modifier = modifier
                .height(200.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "No intraday data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val values = prices.map { it.toDouble() }
    val minY = values.minOrNull() ?: 0.0
    val maxY = values.maxOrNull() ?: minY + 1.0
    val range = if ((maxY - minY) == 0.0) 1.0 else (maxY - minY)
    val lastValue = values.last()

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Interaction state
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPx by remember { mutableStateOf(0f) }
    var tooltipPy by remember { mutableStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Pointer input needs access to canvas size, so attach to this Box/Canvas
        Canvas(modifier = Modifier
            .height(260.dp)
            .fillMaxWidth()
            .onSizeChanged { canvasSize = it }
            .pointerInput(values, canvasSize) {
                if (canvasSize.width == 0) return@pointerInput
                coroutineScope {
                    detectDragGestures(onDragStart = { offset ->
                        // map to index
                        val paddingLeft = 12f
                        val paddingRight = 12f
                        val chartW = canvasSize.width.toFloat() - paddingLeft - paddingRight
                        val count = values.size
                        val xStep = if (count > 1) chartW / (count - 1) else chartW
                        val relativeX = (offset.x - paddingLeft).coerceIn(0f, chartW)
                        val idx = (relativeX / xStep).roundToInt().coerceIn(0, count - 1)
                        tooltipIndex = idx
                        tooltipPx = offset.x
                        tooltipPy = offset.y
                    }, onDrag = { change, _ ->
                        val offset = change.position
                        val paddingLeft = 12f
                        val paddingRight = 12f
                        val chartW = canvasSize.width.toFloat() - paddingLeft - paddingRight
                        val count = values.size
                        val xStep = if (count > 1) chartW / (count - 1) else chartW
                        val relativeX = (offset.x - paddingLeft).coerceIn(0f, chartW)
                        val idx = (relativeX / xStep).roundToInt().coerceIn(0, count - 1)
                        tooltipIndex = idx
                        tooltipPx = offset.x
                        tooltipPy = offset.y
                    }, onDragEnd = {
                        // keep tooltip visible short time? For now keep until next interaction
                    }, onDragCancel = {
                        // clear on cancel
                        tooltipIndex = null
                    })
                }
            }
        ) {
            val w = size.width
            val h = size.height
            val paddingLeft = 12f
            val paddingRight = 12f
            val paddingTop = 16f
            val paddingBottom = 32f
            val chartW = w - paddingLeft - paddingRight
            val chartH = h - paddingTop - paddingBottom

            val count = values.size
            val xStep = if (count > 1) chartW / (count - 1) else chartW

            // grid
            val gridColor = Color(0x15000000)
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = paddingTop + chartH * i / gridLines
                drawLine(color = gridColor, start = Offset(paddingLeft, y), end = Offset(w - paddingRight, y), strokeWidth = 1f)
            }

            // path
            val linePath = Path()
            for (i in values.indices) {
                val v = values[i]
                val x = paddingLeft + i * xStep
                val y = paddingTop + (chartH - (((v - minY) / range) * chartH)).toFloat()
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }

            // gradient fill
            val fillPath = Path().apply { addPath(linePath) }
            fillPath.lineTo(w - paddingRight, paddingTop + chartH)
            fillPath.lineTo(paddingLeft, paddingTop + chartH)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x803297FF), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + chartH
                )
            )

            // gradient stroke for the line
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(Color(0xFF00C6A7), Color(0xFF2979FF))),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // last marker
            val lastX = paddingLeft + (values.size - 1) * xStep
            val lastY = paddingTop + (chartH - (((lastValue - minY) / range) * chartH)).toFloat()
            drawCircle(color = Color(0xFF2979FF), radius = 6f, center = Offset(lastX, lastY))

            // if tooltipIndex present, draw a vertical indicator line
            tooltipIndex?.let { idx ->
                val px = paddingLeft + idx * xStep
                drawLine(color = Color(0xAA444444), start = Offset(px, paddingTop), end = Offset(px, paddingTop + chartH), strokeWidth = 1f)
            }
        }

        // Overlay top-left current price and top-right max.
        Text(
            text = String.format(Locale.getDefault(), "%.2f", lastValue),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 6.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = String.format(Locale.getDefault(), "%.2f", maxY),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 6.dp),
            fontSize = 12.sp,
            color = Color.DarkGray
        )

        Text(
            text = String.format(Locale.getDefault(), "%.2f", minY),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 36.dp),
            fontSize = 12.sp,
            color = Color.DarkGray
        )

        // Tooltip composable overlay
        tooltipIndex?.let { idx ->
            val price = values[idx]
            val timeLabel = times?.getOrNull(idx)?.let { timeFmt.format(Date(it * 1000)) } ?: ""
            // Determine tooltip position in Dp using canvasSize & tooltipIndex
            val paddingLeftPx = 12f
            val paddingRightPx = 12f
            val chartWpx = (canvasSize.width - paddingLeftPx - paddingRightPx).coerceAtLeast(1f)
            val xStep = if (values.size > 1) chartWpx / (values.size - 1) else chartWpx
            val px = paddingLeftPx + idx * xStep
            // position tooltip above chart
            val tooltipOffsetDpX = with(LocalDensity.current) { (px - 60f).toDp() }
            val tooltipOffsetDpY = 40.dp

            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .offset(x = tooltipOffsetDpX, y = tooltipOffsetDpY)
                    .width(120.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = String.format(Locale.getDefault(), "%.2f", price), fontWeight = FontWeight.Bold)
                    if (timeLabel.isNotEmpty()) Text(text = timeLabel, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }

        // Time labels row at bottom
        Row(modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (times != null && times.size == prices.size && times.isNotEmpty()) {
                Text(text = timeFmt.format(Date(times.first() * 1000)), color = Color.DarkGray, fontSize = 12.sp)
                Text(text = timeFmt.format(Date(times[times.size / 2] * 1000)), color = Color.DarkGray, fontSize = 12.sp)
                Text(text = timeFmt.format(Date(times.last() * 1000)), color = Color.DarkGray, fontSize = 12.sp)
            } else {
                Text(text = "Start", color = Color.DarkGray, fontSize = 12.sp)
                Text(text = "Mid", color = Color.DarkGray, fontSize = 12.sp)
                Text(text = "End", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}
