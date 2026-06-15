package com.invest.easymoney.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.util.Resource
import kotlin.math.absoluteValue

private enum class HomeTab(val title: String) {
    GAINERS("Gainers"),
    LOSERS("Losers"),
    TRENDING("Trending")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.topStocksState.collectAsStateWithLifecycle()
    val gainers by viewModel.gainers.collectAsStateWithLifecycle()
    val losers by viewModel.losers.collectAsStateWithLifecycle()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val aiPicksState by viewModel.aiPicksState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.GAINERS) }

    if (aiPicksState != null) {
        AiPicksBottomSheetUltra(
            aiPicksState = aiPicksState!!,
            onDismiss = { viewModel.dismissAiPicks() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeSearchFirstTopBar(
                isRefreshing = state is Resource.Loading,
                onRefresh = { viewModel.loadStocks() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (state) {
            is Resource.Loading -> SearchFirstLoadingState(
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Error -> SearchFirstErrorState(
                message = (state as Resource.Error).message,
                onRetry = { viewModel.loadStocks() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Success -> {
                val visibleStocks = when (selectedTab) {
                    HomeTab.GAINERS -> gainers
                    HomeTab.LOSERS -> losers
                    HomeTab.TRENDING -> trending
                }
                val marketPool = remember(gainers, losers, trending) {
                    (gainers + losers + trending).distinctBy { it.symbol }
                }
                val recommendedStocks = remember(marketPool) { marketPool.take(10) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SearchHeaderCard(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { viewModel.searchSymbols() }
                        )
                    }

                    when (searchState) {
                        is Resource.Loading -> item { InlineLoadingCard(text = "Searching symbols...") }
                        is Resource.Error -> item {
                            InlineErrorCard(
                                message = (searchState as Resource.Error).message ?: "Search error"
                            )
                        }
                        is Resource.Success -> {
                            val results = (searchState as Resource.Success).data
                            if (results.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Search Results",
                                        subtitle = "Tap a stock to open details"
                                    )
                                }
                                items(results, key = { it.symbol }) { result ->
                                    SearchResultCard(
                                        symbol = result.symbol,
                                        name = result.name,
                                        exchange = result.exchange,
                                        onClick = { onStockClick(result.symbol) }
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }

                    item {
                        MarketPulseHero(
                            gainers = gainers,
                            losers = losers,
                            trending = trending,
                            isAiLoading = aiPicksState is Resource.Loading,
                            onAiClick = { viewModel.loadAiPicks() }
                        )
                    }

                    if (marketPool.isNotEmpty()) {
                        item {
                            LiveTickerStrip(
                                stocks = marketPool.take(10),
                                onStockClick = onStockClick
                            )
                        }
                    }

                    stickyHeader {
                        StickyMarketTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Top Movers",
                            subtitle = "Quick-glance movers in the selected category"
                        )
                    }

                    item {
                        TopMoverChips(
                            stocks = visibleStocks,
                            onStockClick = onStockClick
                        )
                    }

                    item {
                        SectionHeader(
                            title = selectedTab.title,
                            subtitle = when (selectedTab) {
                                HomeTab.GAINERS -> "Stocks moving strongly upward today"
                                HomeTab.LOSERS -> "Stocks under selling pressure today"
                                HomeTab.TRENDING -> "Names drawing the most market attention"
                            }
                        )
                    }

                    item {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "home-tab-animated-content"
                        ) { tab ->
                            val tabStocks = when (tab) {
                                HomeTab.GAINERS -> gainers
                                HomeTab.LOSERS -> losers
                                HomeTab.TRENDING -> trending
                            }
                            if (tabStocks.isEmpty()) {
                                EmptyStocksState(message = "No data available")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    tabStocks.forEach { stock ->
                                        ModernStockCard(stock = stock, onClick = { onStockClick(stock.symbol) })
                                    }
                                }
                            }
                        }
                    }

                    if (recommendedStocks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "For You",
                                subtitle = "Names you may want to explore next"
                            )
                        }
                        item {
                            ForYouCarousel(
                                stocks = recommendedStocks,
                                onStockClick = onStockClick
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchFirstTopBar(isRefreshing: Boolean, onRefresh: () -> Unit) {
    LargeTopAppBar(
        title = {
            Column {
                Text("EasyMoney", style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                Text(
                    "Search-first investing dashboard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        },
        actions = {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 6.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun SearchHeaderCard(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Search the market", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Find stocks by symbol or company name first, then explore movers and ideas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search by symbol or company") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = TradingShapes.Input
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.height(56.dp),
                    shape = TradingShapes.Input,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Search", style = TradingTextStyles.Chip)
                }
            }
        }
    }
}

@Composable
private fun MarketPulseHero(
    gainers: List<Stock>,
    losers: List<Stock>,
    trending: List<Stock>,
    isAiLoading: Boolean,
    onAiClick: () -> Unit
) {
    val bullishCount = gainers.size
    val bearishCount = losers.size
    val totalBreadth = (bullishCount + bearishCount).coerceAtLeast(1)
    val bullishRatio = bullishCount.toFloat() / totalBreadth.toFloat()
    val topGainer = gainers.maxByOrNull { it.changePercent }
    val topLoser = losers.minByOrNull { it.changePercent }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isAiLoading, onClick = onAiClick),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Surface(shape = TradingShapes.Card, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAiLoading) "Analyzing market..." else "AI Picks Today",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAiLoading) {
                                    "Scanning leaders, laggards, and momentum names"
                                } else {
                                    "Get AI-generated ideas with quick reasoning and market context"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.84f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Market Breadth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(14.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(bullishRatio).height(14.dp)
                                .background(GainGreen, TradingShapes.Pill)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bullish: $bullishCount", style = MaterialTheme.typography.labelLarge, color = GainGreen, fontWeight = FontWeight.SemiBold)
                    Text("Bearish: $bearishCount", style = MaterialTheme.typography.labelLarge, color = LossRed, fontWeight = FontWeight.SemiBold)
                    Text("Trending: ${trending.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MarketSnapshotCard(
                        title = "Top Gainer",
                        symbol = topGainer?.symbol ?: "--",
                        movement = topGainer?.let { "+${String.format("%.2f", it.changePercent)}%" } ?: "--",
                        color = GainGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MarketSnapshotCard(
                        title = "Top Loser",
                        symbol = topLoser?.symbol ?: "--",
                        movement = topLoser?.let { "${String.format("%.2f", it.changePercent)}%" } ?: "--",
                        color = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketSnapshotCard(title: String, symbol: String, movement: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = color.copy(alpha = 0.10f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(movement, style = TradingTextStyles.PriceChange, color = color)
        }
    }
}

@Composable
private fun LiveTickerStrip(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    if (stocks.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stocks.forEach { stock ->
            val isPositive = stock.changePercent >= 0
            val accent = if (isPositive) GainGreen else LossRed
            Surface(
                modifier = Modifier.clickable { onStockClick(stock.symbol) },
                shape = TradingShapes.Pill,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.symbol, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                        style = TradingTextStyles.Chip,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyMarketTabs(selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    label = { Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Medium) },
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
private fun TopMoverChips(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    val compact = stocks.take(6)
    if (compact.isEmpty()) {
        EmptyStocksState(message = "No movers available")
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        compact.forEach { stock ->
            val isPositive = stock.changePercent >= 0
            val chipColor = if (isPositive) GainGreen else LossRed
            Surface(
                modifier = Modifier.clickable { onStockClick(stock.symbol) },
                shape = TradingShapes.Pill,
                color = chipColor.copy(alpha = 0.12f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.symbol, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold, color = chipColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                        style = TradingTextStyles.Chip,
                        color = chipColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchResultCard(symbol: String, name: String, exchange: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                if (name.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (exchange.isNotBlank()) {
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                    Text(exchange, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ModernStockCard(stock: Stock, onClick: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed
    val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val movementText = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(58.dp).background(
                            color = if (isPositive) GainGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f),
                            shape = TradingShapes.StatTile
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(trendIcon, contentDescription = null, tint = changeColor, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                        if (stock.name.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stock.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (stock.exchange.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                                Text(stock.exchange, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.PriceHero, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = TradingShapes.Pill, color = changeColor.copy(alpha = 0.12f)) {
                        Text(movementText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = TradingTextStyles.Chip, color = changeColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            MiniMomentumBar(value = stock.changePercent, positiveColor = GainGreen, negativeColor = LossRed)
        }
    }
}

@Composable
private fun MiniMomentumBar(value: Double, positiveColor: Color, negativeColor: Color) {
    val positive = value >= 0
    val color = if (positive) positiveColor else negativeColor
    val normalized = (value.absoluteValue / 10.0).coerceIn(0.08, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Momentum", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(normalized).height(10.dp).background(color, TradingShapes.Pill)
                )
            }
        }
    }
}

@Composable
private fun ForYouCarousel(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stocks, key = { it.symbol }) { stock ->
            ForYouCard(stock = stock, onClick = { onStockClick(stock.symbol) })
        }
    }
}

@Composable
private fun ForYouCard(stock: Stock, onClick: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val accent = if (isPositive) GainGreen else LossRed
    Card(
        modifier = Modifier.width(195.dp).clickable(onClick = onClick),
        shape = TradingShapes.CarouselCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = TradingShapes.StatTile, color = accent.copy(alpha = 0.12f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
            if (stock.name.isNotBlank()) {
                Text(stock.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
            Surface(shape = TradingShapes.Pill, color = accent.copy(alpha = 0.12f)) {
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = TradingTextStyles.Chip,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPicksBottomSheetUltra(aiPicksState: Resource<List<AiPick>>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Surface(shape = TradingShapes.BottomSheet, color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("AI Picks Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                when (aiPicksState) {
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator()
                                Text("Analyzing market data...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is Resource.Error -> InlineErrorCard(message = "⚠️ ${aiPicksState.message}")
                    is Resource.Success -> {
                        if (aiPicksState.data.isEmpty()) {
                            Text("No picks available right now.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            aiPicksState.data.forEachIndexed { index, pick ->
                                AiPickPremiumCard(rank = index + 1, pick = pick)
                            }
                        }
                        Text(
                            "⚠️ For informational purposes only. Not financial advice.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPickPremiumCard(rank: Int, pick: AiPick) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.tertiary) {
                Text(
                    text = "#$rank",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = TradingTextStyles.Chip,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pick.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                Text(pick.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun InlineLoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InlineErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun EmptyStocksState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchFirstLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home-loading-alpha"
    )
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(height = 120.dp, alpha = alpha) }
        item { ShimmerBlock(height = 250.dp, alpha = alpha) }
        item { ShimmerBlock(height = 56.dp, alpha = alpha) }
        item { ShimmerBlock(height = 54.dp, alpha = alpha) }
        item { ShimmerBlock(height = 70.dp, alpha = alpha) }
        items(5) { ShimmerBlock(height = 110.dp, alpha = alpha) }
        item { ShimmerBlock(height = 180.dp, alpha = alpha) }
    }
}

@Composable
private fun ShimmerBlock(height: Dp, alpha: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(height).alpha(alpha)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun SearchFirstErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            shape = TradingShapes.Dialog,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ $message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry, shape = TradingShapes.Input) { Text("Retry") }
            }
        }
    }
}
