package com.invest.easymoney.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.ui.charts.MPLineChart
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.ui.webview.WebViewBottomSheet
import com.invest.easymoney.util.Resource
import java.util.Locale

private enum class PremiumSection(val label: String) {
    OVERVIEW("Overview"),
    AI("AI Insight"),
    NEWS("News")
}

private enum class ValuationHealth(val label: String, val color: Color) {
    CHEAP("Cheap", GainGreen),
    FAIR("Fair", Color(0xFFB8860B)),
    EXPENSIVE("Expensive", LossRed),
    UNAVAILABLE("Unavailable", Color(0xFF6B7280))
}

private data class AnalystRatingUi(
    val consensus: String,
    val buyPercent: Int,
    val holdPercent: Int,
    val sellPercent: Int,
    val lowTarget: String,
    val averageTarget: String,
    val highTarget: String,
    val upsideText: String
)

private data class FinancialRatiosUi(
    val peRatio: String? = null,
    val pbRatio: String? = null,
    val eps: String? = null,
    val dividendYield: String? = null,
    val roe: String? = null,
    val debtToEquity: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedSection by remember { mutableStateOf(PremiumSection.OVERVIEW) }

    if (webViewUrl != null) {
        WebViewBottomSheet(url = webViewUrl!!, onDismiss = { webViewUrl = null })
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showAlertDialog) {
        SetPriceAlertDialog(
            onDismissDialog = { showAlertDialog = false },
            onConfirm = { pct, type ->
                viewModel.setAlert(pct, type)
                showAlertDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(viewModel.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                        Text(
                            "Premium Stock Detail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                        )
                    }
                },
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
                            tint = if (isInWatchlist) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showAlertDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Set Alert")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when (stockState) {
            is Resource.Loading -> PremiumShimmerLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            is Resource.Error -> PremiumErrorState(
                message = (stockState as Resource.Error).message,
                onRetry = { viewModel.loadDetail() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Success -> {
                val stock = (stockState as Resource.Success<Stock>).data
                val news = (newsState as? Resource.Success)?.data ?: emptyList()
                val prices = stock.intradayPrices.map { it.price }
                val timesList = stock.intradayPrices.mapNotNull { it.time.toLongOrNull() }
                val timesForChart = if (timesList.size == prices.size) timesList else null
                val analystRatingUi = buildAnalystRatingUi(stock)
                val financialRatiosUi = buildFinancialRatiosUi(stock)

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PremiumHeroCard(
                            stock = stock,
                            isInWatchlist = isInWatchlist,
                            onToggleWatchlist = { viewModel.toggleWatchlist() },
                            onSetAlert = { showAlertDialog = true },
                            onExplainMove = { viewModel.explainMove() }
                        )
                    }
                    item {
                        PremiumChartContainer(
                            prices = prices,
                            timesForChart = timesForChart,
                            insightState = insightState
                        )
                    }
                    stickyHeader {
                        StickySectionTabs(
                            selected = selectedSection,
                            onSelected = { selectedSection = it }
                        )
                    }
                    when (selectedSection) {
                        PremiumSection.OVERVIEW -> {
                            item { OverviewHighlightsRow(stock = stock) }
                            item { EnhancedMarketDataCard(stock) }
                            item { QuickStatsGrid(stock) }
                            item { AnalystRatingCard(data = analystRatingUi, currentPrice = stock.currentPrice) }
                            item { FinancialRatiosExpandableCard(data = financialRatiosUi) }
                            item { CompanyProfileCard(stock) }
                            if (news.isNotEmpty()) {
                                item { SectionHeader(title = "Top Headlines", subtitle = "Latest news and context around ${stock.symbol}") }
                                items(news.take(3), key = { it.id }) { newsItem ->
                                    PremiumNewsCard(news = newsItem, onOpenUrl = { if (newsItem.url.isNotBlank()) webViewUrl = newsItem.url })
                                }
                            }
                        }
                        PremiumSection.AI -> {
                            item { AiSentimentMeter(insightState = insightState) }
                            item { AiInsightCard(insightState = insightState, onRetry = { viewModel.loadDetail() }) }
                            item {
                                ExplainMoveCard(
                                    state = explainState,
                                    onExplain = { viewModel.explainMove() },
                                    onDismiss = { viewModel.dismissExplain() }
                                )
                            }
                        }
                        PremiumSection.NEWS -> {
                            if (news.isEmpty()) {
                                item { EmptyNewsState() }
                            } else {
                                item { SectionHeader(title = "Latest News", subtitle = "Tap any article to open it in the in-app browser") }
                                items(news, key = { it.id }) { newsItem ->
                                    PremiumNewsCard(news = newsItem, onOpenUrl = { if (newsItem.url.isNotBlank()) webViewUrl = newsItem.url })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumHeroCard(
    stock: Stock,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onSetAlert: () -> Unit,
    onExplainMove: () -> Unit
) {
    val isPositive = stock.changePercent >= 0
    val trendColor = if (isPositive) GainGreen else LossRed
    val arrowIcon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                    if (stock.name.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stock.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.PriceHero, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(arrowIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.change)} • ${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                            style = TradingTextStyles.PriceChange,
                            color = trendColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(shape = TradingShapes.Pill, color = trendColor.copy(alpha = 0.12f)) {
                    Text(
                        text = if (isPositive) "Bullish Day" else "Bearish Day",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = trendColor,
                        style = TradingTextStyles.Chip,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = onToggleWatchlist,
                    label = { Text(if (isInWatchlist) "In Watchlist" else "Add Watchlist") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = TradingShapes.Pill
                )
                AssistChip(
                    onClick = onSetAlert,
                    label = { Text("Price Alert") },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = TradingShapes.Pill
                )
                AssistChip(
                    onClick = onExplainMove,
                    label = { Text("Explain Move") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = TradingShapes.Pill
                )
            }
        }
    }
}

@Composable
private fun PremiumChartContainer(
    prices: List<Float>,
    timesForChart: List<Long>?,
    insightState: Resource<StockInsight>
) {
    val sentiment = (insightState as? Resource.Success)?.data?.sentiment?.lowercase()
    val sentimentText = when (sentiment) {
        "bullish" -> "AI sees bullish momentum"
        "bearish" -> "AI sees caution in trend"
        else -> "AI sentiment unavailable or neutral"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.ChartContainer,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Interactive Price Chart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scrub, inspect, and review stock movement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(
                        text = sentimentText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            MPLineChart(prices = prices, times = timesForChart, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StickySectionTabs(selected: PremiumSection, onSelected: (PremiumSection) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumSection.entries.forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(section.label, fontWeight = if (selected == section) FontWeight.SemiBold else FontWeight.Medium) },
                    shape = TradingShapes.Pill,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun OverviewHighlightsRow(stock: Stock) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HighlightTile("Trend", if (isPositive) "Upward" else "Downward", Modifier.weight(1f), valueColor = changeColor)
        HighlightTile("Exchange", stock.exchange.ifBlank { "N/A" }, Modifier.weight(1f))
        HighlightTile("Industry", stock.industry.ifBlank { "N/A" }, Modifier.weight(1f))
    }
}

@Composable
private fun HighlightTile(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EnhancedMarketDataCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Market Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            InfoRowModern("Open", "$${format(stock.openPrice)}")
            InfoRowModern("Prev Close", "$${format(stock.previousClose)}")
            InfoRowModern("Day High", "$${format(stock.highPrice)}", valueColor = GainGreen)
            InfoRowModern("Day Low", "$${format(stock.lowPrice)}", valueColor = LossRed)
            if (stock.marketCap > 0) InfoRowModern("Market Cap", formatMarketCap(stock.marketCap))
            if (stock.exchange.isNotBlank()) InfoRowModern("Exchange", stock.exchange)
            if (stock.industry.isNotBlank()) InfoRowModern("Industry", stock.industry)
        }
    }
}

@Composable
private fun QuickStatsGrid(stock: Stock) {
    val stats = buildList {
        add("Current" to "$${format(stock.currentPrice)}")
        add("Change" to "${if (stock.change >= 0) "+" else ""}${format(stock.change)}")
        add("Open" to "$${format(stock.openPrice)}")
        add("Previous Close" to "$${format(stock.previousClose)}")
        add("High" to "$${format(stock.highPrice)}")
        add("Low" to "$${format(stock.lowPrice)}")
        if (stock.marketCap > 0) add("Market Cap" to formatMarketCap(stock.marketCap))
        if (stock.exchange.isNotBlank()) add("Exchange" to stock.exchange)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            stats.chunked(2).forEachIndexed { index, row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value) ->
                        Surface(modifier = Modifier.weight(1f), shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                if (index != stats.chunked(2).lastIndex) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AnalystRatingCard(data: AnalystRatingUi?, currentPrice: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Analyst Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Consensus, target range, and recommendation mix", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (data == null) {
                Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Analyst data unavailable", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Connect analyst target and recommendation data from your backend or market API to unlock target range visuals and consensus breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DisabledTargetRangeMeter(currentPrice = currentPrice)
                    }
                }
            } else {
                val consensusColor = when (data.consensus.lowercase()) {
                    "buy", "strong buy", "bullish" -> GainGreen
                    "sell", "strong sell", "bearish" -> LossRed
                    else -> Color(0xFFB8860B)
                }
                Surface(shape = TradingShapes.StatTile, color = consensusColor.copy(alpha = 0.12f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Consensus", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(data.consensus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = consensusColor)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Target", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(data.averageTarget, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(data.upsideText, style = TradingTextStyles.Chip, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                AnalystTargetRangeMeter(currentPrice = currentPrice, lowTarget = data.lowTarget, averageTarget = data.averageTarget, highTarget = data.highTarget)
                SentimentBreakdownBar("Buy", data.buyPercent, GainGreen)
                SentimentBreakdownBar("Hold", data.holdPercent, Color(0xFFB8860B))
                SentimentBreakdownBar("Sell", data.sellPercent, LossRed)
            }
        }
    }
}

@Composable
private fun FinancialRatiosExpandableCard(data: FinancialRatiosUi?, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val valuationHealth = data?.let { computeValuationHealth(it.peRatio) } ?: ValuationHealth.UNAVAILABLE
    val ratioChips = remember(data) { buildRatioInterpretationChips(data) }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Financial Ratios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Valuation and profitability snapshot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (expanded) "Collapse" else "Expand")
                }
            }
            ValuationHealthBadge(valuationHealth)
            RatioInterpretationChipRow(chips = ratioChips)
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (data == null) {
                        Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Financial ratio data unavailable", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "P/E, P/B, EPS, dividend yield, ROE, and debt metrics will appear here after you connect them from your data source.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val items = listOf(
                            "P/E Ratio" to (data.peRatio ?: "N/A"),
                            "P/B Ratio" to (data.pbRatio ?: "N/A"),
                            "EPS" to (data.eps ?: "N/A"),
                            "Dividend Yield" to (data.dividendYield ?: "N/A"),
                            "ROE" to (data.roe ?: "N/A"),
                            "Debt / Equity" to (data.debtToEquity ?: "N/A")
                        )
                        items.chunked(2).forEachIndexed { idx, row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (label, value) -> RatioTile(label, value, Modifier.weight(1f)) }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            if (idx != items.chunked(2).lastIndex) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Text(
                        "These interpretation badges are quick heuristics for UI guidance only and should not be treated as financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ValuationHealthBadge(valuationHealth: ValuationHealth) {
    Surface(shape = TradingShapes.Pill, color = valuationHealth.color.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(valuationHealth.color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Valuation Health: ${valuationHealth.label}",
                style = TradingTextStyles.Chip,
                fontWeight = FontWeight.SemiBold,
                color = valuationHealth.color
            )
        }
    }
}

@Composable
private fun RatioInterpretationChipRow(chips: List<Pair<String, Color>>) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (label, color) ->
            Surface(shape = TradingShapes.Pill, color = color.copy(alpha = 0.12f)) {
                Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = TradingTextStyles.Chip, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnalystTargetRangeMeter(currentPrice: Double, lowTarget: String, averageTarget: String, highTarget: String) {
    val low = parseNumericValue(lowTarget)
    val average = parseNumericValue(averageTarget)
    val high = parseNumericValue(highTarget)
    if (low == null || average == null || high == null || high <= low) {
        DisabledTargetRangeMeter(currentPrice = currentPrice)
        return
    }
    val currentPosition = ((currentPrice - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()
    val averagePosition = ((average - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill))
                Box(modifier = Modifier.fillMaxWidth(averagePosition).height(14.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), TradingShapes.Pill))
                Box(modifier = Modifier.fillMaxWidth(currentPosition).height(14.dp).background(GainGreen.copy(alpha = 0.70f), TradingShapes.Pill))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MeterLabel("Low", lowTarget)
            MeterLabel("Avg", averageTarget)
            MeterLabel("High", highTarget)
        }
        Text("Current: $${String.format(Locale.US, "%.2f", currentPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DisabledTargetRangeMeter(currentPrice: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill))
        }
        Text(
            "Current: $${String.format(Locale.US, "%.2f", currentPrice)} • Connect analyst targets to activate the range meter",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MeterLabel(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SentimentBreakdownBar(label: String, percentage: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$percentage%", style = TradingTextStyles.Chip, fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                Box(modifier = Modifier.fillMaxWidth((percentage.coerceIn(0, 100)) / 100f).height(10.dp).background(color, TradingShapes.Pill))
            }
        }
    }
}

@Composable
private fun RatioTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompanyProfileCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Company Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileRow("Symbol", stock.symbol)
                    ProfileRow("Company", stock.name.ifBlank { "N/A" })
                    ProfileRow("Exchange", stock.exchange.ifBlank { "N/A" })
                    ProfileRow("Industry", stock.industry.ifBlank { "N/A" })
                    ProfileRow("Estimated Size", if (stock.marketCap > 0) formatMarketCap(stock.marketCap) else "N/A")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AiSentimentMeter(insightState: Resource<StockInsight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Sentiment Meter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            when (insightState) {
                is Resource.Success -> {
                    val sentiment = insightState.data.sentiment.lowercase()
                    val score = when (sentiment) {
                        "bullish" -> 0.82f
                        "bearish" -> 0.22f
                        else -> 0.50f
                    }
                    val meterColor = when (sentiment) {
                        "bullish" -> GainGreen
                        "bearish" -> LossRed
                        else -> Color(0xFFB8860B)
                    }
                    Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                            Box(modifier = Modifier.fillMaxWidth(score).height(12.dp).background(meterColor, TradingShapes.Pill))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bearish", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(insightState.data.sentiment, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold, color = meterColor)
                        Text("Bullish", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is Resource.Loading -> {
                    Text("Scanning signals and estimating sentiment...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is Resource.Error -> {
                    Text("Unable to load AI sentiment at the moment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(insightState: Resource<StockInsight>, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            when (insightState) {
                is Resource.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Analyzing market data...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                is Resource.Error -> {
                    Text("⚠️ ${insightState.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
                is Resource.Success -> {
                    val insight = insightState.data
                    SentimentBadge(insight.sentiment)
                    Text(insight.insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                    InsightOutlookRow("📅 Short-term", insight.shortTermOutlook)
                    InsightOutlookRow("📈 Long-term", insight.longTermOutlook)
                    Text(
                        "⚠️ For informational purposes only. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
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
    Surface(shape = TradingShapes.Pill, color = color.copy(alpha = 0.15f)) {
        Text(emoji, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = TradingTextStyles.Chip, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InsightOutlookRow(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = TradingTextStyles.StatLabel, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun InfoRowModern(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, TradingShapes.Input).padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun PremiumNewsCard(news: News, onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = { if (news.url.isNotBlank()) onOpenUrl(news.url) }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(news.headline, style = TradingTextStyles.NewsHeadline, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Text(news.source, style = TradingTextStyles.NewsMeta, color = MaterialTheme.colorScheme.primary)
                if (news.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(news.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (news.url.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open article", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp).size(18.dp).align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
private fun EmptyNewsState() {
    Card(modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No news available", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Latest headlines will appear here when available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumShimmerLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ShimmerCard(height = 180.dp, alpha = alpha) }
        item { ShimmerCard(height = 320.dp, alpha = alpha) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp, alpha = alpha)
                ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp, alpha = alpha)
            }
        }
        item { ShimmerCard(height = 220.dp, alpha = alpha) }
        item { ShimmerCard(height = 180.dp, alpha = alpha) }
    }
}

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier, height: Dp, alpha: Float) {
    Card(modifier = modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(modifier = Modifier.fillMaxWidth().height(height).alpha(alpha).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun PremiumErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(shape = TradingShapes.Dialog, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ $message", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry, shape = TradingShapes.Input) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetPriceAlertDialog(onDismissDialog: () -> Unit, onConfirm: (Float, AlertType) -> Unit) {
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
                    singleLine = true,
                    shape = TradingShapes.Input
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedType == AlertType.INCREASE, onClick = { selectedType = AlertType.INCREASE }, label = { Text("📈 Gain") }, shape = TradingShapes.Pill)
                    FilterChip(selected = selectedType == AlertType.DECREASE, onClick = { selectedType = AlertType.DECREASE }, label = { Text("📉 Drop") }, shape = TradingShapes.Pill)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { percentage.toFloatOrNull()?.let { pct -> onConfirm(pct, selectedType) } },
                enabled = percentage.toFloatOrNull() != null && (percentage.toFloatOrNull() ?: 0f) > 0f,
                shape = TradingShapes.Input
            ) { Text("Set Alert") }
        },
        dismissButton = { TextButton(onClick = onDismissDialog) { Text("Cancel") } },
        shape = TradingShapes.Dialog
    )
}

@Composable
private fun ExplainMoveCard(state: Resource<String>?, onExplain: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Why did it move?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                if (state != null && state !is Resource.Loading) {
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                }
            }
            when (state) {
                null -> {
                    Text(
                        "Use AI + retrieved context to explain the drivers behind the stock’s recent move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                    )
                    Button(onClick = onExplain, modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Input, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explain This Move")
                    }
                }
                is Resource.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Retrieving context and generating explanation...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is Resource.Error -> {
                    Text("⚠️ ${state.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onExplain, contentPadding = PaddingValues(0.dp)) { Text("Retry", style = MaterialTheme.typography.labelSmall) }
                }
                is Resource.Success -> {
                    Text(state.data, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "⚠️ AI-generated. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

private fun buildAnalystRatingUi(stock: Stock): AnalystRatingUi? = null
private fun buildFinancialRatiosUi(stock: Stock): FinancialRatiosUi? = null

private fun computeValuationHealth(peRatio: String?): ValuationHealth {
    val pe = parseNumericValue(peRatio)
    return when {
        pe == null -> ValuationHealth.UNAVAILABLE
        pe <= 15.0 -> ValuationHealth.CHEAP
        pe <= 28.0 -> ValuationHealth.FAIR
        else -> ValuationHealth.EXPENSIVE
    }
}

private fun buildRatioInterpretationChips(data: FinancialRatiosUi?): List<Pair<String, Color>> {
    if (data == null) return listOf("Ratios unavailable" to Color(0xFF6B7280))
    val chips = mutableListOf<Pair<String, Color>>()
    val pe = parseNumericValue(data.peRatio)
    chips += when {
        pe == null -> "P/E unavailable" to Color(0xFF6B7280)
        pe <= 15.0 -> "P/E looks value-oriented" to GainGreen
        pe <= 28.0 -> "P/E in fair zone" to Color(0xFFB8860B)
        else -> "P/E looks rich" to LossRed
    }
    val pb = parseNumericValue(data.pbRatio)
    chips += when {
        pb == null -> "P/B unavailable" to Color(0xFF6B7280)
        pb <= 3.0 -> "P/B conservative" to GainGreen
        pb <= 8.0 -> "P/B moderate" to Color(0xFFB8860B)
        else -> "P/B elevated" to LossRed
    }
    val dividend = parseNumericValue(data.dividendYield)
    chips += when {
        dividend == null -> "Yield unavailable" to Color(0xFF6B7280)
        dividend >= 3.0 -> "Yield income-friendly" to GainGreen
        dividend >= 1.0 -> "Yield modest" to Color(0xFFB8860B)
        else -> "Yield low" to LossRed
    }
    val roe = parseNumericValue(data.roe)
    chips += when {
        roe == null -> "ROE unavailable" to Color(0xFF6B7280)
        roe >= 15.0 -> "ROE strong" to GainGreen
        roe >= 8.0 -> "ROE stable" to Color(0xFFB8860B)
        else -> "ROE weak" to LossRed
    }
    val debt = parseNumericValue(data.debtToEquity)
    chips += when {
        debt == null -> "Debt unavailable" to Color(0xFF6B7280)
        debt <= 0.5 -> "Debt light" to GainGreen
        debt <= 1.5 -> "Debt manageable" to Color(0xFFB8860B)
        else -> "Debt heavy" to LossRed
    }
    return chips
}

private fun parseNumericValue(value: String?): Double? {
    if (value.isNullOrBlank()) return null
    return value.replace("$", "").replace(",", "").replace("%", "").trim().toDoubleOrNull()
}

private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun formatMarketCap(cap: Double): String = when {
    cap >= 1_000_000 -> "$${String.format(Locale.US, "%.1f", cap / 1_000_000)}T"
    cap >= 1_000 -> "$${String.format(Locale.US, "%.1f", cap / 1_000)}B"
    else -> "$${String.format(Locale.US, "%.1f", cap)}M"
}
