package com.invest.easymoney.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.ui.charts.IntradayLineChart
import com.invest.easymoney.ui.charts.MPLineChart
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.webview.WebViewBottomSheet
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
    val explainState by viewModel.explainState.collectAsStateWithLifecycle()
    val isInWatchlist by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAlertDialog by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }

    // Show WebView bottom sheet when a news link is tapped
    webViewUrl?.let { url ->
        WebViewBottomSheet(url = url, onDismiss = { webViewUrl = null })
    }

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
                    // Prepare prices and times for richer chart labels. If times cannot be parsed or lengths mismatch,
                    // pass null so chart will skip time labels.
                    val prices = stock.intradayPrices.map { it.price }
                    val timesList = stock.intradayPrices.mapNotNull { it.time.toLongOrNull() }
                    val timesForChart = if (timesList.size == prices.size) timesList else null
                    // Use MPAndroidChart for richer interactions; fallback to Compose chart if needed
                    item { MPLineChart(prices, times = timesForChart, modifier = Modifier) }
                    item { StockPriceCard(stock) }
                    item { StockInfoCard(stock) }
                    item { AiInsightCard(insightState) { viewModel.loadDetail() } }
                    item { ExplainMoveCard(explainState, onExplain = { viewModel.explainMove() }, onDismiss = { viewModel.dismissExplain() }) }
                    if (news.isNotEmpty()) {
                        item {
                            Text(
                                "Latest News",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(news, key = { it.id }) { newsItem ->
                            NewsCard(newsItem, onOpenUrl = { webViewUrl = newsItem.url })
                        }
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
    val arrowIcon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Stock name
            if (stock.name.isNotEmpty()) {
                Text(
                    text = stock.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price
            Text(
                text = "$${String.format("%.2f", stock.currentPrice)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                // Change Chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = changeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = arrowIcon,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                            color = changeColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Absolute change text (cleaner separation)
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.change)}",
                    color = changeColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )
        }
    }
}

@Composable
private fun StockInfoCard(stock: Stock) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Market Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // Grid Layout (2 columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                InfoRowModern("Open", "$${format(stock.openPrice)}")
                InfoRowModern("Prev Close", "$${format(stock.previousClose)}")

                InfoRowModern(
                    "Day High",
                    "$${format(stock.highPrice)}",
                    valueColor = GainGreen
                )

                InfoRowModern(
                    "Day Low",
                    "$${format(stock.lowPrice)}",
                    valueColor = LossRed
                )

                if (stock.marketCap > 0) {
                    InfoRowModern("Market Cap", formatMarketCap(stock.marketCap))
                }

                if (stock.exchange.isNotEmpty()) {
                    InfoRowModern("Exchange", stock.exchange)
                }

                if (stock.industry.isNotEmpty()) {
                    InfoRowModern("Industry", stock.industry)
                }
            }
        }
    }
}

@Composable
private fun InfoRowModern(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// helper
private fun format(value: Double): String = String.format("%.2f", value)


@Composable
private fun NewsCard(news: News, onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = { if (news.url.isNotBlank()) onOpenUrl(news.url) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            if (news.url.isNotBlank()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open article",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                        .align(Alignment.CenterVertically)
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

@Composable
private fun ExplainMoveCard(
    state: Resource<String>?,
    onExplain: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Why did it move?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (state != null && state !is Resource.Loading) {
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            when (state) {
                null -> {
                    Text(
                        "Use RAG to explain the drivers behind this stock's recent movement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                    )
                    Button(
                        onClick = onExplain,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Explain This Move")
                    }
                }
                is Resource.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Retrieving context + generating explanation…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is Resource.Error -> {
                    Text("⚠️ ${state.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onExplain, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
                is Resource.Success -> {
                    Text(
                        state.data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "⚠️ AI-generated. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
