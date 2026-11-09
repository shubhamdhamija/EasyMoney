package com.invest.easymoney.ui.aihub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import com.invest.easymoney.domain.model.StockSearchResult
import com.invest.easymoney.ui.agent.AgentResearchSection
import com.invest.easymoney.ui.agent.AgentResearchUiState
import com.invest.easymoney.ui.agent.AgentResearchViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.ui.components.CompactTopBar
import com.invest.easymoney.ui.home.AiPicksState
import com.invest.easymoney.ui.home.HomeIntent
import com.invest.easymoney.ui.home.HomeViewModel
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.util.Resource

/** Central place for AI features: daily AI picks plus guidance on per-stock research. */
@Composable
fun AiHubScreen(
    onStockClick: (String) -> Unit = {},
    initialSymbol: String? = null,
    onOpenCongress: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    researchViewModel: AgentResearchViewModel = hiltViewModel()
) {
    val researchState by researchViewModel.state.collectAsStateWithLifecycle()
    var handledInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialSymbol) {
        if (!handledInitial && !initialSymbol.isNullOrBlank()) {
            handledInitial = true
            if (researchState is AgentResearchUiState.Idle) {
                researchViewModel.startAgentResearch(initialSymbol.trim().uppercase())
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stocksState = state.stocks
    val picksState = state.aiPicks

    // Generate picks automatically the first time stock data is available.
    LaunchedEffect(stocksState is Resource.Success) {
        if (stocksState is Resource.Success && picksState == AiPicksState.Idle) {
            viewModel.onIntent(HomeIntent.LoadAiPicks)
        }
    }

    Scaffold(
        topBar = { CompactTopBar(title = "AI Hub", modifier = Modifier.testTag("AiHubTopBar")) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("AiHubScreen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ResearchSearchCard(
                    query = state.search.query,
                    onQueryChange = { viewModel.onIntent(HomeIntent.SearchChanged(it)) },
                    results = state.search.results.take(5),
                    isSearching = state.search.isLoading,
                    onResearch = { symbol ->
                        viewModel.onIntent(HomeIntent.SearchChanged(""))
                        researchViewModel.startAgentResearch(symbol)
                    }
                )
            }
            if (researchState !is AgentResearchUiState.Idle) {
                item(key = "agent_research") {
                    AgentResearchSection(
                        state = researchState,
                        onCancel = { researchViewModel.cancelAgentResearch() },
                        onRetry = { researchViewModel.retryAgentResearch() },
                        onClearError = { researchViewModel.clearAgentError() },
                        onDismiss = { researchViewModel.dismissAgentResearch() },
                        onApprove = { researchViewModel.approvePendingAction() },
                        onReject = { researchViewModel.rejectPendingAction() }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "AI Picks Today",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("AiHubTitle")
                    )
                    Text(
                        "AI-generated ideas based on today's market movers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("AiHubDescription")
                    )
                }
            }

            when {
                stocksState is Resource.Loading || picksState is AiPicksState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Analyzing market data...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                stocksState is Resource.Error -> item {
                    AiHubMessageCard(
                        message = stocksState.message,
                        isError = true,
                        onRetry = { viewModel.onIntent(HomeIntent.LoadStocks) }
                    )
                }

                picksState is AiPicksState.Error -> item {
                    AiHubMessageCard(
                        message = picksState.message,
                        isError = true,
                        onRetry = { viewModel.onIntent(HomeIntent.LoadAiPicks) }
                    )
                }

                picksState is AiPicksState.Success -> {
                    if (picksState.picks.isEmpty()) {
                        item { AiHubMessageCard(message = "No picks available right now.", isError = false) }
                    } else {
                        itemsIndexed(picksState.picks, key = { _, pick -> pick.symbol }) { index, pick ->
                            AiPickCard(rank = index + 1, pick = pick, onClick = { onStockClick(pick.symbol) })
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                viewModel.onIntent(HomeIntent.DismissAiPicks)
                                viewModel.onIntent(HomeIntent.LoadAiPicks)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("AiHubRefreshPicks"),
                            shape = TradingShapes.Input
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Refresh picks")
                        }
                    }
                }

                else -> Unit
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCongress).testTag("AiHubCongressCard"),
                    shape = TradingShapes.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Congress Trades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "See stock trades House members disclosed (public STOCK Act filings).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    "For informational purposes only. Not financial advice.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("AiHubDisclaimer")
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AiPickCard(rank: Int, pick: AiPick, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("AiPickCard_${pick.symbol}"),
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
private fun AiHubMessageCard(message: String, isError: Boolean, onRetry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isError) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            if (onRetry != null) {
                Button(onClick = onRetry, shape = TradingShapes.Input, modifier = Modifier.testTag("AiHubRetry")) { Text("Retry") }
            }
        }
    }
}


@Composable
private fun ResearchSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<StockSearchResult>,
    isSearching: Boolean,
    onResearch: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("AiHubResearchCard"),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Research a stock with AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().testTag("AiHubSearchInput"),
                placeholder = { Text("Symbol or company, e.g. ORCL") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = TradingShapes.Input
            )
            if (isSearching) {
                Text("Searching...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            results.forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onResearch(result.symbol) }
                        .testTag("AiHubResult_${result.symbol}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(result.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                    Text(
                        result.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Research", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
