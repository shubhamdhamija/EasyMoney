package com.invest.easymoney.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val stockState by viewModel.stockState.collectAsStateWithLifecycle()
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val insightState by viewModel.insightState.collectAsStateWithLifecycle()
    val isInWatchlist by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAlertDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showAlertDialog) {
        AlertDialog(
            onDismissDialog = { showAlertDialog = false },
            onConfirm = { pct, type ->
                viewModel.setAlert(pct, type)
                showAlertDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(viewModel.symbol, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showAlertDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Set Alert")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when (stockState) {
            is Resource.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is Resource.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ ${(stockState as Resource.Error).message}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadDetail() }) { Text("Retry") }
                }
            }

            is Resource.Success -> {
                val stock = (stockState as Resource.Success<Stock>).data
                val news = (newsState as? Resource.Success)?.data ?: emptyList()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { StockPriceCard(stock) }
                    item { StockInfoCard(stock) }
                    item { AiInsightCard(insightState) { viewModel.loadDetail() } }
                    if (news.isNotEmpty()) {
                        item {
                            Text(
                                "Latest News",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(news, key = { it.id }) { NewsCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(
    insightState: Resource<StockInsight>,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI Insight",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            when (insightState) {
                is Resource.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Analyzing market data…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                is Resource.Error -> {
                    Text(
                        "⚠️ ${insightState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }

                is Resource.Success -> {
                    val insight = insightState.data
                    SentimentBadge(insight.sentiment)
                    Text(
                        insight.insight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                    InsightOutlookRow("📅 Short-term", insight.shortTermOutlook)
                    InsightOutlookRow("📈 Long-term", insight.longTermOutlook)
                    Text(
                        "⚠️ For informational purposes only. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SentimentBadge(sentiment: String) {
    val (emoji, color) = when (sentiment.lowercase()) {
        "bullish" -> "🟢 Bullish" to GainGreen
        "bearish" -> "🔴 Bearish" to LossRed
        else -> "🟡 Neutral" to Color(0xFFB8860B)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InsightOutlookRow(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun StockPriceCard(stock: Stock) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (stock.name.isNotEmpty()) {
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$${String.format("%.2f", stock.currentPrice)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = changeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.change)} " +
                                "(${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StockInfoCard(stock: Stock) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Market Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            InfoRow("Open", "$${String.format("%.2f", stock.openPrice)}")
            InfoRow("Prev Close", "$${String.format("%.2f", stock.previousClose)}")
            InfoRow("Day High", "$${String.format("%.2f", stock.highPrice)}")
            InfoRow("Day Low", "$${String.format("%.2f", stock.lowPrice)}")
            if (stock.marketCap > 0) {
                InfoRow("Market Cap", formatMarketCap(stock.marketCap))
            }
            if (stock.exchange.isNotEmpty()) InfoRow("Exchange", stock.exchange)
            if (stock.industry.isNotEmpty()) InfoRow("Industry", stock.industry)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NewsCard(news: News) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = news.headline,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = news.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (news.summary.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = news.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun AlertDialog(
    onDismissDialog: () -> Unit,
    onConfirm: (Float, AlertType) -> Unit
) {
    var percentage by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AlertType.INCREASE) }

    AlertDialog(
        onDismissRequest = onDismissDialog,
        title = { Text("Set Price Alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = percentage,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) percentage = it },
                    label = { Text("Percentage (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == AlertType.INCREASE,
                        onClick = { selectedType = AlertType.INCREASE },
                        label = { Text("📈 Gain") }
                    )
                    FilterChip(
                        selected = selectedType == AlertType.DECREASE,
                        onClick = { selectedType = AlertType.DECREASE },
                        label = { Text("📉 Drop") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    percentage.toFloatOrNull()?.let { pct -> onConfirm(pct, selectedType) }
                },
                enabled = percentage.toFloatOrNull() != null && percentage.toFloat() > 0
            ) { Text("Set Alert") }
        },
        dismissButton = {
            TextButton(onClick = onDismissDialog) { Text("Cancel") }
        }
    )
}

private fun formatMarketCap(cap: Double): String = when {
    cap >= 1_000_000 -> "$${String.format("%.1f", cap / 1_000_000)}T"
    cap >= 1_000 -> "$${String.format("%.1f", cap / 1_000)}B"
    else -> "$${String.format("%.1f", cap)}M"
}

