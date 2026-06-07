package com.invest.easymoney.ui.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.EarningsQuarter
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.util.Resource
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val state by viewModel.stocksState.collectAsStateWithLifecycle()
    val earningsMap by viewModel.earningsMap.collectAsStateWithLifecycle()
    val upcomingEarnings by viewModel.upcomingEarnings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Watchlist", "Earnings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state !is Resource.Loading) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> WatchlistTab(
                    state = state,
                    onStockClick = onStockClick,
                    onRemove = { viewModel.removeFromWatchlist(it) }
                )
                1 -> EarningsTab(
                    stocksState = state,
                    earningsMap = earningsMap,
                    upcomingEarnings = upcomingEarnings
                )
            }
        }
    }
}

// ── Watchlist Tab ─────────────────────────────────────────────────────────────

@Composable
private fun WatchlistTab(
    state: Resource<List<Stock>>,
    onStockClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    when (state) {
        is Resource.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is Resource.Error -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠️ ${(state as Resource.Error).message}")
        }

        is Resource.Success -> {
            val stocks = (state as Resource.Success<List<Stock>>).data
            if (stocks.isEmpty()) {
                EmptyWatchlistPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stocks, key = { it.symbol }) { stock ->
                        WatchlistItem(
                            stock = stock,
                            onClick = { onStockClick(stock.symbol) },
                            onRemove = { onRemove(stock.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchlistPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⭐", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Your watchlist is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Tap ★ on any stock detail\nto add it here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistItem(stock: Stock, onClick: () -> Unit, onRemove: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (stock.name.isNotEmpty()) {
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (stock.currentPrice > 0) "$${String.format("%.2f", stock.currentPrice)}" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Earnings Tab ──────────────────────────────────────────────────────────────

@Composable
private fun EarningsTab(
    stocksState: Resource<List<Stock>>,
    earningsMap: Map<String, Resource<EarningsReport>>,
    upcomingEarnings: Resource<List<UpcomingEarning>>
) {
    val stocks = (stocksState as? Resource.Success)?.data ?: emptyList()
    val stockNames = stocks.associate { it.symbol to it.name }

    if (stocks.isEmpty()) {
        EmptyWatchlistPlaceholder()
        return
    }

    val upcoming = (upcomingEarnings as? Resource.Success)?.data ?: emptyList()
    val upcomingMap = upcoming.associate { it.symbol.uppercase() to it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (upcoming.isNotEmpty()) {
            item { UpcomingEarningsBanner(upcoming) }
        }

        items(stocks, key = { "earnings_${it.symbol}" }) { stock ->
            val earningsState = earningsMap[stock.symbol]
            StockEarningsCard(
                symbol = stock.symbol,
                companyName = stockNames[stock.symbol] ?: "",
                earningsState = earningsState,
                upcomingEarning = upcomingMap[stock.symbol.uppercase()]
            )
        }
    }
}

@Composable
private fun UpcomingEarningsBanner(upcoming: List<UpcomingEarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "📅 Upcoming Earnings (Next 7 Days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            upcoming.forEach { earning ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = earning.symbol,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        Text(
                            text = formatDate(earning.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        earning.time?.let { time ->
                            Text(
                                text = when (time) {
                                    "bmo" -> "Pre-market"
                                    "amc" -> "After-close"
                                    else -> time
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    earning.epsEstimate?.let { est ->
                        Text(
                            text = "Est. $${String.format("%.2f", est)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockEarningsCard(
    symbol: String,
    companyName: String,
    earningsState: Resource<EarningsReport>?,
    upcomingEarning: UpcomingEarning?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Card header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = symbol,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (companyName.isNotEmpty()) {
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    "📊 EPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Earnings content
            when (earningsState) {
                null, is Resource.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Loading earnings data…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is Resource.Error -> {
                    Text(
                        "⚠️ ${(earningsState as Resource.Error).message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is Resource.Success -> {
                    val report = (earningsState as Resource.Success<EarningsReport>).data
                    if (report.quarters.isEmpty()) {
                        Text(
                            "No earnings data available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        EarningsQuartersTable(report.quarters)
                    }
                }
            }

            // Upcoming earnings date
            upcomingEarning?.let { upcoming ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📅 Next Report",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDate(upcoming.date),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        upcoming.epsEstimate?.let { est ->
                            Text(
                                text = "Est. $${String.format("%.2f", est)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsQuartersTable(quarters: List<EarningsQuarter>) {
    // Column headers
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("Quarter" to 1.2f, "Actual" to 1f, "Estimate" to 1f, "Result" to 1f).forEach { (label, weight) ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(weight),
                textAlign = if (label == "Quarter") TextAlign.Start else if (label == "Result") TextAlign.End else TextAlign.Center
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    quarters.forEachIndexed { idx, quarter ->
        EarningsQuarterRow(quarter)
        if (idx < quarters.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun EarningsQuarterRow(quarter: EarningsQuarter) {
    val beat = when {
        quarter.epsActual == null || quarter.epsEstimate == null -> null
        quarter.epsActual > quarter.epsEstimate -> true
        quarter.epsActual < quarter.epsEstimate -> false
        else -> null
    }
    val exactly = quarter.epsActual != null && quarter.epsEstimate != null &&
            quarter.epsActual == quarter.epsEstimate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPeriodToQuarter(quarter.period),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = quarter.epsActual?.let { "$${String.format("%.2f", it)}" } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = when {
                beat == true -> GainGreen
                beat == false -> LossRed
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = quarter.epsEstimate?.let { "$${String.format("%.2f", it)}" } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            when {
                beat == true -> EarningsBadge("🟢 Beat", GainGreen)
                beat == false -> EarningsBadge("🔴 Miss", LossRed)
                exactly -> EarningsBadge("🟡 Met", Color(0xFFB8860B))
                else -> Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EarningsBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatPeriodToQuarter(period: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(period) ?: return period
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        val quarter = when (month) {
            in 1..3 -> "Q1"
            in 4..6 -> "Q2"
            in 7..9 -> "Q3"
            else -> "Q4"
        }
        "$quarter $year"
    } catch (e: Exception) {
        period
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val date = input.parse(dateStr) ?: return dateStr
        output.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
