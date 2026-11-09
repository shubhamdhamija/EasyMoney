package com.invest.easymoney.ui.congress

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.CongressFiling
import com.invest.easymoney.domain.model.CongressFilingDetail
import com.invest.easymoney.domain.model.CongressTrade
import com.invest.easymoney.ui.components.CompactTopBar
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.util.Resource

const val CONGRESS_DISCLAIMER =
    "Public disclosure under the STOCK Act, filed up to 45 days after the trade. " +
        "Amounts are ranges. Not inside information. For informational purposes only. Not financial advice."

@Composable
fun CongressScreen(
    onBack: () -> Unit,
    onStockClick: (String) -> Unit,
    viewModel: CongressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            CompactTopBar(
                title = "Congress Trades",
                modifier = Modifier.testTag("CongressTopBar"),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("CongressScreen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "House members' stock trades, as filed with the House Clerk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.member,
                        onValueChange = viewModel::onMemberChange,
                        modifier = Modifier.fillMaxWidth().testTag("CongressMemberInput"),
                        label = { Text("Member last name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                        shape = TradingShapes.Input
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::search,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("CongressSearchButton"),
                            shape = TradingShapes.Input
                        ) { Text("Search filings") }
                        OutlinedButton(
                            onClick = viewModel::showAllMembers,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("CongressAllMembersButton"),
                            shape = TradingShapes.Input
                        ) { Text("All members") }
                    }
                }
            }

            when (val filings = state.filings) {
                is Resource.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Loading filings...")
                    }
                }

                is Resource.Error -> item {
                    MessageCard(message = filings.message, isError = true, actionLabel = "Retry", onAction = viewModel::search)
                }

                is Resource.Success -> {
                    if (filings.data.isEmpty()) {
                        item { MessageCard(message = "No trade filings found this year for that member.", isError = false) }
                    } else {
                        items(filings.data, key = { it.docId }) { filing ->
                            FilingCard(
                                filing = filing,
                                expanded = state.expandedDocId == filing.docId,
                                detail = state.details[filing.docId],
                                watchlist = state.watchlist,
                                onToggle = { viewModel.toggle(filing) },
                                onRetry = { viewModel.retryDetail(filing) },
                                onStockClick = onStockClick
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("CongressSenateNote"),
                    shape = TradingShapes.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Senate trades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "This list covers the House only. Senate reports are on efdsearch.senate.gov, " +
                                "which asks you to accept a usage agreement yourself before searching.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { uriHandler.openUri("https://efdsearch.senate.gov/search/") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Open Senate search") }
                    }
                }
            }

            item {
                Text(
                    CONGRESS_DISCLAIMER,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("CongressDisclaimer")
                )
            }
        }
    }
}

@Composable
private fun FilingCard(
    filing: CongressFiling,
    expanded: Boolean,
    detail: Resource<CongressFilingDetail>?,
    watchlist: Set<String>,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onStockClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().testTag("CongressFiling_${filing.docId}"),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(filing.name.ifBlank { "House member" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (isRecent(filing.filed)) {
                            Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    "NEW",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        "${filing.district}  •  Filed ${filing.filed ?: "unknown date"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (expanded) "Hide" else "View trades",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                when (detail) {
                    null, Resource.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Reading filing...", style = MaterialTheme.typography.bodySmall)
                    }

                    is Resource.Error -> MessageCard(detail.message, isError = true, actionLabel = "Retry", onAction = onRetry)

                    is Resource.Success -> FilingDetail(filing, detail.data, watchlist, onStockClick)
                }
            }
        }
    }
}

@Composable
private fun FilingDetail(
    filing: CongressFiling,
    detail: CongressFilingDetail,
    watchlist: Set<String>,
    onStockClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (detail.available) {
            detail.trades.forEach { trade -> TradeRow(trade, trade.ticker?.uppercase() in watchlist, onStockClick) }
        } else {
            Text(
                "Trade details could not be read automatically for this filing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val pdf = detail.pdfUrl ?: filing.pdfUrl
        if (pdf != null) {
            OutlinedButton(
                onClick = { uriHandler.openUri(pdf) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("CongressOpenPdf_${filing.docId}")
            ) { Text("Open original PDF") }
        }
    }
}

@Composable
private fun TradeRow(trade: CongressTrade, onWatchlist: Boolean, onStockClick: (String) -> Unit) {
    val accent = if (trade.isPurchase) GainGreen else LossRed
    Surface(
        shape = TradingShapes.StatTile,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .let { m -> if (trade.ticker != null) m.clickable { onStockClick(trade.ticker) } else m }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = TradingShapes.Pill, color = accent.copy(alpha = 0.12f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (trade.isPurchase) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "  ${trade.typeLabel.replaceFirstChar { it.uppercase() }}",
                            style = TradingTextStyles.Chip,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    trade.ticker ?: "—",
                    style = TradingTextStyles.Ticker,
                    fontWeight = FontWeight.Bold
                )
                if (onWatchlist) {
                    Text(
                        "On your watchlist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(trade.asset, style = MaterialTheme.typography.bodyMedium)
            val detailLine = trade.optionSummary
                ?: trade.shares?.let { "%,d shares".format(it) }
            if (detailLine != null) {
                Text(detailLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                trade.amount,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            val disclosed = trade.daysToDisclose?.let { ", disclosed $it days later" }.orEmpty()
            Text(
                "By ${trade.owner}  •  Traded ${trade.tradeDate ?: "unknown"}$disclosed" +
                    if (trade.late) "  •  LATE (over 45 days)" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (trade.late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isError) Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
            }
        }
    }
}


/** A filing posted in the last 7 days. Uses SimpleDateFormat so it works on API 24 without desugaring. */
internal fun isRecent(filed: String?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (filed.isNullOrBlank()) return false
    val parsed = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }.parse(filed)
    } catch (e: java.text.ParseException) {
        null
    } ?: return false
    val ageDays = (nowMillis - parsed.time) / (24L * 60 * 60 * 1000)
    return ageDays in 0..7
}
