package com.invest.easymoney.ui.charts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class MarketStatus(val label: String) {
    OPEN("Open"),
    CLOSED("Closed"),
    PRE_MARKET("Pre-Market"),
    AFTER_HOURS("After Hours")
}

private enum class ChartRange(val label: String) {
    OneDay("1D"),
    OneWeek("1W"),
    OneMonth("1M"),
    ThreeMonths("3M"),
    OneYear("1Y"),
    All("ALL")
}

private data class ChartSlice(
    val prices: List<Float>,
    val times: List<Long>?,
    val volumes: List<Long>?
)

@Composable
fun MPLineChart(
    prices: List<Float>,
    times: List<Long>? = null,
    modifier: Modifier = Modifier,

    // Optional richer UI data
    symbol: String = "STOCK",
    companyName: String? = null,
    volumes: List<Long>? = null,
    marketStatus: MarketStatus = MarketStatus.OPEN,
    watchlisted: Boolean = false,

    // Portfolio info (optional)
    holdingQuantity: Float = 0f,
    averageCost: Float = 0f,

    // Actions
    onToggleWatchlist: (Boolean) -> Unit = {},
    onSetAlert: () -> Unit = {},
    onBuy: () -> Unit = {},
    onSell: () -> Unit = {}
) {
    if (prices.isEmpty()) {
        EmptyTradingChart(modifier)
        return
    }

    var selectedRange by remember { mutableStateOf(ChartRange.All) }

    val slice = remember(prices, times, volumes, selectedRange) {
        buildSlice(
            prices = prices,
            times = times,
            volumes = volumes,
            range = selectedRange
        )
    }

    if (slice.prices.isEmpty()) {
        EmptyTradingChart(modifier)
        return
    }

    val currentPrices = slice.prices
    val currentTimes = slice.times
    val currentVolumes = slice.volumes

    val isPositive = currentPrices.last() >= currentPrices.first()
    val trendColorTarget = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
    val trendColor by animateColorAsState(trendColorTarget, label = "trend-color")

    val open = currentPrices.first()
    val current = currentPrices.last()
    val high = currentPrices.maxOrNull() ?: current
    val low = currentPrices.minOrNull() ?: current
    val change = current - open
    val changePercent = if (open != 0f) (change / open) * 100f else 0f
    val rangeAmount = high - low
    val volatility = if (low != 0f) (rangeAmount / low) * 100f else 0f

    val hasHolding = holdingQuantity > 0f
    val currentHoldingValue = current * holdingQuantity
    val investedValue = averageCost * holdingQuantity
    val pnl = currentHoldingValue - investedValue
    val pnlPercent = if (investedValue != 0f) (pnl / investedValue) * 100f else 0f
    val pnlColor = if (pnl >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626)

    val marketStatusColor = when (marketStatus) {
        MarketStatus.OPEN -> Color(0xFF16A34A)
        MarketStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
        MarketStatus.PRE_MARKET -> Color(0xFF2563EB)
        MarketStatus.AFTER_HOURS -> Color(0xFF7C3AED)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    companyName?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatPrice(current),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildChangeText(change, changePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(
                        label = marketStatus.label,
                        color = marketStatusColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilterChip(
                        selected = watchlisted,
                        onClick = { onToggleWatchlist(!watchlisted) },
                        label = {
                            Text(if (watchlisted) "Watchlisted" else "Watchlist")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Range selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartRange.entries.forEach { range ->
                    val selected = selectedRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRange = range },
                        label = {
                            Text(
                                text = range.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = trendColor.copy(alpha = 0.14f),
                            selectedLabelColor = trendColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main chart + optional volume bars
            TradingChartPanel(
                prices = currentPrices,
                times = currentTimes,
                volumes = currentVolumes,
                trendColor = trendColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightStat("Open", formatPrice(open))
                InsightStat("High", formatPrice(high), valueColor = Color(0xFF16A34A))
                InsightStat("Low", formatPrice(low), valueColor = Color(0xFFDC2626))
                InsightStat("Volatility", "${formatPercent(volatility)}%")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Portfolio mini card
            if (hasHolding) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Your Position",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PortfolioItem("Qty", trimTrailingZeros(holdingQuantity))
                            PortfolioItem("Avg Cost", formatPrice(averageCost))
                            PortfolioItem("Value", formatPrice(currentHoldingValue))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = pnlColor.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unrealized P&L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatPrice(pnl)} (${formatPercent(pnlPercent)}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pnlColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSetAlert,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Set Alert")
                }

                Button(
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Sell")
                }

                Button(
                    onClick = onBuy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    )
                ) {
                    Text("Buy")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetaText("Trend", if (isPositive) "Bullish" else "Bearish", trendColor)
                    MetaText("Range", formatPrice(rangeAmount), MaterialTheme.colorScheme.primary)
                    MetaText("Points", currentPrices.size.toString(), MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TradingChartPanel(
    prices: List<Float>,
    times: List<Long>?,
    volumes: List<Long>?,
    trendColor: Color,
    modifier: Modifier = Modifier
) {
    val minPrice = prices.minOrNull() ?: 0f
    val maxPrice = prices.maxOrNull() ?: 0f
    val priceRange = (maxPrice - minPrice).takeIf { it > 0f } ?: 1f

    val showVolumes = volumes != null && volumes.size == prices.size && volumes.isNotEmpty()
    val maxVolume = volumes?.maxOrNull()?.takeIf { it > 0L } ?: 1L

    var lineChartSize by remember { mutableStateOf(IntSize.Zero) }
    var volumeChartSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableIntStateOf(prices.lastIndex) }

    val chartSurface = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val lineChartHeight = 280.dp
    val volumeChartHeight = if (showVolumes) 64.dp else 0.dp

    val selectedPrice = prices[selectedIndex]
    val selectedTime = formatTimeLabel(selectedIndex, prices.size, times)
    val selectedDelta = selectedPrice - prices.first()
    val selectedDeltaPercent =
        if (prices.first() != 0f) (selectedDelta / prices.first()) * 100f else 0f

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = chartSurface
    ) {
        Column(
            modifier = modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineChartHeight)
                    .onSizeChanged { lineChartSize = it }
                    .pointerInput(prices) {
                        detectTapGestures { offset ->
                            selectedIndex = nearestIndex(
                                x = offset.x,
                                width = lineChartSize.width.toFloat(),
                                count = prices.size,
                                xPaddingPx = 16f
                            )
                        }
                    }
                    .pointerInput(prices) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = nearestIndex(
                                    x = offset.x,
                                    width = lineChartSize.width.toFloat(),
                                    count = prices.size,
                                    xPaddingPx = 16f
                                )
                            },
                            onDrag = { change, _ ->
                                selectedIndex = nearestIndex(
                                    x = change.position.x,
                                    width = lineChartSize.width.toFloat(),
                                    count = prices.size,
                                    xPaddingPx = 16f
                                )
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lineChartHeight)
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 16f
                    val rightPadding = 16f
                    val topPadding = 30f
                    val bottomPadding = 22f
                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    // Grid
                    repeat(5) { i ->
                        val y = topPadding + chartHeight * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1f
                        )
                    }

                    val points = prices.mapIndexed { index, _ ->
                        chartPoint(
                            index = index,
                            prices = prices,
                            width = width,
                            height = height,
                            minPrice = minPrice,
                            priceRange = priceRange,
                            leftPadding = leftPadding,
                            rightPadding = rightPadding,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding
                        )
                    }

                    val linePath = smoothPath(points)
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, height - bottomPadding)
                        lineTo(points.first().x, height - bottomPadding)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                trendColor.copy(alpha = 0.26f),
                                trendColor.copy(alpha = 0.02f)
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        ),
                        style = Fill
                    )

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(trendColor.copy(alpha = 0.90f), trendColor)
                        ),
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round
                        )
                    )

                    val selectedPoint = points[selectedIndex]

                    // Crosshair
                    drawLine(
                        color = trendColor.copy(alpha = 0.40f),
                        start = Offset(selectedPoint.x, topPadding),
                        end = Offset(selectedPoint.x, height - bottomPadding),
                        strokeWidth = 1.5f
                    )

                    // Selected point glow
                    drawCircle(
                        color = trendColor.copy(alpha = 0.18f),
                        radius = 18f,
                        center = selectedPoint
                    )

                    drawCircle(
                        color = trendColor,
                        radius = 7f,
                        center = selectedPoint
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = selectedPoint
                    )

                    // Last point marker
                    val lastPoint = points.last()
                    drawCircle(
                        color = trendColor,
                        radius = 5f,
                        center = lastPoint
                    )
                }

                // Current selected price
                Text(
                    text = formatPrice(selectedPrice),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = buildChangeText(selectedDelta, selectedDeltaPercent),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedDelta >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )

                // Floating tooltip
                if (lineChartSize.width > 0) {
                    val selectedPoint = chartPoint(
                        index = selectedIndex,
                        prices = prices,
                        width = lineChartSize.width.toFloat(),
                        height = lineChartSize.height.toFloat(),
                        minPrice = minPrice,
                        priceRange = priceRange,
                        leftPadding = 16f,
                        rightPadding = 16f,
                        topPadding = 30f,
                        bottomPadding = 22f
                    )

                    val tooltipWidthPx = 148f
                    val tooltipX = (selectedPoint.x - tooltipWidthPx / 2f)
                        .coerceIn(8f, lineChartSize.width - tooltipWidthPx - 8f)

                    val tooltipY = if (selectedPoint.y < 90f) {
                        selectedPoint.y + 18f
                    } else {
                        selectedPoint.y - 80f
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.offset {
                            IntOffset(tooltipX.roundToInt(), tooltipY.roundToInt())
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = selectedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = formatPrice(selectedPrice),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = buildChangeText(selectedDelta, selectedDeltaPercent),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedDelta >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Edge time labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatBottomEdgeLabel(times?.firstOrNull(), "Start"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = formatBottomEdgeLabel(times?.getOrNull((times?.size ?: 1) / 2), "Middle"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = formatBottomEdgeLabel(times?.lastOrNull(), "End"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }

            if (showVolumes) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(volumeChartHeight)
                        .onSizeChanged { volumeChartSize = it }
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 16f
                    val rightPadding = 16f
                    val chartWidth = width - leftPadding - rightPadding
                    val xStep = if (prices.size <= 1) 0f else chartWidth / prices.lastIndex.toFloat()
                    val barWidth = (xStep * 0.55f).coerceAtLeast(3f)

                    volumes!!.forEachIndexed { index, volume ->
                        val normalized = volume / maxVolume.toFloat()
                        val barHeight = normalized * (height - 6f)

                        val x = leftPadding + (index * xStep) - barWidth / 2f
                        val y = height - barHeight

                        val barColor = if (index == selectedIndex) {
                            trendColor
                        } else {
                            trendColor.copy(alpha = 0.30f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun InsightStat(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun PortfolioItem(
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetaText(
    title: String,
    value: String,
    valueColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$title: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun EmptyTradingChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chart data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildSlice(
    prices: List<Float>,
    times: List<Long>?,
    volumes: List<Long>?,
    range: ChartRange
): ChartSlice {
    if (prices.isEmpty()) return ChartSlice(emptyList(), null, null)

    val validTimes = times != null && times.size == prices.size && times.isNotEmpty()
    val validVolumes = volumes != null && volumes.size == prices.size && volumes.isNotEmpty()

    if (range == ChartRange.All) {
        return ChartSlice(
            prices = prices,
            times = if (validTimes) times else null,
            volumes = if (validVolumes) volumes else null
        )
    }

    if (validTimes) {
        val ts = times!!
        val end = ts.last()
        val durationSeconds = when (range) {
            ChartRange.OneDay -> 1L * 24 * 60 * 60
            ChartRange.OneWeek -> 7L * 24 * 60 * 60
            ChartRange.OneMonth -> 30L * 24 * 60 * 60
            ChartRange.ThreeMonths -> 90L * 24 * 60 * 60
            ChartRange.OneYear -> 365L * 24 * 60 * 60
            ChartRange.All -> Long.MAX_VALUE
        }

        val threshold = end - durationSeconds
        val fromIndex = ts.indexOfFirst { it >= threshold }.let { if (it == -1) 0 else it }

        return ChartSlice(
            prices = prices.drop(fromIndex),
            times = ts.drop(fromIndex),
            volumes = if (validVolumes) volumes!!.drop(fromIndex) else null
        )
    }

    val keepCount = when (range) {
        ChartRange.OneDay -> max(12, prices.size / 8)
        ChartRange.OneWeek -> max(18, prices.size / 6)
        ChartRange.OneMonth -> max(24, prices.size / 4)
        ChartRange.ThreeMonths -> max(32, prices.size / 2)
        ChartRange.OneYear -> max(40, (prices.size * 3) / 4)
        ChartRange.All -> prices.size
    }.coerceAtMost(prices.size)

    return ChartSlice(
        prices = prices.takeLast(keepCount),
        times = if (validTimes) times!!.takeLast(keepCount) else null,
        volumes = if (validVolumes) volumes!!.takeLast(keepCount) else null
    )
}

private fun chartPoint(
    index: Int,
    prices: List<Float>,
    width: Float,
    height: Float,
    minPrice: Float,
    priceRange: Float,
    leftPadding: Float,
    rightPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): Offset {
    val chartWidth = width - leftPadding - rightPadding
    val chartHeight = height - topPadding - bottomPadding
    val xStep = if (prices.size <= 1) 0f else chartWidth / prices.lastIndex.toFloat()

    val x = leftPadding + (index * xStep)
    val normalized = (prices[index] - minPrice) / priceRange
    val y = topPadding + chartHeight - (normalized * chartHeight)

    return Offset(x, y)
}

private fun nearestIndex(
    x: Float,
    width: Float,
    count: Int,
    xPaddingPx: Float
): Int {
    if (count <= 1 || width <= 0f) return 0
    val chartWidth = width - (xPaddingPx * 2f)
    val clamped = (x - xPaddingPx).coerceIn(0f, chartWidth)
    val ratio = clamped / chartWidth
    return (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    path.moveTo(points.first().x, points.first().y)

    for (i in 1 until points.size) {
        val previous = points[i - 1]
        val current = points[i]
        val midX = (previous.x + current.x) / 2f
        val midY = (previous.y + current.y) / 2f
        path.quadraticTo(previous.x, previous.y, midX, midY)
    }

    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

private fun formatTimeLabel(
    index: Int,
    size: Int,
    times: List<Long>?
): String {
    if (times == null || times.size != size || index !in times.indices) {
        return "Point ${index + 1}"
    }

    val formatter = chooseTimeFormatter(times)
    return formatter.format(Date(times[index] * 1000))
}

private fun formatBottomEdgeLabel(
    time: Long?,
    fallback: String
): String {
    if (time == null) return fallback
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(time * 1000))
}

private fun chooseTimeFormatter(times: List<Long>): SimpleDateFormat {
    val spanSeconds = abs((times.lastOrNull() ?: 0L) - (times.firstOrNull() ?: 0L))
    return if (spanSeconds < 24 * 60 * 60) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    } else {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }
}

private fun formatPrice(value: Float): String {
    val sign = if (value < 0) "-" else ""
    return "$sign$" + String.format(Locale.US, "%.2f", abs(value))
}

private fun formatPercent(value: Float): String {
    val sign = if (value > 0) "+" else ""
    return sign + String.format(Locale.US, "%.2f", value)
}

private fun buildChangeText(change: Float, percent: Float): String {
    val sign = if (change >= 0f) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", change)} (${sign}${String.format(Locale.US, "%.2f", percent)}%)"
}

private fun trimTrailingZeros(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}