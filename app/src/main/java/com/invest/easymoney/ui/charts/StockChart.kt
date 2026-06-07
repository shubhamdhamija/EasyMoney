/*
package com.invest.easymoney.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ✅ Vico 3.x imports (IMPORTANT)
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.entry.entryModelOf
import com.patrykandpatrick.vico.compose.m3.marker.rememberMarker

*/
/**
 * ✅ Premium Stock Chart (Vico 3.x)
 *//*

@Composable
fun StockChart(
    prices: List<Float>,
    modifier: Modifier = Modifier
) {

    if (prices.isEmpty()) {
        EmptyChart(modifier)
        return
    }

    // ✅ Bull / Bear detection
    val isPositive = prices.last() >= prices.first()

    val lineColor = if (isPositive) {
        Color(0xFF00C853) // Green
    } else {
        Color(0xFFD50000) // Red
    }

    val model = entryModelOf(*prices.toTypedArray())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Price Chart",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Chart(
                chart = lineChart(
                    lines = listOf(
                        lineChart().lines.first().copy(
                            lineColor = lineColor
                        )
                    )
                ),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                marker = rememberMarker(), // ✅ works in compose-m3
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Open: ${formatPrice(prices.first())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Current: ${formatPrice(prices.last())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = lineColor
                )
            }
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("No chart data")
    }
}

private fun formatPrice(value: Float): String {
    return "₹%.2f".format(value)
}*/
